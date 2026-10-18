package com.quanlynhakhoa.features.employee.repository;

import com.quanlynhakhoa.features.employee.entity.Employee;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface EmployeeRepository  extends Repository<Employee, String> {

    Optional<Employee> findById(String maNhanVien);
}
