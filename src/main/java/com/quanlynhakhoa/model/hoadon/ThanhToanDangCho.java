package com.quanlynhakhoa.model.hoadon;



import java.sql.*;
import java.util.*;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class ThanhToanDangCho {
    private final YeuCauThanhToan request;
    private final String owner;

    public ThanhToanDangCho(YeuCauThanhToan request, String owner) {
        this.request = request;
        this.owner = owner;
    }

    public YeuCauThanhToan request() { return request; }
    public String owner() { return owner; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThanhToanDangCho value)) return false;
        return java.util.Objects.equals(request, value.request)
                && java.util.Objects.equals(owner, value.owner);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(request);
        result = 31 * result + java.util.Objects.hashCode(owner);
        return result;
    }

    @Override
    public String toString() {
        return "Pending[" + "request=" + request + ", " + "owner=" + owner + "]";
    }
}
