package com.quanlynhakhoa.features.inventory.support;

import com.quanlynhakhoa.features.inventory.entity.StockOutReceipt;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.repository.StockOutReceiptRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/** TEST-ONLY: StockOutReceiptRepository trong bộ nhớ. */
public final class InMemoryStockOutReceiptRepository implements StockOutReceiptRepository {
    private final TreeMap<String, StockOutReceipt> rows = new TreeMap<>();
    private int lineId = 1;

    @Override
    public Optional<StockOutReceipt> findWithLines(String ma) {
        return Optional.ofNullable(rows.get(ma));
    }

    @Override
    public Optional<StockOutReceipt> findForUpdate(String ma) {
        return Optional.ofNullable(rows.get(ma));
    }

    @Override
    public List<StockOutReceipt> findAllWithLines() {
        return rows.values().stream()
                .sorted(Comparator.comparing(StockOutReceipt::getNgayXuat).reversed()
                        .thenComparing(StockOutReceipt::getMaPhieuXuat, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public long countByTrangThai(VoucherStatus trangThai) {
        return rows.values().stream().filter(r -> r.getTrangThai() == trangThai).count();
    }

    @Override
    public long countByThoiDiemXuatKhoBetween(LocalDateTime from, LocalDateTime to) {
        return rows.values().stream().filter(r -> r.getThoiDiemXuatKho() != null
                && !r.getThoiDiemXuatKho().isBefore(from) && !r.getThoiDiemXuatKho().isAfter(to)).count();
    }

    @Override
    public StockOutReceipt saveAndFlush(StockOutReceipt receipt) {
        receipt.getLines().forEach(line -> {
            if (line.getMaChiTiet() == null) {
                InMemoryStockLotRepository.setField(line, "maChiTiet", lineId++);
            }
        });
        rows.put(receipt.getMaPhieuXuat(), receipt);
        return receipt;
    }

    @Override
    public Integer lockAndFindMaxCodeNumber() {
        return rows.keySet().stream().map(k -> Integer.valueOf(k.substring(2))).max(Integer::compare).orElse(null);
    }
}
