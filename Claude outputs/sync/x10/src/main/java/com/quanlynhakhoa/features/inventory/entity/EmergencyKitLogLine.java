package com.quanlynhakhoa.features.inventory.entity;

import com.quanlynhakhoa.features.inventory.enums.KitCheckResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Một mặt hàng trong nhật ký hộp cấp cứu — bảng dbo.ChiTietNhatKyHopCapCuu. */
@Entity(name = "ChiTietNhatKyHopCapCuuJpaEntity")
@Table(name = "ChiTietNhatKyHopCapCuu", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmergencyKitLogLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maChiTiet", nullable = false)
    private Integer maChiTiet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maNhatKy", nullable = false)
    @Getter(AccessLevel.NONE)
    private EmergencyKitLog log;

    @Column(name = "maThuoc", length = 10)
    private String maThuoc;

    @Column(name = "maVatTu", length = 10)
    private String maVatTu;

    @Column(name = "soLuongTruoc", nullable = false)
    private int soLuongTruoc;

    @Column(name = "soLuongSau", nullable = false)
    private int soLuongSau;

    @Column(name = "hanDungSau")
    private LocalDate hanDungSau;

    @Enumerated(EnumType.STRING)
    @Column(name = "ketQua", length = 20)
    private KitCheckResult ketQua;

    public static EmergencyKitLogLine of(String maThuoc, String maVatTu, int truoc, int sau, LocalDate hanSau,
                                         KitCheckResult ketQua) {
        EmergencyKitLogLine line = new EmergencyKitLogLine();
        line.maThuoc = maThuoc;
        line.maVatTu = maVatTu;
        line.soLuongTruoc = truoc;
        line.soLuongSau = sau;
        line.hanDungSau = hanSau;
        line.ketQua = ketQua;
        return line;
    }

    void attachTo(EmergencyKitLog owner) {
        this.log = owner;
    }

    public String maMatHang() {
        return maThuoc != null ? maThuoc : maVatTu;
    }
}
