package com.quanlynhakhoa.model.baocao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class BaoCaoDoanhThu {
    private final LocalDate tuNgay;
    private final LocalDate denNgay;
    private final boolean theoThang;
    private final List<DongBaoCaoDoanhThu> danhSach;
    private final BigDecimal doanhThu;
    private final BigDecimal daThu;
    private final BigDecimal congNoCuoiKy;
    private final int hoaDon;

    public BaoCaoDoanhThu(LocalDate tuNgay, LocalDate denNgay, boolean theoThang, List<DongBaoCaoDoanhThu> danhSach, BigDecimal doanhThu, BigDecimal daThu, BigDecimal congNoCuoiKy, int hoaDon) {
        danhSach = List.copyOf(danhSach);
        this.tuNgay = tuNgay;
        this.denNgay = denNgay;
        this.theoThang = theoThang;
        this.danhSach = danhSach;
        this.doanhThu = doanhThu;
        this.daThu = daThu;
        this.congNoCuoiKy = congNoCuoiKy;
        this.hoaDon = hoaDon;
    }

    public LocalDate from() { return tuNgay; }
    public LocalDate to() { return denNgay; }
    public boolean monthly() { return theoThang; }
    public List<DongBaoCaoDoanhThu> rows() { return danhSach; }
    public BigDecimal revenue() { return doanhThu; }
    public BigDecimal collected() { return daThu; }
    public BigDecimal closingDebt() { return congNoCuoiKy; }
    public int invoices() { return hoaDon; }




    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BaoCaoDoanhThu value)) return false;
        return java.util.Objects.equals(tuNgay, value.tuNgay)
                && java.util.Objects.equals(denNgay, value.denNgay)
                && theoThang == value.theoThang
                && java.util.Objects.equals(danhSach, value.danhSach)
                && java.util.Objects.equals(doanhThu, value.doanhThu)
                && java.util.Objects.equals(daThu, value.daThu)
                && java.util.Objects.equals(congNoCuoiKy, value.congNoCuoiKy)
                && hoaDon == value.hoaDon;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.tuNgay);
        result = 31 * result + java.util.Objects.hashCode(this.denNgay);
        result = 31 * result + Boolean.hashCode(this.theoThang);
        result = 31 * result + java.util.Objects.hashCode(this.danhSach);
        result = 31 * result + java.util.Objects.hashCode(this.doanhThu);
        result = 31 * result + java.util.Objects.hashCode(this.daThu);
        result = 31 * result + java.util.Objects.hashCode(this.congNoCuoiKy);
        result = 31 * result + Integer.hashCode(this.hoaDon);
        return result;
    }

    @Override
    public String toString() {
        return "RevenueReport[" + "from=" + tuNgay + ", " + "to=" + denNgay + ", " + "monthly=" + theoThang + ", " + "rows=" + danhSach + ", " + "revenue=" + doanhThu + ", " + "collected=" + daThu + ", " + "closingDebt=" + congNoCuoiKy + ", " + "invoices=" + hoaDon + "]";
    }
}
