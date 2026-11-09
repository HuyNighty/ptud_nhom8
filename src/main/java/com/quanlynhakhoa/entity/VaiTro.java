package com.quanlynhakhoa.entity;

import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Immutable V8 catalog entity; separate from the legacy Role enum in C2A. */
public final class VaiTro {
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,31}");
    private static final Set<String> SYSTEM_CODES = Set.of(
            "RECEPTIONIST", "DOCTOR", "NURSE", "CLINIC_MANAGER", "INVENTORY_MANAGER", "SYSTEM_ADMIN");
    private final String ma;
    private final String tenHienThi;
    private final String moTa;
    private final boolean dangHoatDong;
    private final boolean thuocHeThong;
    private final boolean vaiTroDuocBaoVe;
    private final byte[] phienBan;

    public VaiTro(String ma, String tenHienThi, String moTa, boolean dangHoatDong,
                      boolean thuocHeThong, boolean vaiTroDuocBaoVe, byte[] phienBan) {
        requireCode(ma);
        if (tenHienThi == null || tenHienThi.isBlank() || tenHienThi.length() > 100)
            throw new IllegalArgumentException("Tên vai trò phải có 1–100 ký tự.");
        if (moTa == null || moTa.length() > 500)
            throw new IllegalArgumentException("Mô tả vai trò tối đa 500 ký tự và không được null.");
        if (thuocHeThong != SYSTEM_CODES.contains(ma))
            throw new IllegalArgumentException("Cờ vai trò hệ thống không khớp mã.");
        if (vaiTroDuocBaoVe != ma.equals("SYSTEM_ADMIN") || (vaiTroDuocBaoVe && !dangHoatDong))
            throw new IllegalArgumentException("Vai trò quản trị được bảo vệ không hợp lệ.");
        if (phienBan == null || phienBan.length != 8)
            throw new IllegalArgumentException("Revision phải là rowversion 8 byte.");
        this.ma = ma;
        this.tenHienThi = tenHienThi;
        this.moTa = moTa;
        this.dangHoatDong = dangHoatDong;
        this.thuocHeThong = thuocHeThong;
        this.vaiTroDuocBaoVe = vaiTroDuocBaoVe;
        this.phienBan = phienBan.clone();
    }

    public static void requireCode(String ma) {
        if (ma == null || !CODE.matcher(ma).matches())
            throw new IllegalArgumentException("Mã vai trò phải khớp [A-Z][A-Z0-9_]{0,31}.");
    }
    public String code() { return ma; }
    public String displayName() { return tenHienThi; }
    public String description() { return moTa; }
    public boolean active() { return dangHoatDong; }
    public boolean system() { return thuocHeThong; }
    public boolean protectedRole() { return vaiTroDuocBaoVe; }
    public byte[] revision() { return phienBan.clone(); }

    @Override public boolean equals(Object other) {
        return other instanceof VaiTro role && ma.equals(role.ma);
    }
    @Override public int hashCode() { return Objects.hash(ma); }
    @Override public String toString() { return "RoleEntity[code=" + ma + "]"; }
}

