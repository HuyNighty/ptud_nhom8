package com.quanlynhakhoa.modules.identity.infrastructure.persistence.repository;

import com.quanlynhakhoa.modules.identity.infrastructure.persistence.entity.TaiKhoanJpaEntity;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface TaiKhoanJpaRepository extends Repository<TaiKhoanJpaEntity, String> {

    Optional<TaiKhoanJpaEntity> findById(String maTaiKhoan);

    Optional<TaiKhoanJpaEntity> findByTenDangNhap(String tenDangNhap);

    boolean existsByTenDangNhap(String tenDangNhap);
}