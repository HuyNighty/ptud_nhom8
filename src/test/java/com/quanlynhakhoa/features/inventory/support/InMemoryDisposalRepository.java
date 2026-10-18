package com.quanlynhakhoa.features.inventory.support;

import com.quanlynhakhoa.features.inventory.entity.Disposal;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.repository.DisposalRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/** TEST-ONLY: DisposalRepository trong bộ nhớ. */
public final class InMemoryDisposalRepository implements DisposalRepository {
    private final TreeMap<String, Disposal> rows = new TreeMap<>();
    private int lineId = 1;

    @Override
    public Optional<Disposal> findWithLines(String ma) {
        return Optional.ofNullable(rows.get(ma));
    }

    @Override
    public Optional<Disposal> findForUpdate(String ma) {
        return Optional.ofNullable(rows.get(ma));
    }

    @Override
    public List<Disposal> findAllWithLines() {
        return rows.values().stream()
                .sorted(Comparator.comparing(Disposal::getNgayHuy).reversed()
                        .thenComparing(Disposal::getMaPhieuHuy, Comparator.reverseOrder()))
                .toList();
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
    public Disposal saveAndFlush(Disposal stocktake) {
        stocktake.getLines().forEach(line -> {
            if (line.getMaChiTiet() == null) {
                InMemoryStockLotRepository.setField(line, "maChiTiet", lineId++);
            }
        });
        rows.put(stocktake.getMaPhieuHuy(), stocktake);
        return stocktake;
    }

    @Override
    public Integer lockAndFindMaxCodeNumber() {
        return rows.keySet().stream().map(k -> Integer.valueOf(k.substring(2))).max(Integer::compare).orElse(null);
    }
}
