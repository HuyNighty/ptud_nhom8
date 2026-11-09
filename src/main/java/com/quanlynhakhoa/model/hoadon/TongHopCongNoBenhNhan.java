package com.quanlynhakhoa.model.hoadon;

import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import java.math.BigDecimal;
import java.util.List;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class TongHopCongNoBenhNhan {
    private final String maBenhNhan;
    private final String ten;
    private final BigDecimal tongTien;
    private final BigDecimal daThu;
    private final BigDecimal congNo;
    private final List<ThongTinHoaDon> hoaDon;

    public TongHopCongNoBenhNhan(String maBenhNhan, String ten, BigDecimal tongTien, BigDecimal daThu, BigDecimal congNo, List<ThongTinHoaDon> hoaDon) {
        hoaDon=List.copyOf(hoaDon);
        this.maBenhNhan = maBenhNhan;
        this.ten = ten;
        this.tongTien = tongTien;
        this.daThu = daThu;
        this.congNo = congNo;
        this.hoaDon = hoaDon;
    }

    public String patientId() { return maBenhNhan; }
    public String name() { return ten; }
    public BigDecimal total() { return tongTien; }
    public BigDecimal collected() { return daThu; }
    public BigDecimal debt() { return congNo; }
    public List<ThongTinHoaDon> invoices() { return hoaDon; }



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TongHopCongNoBenhNhan value)) return false;
        return java.util.Objects.equals(maBenhNhan, value.maBenhNhan)
                && java.util.Objects.equals(ten, value.ten)
                && java.util.Objects.equals(tongTien, value.tongTien)
                && java.util.Objects.equals(daThu, value.daThu)
                && java.util.Objects.equals(congNo, value.congNo)
                && java.util.Objects.equals(hoaDon, value.hoaDon);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(ten);
        result = 31 * result + java.util.Objects.hashCode(tongTien);
        result = 31 * result + java.util.Objects.hashCode(daThu);
        result = 31 * result + java.util.Objects.hashCode(congNo);
        result = 31 * result + java.util.Objects.hashCode(hoaDon);
        return result;
    }

    @Override
    public String toString() {
        return "PatientDebt[" + "patientId=" + maBenhNhan + ", " + "name=" + ten + ", " + "total=" + tongTien + ", " + "collected=" + daThu + ", " + "debt=" + congNo + ", " + "invoices=" + hoaDon + "]";
    }
}
