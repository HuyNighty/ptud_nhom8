package com.quanlynhakhoa.model.tongquan;






import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;




/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class TomTatTongQuan {
    private final LocalDate ngay;
    private final Long benhNhan;
    private final Long lichHen;
    private final Long phieuKhamDangXuLy;
    private final BigDecimal daThu;

    public TomTatTongQuan(LocalDate ngay, Long benhNhan, Long lichHen, Long phieuKhamDangXuLy, BigDecimal daThu) {
        this.ngay = ngay;
        this.benhNhan = benhNhan;
        this.lichHen = lichHen;
        this.phieuKhamDangXuLy = phieuKhamDangXuLy;
        this.daThu = daThu;
    }

    public LocalDate date() { return ngay; }
    public Long patients() { return benhNhan; }
    public Long appointments() { return lichHen; }
    public Long inProgressExams() { return phieuKhamDangXuLy; }
    public BigDecimal collected() { return daThu; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TomTatTongQuan value)) return false;
        return java.util.Objects.equals(ngay, value.ngay)
                && java.util.Objects.equals(benhNhan, value.benhNhan)
                && java.util.Objects.equals(lichHen, value.lichHen)
                && java.util.Objects.equals(phieuKhamDangXuLy, value.phieuKhamDangXuLy)
                && java.util.Objects.equals(daThu, value.daThu);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.ngay);
        result = 31 * result + java.util.Objects.hashCode(this.benhNhan);
        result = 31 * result + java.util.Objects.hashCode(this.lichHen);
        result = 31 * result + java.util.Objects.hashCode(this.phieuKhamDangXuLy);
        result = 31 * result + java.util.Objects.hashCode(this.daThu);
        return result;
    }

    @Override
    public String toString() {
        return "Summary[" + "date=" + ngay + ", " + "patients=" + benhNhan + ", " + "appointments=" + lichHen + ", " + "inProgressExams=" + phieuKhamDangXuLy + ", " + "collected=" + daThu + "]";
    }
}
