package com.quanlynhakhoa.features.inventory.entity;

import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;
import org.springframework.data.domain.Persistable;

/** Phiếu nhập kho — bảng dbo.PhieuNhapKho. Chuyển trạng thái chỉ qua các phương thức bên dưới. */
@Entity(name = "PhieuNhapKhoJpaEntity")
@Table(name = "PhieuNhapKho", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockInReceipt implements Persistable<String> {

    @Id
    @Column(name = "maPhieuNhap", length = 10, nullable = false)
    private String maPhieuNhap;

    @Column(name = "ngayNhap", nullable = false)
    private LocalDate ngayNhap;

    @Column(name = "maNhaCungCap", length = 10, nullable = false)
    private String maNhaCungCap;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", length = 20, nullable = false)
    private VoucherStatus trangThai;

    @Nationalized
    @Column(name = "ghiChu", length = 300)
    private String ghiChu;

    @Column(name = "maNhanVienLap", length = 10, nullable = false)
    private String maNhanVienLap;

    @Column(name = "thoiDiemLap", nullable = false)
    private LocalDateTime thoiDiemLap;

    @Column(name = "maNhanVienNhapKho", length = 10)
    private String maNhanVienNhapKho;

    @Column(name = "thoiDiemNhapKho")
    private LocalDateTime thoiDiemNhapKho;

    @Column(name = "maNhanVienHuy", length = 10)
    private String maNhanVienHuy;

    @Column(name = "thoiDiemHuy")
    private LocalDateTime thoiDiemHuy;

    @Nationalized
    @Column(name = "lyDoHuy", length = 300)
    private String lyDoHuy;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("maChiTiet")
    @Getter(AccessLevel.NONE)
    private List<StockInLine> lines = new ArrayList<>();

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    public static StockInReceipt draft(String ma, LocalDate ngayNhap, String maNhaCungCap, String ghiChu,
                                       String maNhanVienLap, LocalDateTime now) {
        StockInReceipt receipt = new StockInReceipt();
        receipt.maPhieuNhap = ma;
        receipt.trangThai = VoucherStatus.CHO_XU_LY;
        receipt.maNhanVienLap = maNhanVienLap;
        receipt.thoiDiemLap = now;
        receipt.updateHeader(ngayNhap, maNhaCungCap, ghiChu);
        return receipt;
    }

    public List<StockInLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public BigDecimal tongTien() {
        return lines.stream().map(StockInLine::thanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Chỉ phiếu chờ xử lý mới được đổi nhà cung cấp, ngày và các dòng hàng. */
    public void updateHeader(LocalDate ngay, String maNcc, String note) {
        requireStatus(VoucherStatus.CHO_XU_LY);
        this.ngayNhap = ngay;
        this.maNhaCungCap = maNcc;
        this.ghiChu = note;
    }

    public void replaceLines(List<StockInLine> newLines) {
        requireStatus(VoucherStatus.CHO_XU_LY);
        lines.clear();
        for (StockInLine line : newLines) {
            line.attachTo(this);
            lines.add(line);
        }
    }

    /** Ghi chú sửa được cả khi đã hoàn thành (không ảnh hưởng tồn kho). */
    public void updateNote(String note) {
        if (trangThai == VoucherStatus.DA_HUY) {
            throw new IllegalStateException("Phiếu đã hủy không được sửa");
        }
        this.ghiChu = note;
    }

    public void markCompleted(String maNhanVien, LocalDateTime now) {
        requireStatus(VoucherStatus.CHO_XU_LY);
        trangThai = VoucherStatus.DA_HOAN_THANH;
        maNhanVienNhapKho = maNhanVien;
        thoiDiemNhapKho = now;
    }

    public void cancel(String maNhanVien, String reason, LocalDateTime now) {
        requireStatus(VoucherStatus.CHO_XU_LY);
        trangThai = VoucherStatus.DA_HUY;
        maNhanVienHuy = maNhanVien;
        thoiDiemHuy = now;
        lyDoHuy = reason;
    }

    private void requireStatus(VoucherStatus expected) {
        if (trangThai != expected) {
            throw new IllegalStateException("Phiếu " + maPhieuNhap + " đang ở trạng thái " + trangThai);
        }
    }

    @Override
    public String getId() {
        return maPhieuNhap;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        newEntity = false;
    }
}
