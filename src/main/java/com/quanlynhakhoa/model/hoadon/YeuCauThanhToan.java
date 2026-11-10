package com.quanlynhakhoa.model.hoadon;

import java.math.BigDecimal;
import java.util.List;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class YeuCauThanhToan {
    private final String maHoaDon;
    private final BigDecimal soTien;
    private final List<PhanThanhToan> cacPhanThanhToan;
    private final boolean daXacNhanChuyenKhoan;

    public YeuCauThanhToan(String maHoaDon, BigDecimal soTien, List<PhanThanhToan> cacPhanThanhToan, boolean daXacNhanChuyenKhoan) {
        cacPhanThanhToan = List.copyOf(cacPhanThanhToan);
        this.maHoaDon = maHoaDon;
        this.soTien = soTien;
        this.cacPhanThanhToan = cacPhanThanhToan;
        this.daXacNhanChuyenKhoan = daXacNhanChuyenKhoan;
    }

    public String invoiceId() { return maHoaDon; }
    public BigDecimal amount() { return soTien; }
    public List<PhanThanhToan> parts() { return cacPhanThanhToan; }
    public boolean transferConfirmed() { return daXacNhanChuyenKhoan; }




    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof YeuCauThanhToan value)) return false;
        return java.util.Objects.equals(maHoaDon, value.maHoaDon)
                && java.util.Objects.equals(soTien, value.soTien)
                && java.util.Objects.equals(cacPhanThanhToan, value.cacPhanThanhToan)
                && daXacNhanChuyenKhoan == value.daXacNhanChuyenKhoan;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maHoaDon);
        result = 31 * result + java.util.Objects.hashCode(soTien);
        result = 31 * result + java.util.Objects.hashCode(cacPhanThanhToan);
        result = 31 * result + Boolean.hashCode(daXacNhanChuyenKhoan);
        return result;
    }

    @Override
    public String toString() {
        return "PaymentRequest[" + "invoiceId=" + maHoaDon + ", " + "amount=" + soTien + ", " + "parts=" + cacPhanThanhToan + ", " + "transferConfirmed=" + daXacNhanChuyenKhoan + "]";
    }
}
