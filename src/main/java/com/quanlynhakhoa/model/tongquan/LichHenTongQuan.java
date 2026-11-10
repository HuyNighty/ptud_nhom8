package com.quanlynhakhoa.model.tongquan;












import java.util.*;
import java.time.LocalDateTime;



/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class LichHenTongQuan {
    private final String id;
    private final LocalDateTime time;
    private final String patient;
    private final String doctor;
    private final String status;

    public LichHenTongQuan(String id, LocalDateTime time, String patient, String doctor, String status) {
        this.id = id;
        this.time = time;
        this.patient = patient;
        this.doctor = doctor;
        this.status = status;
    }

    public String id() { return id; }
    public LocalDateTime time() { return time; }
    public String patient() { return patient; }
    public String doctor() { return doctor; }
    public String status() { return status; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LichHenTongQuan value)) return false;
        return java.util.Objects.equals(id, value.id)
                && java.util.Objects.equals(time, value.time)
                && java.util.Objects.equals(patient, value.patient)
                && java.util.Objects.equals(doctor, value.doctor)
                && java.util.Objects.equals(status, value.status);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.id);
        result = 31 * result + java.util.Objects.hashCode(this.time);
        result = 31 * result + java.util.Objects.hashCode(this.patient);
        result = 31 * result + java.util.Objects.hashCode(this.doctor);
        result = 31 * result + java.util.Objects.hashCode(this.status);
        return result;
    }

    @Override
    public String toString() {
        return "Appointment[" + "id=" + id + ", " + "time=" + time + ", " + "patient=" + patient + ", " + "doctor=" + doctor + ", " + "status=" + status + "]";
    }
}
