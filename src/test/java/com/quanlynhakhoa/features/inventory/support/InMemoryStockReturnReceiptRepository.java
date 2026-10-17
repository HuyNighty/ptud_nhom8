package com.quanlynhakhoa.features.inventory.support;

import com.quanlynhakhoa.features.inventory.entity.StockReturnReceipt;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.repository.StockReturnReceiptRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/** TEST-ONLY: StockReturnReceiptRepository trong bộ nhớ. */
public final class InMemoryStockReturnReceiptRepository implements StockReturnReceiptRepository {
    private final TreeMap<String, StockReturnReceipt> rows = new TreeMap<>();
    private int lineId = 1;

    @Override
    public Optional<StockReturnReceipt> findWithLines(String ma) {
        return Optional.ofNullable(rows.get(ma));
    }

    @Override
    public Optional<StockReturnReceipt> findForUpdate(String ma) {
        return Optional.ofNullable(rows.get(ma));
    }

    @Override
    public List<StockReturnReceipt> findAllWithLines() {
        return rows.values().stream()
                .sorted(Comparator.comparing(StockReturnReceipt::getNgayHoanTra).reversed()
                        .thenComparing(StockReturnReceipt::getMaPhieuHoanTra, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public List<StockReturnReceipt> findByPhieuXuatWithLines(String maPhieuXuat) {
        return rows.values().stream().filter(r -> r.getMaPhieuXuat().equals(maPhieuXuat)).toList();
    }

    @Override
    public long countByTrangThai(VoucherStatus trangThai) {
        return rows.values().stream().filter(r -> r.getTrangThai() == trangThai).count();
    }

    @Override
    public long countByThoiDiemDuyetBetween(LocalDateTime from, LocalDateTime to) {
        return rows.values().stream().filter(r -> r.getThoiDiemDuyet() != null
                && !r.getThoiDiemDuyet().isBefore(from) && !r.getThoiDiemDuyet().isAfter(to)).count();
    }

    @Override
    public StockReturnReceipt saveAndFlush(StockReturnReceipt receipt) {
        receipt.getLines().forEach(line -> {
            if (line.getMaChiTiet() == null) {
                InMemoryStockLotRepository.setField(line, "maChiTiet", lineId++);
            }
        });
        rows.put(receipt.getMaPhieuHoanTra(), receipt);
        return receipt;
    }

    @Override
    public Integer lockAndFindMaxCodeNumber() {
        return rows.keySet().stream().map(k -> Integer.valueOf(k.substring(2))).max(Integer::compare).orElse(null);
    }
}
