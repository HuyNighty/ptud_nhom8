package com.quanlynhakhoa.model.hoadon;



import java.sql.*;
import java.util.*;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class ThanhToanDangCho {
    private final YeuCauThanhToan yeuCau;
    private final String chuSoHuu;

    public ThanhToanDangCho(YeuCauThanhToan yeuCau, String chuSoHuu) {
        this.yeuCau = yeuCau;
        this.chuSoHuu = chuSoHuu;
    }

    public YeuCauThanhToan request() { return yeuCau; }
    public String owner() { return chuSoHuu; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThanhToanDangCho value)) return false;
        return java.util.Objects.equals(yeuCau, value.yeuCau)
                && java.util.Objects.equals(chuSoHuu, value.chuSoHuu);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(yeuCau);
        result = 31 * result + java.util.Objects.hashCode(chuSoHuu);
        return result;
    }

    @Override
    public String toString() {
        return "Pending[" + "request=" + yeuCau + ", " + "owner=" + chuSoHuu + "]";
    }
}
