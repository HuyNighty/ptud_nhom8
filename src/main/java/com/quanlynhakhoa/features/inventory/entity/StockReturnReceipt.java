package com.quanlynhakhoa.features.inventory.entity;

import com.quanlynhakhoa.features.inventory.enums.ReturnReason;
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

/** Phiếu hoàn trả kho — bảng dbo.PhieuHoanTra. Chuyển trạng thái chỉ qua các phương thức bên dưới. */
@Entity(name = "PhieuHoanTraJpaEntity")
@Table(name = "PhieuHoanTra", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReturnReceipt implements Persistable<String> {

    @Id
    @Column(name = "maPhieuHoanTra", length = 10, nullable = false)
    private String maPhieuHoanTra;

    @Column(name = "ngayHoanTra", nullable = false)
    private LocalDate ngayHoanTra;

    @Column(name = "maPhieuXuat", length = 10, nullable = false)
    private String maPhieuXuat;

    @Enumerated(EnumType.STRING)
    @Column(name = "lyDo", length = 20, nullable = false)
    private ReturnReason lyDo;

    @Nationalized
    @Column(name = "nguonHoanTra", length = 100, nullable = false)
    private String nguonHoanTra;

    @Nationalized
    @Column(name = "nguoiHoanTra", length = 100, nullable = false)
    private String nguoiHoanTra;

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

    @Column(name = "maNhanVienDuyet", length = 10)
    private String maNhanVienDuyet;

    @Column(name = "thoiDiemDuyet")
    private LocalDateTime thoiDiemDuyet;

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
    private List<StockReturnLine> lines = new ArrayList<>();

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    public static StockReturnReceipt draft(String ma, String maPhieuXuat, LocalDate ngay, ReturnReason lyDo,
                                           String nguon, String nguoi, String ghiChu, String maNhanVienLap,
                                           LocalDateTime now) {
        StockReturnReceipt receipt = new StockReturnReceipt();
        receipt.maPhieuHoanTra = ma;
        receipt.maPhieuXuat = maPhieuXuat;
        receipt.trangThai = VoucherStatus.CHO_XU_LY;
        receipt.maNhanVienLap = maNhanVienLap;
        receipt.thoiDiemLap = now;
        receipt.updateHeader(ngay, lyDo, nguon, nguoi, ghiChu);
        return receipt;
    }

    public List<StockReturnLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public BigDecimal tongTien() {
        return lines.stream().map(StockReturnLine::thanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void updateHeader(LocalDate ngay, ReturnReason reason, String nguon, String nguoi, String note) {
        requireStatus(VoucherStatus.CHO_XU_LY);
        this.ngayHoanTra = ngay;
        this.lyDo = reason;
        this.nguonHoanTra = nguon;
        this.nguoiHoanTra = nguoi;
        this.ghiChu = note;
    }

    public void replaceLines(List<StockReturnLine> newLines) {
        requireStatus(VoucherStatus.CHO_XU_LY);
        lines.clear();
        for (StockReturnLine line : newLines) {
            line.attachTo(this);
            lines.add(line);
        }
    }

    public void updateNote(String note) {
        if (trangThai == VoucherStatus.DA_HUY) {
            throw new IllegalStateException("Phiếu đã hủy không được sửa");
        }
        this.ghiChu = note;
    }

    public void approve(String maNhanVien, LocalDateTime now) {
        requireStatus(VoucherStatus.CHO_XU_LY);
        trangThai = VoucherStatus.DA_HOAN_THANH;
        maNhanVienDuyet = maNhanVien;
        thoiDiemDuyet = now;
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
            throw new IllegalStateException("Phiếu " + maPhieuHoanTra + " đang ở trạng thái " + trangThai);
        }
    }

    @Override
    public String getId() {
        return maPhieuHoanTra;
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
