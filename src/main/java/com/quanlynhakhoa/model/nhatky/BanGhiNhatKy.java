package com.quanlynhakhoa.model.nhatky;

import java.time.LocalDateTime;
import java.util.Objects;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class BanGhiNhatKy {
    private final long ma;
    private final LocalDateTime thoiGianUtc;
    private final String loaiChuThe;
    private final String maTaiKhoan;
    private final String chucNang;
    private final String hanhDong;
    private final String ketQua;
    private final String loaiDoiTuong;
    private final String maDoiTuong;

    public BanGhiNhatKy(long ma, LocalDateTime thoiGianUtc, String loaiChuThe, String maTaiKhoan, String chucNang, String hanhDong, String ketQua, String loaiDoiTuong, String maDoiTuong) {
        if (ma <= 0) throw new IllegalArgumentException("Invalid audit ID");
        Objects.requireNonNull(thoiGianUtc, "utcTime");
        Objects.requireNonNull(loaiChuThe, "actorType");
        Objects.requireNonNull(chucNang, "function");
        Objects.requireNonNull(hanhDong, "action");
        Objects.requireNonNull(ketQua, "result");
        this.ma = ma;
        this.thoiGianUtc = thoiGianUtc;
        this.loaiChuThe = loaiChuThe;
        this.maTaiKhoan = maTaiKhoan;
        this.chucNang = chucNang;
        this.hanhDong = hanhDong;
        this.ketQua = ketQua;
        this.loaiDoiTuong = loaiDoiTuong;
        this.maDoiTuong = maDoiTuong;
    }

    public long id() { return ma; }
    public LocalDateTime utcTime() { return thoiGianUtc; }
    public String actorType() { return loaiChuThe; }
    public String accountId() { return maTaiKhoan; }
    public String function() { return chucNang; }
    public String action() { return hanhDong; }
    public String result() { return ketQua; }
    public String targetType() { return loaiDoiTuong; }
    public String targetId() { return maDoiTuong; }



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BanGhiNhatKy value)) return false;
        return ma == value.ma
                && java.util.Objects.equals(thoiGianUtc, value.thoiGianUtc)
                && java.util.Objects.equals(loaiChuThe, value.loaiChuThe)
                && java.util.Objects.equals(maTaiKhoan, value.maTaiKhoan)
                && java.util.Objects.equals(chucNang, value.chucNang)
                && java.util.Objects.equals(hanhDong, value.hanhDong)
                && java.util.Objects.equals(ketQua, value.ketQua)
                && java.util.Objects.equals(loaiDoiTuong, value.loaiDoiTuong)
                && java.util.Objects.equals(maDoiTuong, value.maDoiTuong);
    }

    @Override
    public int hashCode() {
        int ketQua = 0;
        ketQua = 31 * ketQua + Long.hashCode(this.ma);
        ketQua = 31 * ketQua + java.util.Objects.hashCode(this.thoiGianUtc);
        ketQua = 31 * ketQua + java.util.Objects.hashCode(this.loaiChuThe);
        ketQua = 31 * ketQua + java.util.Objects.hashCode(this.maTaiKhoan);
        ketQua = 31 * ketQua + java.util.Objects.hashCode(this.chucNang);
        ketQua = 31 * ketQua + java.util.Objects.hashCode(this.hanhDong);
        ketQua = 31 * ketQua + java.util.Objects.hashCode(this.ketQua);
        ketQua = 31 * ketQua + java.util.Objects.hashCode(this.loaiDoiTuong);
        ketQua = 31 * ketQua + java.util.Objects.hashCode(this.maDoiTuong);
        return ketQua;
    }

    @Override
    public String toString() {
        return "AuditEntry[" + "id=" + ma + ", " + "utcTime=" + thoiGianUtc + ", " + "actorType=" + loaiChuThe + ", " + "accountId=" + maTaiKhoan + ", " + "function=" + chucNang + ", " + "action=" + hanhDong + ", " + "result=" + ketQua + ", " + "targetType=" + loaiDoiTuong + ", " + "targetId=" + maDoiTuong + "]";
    }
}
