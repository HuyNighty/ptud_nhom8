package com.quanlynhakhoa.model.hoadon;



/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class LuaChonLapHoaDon {
    private final String id;
    private final String label;

    public LuaChonLapHoaDon(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() { return id; }
    public String label() { return label; }

    @Override public String toString() { return id + " · " + label; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof LuaChonLapHoaDon value)) return false;
        return java.util.Objects.equals(id, value.id)
                && java.util.Objects.equals(label, value.label);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(id);
        result = 31 * result + java.util.Objects.hashCode(label);
        return result;
    }

}
