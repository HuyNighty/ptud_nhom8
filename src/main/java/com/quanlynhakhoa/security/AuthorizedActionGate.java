package com.quanlynhakhoa.security;

import com.quanlynhakhoa.enums.Quyen;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Explicit authorization boundary for future Java desktop service actions.
 *
 * <p>This is not a DB transaction manager or a JVM security sandbox. Business
 * services must call this gate (or AuthorizationService.require) before DAO work.
 */
public final class AuthorizedActionGate {
    private final AuthorizationService authorization;

    public AuthorizedActionGate(AuthorizationService authorization) {
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    public void run(DesktopSession session, Quyen permission, Runnable action) {
        Objects.requireNonNull(action, "action");
        authorization.require(session, permission);
        action.run();
    }

    public <T> T query(DesktopSession session, Quyen permission, Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        authorization.require(session, permission);
        return action.get();
    }
}
