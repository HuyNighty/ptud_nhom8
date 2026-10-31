package com.quanlynhakhoa.audit;

import java.time.LocalDateTime;
import java.util.Objects;

/** Redacted audit projection: intentionally excludes free-form noiDung and sensitive tokens. */
public record AuditEntry(long id, LocalDateTime utcTime, String actorType, String accountId,
                         String function, String action, String result,
                         String targetType, String targetId) {
    public AuditEntry {
        if (id <= 0) throw new IllegalArgumentException("Invalid audit ID");
        Objects.requireNonNull(utcTime, "utcTime");
        Objects.requireNonNull(actorType, "actorType");
        Objects.requireNonNull(function, "function");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(result, "result");
    }
}
