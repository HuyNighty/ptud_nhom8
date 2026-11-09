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
    private final List<ThongTinYeuCauHuyHoaDon> cancellationHistory;
    private final List<ThuocKeTinhTien> prescribedMedicines;

    public DuLieuChiTietHoaDon(ThongTinHoaDon hoaDon, List<DongChiTietHoaDon> chiTiet, List<ThongTinThanhToan> thanhToan, List<ThongTinYeuCauHuyHoaDon> cancellationHistory, List<ThuocKeTinhTien> prescribedMedicines) {
        chiTiet = List.copyOf(chiTiet); thanhToan = List.copyOf(thanhToan); cancellationHistory = List.copyOf(cancellationHistory); prescribedMedicines = List.copyOf(prescribedMedicines);
        this.hoaDon = hoaDon;
        this.chiTiet = chiTiet;
        this.thanhToan = thanhToan;
        this.cancellationHistory = cancellationHistory;
        this.prescribedMedicines = prescribedMedicines;
    }

    public ThongTinHoaDon hoaDon() { return hoaDon; }
    public List<DongChiTietHoaDon> chiTiet() { return chiTiet; }
    public List<ThongTinThanhToan> thanhToan() { return thanhToan; }
    public List<ThongTinYeuCauHuyHoaDon> cancellationHistory() { return cancellationHistory; }
    public List<ThuocKeTinhTien> prescribedMedicines() { return prescribedMedicines; }

    public DuLieuChiTietHoaDon(ThongTinHoaDon hoaDon, List<DongChiTietHoaDon> chiTiet, List<ThongTinThanhToan> thanhToan) {
        this(hoaDon, chiTiet, thanhToan, List.of());
    }
    public DuLieuChiTietHoaDon(ThongTinHoaDon hoaDon, List<DongChiTietHoaDon> chiTiet, List<ThongTinThanhToan> thanhToan,
                          List<ThongTinYeuCauHuyHoaDon> cancellationHistory) {
        this(hoaDon, chiTiet, thanhToan, cancellationHistory, List.of());
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DuLieuChiTietHoaDon value)) return false;
        return java.util.Objects.equals(hoaDon, value.hoaDon)
                && java.util.Objects.equals(chiTiet, value.chiTiet)
                && java.util.Objects.equals(thanhToan, value.thanhToan)
                && java.util.Objects.equals(cancellationHistory, value.cancellationHistory)
                && java.util.Objects.equals(prescribedMedicines, value.prescribedMedicines);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(hoaDon);
        result = 31 * result + java.util.Objects.hashCode(chiTiet);
        result = 31 * result + java.util.Objects.hashCode(thanhToan);
        result = 31 * result + java.util.Objects.hashCode(cancellationHistory);
        result = 31 * result + java.util.Objects.hashCode(prescribedMedicines);
        return result;
    }

    @Override
    public String toString() {
        return "InvoiceDetails[" + "hoaDon=" + hoaDon + ", " + "chiTiet=" + chiTiet + ", " + "thanhToan=" + thanhToan + ", " + "cancellationHistory=" + cancellationHistory + ", " + "prescribedMedicines=" + prescribedMedicines + "]";
    }
}
