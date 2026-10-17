package com.quanlynhakhoa.features.inventory.repository;

import com.quanlynhakhoa.features.inventory.entity.StockReturnReceipt;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface StockReturnReceiptRepository extends Repository<StockReturnReceipt, String> {

    @Query("select r from PhieuHoanTraJpaEntity r left join fetch r.lines where r.maPhieuHoanTra = :ma")
    Optional<StockReturnReceipt> findWithLines(@Param("ma") String maPhieuHoanTra);

    /** Khóa phiếu khi đổi trạng thái để hai người không cùng duyệt một phiếu. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PhieuHoanTraJpaEntity r where r.maPhieuHoanTra = :ma")
    Optional<StockReturnReceipt> findForUpdate(@Param("ma") String maPhieuHoanTra);

    @Query("""
            select r from PhieuHoanTraJpaEntity r left join fetch r.lines
            order by r.ngayHoanTra desc, r.maPhieuHoanTra desc
            """)
    List<StockReturnReceipt> findAllWithLines();

    /** Các phiếu hoàn trả của một phiếu xuất gốc — để tính số lượng đã/đang trả theo lô. */
    @Query("select r from PhieuHoanTraJpaEntity r left join fetch r.lines where r.maPhieuXuat = :px")
    List<StockReturnReceipt> findByPhieuXuatWithLines(@Param("px") String maPhieuXuat);

    long countByTrangThai(VoucherStatus trangThai);

    long countByThoiDiemDuyetBetween(LocalDateTime from, LocalDateTime to);

    StockReturnReceipt saveAndFlush(StockReturnReceipt receipt);

    /** Số lớn nhất sau "HT", giữ khóa UPDLOCK/HOLDLOCK tới hết transaction (SQL Server). */
    @Query(value = "SELECT MAX(CAST(SUBSTRING(maPhieuHoanTra, 3, 8) AS INT)) "
            + "FROM dbo.PhieuHoanTra WITH (UPDLOCK, HOLDLOCK)", nativeQuery = true)
    Integer lockAndFindMaxCodeNumber();
}
