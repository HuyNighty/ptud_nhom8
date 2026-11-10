package com.quanlynhakhoa.model.tongquan;






import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;




/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class TomTatTongQuan {
    private final LocalDate date;
    private final Long patients;
    private final Long appointments;
    private final Long inProgressExams;
    private final BigDecimal collected;

    public TomTatTongQuan(LocalDate date, Long patients, Long appointments, Long inProgressExams, BigDecimal collected) {
        this.date = date;
        this.patients = patients;
        this.appointments = appointments;
        this.inProgressExams = inProgressExams;
        this.collected = collected;
    }

    public LocalDate date() { return date; }
    public Long patients() { return patients; }
    public Long appointments() { return appointments; }
    public Long inProgressExams() { return inProgressExams; }
    public BigDecimal collected() { return collected; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TomTatTongQuan value)) return false;
        return java.util.Objects.equals(date, value.date)
                && java.util.Objects.equals(patients, value.patients)
                && java.util.Objects.equals(appointments, value.appointments)
                && java.util.Objects.equals(inProgressExams, value.inProgressExams)
                && java.util.Objects.equals(collected, value.collected);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.date);
        result = 31 * result + java.util.Objects.hashCode(this.patients);
        result = 31 * result + java.util.Objects.hashCode(this.appointments);
        result = 31 * result + java.util.Objects.hashCode(this.inProgressExams);
        result = 31 * result + java.util.Objects.hashCode(this.collected);
        return result;
    }

    @Override
    public String toString() {
        return "Summary[" + "date=" + date + ", " + "patients=" + patients + ", " + "appointments=" + appointments + ", " + "inProgressExams=" + inProgressExams + ", " + "collected=" + collected + "]";
    }
}
