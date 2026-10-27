package com.quanlynhakhoa.auth;

import com.quanlynhakhoa.model.Role;

/** Result visible to the desktop caller; never contains credentials or hashes. */
public record AuthDecision(boolean authenticated, String accountId,
                           Role role, boolean forcePasswordChange) {
    public static AuthDecision rejected() {
        return new AuthDecision(false, null, null, false);
    }
}
