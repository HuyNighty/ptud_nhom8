package com.quanlynhakhoa.model.phieukham;

import java.time.LocalDateTime;
import java.util.Objects;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class DongHangChoKham {
    private final String examinationId;
    private final String patientId;
    private final String patientName;
    private final String doctorId;
    private final String doctorName;
    private final String appointmentId;
    private final LocalDateTime arrivedAt;
    private final String status;

    public DongHangChoKham(String examinationId, String patientId, String patientName, String doctorId, String doctorName, String appointmentId, LocalDateTime arrivedAt, String status) {
        Objects.requireNonNull(examinationId, "examinationId");
        Objects.requireNonNull(patientId, "patientId");
        Objects.requireNonNull(patientName, "patientName");
        Objects.requireNonNull(doctorId, "doctorId");
        Objects.requireNonNull(arrivedAt, "arrivedAt");
        if (!"CHO_KHAM".equals(status) && !"DANG_KHAM".equals(status)) {
        throw new IllegalArgumentException("Unexpected open examination state");
        }
        this.examinationId = examinationId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.appointmentId = appointmentId;
        this.arrivedAt = arrivedAt;
        this.status = status;
    }

    public String examinationId() { return examinationId; }
    public String patientId() { return patientId; }
    public String patientName() { return patientName; }
    public String doctorId() { return doctorId; }
    public String doctorName() { return doctorName; }
    public String appointmentId() { return appointmentId; }
    public LocalDateTime arrivedAt() { return arrivedAt; }
    public String status() { return status; }

    public String statusText() {
        return "CHO_KHAM".equals(status) ? "Chờ khám" : "Đang khám";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongHangChoKham value)) return false;
        return java.util.Objects.equals(examinationId, value.examinationId)
                && java.util.Objects.equals(patientId, value.patientId)
                && java.util.Objects.equals(patientName, value.patientName)
                && java.util.Objects.equals(doctorId, value.doctorId)
                && java.util.Objects.equals(doctorName, value.doctorName)
                && java.util.Objects.equals(appointmentId, value.appointmentId)
                && java.util.Objects.equals(arrivedAt, value.arrivedAt)
                && java.util.Objects.equals(status, value.status);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(examinationId);
        result = 31 * result + java.util.Objects.hashCode(patientId);
        result = 31 * result + java.util.Objects.hashCode(patientName);
        result = 31 * result + java.util.Objects.hashCode(doctorId);
        result = 31 * result + java.util.Objects.hashCode(doctorName);
        result = 31 * result + java.util.Objects.hashCode(appointmentId);
        result = 31 * result + java.util.Objects.hashCode(arrivedAt);
        result = 31 * result + java.util.Objects.hashCode(status);
        return result;
    }

    @Override
    public String toString() {
        return "ExamQueueRow[" + "examinationId=" + examinationId + ", " + "patientId=" + patientId + ", " + "patientName=" + patientName + ", " + "doctorId=" + doctorId + ", " + "doctorName=" + doctorName + ", " + "appointmentId=" + appointmentId + ", " + "arrivedAt=" + arrivedAt + ", " + "status=" + status + "]";
    }
}
