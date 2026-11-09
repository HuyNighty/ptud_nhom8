package com.quanlynhakhoa.model.tongquan;






import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;




/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class SoLieuNgay {
    private final LocalDate ngay;
    private final Long luotKham;
    private final BigDecimal daThu;

    public SoLieuNgay(LocalDate ngay, Long luotKham, BigDecimal daThu) {
        this.ngay = ngay;
        this.luotKham = luotKham;
        this.daThu = daThu;
    }

    public LocalDate date() { return ngay; }
    public Long visits() { return luotKham; }
    public BigDecimal collected() { return daThu; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SoLieuNgay value)) return false;
        return java.util.Objects.equals(ngay, value.ngay)
                && java.util.Objects.equals(luotKham, value.luotKham)
                && java.util.Objects.equals(daThu, value.daThu);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.ngay);
        result = 31 * result + java.util.Objects.hashCode(this.luotKham);
        result = 31 * result + java.util.Objects.hashCode(this.daThu);
        return result;
    }

    @Override
    public String toString() {
        return "Day[" + "date=" + ngay + ", " + "visits=" + luotKham + ", " + "collected=" + daThu + "]";
    }
}
