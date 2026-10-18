package com.quanlynhakhoa.features.auth.service;

import com.quanlynhakhoa.features.auth.enums.Role;

public record AuthenticationResult(boolean successful, String accountId, Role role, boolean mustChangePassword) {

    public static AuthenticationResult rejected() {
        return new AuthenticationResult(false, null, null, false);
    }

    public static AuthenticationResult accepted(String accountId, Role role, boolean mustChangePassword) {
        return new AuthenticationResult(true, accountId, role, mustChangePassword);
    }
}