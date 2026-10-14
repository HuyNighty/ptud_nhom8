package com.quanlynhakhoa.modules.workforce.infrastructure.persistence.repository;

import com.quanlynhakhoa.modules.workforce.infrastructure.persistence.entity.NhanVienJpaEntity;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface NhanVienJpaRepository  extends Repository<NhanVienJpaEntity, String> {

    Optional<NhanVienJpaEntity> findById(String maNhanVien);
}
