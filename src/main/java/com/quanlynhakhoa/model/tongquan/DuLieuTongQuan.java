package com.quanlynhakhoa.model.tongquan;












import java.util.*;




/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DuLieuTongQuan {
    private final TomTatTongQuan summary;
    private final List<LichHenTongQuan> appointments;
    private final List<CongViecTongQuan> work;
    private final List<CanhBaoTonKho> alerts;
    private final List<SoLieuNgay> days;
    private final List<String> warnings;

    public DuLieuTongQuan(TomTatTongQuan summary, List<LichHenTongQuan> appointments, List<CongViecTongQuan> work, List<CanhBaoTonKho> alerts, List<SoLieuNgay> days, List<String> warnings) {
        appointments=List.copyOf(appointments);work=List.copyOf(work);alerts=List.copyOf(alerts);
        days=List.copyOf(days);warnings=List.copyOf(warnings);
        this.summary = summary;
        this.appointments = appointments;
        this.work = work;
        this.alerts = alerts;
        this.days = days;
        this.warnings = warnings;
    }

    public TomTatTongQuan summary() { return summary; }
    public List<LichHenTongQuan> appointments() { return appointments; }
    public List<CongViecTongQuan> work() { return work; }
    public List<CanhBaoTonKho> alerts() { return alerts; }
    public List<SoLieuNgay> days() { return days; }
    public List<String> warnings() { return warnings; }



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DuLieuTongQuan value)) return false;
        return java.util.Objects.equals(summary, value.summary)
                && java.util.Objects.equals(appointments, value.appointments)
                && java.util.Objects.equals(work, value.work)
                && java.util.Objects.equals(alerts, value.alerts)
                && java.util.Objects.equals(days, value.days)
                && java.util.Objects.equals(warnings, value.warnings);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.summary);
        result = 31 * result + java.util.Objects.hashCode(this.appointments);
        result = 31 * result + java.util.Objects.hashCode(this.work);
        result = 31 * result + java.util.Objects.hashCode(this.alerts);
        result = 31 * result + java.util.Objects.hashCode(this.days);
        result = 31 * result + java.util.Objects.hashCode(this.warnings);
        return result;
    }

    @Override
    public String toString() {
        return "Overview[" + "summary=" + summary + ", " + "appointments=" + appointments + ", " + "work=" + work + ", " + "alerts=" + alerts + ", " + "days=" + days + ", " + "warnings=" + warnings + "]";
    }
}
