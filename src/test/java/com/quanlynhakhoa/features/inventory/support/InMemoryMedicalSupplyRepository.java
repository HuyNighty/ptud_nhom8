package com.quanlynhakhoa.features.inventory.support;

import com.quanlynhakhoa.features.inventory.entity.MedicalSupply;
import com.quanlynhakhoa.features.inventory.repository.MedicalSupplyRepository;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/** TEST-ONLY: MedicalSupplyRepository trong bộ nhớ, mô phỏng đúng các truy vấn của bản JPA. */
public final class InMemoryMedicalSupplyRepository implements MedicalSupplyRepository {
    private final TreeMap<String, MedicalSupply> rows = new TreeMap<>();

    @Override
    public Optional<MedicalSupply> findById(String ma) {
        return Optional.ofNullable(rows.get(ma));
    }

    @Override
    public boolean existsById(String ma) {
        return rows.containsKey(ma);
    }

    @Override
    public MedicalSupply saveAndFlush(MedicalSupply supply) {
        rows.put(supply.getMaVatTu(), supply);
        return supply;
    }

    @Override
    public long count() {
        return rows.size();
    }

    @Override
    public List<MedicalSupply> search(String keyword) {
        return rows.values().stream().filter(v -> keyword.isEmpty()
                || InMemoryMedicineRepository.contains(v.getMaVatTu(), keyword)
                || InMemoryMedicineRepository.contains(v.getTenVatTu(), keyword)
                || InMemoryMedicineRepository.contains(v.getMoTa(), keyword)).toList();
    }

    @Override
    public Integer findMaxCodeNumber() {
        return rows.keySet().stream().map(k -> Integer.valueOf(k.substring(2))).max(Integer::compare).orElse(null);
    }
}
