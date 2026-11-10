package com.quanlynhakhoa.model.nhanvien;

import java.time.LocalDate;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class YeuCauTaoNhanVien {
    private final String employeeId;
    private final String fullName;
    private final String phone;
    private final String jobTitle;
    private final LocalDate birthDate;
    private final String gender;
    private final String email;
    private final LocalDate joinedAt;
    private final String specialty;

    public YeuCauTaoNhanVien(String employeeId, String fullName, String phone, String jobTitle, LocalDate birthDate, String gender, String email, LocalDate joinedAt, String specialty) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.phone = phone;
        this.jobTitle = jobTitle;
        this.birthDate = birthDate;
        this.gender = gender;
        this.email = email;
        this.joinedAt = joinedAt;
        this.specialty = specialty;
    }

    public String employeeId() { return employeeId; }
    public String fullName() { return fullName; }
    public String phone() { return phone; }
    public String jobTitle() { return jobTitle; }
    public LocalDate birthDate() { return birthDate; }
    public String gender() { return gender; }
    public String email() { return email; }
    public LocalDate joinedAt() { return joinedAt; }
    public String specialty() { return specialty; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof YeuCauTaoNhanVien value)) return false;
        return java.util.Objects.equals(employeeId, value.employeeId)
                && java.util.Objects.equals(fullName, value.fullName)
                && java.util.Objects.equals(phone, value.phone)
                && java.util.Objects.equals(jobTitle, value.jobTitle)
                && java.util.Objects.equals(birthDate, value.birthDate)
                && java.util.Objects.equals(gender, value.gender)
                && java.util.Objects.equals(email, value.email)
                && java.util.Objects.equals(joinedAt, value.joinedAt)
                && java.util.Objects.equals(specialty, value.specialty);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(employeeId);
        result = 31 * result + java.util.Objects.hashCode(fullName);
        result = 31 * result + java.util.Objects.hashCode(phone);
        result = 31 * result + java.util.Objects.hashCode(jobTitle);
        result = 31 * result + java.util.Objects.hashCode(birthDate);
        result = 31 * result + java.util.Objects.hashCode(gender);
        result = 31 * result + java.util.Objects.hashCode(email);
        result = 31 * result + java.util.Objects.hashCode(joinedAt);
        result = 31 * result + java.util.Objects.hashCode(specialty);
        return result;
    }

    @Override
    public String toString() {
        return "EmployeeCreateRequest[" + "employeeId=" + employeeId + ", " + "fullName=" + fullName + ", " + "phone=" + phone + ", " + "jobTitle=" + jobTitle + ", " + "birthDate=" + birthDate + ", " + "gender=" + gender + ", " + "email=" + email + ", " + "joinedAt=" + joinedAt + ", " + "specialty=" + specialty + "]";
    }
}
