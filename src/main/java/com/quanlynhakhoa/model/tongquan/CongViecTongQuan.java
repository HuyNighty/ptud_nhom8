package com.quanlynhakhoa.model.tongquan;












import java.util.*;
import com.quanlynhakhoa.enums.Module;


/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class CongViecTongQuan {
    private final String nhan;
    private final long soLuong;
    private final Module phanHe;
    private final String nguCanh;

    public CongViecTongQuan(String nhan, long soLuong, Module phanHe, String nguCanh) {
        this.nhan = nhan;
        this.soLuong = soLuong;
        this.phanHe = phanHe;
        this.nguCanh = nguCanh;
    }

    public String label() { return nhan; }
    public long count() { return soLuong; }
    public Module module() { return phanHe; }
    public String context() { return nguCanh; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CongViecTongQuan value)) return false;
        return java.util.Objects.equals(nhan, value.nhan)
                && soLuong == value.soLuong
                && java.util.Objects.equals(phanHe, value.phanHe)
                && java.util.Objects.equals(nguCanh, value.nguCanh);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.nhan);
        result = 31 * result + Long.hashCode(this.soLuong);
        result = 31 * result + java.util.Objects.hashCode(this.phanHe);
        result = 31 * result + java.util.Objects.hashCode(this.nguCanh);
        return result;
    }

    @Override
    public String toString() {
        return "WorkItem[" + "label=" + nhan + ", " + "count=" + soLuong + ", " + "module=" + phanHe + ", " + "context=" + nguCanh + "]";
    }
}
