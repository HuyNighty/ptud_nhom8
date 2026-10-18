package com.quanlynhakhoa.features.inventory.repository;

import com.quanlynhakhoa.features.inventory.entity.StockOutReceipt;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface StockOutReceiptRepository extends Repository<StockOutReceipt, String> {

    @Query("select r from PhieuXuatKhoJpaEntity r left join fetch r.lines where r.maPhieuXuat = :ma")
    Optional<StockOutReceipt> findWithLines(@Param("ma") String maPhieuXuat);

    /** Khóa phiếu khi đổi trạng thái để hai người không cùng "Xuất kho" một phiếu. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PhieuXuatKhoJpaEntity r where r.maPhieuXuat = :ma")
    Optional<StockOutReceipt> findForUpdate(@Param("ma") String maPhieuXuat);

    @Query("""
            select r from PhieuXuatKhoJpaEntity r left join fetch r.lines
            order by r.ngayXuat desc, r.maPhieuXuat desc
            """)
    List<StockOutReceipt> findAllWithLines();

    long countByTrangThai(VoucherStatus trangThai);

    long countByThoiDiemXuatKhoBetween(LocalDateTime from, LocalDateTime to);

    StockOutReceipt saveAndFlush(StockOutReceipt receipt);

    /** Số lớn nhất sau "PX", giữ khóa UPDLOCK/HOLDLOCK tới hết transaction (SQL Server). */
    @Query(value = "SELECT MAX(CAST(SUBSTRING(maPhieuXuat, 3, 8) AS INT)) "
            + "FROM dbo.PhieuXuatKho WITH (UPDLOCK, HOLDLOCK)", nativeQuery = true)
    Integer lockAndFindMaxCodeNumber();
}
