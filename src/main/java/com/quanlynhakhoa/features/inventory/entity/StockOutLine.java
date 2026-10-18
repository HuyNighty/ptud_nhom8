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

/** Một dòng phiếu xuất = một lô và số lượng xuất từ lô đó — bảng dbo.ChiTietPhieuXuat. */
@Entity(name = "ChiTietPhieuXuatJpaEntity")
@Table(name = "ChiTietPhieuXuat", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockOutLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maChiTiet", nullable = false)
    private Integer maChiTiet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maPhieuXuat", nullable = false)
    @Getter(AccessLevel.NONE)
    private StockOutReceipt receipt;

    @Column(name = "maLo", nullable = false)
    private Integer maLo;

    @Column(name = "soLuong", nullable = false)
    private int soLuong;

    @Column(name = "donGiaXuat", precision = 18, scale = 0)
    private BigDecimal donGiaXuat;

    public static StockOutLine of(Integer maLo, int soLuong) {
        StockOutLine line = new StockOutLine();
        line.maLo = maLo;
        line.soLuong = soLuong;
        return line;
    }

    void attachTo(StockOutReceipt owner) {
        this.receipt = owner;
    }

    /** Chốt giá vốn khi xuất kho. */
    public void fixUnitCost(BigDecimal donGia) {
        this.donGiaXuat = donGia;
    }

    public BigDecimal thanhTien() {
        return donGiaXuat == null ? BigDecimal.ZERO : donGiaXuat.multiply(BigDecimal.valueOf(soLuong));
    }
}
