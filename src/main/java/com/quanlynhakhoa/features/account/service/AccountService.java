package com.quanlynhakhoa.features.account.service;

import com.quanlynhakhoa.features.account.entity.Account;
import com.quanlynhakhoa.features.account.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AccountService {
    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<Account> findByUsername(String username) {
        return repository.findByTenDangNhap(username);
    }

    @Transactional(readOnly = true)
    public Optional<Account> findById(String accountId) {
        return repository.findById(accountId);
    }
}
