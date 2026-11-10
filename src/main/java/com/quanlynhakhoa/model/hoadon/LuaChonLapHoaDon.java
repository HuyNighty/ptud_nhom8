package com.quanlynhakhoa.model.hoadon;



/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class LuaChonLapHoaDon {
    private final String ma;
    private final String nhan;

    public LuaChonLapHoaDon(String ma, String nhan) {
        this.ma = ma;
        this.nhan = nhan;
    }

    public String id() { return ma; }
    public String label() { return nhan; }

    @Override public String toString() { return ma + " · " + nhan; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LuaChonLapHoaDon value)) return false;
        return java.util.Objects.equals(ma, value.ma)
                && java.util.Objects.equals(nhan, value.nhan);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(ma);
        result = 31 * result + java.util.Objects.hashCode(nhan);
        return result;
    }

}
