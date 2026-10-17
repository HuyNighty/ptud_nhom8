package com.quanlynhakhoa.features.inventory.support;

import com.quanlynhakhoa.features.inventory.entity.Medicine;
import com.quanlynhakhoa.features.inventory.repository.MedicineRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;

/** TEST-ONLY: MedicineRepository trong bộ nhớ, mô phỏng đúng các truy vấn của bản JPA. */
public final class InMemoryMedicineRepository implements MedicineRepository {
    private final TreeMap<String, Medicine> rows = new TreeMap<>();

    @Override
    public Optional<Medicine> findById(String ma) {
        return Optional.ofNullable(rows.get(ma));
    }

    @Override
    public boolean existsById(String ma) {
        return rows.containsKey(ma);
    }

    @Override
    public Medicine saveAndFlush(Medicine medicine) {
        rows.put(medicine.getMaThuoc(), medicine);
        return medicine;
    }

    @Override
    public long count() {
        return rows.size();
    }

    @Override
    public long countByKiemSoatDacBietTrue() {
        return rows.values().stream().filter(Medicine::isKiemSoatDacBiet).count();
    }

    @Override
    public List<Medicine> search(String keyword) {
        return rows.values().stream().filter(m -> keyword.isEmpty()
                || contains(m.getMaThuoc(), keyword)
                || contains(m.getTenThuoc(), keyword)
                || contains(m.getHoatChat(), keyword)).toList();
    }

    @Override
    public Integer findMaxCodeNumber() {
        return rows.keySet().stream().map(k -> Integer.valueOf(k.substring(2))).max(Integer::compare).orElse(null);
    }

    static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}
