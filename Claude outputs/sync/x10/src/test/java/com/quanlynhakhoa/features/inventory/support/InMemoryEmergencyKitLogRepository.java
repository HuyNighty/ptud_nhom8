package com.quanlynhakhoa.features.inventory.support;

import com.quanlynhakhoa.features.inventory.entity.EmergencyKitLog;
import com.quanlynhakhoa.features.inventory.repository.EmergencyKitLogRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** TEST-ONLY: EmergencyKitLogRepository trong bộ nhớ. */
public final class InMemoryEmergencyKitLogRepository implements EmergencyKitLogRepository {
    private final List<EmergencyKitLog> rows = new ArrayList<>();
    private int nextId = 1;
    private int lineId = 1;

    @Override
    public List<EmergencyKitLog> findAllWithLines() {
        return rows.stream().sorted(Comparator.comparing(EmergencyKitLog::getThoiDiem).reversed()
                .thenComparing(EmergencyKitLog::getMaNhatKy, Comparator.reverseOrder())).toList();
    }

    @Override
    public EmergencyKitLog saveAndFlush(EmergencyKitLog log) {
        if (log.getMaNhatKy() == null) {
            InMemoryStockLotRepository.setField(log, "maNhatKy", nextId++);
            rows.add(log);
        }
        log.getLines().forEach(line -> {
            if (line.getMaChiTiet() == null) {
                InMemoryStockLotRepository.setField(line, "maChiTiet", lineId++);
            }
        });
        return log;
    }
}
