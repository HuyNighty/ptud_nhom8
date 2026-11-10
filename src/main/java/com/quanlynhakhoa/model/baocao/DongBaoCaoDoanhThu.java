package com.quanlynhakhoa.model.baocao;

import java.math.BigDecimal;
import java.time.LocalDate;


/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DongBaoCaoDoanhThu {
    private final LocalDate kyBaoCao;
    private final int hoaDon;
    private final BigDecimal doanhThu;
    private final BigDecimal daThu;

    public DongBaoCaoDoanhThu(LocalDate kyBaoCao, int hoaDon, BigDecimal doanhThu, BigDecimal daThu) {
        this.kyBaoCao = kyBaoCao;
        this.hoaDon = hoaDon;
        this.doanhThu = doanhThu;
        this.daThu = daThu;
    }

    public LocalDate period() { return kyBaoCao; }
    public int invoices() { return hoaDon; }
    public BigDecimal revenue() { return doanhThu; }
    public BigDecimal collected() { return daThu; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongBaoCaoDoanhThu value)) return false;
        return java.util.Objects.equals(kyBaoCao, value.kyBaoCao)
                && hoaDon == value.hoaDon
                && java.util.Objects.equals(doanhThu, value.doanhThu)
                && java.util.Objects.equals(daThu, value.daThu);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.kyBaoCao);
        result = 31 * result + Integer.hashCode(this.hoaDon);
        result = 31 * result + java.util.Objects.hashCode(this.doanhThu);
        result = 31 * result + java.util.Objects.hashCode(this.daThu);
        return result;
    }

    @Override
    public String toString() {
        return "Row[" + "period=" + kyBaoCao + ", " + "invoices=" + hoaDon + ", " + "revenue=" + doanhThu + ", " + "collected=" + daThu + "]";
    }
}
