package com.quanlynhakhoa.model.nhatky;

import java.time.LocalDateTime;
import java.util.Objects;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class BanGhiNhatKy {
    private final long id;
    private final LocalDateTime utcTime;
    private final String actorType;
    private final String accountId;
    private final String function;
    private final String action;
    private final String result;
    private final String targetType;
    private final String targetId;

    public BanGhiNhatKy(long id, LocalDateTime utcTime, String actorType, String accountId, String function, String action, String result, String targetType, String targetId) {
        if (id <= 0) throw new IllegalArgumentException("Invalid audit ID");
        Objects.requireNonNull(utcTime, "utcTime");
        Objects.requireNonNull(actorType, "actorType");
        Objects.requireNonNull(function, "function");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(result, "result");
        this.id = id;
        this.utcTime = utcTime;
        this.actorType = actorType;
        this.accountId = accountId;
        this.function = function;
        this.action = action;
        this.result = result;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public long id() { return id; }
    public LocalDateTime utcTime() { return utcTime; }
    public String actorType() { return actorType; }
    public String accountId() { return accountId; }
    public String function() { return function; }
    public String action() { return action; }
    public String result() { return result; }
    public String targetType() { return targetType; }
    public String targetId() { return targetId; }



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BanGhiNhatKy value)) return false;
        return id == value.id
                && java.util.Objects.equals(utcTime, value.utcTime)
                && java.util.Objects.equals(actorType, value.actorType)
                && java.util.Objects.equals(accountId, value.accountId)
                && java.util.Objects.equals(function, value.function)
                && java.util.Objects.equals(action, value.action)
                && java.util.Objects.equals(result, value.result)
                && java.util.Objects.equals(targetType, value.targetType)
                && java.util.Objects.equals(targetId, value.targetId);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Long.hashCode(this.id);
        result = 31 * result + java.util.Objects.hashCode(this.utcTime);
        result = 31 * result + java.util.Objects.hashCode(this.actorType);
        result = 31 * result + java.util.Objects.hashCode(this.accountId);
        result = 31 * result + java.util.Objects.hashCode(this.function);
        result = 31 * result + java.util.Objects.hashCode(this.action);
        result = 31 * result + java.util.Objects.hashCode(this.result);
        result = 31 * result + java.util.Objects.hashCode(this.targetType);
        result = 31 * result + java.util.Objects.hashCode(this.targetId);
        return result;
    }

    @Override
    public String toString() {
        return "AuditEntry[" + "id=" + id + ", " + "utcTime=" + utcTime + ", " + "actorType=" + actorType + ", " + "accountId=" + accountId + ", " + "function=" + function + ", " + "action=" + action + ", " + "result=" + result + ", " + "targetType=" + targetType + ", " + "targetId=" + targetId + "]";
    }
}
