package com.quanlynhakhoa.model.nhatky;

import java.time.LocalDate;
import java.util.Set;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class BoLocNhatKy {
    private final LocalDate from;
    private final LocalDate to;
    private final String keyword;
    private final String result;

    public BoLocNhatKy(LocalDate from, LocalDate to, String keyword, String result) {
        keyword = keyword == null ? "" : keyword.trim();
        result = result == null || result.isBlank() ? null : result.trim();
        if (keyword.length() > 80) {
        throw new IllegalArgumentException("Từ khóa tối đa 80 ký tự.");
        }
        if (from != null && to != null && from.isAfter(to)) {
        throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
        }
        if (result != null && !RESULTS.contains(result)) {
        throw new IllegalArgumentException("Bộ lọc kết quả không hợp lệ.");
        }
        this.from = from;
        this.to = to;
        this.keyword = keyword;
        this.result = result;
    }

    public LocalDate from() { return from; }
    public LocalDate to() { return to; }
    public String keyword() { return keyword; }
    public String result() { return result; }

    private static final Set<String> RESULTS = Set.of("SUCCESS", "DENIED", "FAILED");



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BoLocNhatKy value)) return false;
        return java.util.Objects.equals(from, value.from)
                && java.util.Objects.equals(to, value.to)
                && java.util.Objects.equals(keyword, value.keyword)
                && java.util.Objects.equals(result, value.result);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.from);
        result = 31 * result + java.util.Objects.hashCode(this.to);
        result = 31 * result + java.util.Objects.hashCode(this.keyword);
        result = 31 * result + java.util.Objects.hashCode(this.result);
        return result;
    }

    @Override
    public String toString() {
        return "AuditFilter[" + "from=" + from + ", " + "to=" + to + ", " + "keyword=" + keyword + ", " + "result=" + result + "]";
    }
}
