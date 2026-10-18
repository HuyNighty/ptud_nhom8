package com.quanlynhakhoa.features.auth.service;

import com.quanlynhakhoa.features.account.entity.Account;
import com.quanlynhakhoa.features.account.enums.AccountStatus;
import com.quanlynhakhoa.features.account.repository.AccountRepository;
import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.employee.enums.EmployeeStatus;
import com.quanlynhakhoa.features.employee.repository.EmployeeRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class AuthService {

    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AuthService(
            AccountRepository accountRepository,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public AuthenticationResult authenticate(String username, String rawPassword) {
        if (username == null || username.isBlank()
                || rawPassword == null || rawPassword.isBlank()) {
            return AuthenticationResult.rejected();
        }

        Optional<Account> accountOptional = accountRepository.findForAuthentication(username);

        if (accountOptional.isEmpty()) {
            return AuthenticationResult.rejected();
        }

        Account account = accountOptional.get();

        LocalDateTime nowUtc = LocalDateTime.ofInstant(
                clock.instant(), ZoneOffset.UTC
        );

        account.unlockExpiredAutomaticLock(nowUtc);

        if (account.getTrangThai() != AccountStatus.HOAT_DONG) {
            return AuthenticationResult.rejected();
        }

        if (!isEmployeeEligible(account)) {
            return AuthenticationResult.rejected();
        }

        if (!passwordEncoder.matches(
                rawPassword,
                account.getMatKhauHash()
        )) {
            account.recordFailedPasswordAttempt(nowUtc);

            return AuthenticationResult.rejected();
        }

        account.recordSuccessfulLogin(nowUtc);

        return AuthenticationResult.accepted(
                account.getMaTaiKhoan(),
                account.getVaiTro(),
                account.isPhaiDoiMatKhau()
        );
    }

    private boolean isEmployeeEligible(Account account) {

        String employeeId = account.getMaNhanVien();

        if (employeeId == null) {
            return account.getVaiTro() == Role.SYSTEM_ADMIN;
        }

        return employeeRepository.findById(employeeId)
                .map(employee ->
                        employee.getTrangThai() == EmployeeStatus.DANG_LAM_VIEC)
                .orElse(false);
    }
}