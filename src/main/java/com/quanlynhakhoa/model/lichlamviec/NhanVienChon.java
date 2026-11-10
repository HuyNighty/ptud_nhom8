package com.quanlynhakhoa.model.lichlamviec;



/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class NhanVienChon {
    private final String id;
    private final String ten;

    public NhanVienChon(String id, String ten) {
        this.id = id;
        this.ten = ten;
    }

    public String id() { return id; }
    public String ten() { return ten; }

    @Override public String toString() { return id + " — " + ten; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NhanVienChon value)) return false;
        return java.util.Objects.equals(id, value.id)
                && java.util.Objects.equals(ten, value.ten);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(id);
        result = 31 * result + java.util.Objects.hashCode(ten);
        return result;
    }

}
