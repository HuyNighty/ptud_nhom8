package com.quanlynhakhoa.model.hoadon;

import com.quanlynhakhoa.model.hoadon.ThongTinThanhToan;
import com.quanlynhakhoa.model.hoadon.ThuocKeTinhTien;
import com.quanlynhakhoa.model.hoadon.DongChiTietHoaDon;
import java.util.List;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DuLieuChiTietHoaDon {
    private final ThongTinHoaDon hoaDon;
    private final List<DongChiTietHoaDon> chiTiet;
    private final List<ThongTinThanhToan> thanhToan;
    private final List<ThongTinYeuCauHuyHoaDon> lichSuYeuCauHuy;
    private final List<ThuocKeTinhTien> thuocDaKe;

    public DuLieuChiTietHoaDon(ThongTinHoaDon hoaDon, List<DongChiTietHoaDon> chiTiet, List<ThongTinThanhToan> thanhToan, List<ThongTinYeuCauHuyHoaDon> lichSuYeuCauHuy, List<ThuocKeTinhTien> thuocDaKe) {
        chiTiet = List.copyOf(chiTiet); thanhToan = List.copyOf(thanhToan); lichSuYeuCauHuy = List.copyOf(lichSuYeuCauHuy); thuocDaKe = List.copyOf(thuocDaKe);
        this.hoaDon = hoaDon;
        this.chiTiet = chiTiet;
        this.thanhToan = thanhToan;
        this.lichSuYeuCauHuy = lichSuYeuCauHuy;
        this.thuocDaKe = thuocDaKe;
    }

    public ThongTinHoaDon hoaDon() { return hoaDon; }
    public List<DongChiTietHoaDon> chiTiet() { return chiTiet; }
    public List<ThongTinThanhToan> thanhToan() { return thanhToan; }
    public List<ThongTinYeuCauHuyHoaDon> cancellationHistory() { return lichSuYeuCauHuy; }
    public List<ThuocKeTinhTien> prescribedMedicines() { return thuocDaKe; }

    public DuLieuChiTietHoaDon(ThongTinHoaDon hoaDon, List<DongChiTietHoaDon> chiTiet, List<ThongTinThanhToan> thanhToan) {
        this(hoaDon, chiTiet, thanhToan, List.of());
    }
    public DuLieuChiTietHoaDon(ThongTinHoaDon hoaDon, List<DongChiTietHoaDon> chiTiet, List<ThongTinThanhToan> thanhToan,
                          List<ThongTinYeuCauHuyHoaDon> lichSuYeuCauHuy) {
        this(hoaDon, chiTiet, thanhToan, lichSuYeuCauHuy, List.of());
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DuLieuChiTietHoaDon value)) return false;
        return java.util.Objects.equals(hoaDon, value.hoaDon)
                && java.util.Objects.equals(chiTiet, value.chiTiet)
                && java.util.Objects.equals(thanhToan, value.thanhToan)
                && java.util.Objects.equals(lichSuYeuCauHuy, value.lichSuYeuCauHuy)
                && java.util.Objects.equals(thuocDaKe, value.thuocDaKe);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(hoaDon);
        result = 31 * result + java.util.Objects.hashCode(chiTiet);
        result = 31 * result + java.util.Objects.hashCode(thanhToan);
        result = 31 * result + java.util.Objects.hashCode(lichSuYeuCauHuy);
        result = 31 * result + java.util.Objects.hashCode(thuocDaKe);
        return result;
    }

    @Override
    public String toString() {
        return "InvoiceDetails[" + "hoaDon=" + hoaDon + ", " + "chiTiet=" + chiTiet + ", " + "thanhToan=" + thanhToan + ", " + "cancellationHistory=" + lichSuYeuCauHuy + ", " + "prescribedMedicines=" + thuocDaKe + "]";
    }
}
