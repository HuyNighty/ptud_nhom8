package com.quanlynhakhoa.model.vaitro;


















/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DongQuyen {
    private final String ma;
    private final String ten;
    private final boolean coSan;

    public DongQuyen(String ma, String ten, boolean coSan) {
        this.ma = ma;
        this.ten = ten;
        this.coSan = coSan;
    }

    public String code() { return ma; }
    public String name() { return ten; }
    public boolean builtIn() { return coSan; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongQuyen value)) return false;
        return java.util.Objects.equals(ma, value.ma)
                && java.util.Objects.equals(ten, value.ten)
                && coSan == value.coSan;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(ma);
        result = 31 * result + java.util.Objects.hashCode(ten);
        result = 31 * result + Boolean.hashCode(coSan);
        return result;
    }

    @Override
    public String toString() {
        return "PermissionRow[" + "code=" + ma + ", " + "name=" + ten + ", " + "builtIn=" + coSan + "]";
    }
}
