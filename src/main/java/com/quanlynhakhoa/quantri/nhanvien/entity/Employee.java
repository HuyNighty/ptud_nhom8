package com.quanlynhakhoa.quantri.nhanvien.entity;

import java.time.LocalDate;

/** Minimal read-only snapshot of dbo.NhanVien. */
public record Employee(
        String id,
        String fullName,
        String jobTitle,
        EmployeeStatus status,
        LocalDate departureDate
) { }
