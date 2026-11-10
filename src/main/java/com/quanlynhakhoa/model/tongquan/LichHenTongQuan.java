package com.quanlynhakhoa.model.tongquan;












import java.util.*;
import java.time.LocalDateTime;



/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class LichHenTongQuan {
    private final String ma;
    private final LocalDateTime thoiGian;
    private final String benhNhan;
    private final String bacSi;
    private final String trangThai;

    public LichHenTongQuan(String ma, LocalDateTime thoiGian, String benhNhan, String bacSi, String trangThai) {
        this.ma = ma;
        this.thoiGian = thoiGian;
        this.benhNhan = benhNhan;
        this.bacSi = bacSi;
        this.trangThai = trangThai;
    }

    public String id() { return ma; }
    public LocalDateTime time() { return thoiGian; }
    public String patient() { return benhNhan; }
    public String doctor() { return bacSi; }
    public String status() { return trangThai; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LichHenTongQuan value)) return false;
        return java.util.Objects.equals(ma, value.ma)
                && java.util.Objects.equals(thoiGian, value.thoiGian)
                && java.util.Objects.equals(benhNhan, value.benhNhan)
                && java.util.Objects.equals(bacSi, value.bacSi)
                && java.util.Objects.equals(trangThai, value.trangThai);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.ma);
        result = 31 * result + java.util.Objects.hashCode(this.thoiGian);
        result = 31 * result + java.util.Objects.hashCode(this.benhNhan);
        result = 31 * result + java.util.Objects.hashCode(this.bacSi);
        result = 31 * result + java.util.Objects.hashCode(this.trangThai);
        return result;
    }

    @Override
    public String toString() {
        return "Appointment[" + "id=" + ma + ", " + "time=" + thoiGian + ", " + "patient=" + benhNhan + ", " + "doctor=" + bacSi + ", " + "status=" + trangThai + "]";
    }
}
