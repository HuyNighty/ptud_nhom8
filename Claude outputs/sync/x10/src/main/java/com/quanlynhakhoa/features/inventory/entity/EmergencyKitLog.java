package com.quanlynhakhoa.features.inventory.entity;

import com.quanlynhakhoa.features.inventory.enums.KitLogType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;

/** Một lần kiểm tra / bổ sung / thay thế hộp cấp cứu — bảng dbo.NhatKyHopCapCuu. Chỉ thêm, không sửa. */
@Entity(name = "NhatKyHopCapCuuJpaEntity")
@Table(name = "NhatKyHopCapCuu", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmergencyKitLog {

    public static final String DAT = "DAT";
    public static final String KHONG_DAT = "KHONG_DAT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maNhatKy", nullable = false)
    private Integer maNhatKy;

    @Column(name = "maHop", length = 10, nullable = false)
    private String maHop;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai", length = 20, nullable = false)
    private KitLogType loai;

    /** Chỉ có với KIEM_TRA: DAT / KHONG_DAT. */
    @Column(name = "ketQua", length = 20)
    private String ketQua;

    @Column(name = "maPhieuXuat", length = 10)
    private String maPhieuXuat;

    @Column(name = "maNhanVien", length = 10, nullable = false)
    private String maNhanVien;

    @Column(name = "thoiDiem", nullable = false)
    private LocalDateTime thoiDiem;

    @Nationalized
    @Column(name = "ghiChu", length = 300)
    private String ghiChu;

    @OneToMany(mappedBy = "log", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("maChiTiet")
    @Getter(AccessLevel.NONE)
    private List<EmergencyKitLogLine> lines = new ArrayList<>();

    public static EmergencyKitLog check(String maHop, boolean dat, String maNhanVien, LocalDateTime now,
                                        String ghiChu) {
        EmergencyKitLog log = base(maHop, KitLogType.KIEM_TRA, maNhanVien, now, ghiChu);
        log.ketQua = dat ? DAT : KHONG_DAT;
        return log;
    }

    public static EmergencyKitLog restock(String maHop, KitLogType loai, String maPhieuXuat, String maNhanVien,
                                          LocalDateTime now, String ghiChu) {
        if (loai == KitLogType.KIEM_TRA || maPhieuXuat == null) {
            throw new IllegalArgumentException("Bổ sung / thay thế phải gắn phiếu xuất kho");
        }
        EmergencyKitLog log = base(maHop, loai, maNhanVien, now, ghiChu);
        log.maPhieuXuat = maPhieuXuat;
        return log;
    }

    private static EmergencyKitLog base(String maHop, KitLogType loai, String maNhanVien, LocalDateTime now,
                                        String ghiChu) {
        EmergencyKitLog log = new EmergencyKitLog();
        log.maHop = maHop;
        log.loai = loai;
        log.maNhanVien = maNhanVien;
        log.thoiDiem = now;
        log.ghiChu = ghiChu;
        return log;
    }

    public void addLine(EmergencyKitLogLine line) {
        line.attachTo(this);
        lines.add(line);
    }

    public List<EmergencyKitLogLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public boolean isDat() {
        return DAT.equals(ketQua);
    }
}
