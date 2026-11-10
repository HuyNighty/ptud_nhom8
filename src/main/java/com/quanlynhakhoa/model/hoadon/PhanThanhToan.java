package com.quanlynhakhoa.model.hoadon;

import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import java.math.BigDecimal;


/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class PhanThanhToan {
    private final String ma;
    private final BigDecimal soTien;
    private final PhuongThucThanhToan phuongThuc;
    private final String maThamChieu;
    private final BigDecimal tienKhachDua;

    public PhanThanhToan(String ma, BigDecimal soTien, PhuongThucThanhToan phuongThuc, String maThamChieu, BigDecimal tienKhachDua) {
        this.ma = ma;
        this.soTien = soTien;
        this.phuongThuc = phuongThuc;
        this.maThamChieu = maThamChieu;
        this.tienKhachDua = tienKhachDua;
    }

    public String id() { return ma; }
    public BigDecimal amount() { return soTien; }
    public PhuongThucThanhToan method() { return phuongThuc; }
    public String reference() { return maThamChieu; }
    public BigDecimal customerTender() { return tienKhachDua; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PhanThanhToan value)) return false;
        return java.util.Objects.equals(ma, value.ma)
                && java.util.Objects.equals(soTien, value.soTien)
                && java.util.Objects.equals(phuongThuc, value.phuongThuc)
                && java.util.Objects.equals(maThamChieu, value.maThamChieu)
                && java.util.Objects.equals(tienKhachDua, value.tienKhachDua);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(ma);
        result = 31 * result + java.util.Objects.hashCode(soTien);
        result = 31 * result + java.util.Objects.hashCode(phuongThuc);
        result = 31 * result + java.util.Objects.hashCode(maThamChieu);
        result = 31 * result + java.util.Objects.hashCode(tienKhachDua);
        return result;
    }

    @Override
    public String toString() {
        return "Part[" + "id=" + ma + ", " + "amount=" + soTien + ", " + "method=" + phuongThuc + ", " + "reference=" + maThamChieu + ", " + "customerTender=" + tienKhachDua + "]";
    }
}
