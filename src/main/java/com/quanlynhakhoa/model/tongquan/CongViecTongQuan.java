package com.quanlynhakhoa.model.tongquan;












import java.util.*;
import com.quanlynhakhoa.hethong.dieuhuong.Module;


/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class CongViecTongQuan {
    private final String label;
    private final long count;
    private final Module module;
    private final String context;

    public CongViecTongQuan(String label, long count, Module module, String context) {
        this.label = label;
        this.count = count;
        this.module = module;
        this.context = context;
    }

    public String label() { return label; }
    public long count() { return count; }
    public Module module() { return module; }
    public String context() { return context; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CongViecTongQuan value)) return false;
        return java.util.Objects.equals(label, value.label)
                && count == value.count
                && java.util.Objects.equals(module, value.module)
                && java.util.Objects.equals(context, value.context);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.label);
        result = 31 * result + Long.hashCode(this.count);
        result = 31 * result + java.util.Objects.hashCode(this.module);
        result = 31 * result + java.util.Objects.hashCode(this.context);
        return result;
    }

    @Override
    public String toString() {
        return "WorkItem[" + "label=" + label + ", " + "count=" + count + ", " + "module=" + module + ", " + "context=" + context + "]";
    }
}
