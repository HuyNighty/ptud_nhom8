package com.quanlynhakhoa.features.inventory.repository;

import com.quanlynhakhoa.features.inventory.entity.EmergencyKit;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface EmergencyKitRepository extends Repository<EmergencyKit, String> {

    @Query("select k from HopCapCuuJpaEntity k left join fetch k.items order by k.maHop")
    List<EmergencyKit> findAllWithItems();

    @Query("select k from HopCapCuuJpaEntity k left join fetch k.items where k.maHop = :ma")
    Optional<EmergencyKit> findWithItems(@Param("ma") String maHop);

    /** Khóa hộp khi kiểm tra / bổ sung để hai người không cùng ghi đè số hiện có. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from HopCapCuuJpaEntity k where k.maHop = :ma")
    Optional<EmergencyKit> findForUpdate(@Param("ma") String maHop);

    EmergencyKit saveAndFlush(EmergencyKit kit);

    /** Số lớn nhất sau "HTCC-", giữ khóa UPDLOCK/HOLDLOCK tới hết transaction (SQL Server). */
    @Query(value = "SELECT MAX(CAST(SUBSTRING(maHop, 6, 5) AS INT)) "
            + "FROM dbo.HopCapCuu WITH (UPDLOCK, HOLDLOCK)", nativeQuery = true)
    Integer lockAndFindMaxCodeNumber();
}
