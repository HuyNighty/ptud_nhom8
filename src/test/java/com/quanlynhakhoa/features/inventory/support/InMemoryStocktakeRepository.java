package com.quanlynhakhoa.features.inventory.support;

import com.quanlynhakhoa.features.inventory.entity.Stocktake;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.repository.StocktakeRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/** TEST-ONLY: StocktakeRepository trong bộ nhớ. */
public final class InMemoryStocktakeRepository implements StocktakeRepository {
    private final TreeMap<String, Stocktake> rows = new TreeMap<>();
    private int lineId = 1;

    @Override
    public Optional<Stocktake> findWithLines(String ma) {
        return Optional.ofNullable(rows.get(ma));
    }

    @Override
    public Optional<Stocktake> findForUpdate(String ma) {
        return Optional.ofNullable(rows.get(ma));
    }

    @Override
    public List<Stocktake> findAllWithLines() {
        return rows.values().stream()
                .sorted(Comparator.comparing(Stocktake::getNgayKiemKe).reversed()
                        .thenComparing(Stocktake::getMaKiemKe, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public long countByTrangThai(VoucherStatus trangThai) {
        return rows.values().stream().filter(r -> r.getTrangThai() == trangThai).count();
    }

    @Override
    public Stocktake saveAndFlush(Stocktake stocktake) {
        stocktake.getLines().forEach(line -> {
            if (line.getMaChiTiet() == null) {
                InMemoryStockLotRepository.setField(line, "maChiTiet", lineId++);
            }
        });
        rows.put(stocktake.getMaKiemKe(), stocktake);
        return stocktake;
    }

    @Override
    public Integer lockAndFindMaxCodeNumber() {
        return rows.keySet().stream().map(k -> Integer.valueOf(k.substring(2))).max(Integer::compare).orElse(null);
    }
}
