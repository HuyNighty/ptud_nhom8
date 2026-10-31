package com.quanlynhakhoa.examination;

import java.time.LocalDateTime;
import java.util.Objects;

/** Existing examination/visit identity, read-only. Never creates a new PhieuKham. */
public record ExamQueueRow(
        String examinationId,
        String patientId,
        String patientName,
        String doctorId,
        String doctorName,
        String appointmentId,
        LocalDateTime arrivedAt,
        String status) {

    public ExamQueueRow {
        Objects.requireNonNull(examinationId, "examinationId");
        Objects.requireNonNull(patientId, "patientId");
        Objects.requireNonNull(patientName, "patientName");
        Objects.requireNonNull(doctorId, "doctorId");
        Objects.requireNonNull(arrivedAt, "arrivedAt");
        if (!"CHO_KHAM".equals(status) && !"DANG_KHAM".equals(status)) {
            throw new IllegalArgumentException("Unexpected open examination state");
        }
    }

    public String statusText() {
        return "CHO_KHAM".equals(status) ? "Chờ khám" : "Đang khám";
    }
}
