package com.quanlynhakhoa.features.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Một dòng hàng của phiếu nhập — bảng dbo.ChiTietPhieuNhap. */
@Entity(name = "ChiTietPhieuNhapJpaEntity")
@Table(name = "ChiTietPhieuNhap", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockInLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maChiTiet", nullable = false)
    private Integer maChiTiet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maPhieuNhap", nullable = false)
    @Getter(AccessLevel.NONE)
    private StockInReceipt receipt;

    @Column(name = "maThuoc", length = 10)
    private String maThuoc;

    @Column(name = "maVatTu", length = 10)
    private String maVatTu;

    @Column(name = "soLo", length = 30, nullable = false)
    private String soLo;

    @Column(name = "hanSuDung")
    private LocalDate hanSuDung;

    @Column(name = "soLuong", nullable = false)
    private int soLuong;

    @Column(name = "donGiaNhap", precision = 18, scale = 0, nullable = false)
    private BigDecimal donGiaNhap;

    @Column(name = "maLo")
    private Integer maLo;

    public static StockInLine of(String maThuoc, String maVatTu, String soLo, LocalDate hanSuDung,
                                 int soLuong, BigDecimal donGiaNhap) {
        StockInLine line = new StockInLine();
        line.maThuoc = maThuoc;
        line.maVatTu = maVatTu;
        line.soLo = soLo;
        line.hanSuDung = hanSuDung;
        line.soLuong = soLuong;
        line.donGiaNhap = donGiaNhap;
        return line;
    }

    void attachTo(StockInReceipt owner) {
        this.receipt = owner;
    }

    public void linkLot(Integer lotId) {
        this.maLo = lotId;
    }

    public BigDecimal thanhTien() {
        return donGiaNhap.multiply(BigDecimal.valueOf(soLuong));
    }

    public String maMatHang() {
        return maThuoc != null ? maThuoc : maVatTu;
    }
}
