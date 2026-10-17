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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Một dòng phiếu hoàn trả = một lô đã xuất và số lượng trả về — bảng dbo.ChiTietPhieuHoanTra. */
@Entity(name = "ChiTietPhieuHoanTraJpaEntity")
@Table(name = "ChiTietPhieuHoanTra", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReturnLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maChiTiet", nullable = false)
    private Integer maChiTiet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maPhieuHoanTra", nullable = false)
    @Getter(AccessLevel.NONE)
    private StockReturnReceipt receipt;

    @Column(name = "maLo", nullable = false)
    private Integer maLo;

    @Column(name = "soLuong", nullable = false)
    private int soLuong;

    @Column(name = "donGia", precision = 18, scale = 0, nullable = false)
    private BigDecimal donGia;

    public static StockReturnLine of(Integer maLo, int soLuong, BigDecimal donGia) {
        StockReturnLine line = new StockReturnLine();
        line.maLo = maLo;
        line.soLuong = soLuong;
        line.donGia = donGia;
        return line;
    }

    void attachTo(StockReturnReceipt owner) {
        this.receipt = owner;
    }

    public BigDecimal thanhTien() {
        return donGia.multiply(BigDecimal.valueOf(soLuong));
    }
}
