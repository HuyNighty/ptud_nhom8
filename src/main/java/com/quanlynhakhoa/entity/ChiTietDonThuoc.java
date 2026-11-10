package com.quanlynhakhoa.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.quanlynhakhoa.enums.*;

/** Dữ liệu nghiệp vụ; giao dịch và chuyển trạng thái được điều phối tại Service. */
public final class ChiTietDonThuoc {
    private final String maDonThuoc;
    private final String maThuoc;
    private final int soLuong;
    private final String lieuDung;
    private final String cachDung;
    private final int soNgayDung;

    public ChiTietDonThuoc(String maThuoc, int soLuong, String lieuDung, String cachDung, int soNgayDung) {
        this(null, maThuoc, soLuong, lieuDung, cachDung, soNgayDung);
    }

    public ChiTietDonThuoc(String maDonThuoc, String maThuoc, int soLuong, String lieuDung, String cachDung, int soNgayDung) {
        this.maDonThuoc = maDonThuoc;
        this.maThuoc = maThuoc;
        this.soLuong = soLuong;
        this.lieuDung = lieuDung;
        this.cachDung = cachDung;
        this.soNgayDung = soNgayDung;
    }

    public String getMaDonThuoc() { return maDonThuoc; }
    public String getMaThuoc() { return maThuoc; }
    public int getSoLuong() { return soLuong; }
    public String getLieuDung() { return lieuDung; }
    public String getCachDung() { return cachDung; }
    public int getSoNgayDung() { return soNgayDung; }
}
