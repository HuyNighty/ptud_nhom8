package com.quanlynhakhoa.model.tongquan;












import java.util.*;




/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class CanhBaoTonKho {
    private final String kind;
    private final String item;
    private final String detail;
    private final String context;

    public CanhBaoTonKho(String kind, String item, String detail, String context) {
        this.kind = kind;
        this.item = item;
        this.detail = detail;
        this.context = context;
    }

    public String kind() { return kind; }
    public String item() { return item; }
    public String detail() { return detail; }
    public String context() { return context; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CanhBaoTonKho value)) return false;
        return java.util.Objects.equals(kind, value.kind)
                && java.util.Objects.equals(item, value.item)
                && java.util.Objects.equals(detail, value.detail)
                && java.util.Objects.equals(context, value.context);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.kind);
        result = 31 * result + java.util.Objects.hashCode(this.item);
        result = 31 * result + java.util.Objects.hashCode(this.detail);
        result = 31 * result + java.util.Objects.hashCode(this.context);
        return result;
    }

    @Override
    public String toString() {
        return "StockAlert[" + "kind=" + kind + ", " + "item=" + item + ", " + "detail=" + detail + ", " + "context=" + context + "]";
    }
}
