package com.quanlynhakhoa.features.examination.repository;

import com.quanlynhakhoa.features.examination.entity.DichVu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DichVuRepository extends JpaRepository<DichVu, String> {

    List<DichVu> findAllByOrderByTenDichVuAsc();

    boolean existsByTenDichVuIgnoreCase(String tenDichVu);

    boolean existsByTenDichVuIgnoreCaseAndMaDichVuNot(
            String tenDichVu,
            String maDichVu
    );
}
