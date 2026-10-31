package com.quanlynhakhoa.workforce;

import java.time.LocalDate;

/** Employee data only. Account creation is a separate authorized workflow. */
public record EmployeeCreateRequest(
        String employeeId,
        String fullName,
        String phone,
        String jobTitle,
        LocalDate birthDate,
        String gender,
        String email,
        LocalDate joinedAt,
        String specialty
) { }
