package com.quanlynhakhoa.model.nhanvien;

import com.quanlynhakhoa.enums.TrangThaiNhanVien;
import java.time.LocalDate;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class ThongTinNhanVien {
    private final String id;
    private final String fullName;
    private final String jobTitle;
    private final TrangThaiNhanVien status;
    private final LocalDate departureDate;

    public ThongTinNhanVien(String id, String fullName, String jobTitle, TrangThaiNhanVien status, LocalDate departureDate) {
        this.id = id;
        this.fullName = fullName;
        this.jobTitle = jobTitle;
        this.status = status;
        this.departureDate = departureDate;
    }

    public String id() { return id; }
    public String fullName() { return fullName; }
    public String jobTitle() { return jobTitle; }
    public TrangThaiNhanVien status() { return status; }
    public LocalDate departureDate() { return departureDate; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinNhanVien value)) return false;
        return java.util.Objects.equals(id, value.id)
                && java.util.Objects.equals(fullName, value.fullName)
                && java.util.Objects.equals(jobTitle, value.jobTitle)
                && java.util.Objects.equals(status, value.status)
                && java.util.Objects.equals(departureDate, value.departureDate);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(id);
        result = 31 * result + java.util.Objects.hashCode(fullName);
        result = 31 * result + java.util.Objects.hashCode(jobTitle);
        result = 31 * result + java.util.Objects.hashCode(status);
        result = 31 * result + java.util.Objects.hashCode(departureDate);
        return result;
    }

    @Override
    public String toString() {
        return "Employee[" + "id=" + id + ", " + "fullName=" + fullName + ", " + "jobTitle=" + jobTitle + ", " + "status=" + status + ", " + "departureDate=" + departureDate + "]";
    }
}
