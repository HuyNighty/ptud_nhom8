package com.quanlynhakhoa.model.vaitro;


















/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DongVaiTro {
    private final String ma;
    private final String ten;
    private final boolean thuocHeThong;
    private final boolean dangHoatDong;

    public DongVaiTro(String ma, String ten, boolean thuocHeThong, boolean dangHoatDong) {
        this.ma = ma;
        this.ten = ten;
        this.thuocHeThong = thuocHeThong;
        this.dangHoatDong = dangHoatDong;
    }

    public String code() { return ma; }
    public String name() { return ten; }
    public boolean system() { return thuocHeThong; }
    public boolean active() { return dangHoatDong; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongVaiTro value)) return false;
        return java.util.Objects.equals(ma, value.ma)
                && java.util.Objects.equals(ten, value.ten)
                && thuocHeThong == value.thuocHeThong
                && dangHoatDong == value.dangHoatDong;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(ma);
        result = 31 * result + java.util.Objects.hashCode(ten);
        result = 31 * result + Boolean.hashCode(thuocHeThong);
        result = 31 * result + Boolean.hashCode(dangHoatDong);
        return result;
    }

    @Override
    public String toString() {
        return "RoleRow[" + "code=" + ma + ", " + "name=" + ten + ", " + "system=" + thuocHeThong + ", " + "active=" + dangHoatDong + "]";
    }
}
