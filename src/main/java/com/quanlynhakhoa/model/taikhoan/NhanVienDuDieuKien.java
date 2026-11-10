package com.quanlynhakhoa.model.taikhoan;

import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.quantri.nhanvien.JobTitleLabels;


/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class NhanVienDuDieuKien {
    private final String employeeId;
    private final String fullName;
    private final String jobTitle;
    private final VaiTroHeThong role;

    public NhanVienDuDieuKien(String employeeId, String fullName, String jobTitle, VaiTroHeThong role) {
        if (employeeId == null || !employeeId.matches("[A-Z0-9]{1,10}")) {
        throw new IllegalArgumentException("Mã nhân viên không hợp lệ.");
        }
        if (fullName == null || fullName.isBlank()) {
        throw new IllegalArgumentException("Nhân viên phải có họ tên.");
        }
        if (role != roleForJob(jobTitle)) {
        throw new IllegalArgumentException("Vai trò không khớp chức danh nhân viên.");
        }
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.jobTitle = jobTitle;
        this.role = role;
    }

    public String employeeId() { return employeeId; }
    public String fullName() { return fullName; }
    public String jobTitle() { return jobTitle; }
    public VaiTroHeThong role() { return role; }



    public static VaiTroHeThong roleForJob(String jobTitle) {
        if (jobTitle == null) throw new IllegalArgumentException("Chức danh không hợp lệ.");
        return switch (jobTitle) {
            case "LT" -> VaiTroHeThong.RECEPTIONIST;
            case "BS" -> VaiTroHeThong.DOCTOR;
            case "YT" -> VaiTroHeThong.NURSE;
            case "QL" -> VaiTroHeThong.CLINIC_MANAGER;
            case "KHO" -> VaiTroHeThong.INVENTORY_MANAGER;
            default -> throw new IllegalArgumentException("Không thể cấp tài khoản cho chức danh này.");
        };
    }

    public String displayLabel() {
        return employeeId + " — " + fullName + " (" + JobTitleLabels.label(jobTitle) + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NhanVienDuDieuKien value)) return false;
        return java.util.Objects.equals(employeeId, value.employeeId)
                && java.util.Objects.equals(fullName, value.fullName)
                && java.util.Objects.equals(jobTitle, value.jobTitle)
                && java.util.Objects.equals(role, value.role);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(employeeId);
        result = 31 * result + java.util.Objects.hashCode(fullName);
        result = 31 * result + java.util.Objects.hashCode(jobTitle);
        result = 31 * result + java.util.Objects.hashCode(role);
        return result;
    }

    @Override
    public String toString() {
        return "EligibleEmployee[" + "employeeId=" + employeeId + ", " + "fullName=" + fullName + ", " + "jobTitle=" + jobTitle + ", " + "role=" + role + "]";
    }
}
