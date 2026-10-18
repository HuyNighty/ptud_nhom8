package com.quanlynhakhoa.features.inventory.repository;

import com.quanlynhakhoa.features.inventory.entity.Disposal;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface DisposalRepository extends Repository<Disposal, String> {

    @Query("select d from PhieuHuyJpaEntity d left join fetch d.lines where d.maPhieuHuy = :ma")
    Optional<Disposal> findWithLines(@Param("ma") String maPhieuHuy);

    /** Khóa phiếu khi đổi trạng thái để hai người không cùng duyệt một phiếu. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from PhieuHuyJpaEntity d where d.maPhieuHuy = :ma")
    Optional<Disposal> findForUpdate(@Param("ma") String maPhieuHuy);

    @Query("""
            select d from PhieuHuyJpaEntity d left join fetch d.lines
            order by d.ngayHuy desc, d.maPhieuHuy desc
            """)
    List<Disposal> findAllWithLines();

    long countByTrangThai(VoucherStatus trangThai);

    long countByThoiDiemDuyetBetween(LocalDateTime from, LocalDateTime to);

    Disposal saveAndFlush(Disposal disposal);

    /** Số lớn nhất sau "HH", giữ khóa UPDLOCK/HOLDLOCK tới hết transaction (SQL Server). */
    @Query(value = "SELECT MAX(CAST(SUBSTRING(maPhieuHuy, 3, 8) AS INT)) "
            + "FROM dbo.PhieuHuy WITH (UPDLOCK, HOLDLOCK)", nativeQuery = true)
    Integer lockAndFindMaxCodeNumber();
}
