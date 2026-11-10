package com.quanlynhakhoa.nghiepvu.hoadon.service;

import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import com.quanlynhakhoa.model.hoadon.PhanThanhToan;
import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.model.hoadon.ThongTinThanhToan;
import com.quanlynhakhoa.model.hoadon.YeuCauThanhToan;
import java.io.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Local recovery journal, scoped to the configured database; never stores patient names or credentials. */
public final class PendingPaymentStore {
    private final Path directory;
    public PendingPaymentStore(Path directory) { this.directory = directory.toAbsolutePath().normalize(); }

    public static PendingPaymentStore local() {
        try {
            Properties config = new Properties();
            Path configPath = TeamDevConnection.configPath();
            if (Files.isRegularFile(configPath)) {
                try (Reader reader = Files.newBufferedReader(configPath)) { config.load(reader); }
            }
            String scope = config.getProperty("dev.database", configPath.toString()) + ":" + config.getProperty("dev.port", "");
            String key = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(scope.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            return new PendingPaymentStore(Path.of(System.getProperty("user.home"), ".n08-dental", "pending-payments", key));
        } catch (Exception failure) { throw new IllegalStateException("Không mở được nhật ký đối soát thanh toán.", failure); }
    }

    private Path path(String invoice) {
        if (invoice == null || invoice.isBlank() || invoice.length() > 10) throw new IllegalArgumentException("Mã hóa đơn không hợp lệ.");
        // Encoding keeps identifiers from ever becoming filesystem path segments.
        return directory.resolve(HexFormat.of().formatHex(invoice.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + ".properties");
    }

    private static final Object MONITOR = new Object();
    private interface Operation<T> { T run() throws IOException; }
    private <T> T locked(String invoice,Operation<T> operation) throws IOException {
        synchronized(MONITOR) {
            Files.createDirectories(directory);
            Path lockPath=directory.resolve(path(invoice).getFileName().toString()+".lck");
            try(FileChannel channel=FileChannel.open(lockPath,StandardOpenOption.CREATE,StandardOpenOption.WRITE);
                    java.nio.channels.FileLock lock=channel.lock()) { return operation.run(); }
        }
    }
    public YeuCauThanhToan read(final String invoice) throws IOException {
        return locked(invoice,new Operation<YeuCauThanhToan>(){public YeuCauThanhToan run()throws IOException{return readUnlocked(invoice);}});
    }
    private YeuCauThanhToan readUnlocked(String invoice) throws IOException {
        Path file = path(invoice);
        if (!Files.exists(file)) return null;
        try {
            Properties data = new Properties();
            try (InputStream input = Files.newInputStream(file)) { data.load(input); }
            if (!"1".equals(data.getProperty("version")) || !invoice.equals(data.getProperty("invoice"))) throw new IOException("Invalid journal");
            int count = Integer.parseInt(data.getProperty("count"));
            if (count < 1 || count > 2) throw new IOException("Invalid journal parts");
            List<PhanThanhToan> parts = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String prefix = "part." + i + ".";
                String tender = data.getProperty(prefix + "tender");
                parts.add(new PhanThanhToan(data.getProperty(prefix + "id"), new BigDecimal(data.getProperty(prefix + "amount")),
                        PhuongThucThanhToan.valueOf(data.getProperty(prefix + "method")), data.getProperty(prefix + "reference"),
                        tender == null ? null : new BigDecimal(tender)));
            }
            YeuCauThanhToan request=new YeuCauThanhToan(invoice, new BigDecimal(data.getProperty("amount")), parts, Boolean.parseBoolean(data.getProperty("confirmed")));
            PaymentService.validate(request);return request;
        } catch (RuntimeException failure) { throw new IOException("Nhật ký thanh toán không đọc được; cần đối soát trước khi thu tiếp.", failure); }
    }

    public void create(final YeuCauThanhToan request) throws IOException {
        locked(request.invoiceId(),new Operation<Void>(){public Void run()throws IOException{createUnlocked(request);return null;}});
    }
    private void createUnlocked(YeuCauThanhToan request) throws IOException {
        PaymentService.validate(request);
        Properties data = new Properties();
        data.setProperty("version", "1"); data.setProperty("invoice", request.invoiceId());
        data.setProperty("amount", request.amount().toPlainString()); data.setProperty("count", Integer.toString(request.parts().size()));
        data.setProperty("confirmed", Boolean.toString(request.transferConfirmed()));
        for (int i = 0; i < request.parts().size(); i++) {
            var part = request.parts().get(i); String prefix = "part." + i + ".";
            data.setProperty(prefix + "id", part.id()); data.setProperty(prefix + "amount", part.amount().toPlainString());
            data.setProperty(prefix + "method", part.method().name());
            if (part.reference() != null) data.setProperty(prefix + "reference", part.reference());
            if (part.customerTender() != null) data.setProperty(prefix + "tender", part.customerTender().toPlainString());
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); data.store(bytes, "Pending payment recovery");
        Files.createDirectories(directory);
        // CREATE_NEW rejects another window/process claiming this invoice. Flush BEFORE sending SQL.
        try (FileChannel channel = FileChannel.open(path(request.invoiceId()), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes.toByteArray());
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
    }

    public void clear(final YeuCauThanhToan expected) throws IOException {
        locked(expected.invoiceId(),new Operation<Void>(){public Void run()throws IOException{clearUnlocked(expected);return null;}});
    }
    private void clearUnlocked(YeuCauThanhToan expected) throws IOException {
        YeuCauThanhToan current = readUnlocked(expected.invoiceId());
        if (current == null) return;
        if (!current.equals(expected)) throw new IOException("Lần thu đang đối soát đã thay đổi; không được ghi đè.");
        Files.delete(path(expected.invoiceId()));
    }
}
