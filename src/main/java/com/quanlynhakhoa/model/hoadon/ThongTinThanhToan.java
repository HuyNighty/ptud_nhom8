package com.quanlynhakhoa.model.hoadon;

import com.quanlynhakhoa.enums.TrangThaiThanhToan;
import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class ThongTinThanhToan {
    private final String maThanhToan;
    private final String maHoaDon;
    private final LocalDateTime thoiGian;
    private final BigDecimal soTien;
    private final PhuongThucThanhToan phuongThuc;
    private final String maGiaoDich;
    private final BigDecimal tienKhachDua;
    private final TrangThaiThanhToan trangThai;
    private final String maNhanVien;
    private final String maTaiKhoanHuy;
    private final LocalDateTime thoiGianHuy;
    private final String lyDoHuy;

    public ThongTinThanhToan(String maThanhToan, String maHoaDon, LocalDateTime thoiGian, BigDecimal soTien, PhuongThucThanhToan phuongThuc, String maGiaoDich, BigDecimal tienKhachDua, TrangThaiThanhToan trangThai, String maNhanVien, String maTaiKhoanHuy, LocalDateTime thoiGianHuy, String lyDoHuy) {
        if (soTien == null || soTien.signum() <= 0 || soTien.stripTrailingZeros().scale() > 0
        || trangThai == null || phuongThuc == null) throw new IllegalArgumentException("Lần thu bất nhất.");
        this.maThanhToan = maThanhToan;
        this.maHoaDon = maHoaDon;
        this.thoiGian = thoiGian;
        this.soTien = soTien;
        this.phuongThuc = phuongThuc;
        this.maGiaoDich = maGiaoDich;
        this.tienKhachDua = tienKhachDua;
        this.trangThai = trangThai;
        this.maNhanVien = maNhanVien;
        this.maTaiKhoanHuy = maTaiKhoanHuy;
        this.thoiGianHuy = thoiGianHuy;
        this.lyDoHuy = lyDoHuy;
    }

    public String maThanhToan() { return maThanhToan; }
    public String maHoaDon() { return maHoaDon; }
    public LocalDateTime thoiGian() { return thoiGian; }
    public BigDecimal soTien() { return soTien; }
    public PhuongThucThanhToan phuongThuc() { return phuongThuc; }
    public String maGiaoDich() { return maGiaoDich; }
    public BigDecimal tienKhachDua() { return tienKhachDua; }
    public TrangThaiThanhToan trangThai() { return trangThai; }
    public String maNhanVien() { return maNhanVien; }
    public String maTaiKhoanHuy() { return maTaiKhoanHuy; }
    public LocalDateTime thoiGianHuy() { return thoiGianHuy; }
    public String lyDoHuy() { return lyDoHuy; }





    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinThanhToan value)) return false;
        return java.util.Objects.equals(maThanhToan, value.maThanhToan)
                && java.util.Objects.equals(maHoaDon, value.maHoaDon)
                && java.util.Objects.equals(thoiGian, value.thoiGian)
                && java.util.Objects.equals(soTien, value.soTien)
                && java.util.Objects.equals(phuongThuc, value.phuongThuc)
                && java.util.Objects.equals(maGiaoDich, value.maGiaoDich)
                && java.util.Objects.equals(tienKhachDua, value.tienKhachDua)
                && java.util.Objects.equals(trangThai, value.trangThai)
                && java.util.Objects.equals(maNhanVien, value.maNhanVien)
                && java.util.Objects.equals(maTaiKhoanHuy, value.maTaiKhoanHuy)
                && java.util.Objects.equals(thoiGianHuy, value.thoiGianHuy)
                && java.util.Objects.equals(lyDoHuy, value.lyDoHuy);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maThanhToan);
        result = 31 * result + java.util.Objects.hashCode(maHoaDon);
        result = 31 * result + java.util.Objects.hashCode(thoiGian);
        result = 31 * result + java.util.Objects.hashCode(soTien);
        result = 31 * result + java.util.Objects.hashCode(phuongThuc);
        result = 31 * result + java.util.Objects.hashCode(maGiaoDich);
        result = 31 * result + java.util.Objects.hashCode(tienKhachDua);
        result = 31 * result + java.util.Objects.hashCode(trangThai);
        result = 31 * result + java.util.Objects.hashCode(maNhanVien);
        result = 31 * result + java.util.Objects.hashCode(maTaiKhoanHuy);
        result = 31 * result + java.util.Objects.hashCode(thoiGianHuy);
        result = 31 * result + java.util.Objects.hashCode(lyDoHuy);
        return result;
    }

    @Override
    public String toString() {
        return "Payment[" + "maThanhToan=" + maThanhToan + ", " + "maHoaDon=" + maHoaDon + ", " + "thoiGian=" + thoiGian + ", " + "soTien=" + soTien + ", " + "phuongThuc=" + phuongThuc + ", " + "maGiaoDich=" + maGiaoDich + ", " + "tienKhachDua=" + tienKhachDua + ", " + "trangThai=" + trangThai + ", " + "maNhanVien=" + maNhanVien + ", " + "maTaiKhoanHuy=" + maTaiKhoanHuy + ", " + "thoiGianHuy=" + thoiGianHuy + ", " + "lyDoHuy=" + lyDoHuy + "]";
    }
}
