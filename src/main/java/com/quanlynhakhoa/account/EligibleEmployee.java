package com.quanlynhakhoa.account;

import com.quanlynhakhoa.model.Role;
import java.util.Objects;

/** A selectable employee who is active and has no linked account at query time. */
public record EligibleEmployee(String employeeId, String fullName, String jobTitle, Role role) {
    public EligibleEmployee {
        if (employeeId == null || !employeeId.matches("[A-Z0-9]{1,10}")) {
            throw new IllegalArgumentException("Mã nhân viên không hợp lệ.");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Nhân viên phải có họ tên.");
        }
        if (role != roleForJob(jobTitle)) {
            throw new IllegalArgumentException("Vai trò không khớp chức danh nhân viên.");
        }
    }

    public static Role roleForJob(String jobTitle) {
        if (jobTitle == null) throw new IllegalArgumentException("Chức danh không hợp lệ.");
        return switch (jobTitle) {
            case "LT" -> Role.RECEPTIONIST;
            case "BS" -> Role.DOCTOR;
            case "YT" -> Role.NURSE;
            case "QL" -> Role.CLINIC_MANAGER;
            case "KHO" -> Role.INVENTORY_MANAGER;
            default -> throw new IllegalArgumentException("Không thể cấp tài khoản cho chức danh này.");
        };
    }

    public String displayLabel() {
        return employeeId + " — " + fullName + " (" + jobTitle + ")";
    }
}
