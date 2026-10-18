package com.quanlynhakhoa.features.inventory.support;

import com.quanlynhakhoa.features.inventory.entity.Supplier;
import com.quanlynhakhoa.features.inventory.repository.SupplierRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/** TEST-ONLY: SupplierRepository trong bộ nhớ. */
public final class InMemorySupplierRepository implements SupplierRepository {
    private final TreeMap<String, Supplier> rows = new TreeMap<>();

    @Override
    public Optional<Supplier> findById(String ma) {
        return Optional.ofNullable(rows.get(ma));
    }

    @Override
    public List<Supplier> findAllByOrderByTenNhaCungCapAsc() {
        return rows.values().stream().sorted(Comparator.comparing(Supplier::getTenNhaCungCap)).toList();
    }

    @Override
    public Supplier saveAndFlush(Supplier supplier) {
        rows.put(supplier.getMaNhaCungCap(), supplier);
        return supplier;
    }

    @Override
    public Integer findMaxCodeNumber() {
        return rows.keySet().stream().map(k -> Integer.valueOf(k.substring(3))).max(Integer::compare).orElse(null);
    }
}
