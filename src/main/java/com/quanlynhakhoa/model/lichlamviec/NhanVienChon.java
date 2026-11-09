package com.quanlynhakhoa.model.lichlamviec;



/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class NhanVienChon {
    private final String ma;
    private final String ten;

    public NhanVienChon(String ma, String ten) {
        this.ma = ma;
        this.ten = ten;
    }

    public String id() { return ma; }
    public String ten() { return ten; }

    @Override public String toString() { return ma + " — " + ten; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NhanVienChon value)) return false;
        return java.util.Objects.equals(ma, value.ma)
                && java.util.Objects.equals(ten, value.ten);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(ma);
        result = 31 * result + java.util.Objects.hashCode(ten);
        return result;
    }

}
