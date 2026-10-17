package com.quanlynhakhoa.features.inventory.entity;

import com.quanlynhakhoa.features.inventory.enums.StockOutReason;
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

/** Phiếu xuất kho — bảng dbo.PhieuXuatKho. Chuyển trạng thái chỉ qua các phương thức bên dưới. */
@Entity(name = "PhieuXuatKhoJpaEntity")
@Table(name = "PhieuXuatKho", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockOutReceipt implements Persistable<String> {

    @Id
    @Column(name = "maPhieuXuat", length = 10, nullable = false)
    private String maPhieuXuat;

    @Column(name = "ngayXuat", nullable = false)
    private LocalDate ngayXuat;

    @Enumerated(EnumType.STRING)
    @Column(name = "lyDoXuat", length = 30, nullable = false)
    private StockOutReason lyDoXuat;

    @Nationalized
    @Column(name = "nguoiNhan", length = 100, nullable = false)
    private String nguoiNhan;

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

    @Column(name = "maNhanVienXuatKho", length = 10)
    private String maNhanVienXuatKho;

    @Column(name = "thoiDiemXuatKho")
    private LocalDateTime thoiDiemXuatKho;

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
    private List<StockOutLine> lines = new ArrayList<>();

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    public static StockOutReceipt draft(String ma, LocalDate ngayXuat, StockOutReason lyDo, String nguoiNhan,
                                        String ghiChu, String maNhanVienLap, LocalDateTime now) {
        StockOutReceipt receipt = new StockOutReceipt();
        receipt.maPhieuXuat = ma;
        receipt.trangThai = VoucherStatus.CHO_XU_LY;
        receipt.maNhanVienLap = maNhanVienLap;
        receipt.thoiDiemLap = now;
        receipt.updateHeader(ngayXuat, lyDo, nguoiNhan, ghiChu);
        return receipt;
    }

    public List<StockOutLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /** Tổng giá trị theo giá vốn — chỉ có nghĩa khi đã xuất kho (đơn giá được chốt lúc xuất). */
    public BigDecimal tongTien() {
        return lines.stream().map(StockOutLine::thanhTien).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void updateHeader(LocalDate ngay, StockOutReason lyDo, String nhan, String note) {
        requireStatus(VoucherStatus.CHO_XU_LY);
        this.ngayXuat = ngay;
        this.lyDoXuat = lyDo;
        this.nguoiNhan = nhan;
        this.ghiChu = note;
    }

    public void replaceLines(List<StockOutLine> newLines) {
        requireStatus(VoucherStatus.CHO_XU_LY);
        lines.clear();
        for (StockOutLine line : newLines) {
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

    public void markCompleted(String maNhanVien, LocalDateTime now) {
        requireStatus(VoucherStatus.CHO_XU_LY);
        trangThai = VoucherStatus.DA_HOAN_THANH;
        maNhanVienXuatKho = maNhanVien;
        thoiDiemXuatKho = now;
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
            throw new IllegalStateException("Phiếu " + maPhieuXuat + " đang ở trạng thái " + trangThai);
        }
    }

    @Override
    public String getId() {
        return maPhieuXuat;
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
