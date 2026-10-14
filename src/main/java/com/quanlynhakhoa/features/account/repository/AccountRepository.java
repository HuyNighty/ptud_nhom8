package com.quanlynhakhoa.features.account.repository;

import com.quanlynhakhoa.features.account.entity.Account;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface AccountRepository extends Repository<Account, String> {

    Optional<Account> findById(String maTaiKhoan);

    Optional<Account> findByTenDangNhap(String tenDangNhap);

    boolean existsByTenDangNhap(String tenDangNhap);
}