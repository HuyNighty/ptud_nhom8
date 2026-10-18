package com.quanlynhakhoa.features.account.repository;

import com.quanlynhakhoa.features.account.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends Repository<Account, String> {

    Optional<Account> findById(String maTaiKhoan);

    Optional<Account> findByTenDangNhap(String tenDangNhap);

    boolean existsByTenDangNhap(String tenDangNhap);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a
        FROM TaiKhoanJpaEntity a
        WHERE a.tenDangNhap = :username
        """)
    Optional<Account> findForAuthentication(@Param("username") String username);
}