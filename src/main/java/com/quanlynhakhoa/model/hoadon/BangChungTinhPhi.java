package com.quanlynhakhoa.model.hoadon;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class BangChungTinhPhi {
    private final String loai;
    private final String maNguon;
    private final String maDichVu;
    private final String ten;
    private final BigDecimal donGia;
    private final boolean dangHoatDong;
    private final boolean laDichVuLamSang;

    public BangChungTinhPhi(String loai, String maNguon, String maDichVu, String ten, BigDecimal donGia, boolean dangHoatDong, boolean laDichVuLamSang) {
        this.loai = loai;
        this.maNguon = maNguon;
        this.maDichVu = maDichVu;
        this.ten = ten;
        this.donGia = donGia;
        this.dangHoatDong = dangHoatDong;
        this.laDichVuLamSang = laDichVuLamSang;
    }

    public String kind() { return loai; }
    public String sourceId() { return maNguon; }
    public String serviceId() { return maDichVu; }
    public String name() { return ten; }
    public BigDecimal price() { return donGia; }
    public boolean active() { return dangHoatDong; }
    public boolean clinicalService() { return laDichVuLamSang; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BangChungTinhPhi value)) return false;
        return java.util.Objects.equals(loai, value.loai)
                && java.util.Objects.equals(maNguon, value.maNguon)
                && java.util.Objects.equals(maDichVu, value.maDichVu)
                && java.util.Objects.equals(ten, value.ten)
                && java.util.Objects.equals(donGia, value.donGia)
                && dangHoatDong == value.dangHoatDong
                && laDichVuLamSang == value.laDichVuLamSang;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(loai);
        result = 31 * result + java.util.Objects.hashCode(maNguon);
        result = 31 * result + java.util.Objects.hashCode(maDichVu);
        result = 31 * result + java.util.Objects.hashCode(ten);
        result = 31 * result + java.util.Objects.hashCode(donGia);
        result = 31 * result + Boolean.hashCode(dangHoatDong);
        result = 31 * result + Boolean.hashCode(laDichVuLamSang);
        return result;
    }

    @Override
    public String toString() {
        return "Evidence[" + "kind=" + loai + ", " + "sourceId=" + maNguon + ", " + "serviceId=" + maDichVu + ", " + "name=" + ten + ", " + "price=" + donGia + ", " + "active=" + dangHoatDong + ", " + "clinicalService=" + laDichVuLamSang + "]";
    }
}
