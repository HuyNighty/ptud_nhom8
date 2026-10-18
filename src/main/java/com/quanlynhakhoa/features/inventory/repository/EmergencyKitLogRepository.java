package com.quanlynhakhoa.features.inventory.repository;

import com.quanlynhakhoa.features.inventory.entity.EmergencyKitLog;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface EmergencyKitLogRepository extends Repository<EmergencyKitLog, Integer> {

    @Query("""
            select g from NhatKyHopCapCuuJpaEntity g left join fetch g.lines
            order by g.thoiDiem desc, g.maNhatKy desc
            """)
    List<EmergencyKitLog> findAllWithLines();

    EmergencyKitLog saveAndFlush(EmergencyKitLog log);
}
