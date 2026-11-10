package com.quanlynhakhoa.model.nhanvien;

import com.quanlynhakhoa.enums.TrangThaiNhanVien;
import java.time.LocalDate;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class ThongTinNhanVien {
    private final String ma;
    private final String hoTen;
    private final String chucDanh;
    private final TrangThaiNhanVien trangThai;
    private final LocalDate ngayNghiViec;

    public ThongTinNhanVien(String ma, String hoTen, String chucDanh, TrangThaiNhanVien trangThai, LocalDate ngayNghiViec) {
        this.ma = ma;
        this.hoTen = hoTen;
        this.chucDanh = chucDanh;
        this.trangThai = trangThai;
        this.ngayNghiViec = ngayNghiViec;
    }

    public String id() { return ma; }
    public String fullName() { return hoTen; }
    public String jobTitle() { return chucDanh; }
    public TrangThaiNhanVien status() { return trangThai; }
    public LocalDate departureDate() { return ngayNghiViec; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinNhanVien value)) return false;
        return java.util.Objects.equals(ma, value.ma)
                && java.util.Objects.equals(hoTen, value.hoTen)
                && java.util.Objects.equals(chucDanh, value.chucDanh)
                && java.util.Objects.equals(trangThai, value.trangThai)
                && java.util.Objects.equals(ngayNghiViec, value.ngayNghiViec);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(ma);
        result = 31 * result + java.util.Objects.hashCode(hoTen);
        result = 31 * result + java.util.Objects.hashCode(chucDanh);
        result = 31 * result + java.util.Objects.hashCode(trangThai);
        result = 31 * result + java.util.Objects.hashCode(ngayNghiViec);
        return result;
    }

    @Override
    public String toString() {
        return "Employee[" + "id=" + ma + ", " + "fullName=" + hoTen + ", " + "jobTitle=" + chucDanh + ", " + "status=" + trangThai + ", " + "departureDate=" + ngayNghiViec + "]";
    }
}
