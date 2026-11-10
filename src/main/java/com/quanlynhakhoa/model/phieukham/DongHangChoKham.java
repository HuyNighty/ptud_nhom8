package com.quanlynhakhoa.model.phieukham;

import java.time.LocalDateTime;
import java.util.Objects;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class DongHangChoKham {
    private final String maPhieuKham;
    private final String maBenhNhan;
    private final String tenBenhNhan;
    private final String maBacSi;
    private final String tenBacSi;
    private final String maLichHen;
    private final LocalDateTime thoiGianDen;
    private final String trangThai;

    public DongHangChoKham(String maPhieuKham, String maBenhNhan, String tenBenhNhan, String maBacSi, String tenBacSi, String maLichHen, LocalDateTime thoiGianDen, String trangThai) {
        Objects.requireNonNull(maPhieuKham, "examinationId");
        Objects.requireNonNull(maBenhNhan, "patientId");
        Objects.requireNonNull(tenBenhNhan, "patientName");
        Objects.requireNonNull(maBacSi, "doctorId");
        Objects.requireNonNull(thoiGianDen, "arrivedAt");
        if (!"CHO_KHAM".equals(trangThai) && !"DANG_KHAM".equals(trangThai)) {
        throw new IllegalArgumentException("Unexpected open examination state");
        }
        this.maPhieuKham = maPhieuKham;
        this.maBenhNhan = maBenhNhan;
        this.tenBenhNhan = tenBenhNhan;
        this.maBacSi = maBacSi;
        this.tenBacSi = tenBacSi;
        this.maLichHen = maLichHen;
        this.thoiGianDen = thoiGianDen;
        this.trangThai = trangThai;
    }

    public String examinationId() { return maPhieuKham; }
    public String patientId() { return maBenhNhan; }
    public String patientName() { return tenBenhNhan; }
    public String doctorId() { return maBacSi; }
    public String doctorName() { return tenBacSi; }
    public String appointmentId() { return maLichHen; }
    public LocalDateTime arrivedAt() { return thoiGianDen; }
    public String status() { return trangThai; }

    public String statusText() {
        return "CHO_KHAM".equals(trangThai) ? "Chờ khám" : "Đang khám";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongHangChoKham value)) return false;
        return java.util.Objects.equals(maPhieuKham, value.maPhieuKham)
                && java.util.Objects.equals(maBenhNhan, value.maBenhNhan)
                && java.util.Objects.equals(tenBenhNhan, value.tenBenhNhan)
                && java.util.Objects.equals(maBacSi, value.maBacSi)
                && java.util.Objects.equals(tenBacSi, value.tenBacSi)
                && java.util.Objects.equals(maLichHen, value.maLichHen)
                && java.util.Objects.equals(thoiGianDen, value.thoiGianDen)
                && java.util.Objects.equals(trangThai, value.trangThai);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maPhieuKham);
        result = 31 * result + java.util.Objects.hashCode(maBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(tenBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(maBacSi);
        result = 31 * result + java.util.Objects.hashCode(tenBacSi);
        result = 31 * result + java.util.Objects.hashCode(maLichHen);
        result = 31 * result + java.util.Objects.hashCode(thoiGianDen);
        result = 31 * result + java.util.Objects.hashCode(trangThai);
        return result;
    }

    @Override
    public String toString() {
        return "ExamQueueRow[" + "examinationId=" + maPhieuKham + ", " + "patientId=" + maBenhNhan + ", " + "patientName=" + tenBenhNhan + ", " + "doctorId=" + maBacSi + ", " + "doctorName=" + tenBacSi + ", " + "appointmentId=" + maLichHen + ", " + "arrivedAt=" + thoiGianDen + ", " + "status=" + trangThai + "]";
    }
}
