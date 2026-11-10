package com.quanlynhakhoa.model.tongquan;












import java.util.*;




/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class CanhBaoTonKho {
    private final String loai;
    private final String matHang;
    private final String chiTiet;
    private final String nguCanh;

    public CanhBaoTonKho(String loai, String matHang, String chiTiet, String nguCanh) {
        this.loai = loai;
        this.matHang = matHang;
        this.chiTiet = chiTiet;
        this.nguCanh = nguCanh;
    }

    public String kind() { return loai; }
    public String item() { return matHang; }
    public String detail() { return chiTiet; }
    public String context() { return nguCanh; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CanhBaoTonKho value)) return false;
        return java.util.Objects.equals(loai, value.loai)
                && java.util.Objects.equals(matHang, value.matHang)
                && java.util.Objects.equals(chiTiet, value.chiTiet)
                && java.util.Objects.equals(nguCanh, value.nguCanh);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.loai);
        result = 31 * result + java.util.Objects.hashCode(this.matHang);
        result = 31 * result + java.util.Objects.hashCode(this.chiTiet);
        result = 31 * result + java.util.Objects.hashCode(this.nguCanh);
        return result;
    }

    @Override
    public String toString() {
        return "StockAlert[" + "kind=" + loai + ", " + "item=" + matHang + ", " + "detail=" + chiTiet + ", " + "context=" + nguCanh + "]";
    }
}
