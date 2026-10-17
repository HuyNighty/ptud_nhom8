package com.quanlynhakhoa.features.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Lô hàng của một thuốc HOẶC một vật tư — bảng dbo.LoHang. Tồn chỉ đổi qua phiếu kho. */
@Entity(name = "LoHangJpaEntity")
@Table(name = "LoHang", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maLo", nullable = false)
    private Integer maLo;

    @Column(name = "maThuoc", length = 10)
    private String maThuoc;

    @Column(name = "maVatTu", length = 10)
    private String maVatTu;

    @Column(name = "soLo", length = 30, nullable = false)
    private String soLo;

    @Column(name = "hanSuDung")
    private LocalDate hanSuDung;

    @Column(name = "maNhaCungCap", length = 10, nullable = false)
    private String maNhaCungCap;

    @Column(name = "soLuongTon", nullable = false)
    private int soLuongTon;

    @Column(name = "ngayNhapDau", nullable = false)
    private LocalDate ngayNhapDau;

    public static StockLot open(String maThuoc, String maVatTu, String soLo, LocalDate hanSuDung,
                                String maNhaCungCap, LocalDate ngayNhap) {
        StockLot lot = new StockLot();
        lot.maThuoc = maThuoc;
        lot.maVatTu = maVatTu;
        lot.soLo = soLo;
        lot.hanSuDung = hanSuDung;
        lot.maNhaCungCap = maNhaCungCap;
        lot.ngayNhapDau = ngayNhap;
        return lot;
    }

    public void receive(int soLuong) {
        if (soLuong <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
        }
        soLuongTon = Math.addExact(soLuongTon, soLuong);
    }

    /** Trừ tồn khi xuất kho; không cho phép âm tồn. */
    public void issue(int soLuong) {
        if (soLuong <= 0) {
            throw new IllegalArgumentException("Số lượng xuất phải lớn hơn 0");
        }
        if (soLuong > soLuongTon) {
            throw new IllegalStateException("Lô " + soLo + " chỉ còn " + soLuongTon);
        }
        soLuongTon -= soLuong;
    }

    /** Mã mặt hàng (thuốc hoặc vật tư) của lô. */
    public String maMatHang() {
        return maThuoc != null ? maThuoc : maVatTu;
    }
}
