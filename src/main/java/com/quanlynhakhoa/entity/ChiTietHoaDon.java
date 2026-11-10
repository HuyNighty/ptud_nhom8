package com.quanlynhakhoa.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.quanlynhakhoa.enums.*;

/** Dữ liệu nghiệp vụ; giao dịch và chuyển trạng thái được điều phối tại Service. */
public final class ChiTietHoaDon {
    private final int maChiTiet;
    private final String maHoaDon;
    private final String maDichVu;
    private final String maThuoc;
    private final String maVatTu;
    private final int soLuong;
    private final BigDecimal donGiaApDung;

    public ChiTietHoaDon(int maChiTiet, String maHoaDon, String maDichVu, String maThuoc, String maVatTu, int soLuong, BigDecimal donGiaApDung) {

        int kinds = (maDichVu == null ? 0 : 1) + (maThuoc == null ? 0 : 1) + (maVatTu == null ? 0 : 1);
        if (kinds != 1 || soLuong <= 0 || donGiaApDung == null || donGiaApDung.signum() < 0
        || donGiaApDung.stripTrailingZeros().scale() > 0) {
        throw new IllegalArgumentException("Chi tiết hóa đơn bất nhất: loại hàng, số lượng hoặc giá chốt.");
        }
        this.maChiTiet = maChiTiet;
        this.maHoaDon = maHoaDon;
        this.maDichVu = maDichVu;
        this.maThuoc = maThuoc;
        this.maVatTu = maVatTu;
        this.soLuong = soLuong;
        this.donGiaApDung = donGiaApDung;
    }

    public int getMaChiTiet() { return maChiTiet; }
    public String getMaHoaDon() { return maHoaDon; }
    public String getMaDichVu() { return maDichVu; }
    public String getMaThuoc() { return maThuoc; }
    public String getMaVatTu() { return maVatTu; }
    public int getSoLuong() { return soLuong; }
    public BigDecimal getDonGiaApDung() { return donGiaApDung; }
}
