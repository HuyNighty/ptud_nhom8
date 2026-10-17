package com.quanlynhakhoa.features.inventory.repository;

import com.quanlynhakhoa.features.inventory.entity.StockInReceipt;
import com.quanlynhakhoa.features.inventory.enums.StockInStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface StockInReceiptRepository extends Repository<StockInReceipt, String> {

    @Query("select r from PhieuNhapKhoJpaEntity r left join fetch r.lines where r.maPhieuNhap = :ma")
    Optional<StockInReceipt> findWithLines(@Param("ma") String maPhieuNhap);

    /** Khóa phiếu khi đổi trạng thái để hai người không cùng "Nhập kho" một phiếu. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PhieuNhapKhoJpaEntity r where r.maPhieuNhap = :ma")
    Optional<StockInReceipt> findForUpdate(@Param("ma") String maPhieuNhap);

    @Query("""
            select r from PhieuNhapKhoJpaEntity r left join fetch r.lines
            order by r.ngayNhap desc, r.maPhieuNhap desc
            """)
    List<StockInReceipt> findAllWithLines();

    long countByTrangThai(StockInStatus trangThai);

    long countByThoiDiemNhapKhoBetween(LocalDateTime from, LocalDateTime to);

    StockInReceipt saveAndFlush(StockInReceipt receipt);

    /**
     * Số lớn nhất sau "PN", giữ khóa UPDLOCK/HOLDLOCK tới hết transaction để hai người
     * tạo phiếu cùng lúc không lấy trùng mã (SQL Server).
     */
    @Query(value = "SELECT MAX(CAST(SUBSTRING(maPhieuNhap, 3, 8) AS INT)) "
            + "FROM dbo.PhieuNhapKho WITH (UPDLOCK, HOLDLOCK)", nativeQuery = true)
    Integer lockAndFindMaxCodeNumber();

    /** Họ tên nhân viên theo mã — chỉ đọc bảng NhanVien (thuộc feature employee). */
    @Query("select e.maNhanVien, e.hoTen from NhanVienJpaEntity e where e.maNhanVien in :ids")
    List<Object[]> findEmployeeNames(@Param("ids") Collection<String> maNhanVien);
}
