package com.quanlynhakhoa.features.inventory.entity;

import com.quanlynhakhoa.features.inventory.enums.StocktakeScope;
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

/**
 * Đợt kiểm kê — bảng dbo.PhieuKiemKe. Trạng thái dùng chung {@link VoucherStatus}:
 * CHO_XU_LY = "Đang kiểm kê", DA_HOAN_THANH = đã duyệt và điều chỉnh tồn, DA_HUY.
 */
@Entity(name = "PhieuKiemKeJpaEntity")
@Table(name = "PhieuKiemKe", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stocktake implements Persistable<String> {

    @Id
    @Column(name = "maKiemKe", length = 10, nullable = false)
    private String maKiemKe;

    @Column(name = "ngayKiemKe", nullable = false)
    private LocalDate ngayKiemKe;

    @Enumerated(EnumType.STRING)
    @Column(name = "phamVi", length = 10, nullable = false)
    private StocktakeScope phamVi;

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

    @OneToMany(mappedBy = "stocktake", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("maChiTiet")
    @Getter(AccessLevel.NONE)
    private List<StocktakeLine> lines = new ArrayList<>();

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    public static Stocktake open(String ma, LocalDate ngay, StocktakeScope phamVi, String ghiChu,
                                 String maNhanVienLap, LocalDateTime now, List<StocktakeLine> snapshot) {
        Stocktake s = new Stocktake();
        s.maKiemKe = ma;
        s.ngayKiemKe = ngay;
        s.phamVi = phamVi;
        s.ghiChu = ghiChu;
        s.trangThai = VoucherStatus.CHO_XU_LY;
        s.maNhanVienLap = maNhanVienLap;
        s.thoiDiemLap = now;
        for (StocktakeLine line : snapshot) {
            line.attachTo(s);
            s.lines.add(line);
        }
        return s;
    }

    public List<StocktakeLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public void updateHeader(LocalDate ngay, String note) {
        requireStatus(VoucherStatus.CHO_XU_LY);
        this.ngayKiemKe = ngay;
        this.ghiChu = note;
    }

    public void updateNote(String note) {
        if (trangThai == VoucherStatus.DA_HUY) {
            throw new IllegalStateException("Đợt kiểm kê đã hủy không được sửa");
        }
        this.ghiChu = note;
    }

    /** Ghi số đếm của một lô trong đợt (đang kiểm kê). */
    public void record(StocktakeLine line, Integer thucTe, String note) {
        requireStatus(VoucherStatus.CHO_XU_LY);
        line.record(thucTe, note);
    }

    public void approve(String maNhanVien, LocalDateTime now) {
        requireStatus(VoucherStatus.CHO_XU_LY);
        if (maNhanVien.equals(maNhanVienLap)) {
            throw new IllegalStateException("Người duyệt phải khác người lập");
        }
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
            throw new IllegalStateException("Đợt " + maKiemKe + " đang ở trạng thái " + trangThai);
        }
    }

    @Override
    public String getId() {
        return maKiemKe;
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
