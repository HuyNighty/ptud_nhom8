package com.quanlynhakhoa.features.inventory.support;

import com.quanlynhakhoa.features.inventory.entity.EmergencyKit;
import com.quanlynhakhoa.features.inventory.repository.EmergencyKitRepository;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/** TEST-ONLY: EmergencyKitRepository trong bộ nhớ; tự cấp mã chi tiết như cột IDENTITY. */
public final class InMemoryEmergencyKitRepository implements EmergencyKitRepository {
    private final TreeMap<String, EmergencyKit> rows = new TreeMap<>();
    private int itemId = 1;

    @Override
    public List<EmergencyKit> findAllWithItems() {
        return List.copyOf(rows.values());
    }

    @Override
    public Optional<EmergencyKit> findWithItems(String maHop) {
        return Optional.ofNullable(rows.get(maHop));
    }

    @Override
    public Optional<EmergencyKit> findForUpdate(String maHop) {
        return Optional.ofNullable(rows.get(maHop));
    }

    @Override
    public EmergencyKit saveAndFlush(EmergencyKit kit) {
        kit.getItems().forEach(item -> {
            if (item.getMaChiTiet() == null) {
                InMemoryStockLotRepository.setField(item, "maChiTiet", itemId++);
            }
        });
        rows.put(kit.getMaHop(), kit);
        return kit;
    }

    @Override
    public Integer lockAndFindMaxCodeNumber() {
        return rows.keySet().stream().map(k -> Integer.valueOf(k.substring(5))).max(Integer::compare).orElse(null);
    }
}
