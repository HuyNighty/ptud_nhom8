package com.quanlynhakhoa.features.inventory.support;

import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.repository.StockLotRepository;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** TEST-ONLY: StockLotRepository trong bộ nhớ; tự cấp mã lô như cột IDENTITY. */
public final class InMemoryStockLotRepository implements StockLotRepository {
    private final List<StockLot> rows = new ArrayList<>();
    private int nextId = 1;

    @Override
    public Optional<StockLot> findByMaThuocAndSoLo(String maThuoc, String soLo) {
        return rows.stream().filter(l -> Objects.equals(l.getMaThuoc(), maThuoc) && l.getSoLo().equals(soLo)).findFirst();
    }

    @Override
    public Optional<StockLot> findByMaVatTuAndSoLo(String maVatTu, String soLo) {
        return rows.stream().filter(l -> Objects.equals(l.getMaVatTu(), maVatTu) && l.getSoLo().equals(soLo)).findFirst();
    }

    @Override
    public List<StockLot> findBySoLuongTonGreaterThan(int soLuong) {
        return rows.stream().filter(l -> l.getSoLuongTon() > soLuong).toList();
    }

    @Override
    public Optional<StockLot> findById(Integer maLo) {
        return rows.stream().filter(l -> l.getMaLo().equals(maLo)).findFirst();
    }

    @Override
    public List<StockLot> findByMaLoIn(java.util.Collection<Integer> maLo) {
        return rows.stream().filter(l -> maLo.contains(l.getMaLo())).toList();
    }

    @Override
    public Optional<StockLot> findForUpdate(Integer maLo) {
        return findById(maLo);
    }

    @Override
    public List<StockLot> findByMaThuocAndSoLuongTonGreaterThan(String maThuoc, int soLuong) {
        return rows.stream().filter(l -> Objects.equals(l.getMaThuoc(), maThuoc) && l.getSoLuongTon() > soLuong).toList();
    }

    @Override
    public List<StockLot> findByMaVatTuAndSoLuongTonGreaterThan(String maVatTu, int soLuong) {
        return rows.stream().filter(l -> Objects.equals(l.getMaVatTu(), maVatTu) && l.getSoLuongTon() > soLuong).toList();
    }

    @Override
    public StockLot saveAndFlush(StockLot lot) {
        if (lot.getMaLo() == null) {
            setField(lot, "maLo", nextId++);
            rows.add(lot);
        }
        return lot;
    }

    public List<StockLot> all() {
        return List.copyOf(rows);
    }

    static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
