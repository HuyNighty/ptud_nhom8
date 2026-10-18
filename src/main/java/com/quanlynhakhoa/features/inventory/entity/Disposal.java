package com.quanlynhakhoa.features.inventory.entity;

import com.quanlynhakhoa.features.inventory.enums.DisposalReason;
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

/**
 * Phiếu hủy / hao hụt — bảng dbo.PhieuHuy. CHO_XU_LY = "Chờ duyệt", DA_HOAN_THANH = đã duyệt và trừ tồn,
 * DA_HUY = phiếu bị hủy (không duyệt), tồn không đổi.
 */
@Entity(name = "PhieuHuyJpaEntity")
@Table(name = "PhieuHuy", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Disposal implements Persistable<String> {

    @Id
    @Column(name = "maPhieuHuy", length = 10, nullable = false)
    private String maPhieuHuy;

    @Column(name = "ngayHuy", nullable = false)
    private LocalDate ngayHuy;

    @Enumerated(EnumType.STRING)
    @Column(name = "lyDo", length = 20, nullable = false)
    private DisposalReason lyDo;

    @Nationalized
    @Column(name = "moTa", length = 200)
    private String moTa;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", length = 20, nullable = false)
    private VoucherStatus trangThai;

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

    @OneToMany(mappedBy = "disposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("maChiTiet")
    @Getter(AccessLevel.NONE)
    private List<DisposalLine> lines = new ArrayList<>();

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    public static Disposal draft(String ma, LocalDate ngay, DisposalReason lyDo, String moTa, String maNhanVienLap,
                                 LocalDateTime now) {
        Disposal d = new Disposal();
        d.maPhieuHuy = ma;
        d.trangThai = VoucherStatus.CHO_XU_LY;
        d.maNhanVienLap = maNhanVienLap;
        d.thoiDiemLap = now;
        d.updateHeader(ngay, lyDo, moTa);
        return d;
    }

    public List<DisposalLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public BigDecimal tongTien() {
        return lines.stream().map(DisposalLine::thanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void updateHeader(LocalDate ngay, DisposalReason reason, String description) {
        requireStatus(VoucherStatus.CHO_XU_LY);
        this.ngayHuy = ngay;
        this.lyDo = reason;
        this.moTa = description;
    }

    public void replaceLines(List<DisposalLine> newLines) {
        requireStatus(VoucherStatus.CHO_XU_LY);
        lines.clear();
        for (DisposalLine line : newLines) {
            line.attachTo(this);
            lines.add(line);
        }
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
            throw new IllegalStateException("Phiếu " + maPhieuHuy + " đang ở trạng thái " + trangThai);
        }
    }

    @Override
    public String getId() {
        return maPhieuHuy;
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
