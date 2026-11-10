package com.quanlynhakhoa.entity;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Immutable V8 catalog entity; separate from the legacy Role enum in C2A. */
public final class VaiTro {
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,31}");
    private static final Set<String> SYSTEM_CODES = Set.of(
            "RECEPTIONIST", "DOCTOR", "NURSE", "CLINIC_MANAGER", "INVENTORY_MANAGER", "SYSTEM_ADMIN");
    private final String code;
    private final String displayName;
    private final String description;
    private final boolean active;
    private final boolean system;
    private final boolean protectedRole;
    private final byte[] revision;

    public VaiTro(String code, String displayName, String description, boolean active,
                      boolean system, boolean protectedRole, byte[] revision) {
        requireCode(code);
        if (displayName == null || displayName.isBlank() || displayName.length() > 100)
            throw new IllegalArgumentException("Tên vai trò phải có 1–100 ký tự.");
        if (description == null || description.length() > 500)
            throw new IllegalArgumentException("Mô tả vai trò tối đa 500 ký tự và không được null.");
        if (system != SYSTEM_CODES.contains(code))
            throw new IllegalArgumentException("Cờ vai trò hệ thống không khớp mã.");
        if (protectedRole != code.equals("SYSTEM_ADMIN") || (protectedRole && !active))
            throw new IllegalArgumentException("Vai trò quản trị được bảo vệ không hợp lệ.");
        if (revision == null || revision.length != 8)
            throw new IllegalArgumentException("Revision phải là rowversion 8 byte.");
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.active = active;
        this.system = system;
        this.protectedRole = protectedRole;
        this.revision = revision.clone();
    }

    public static void requireCode(String code) {
        if (code == null || !CODE.matcher(code).matches())
            throw new IllegalArgumentException("Mã vai trò phải khớp [A-Z][A-Z0-9_]{0,31}.");
    }
    public String code() { return code; }
    public String displayName() { return displayName; }
    public String description() { return description; }
    public boolean active() { return active; }
    public boolean system() { return system; }
    public boolean protectedRole() { return protectedRole; }
    public byte[] revision() { return revision.clone(); }

    @Override public boolean equals(Object other) {
        return other instanceof VaiTro role && code.equals(role.code);
    }
    @Override public int hashCode() { return Objects.hash(code); }
    @Override public String toString() { return "RoleEntity[code=" + code + "]"; }
}

