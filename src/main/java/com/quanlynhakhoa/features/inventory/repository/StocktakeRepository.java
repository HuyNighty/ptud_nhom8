package com.quanlynhakhoa.features.inventory.repository;

import com.quanlynhakhoa.features.inventory.entity.Stocktake;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface StocktakeRepository extends Repository<Stocktake, String> {

    @Query("select s from PhieuKiemKeJpaEntity s left join fetch s.lines where s.maKiemKe = :ma")
    Optional<Stocktake> findWithLines(@Param("ma") String maKiemKe);

    /** Khóa đợt khi đổi trạng thái / ghi số đếm. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PhieuKiemKeJpaEntity s where s.maKiemKe = :ma")
    Optional<Stocktake> findForUpdate(@Param("ma") String maKiemKe);

    @Query("""
            select s from PhieuKiemKeJpaEntity s left join fetch s.lines
            order by s.ngayKiemKe desc, s.maKiemKe desc
            """)
    List<Stocktake> findAllWithLines();

    long countByTrangThai(VoucherStatus trangThai);

    Stocktake saveAndFlush(Stocktake stocktake);

    /** Số lớn nhất sau "KK", giữ khóa UPDLOCK/HOLDLOCK tới hết transaction (SQL Server). */
    @Query(value = "SELECT MAX(CAST(SUBSTRING(maKiemKe, 3, 8) AS INT)) "
            + "FROM dbo.PhieuKiemKe WITH (UPDLOCK, HOLDLOCK)", nativeQuery = true)
    Integer lockAndFindMaxCodeNumber();
}
