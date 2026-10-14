package com.quanlynhakhoa.modules.identity.application.port.out;

import com.quanlynhakhoa.modules.identity.application.model.AccountSnapshot;

import java.util.Optional;

public interface AccountRepositoryPort {

    Optional<AccountSnapshot> findByUsername(String username);

    Optional<AccountSnapshot> findById(String accountId);
}