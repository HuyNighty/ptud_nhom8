package com.quanlynhakhoa.security;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.auth.DesktopLoginGate;
import com.quanlynhakhoa.model.Role;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An in-memory desktop principal. NOT an SQL PhienDangNhap record or a server token.
 * Never contains the password, BCrypt hash or SQL connection credentials.
 */
public final class DesktopSession {
    private final String accountId;
    private final Role role;
    private final Instant startedAt;
    private final AtomicBoolean active = new AtomicBoolean(true);

    private DesktopSession(String accountId, Role role, Instant startedAt) {
        this.accountId = accountId;
        this.role = role;
        this.startedAt = startedAt;
    }

    public static DesktopSession fromSuccessfulLogin(AuthDecision decision) {
        if (!DesktopLoginGate.mayOpenMain(decision)) throw new AccessDeniedException();
        return new DesktopSession(decision.accountId(), decision.role(), Instant.now());
    }

    public String accountId() { return accountId; }
    public Role role() { return role; }
    public Instant startedAt() { return startedAt; }
    public boolean isActive() { return active.get(); }

    /** Invalidate local authorization immediately; database-session revocation is not part of R4-A1. */
    public void logout() { active.set(false); }

    @Override public String toString() {
        return "DesktopSession[role=" + Objects.toString(role) + ",active=" + isActive() + "]";
    }
}
