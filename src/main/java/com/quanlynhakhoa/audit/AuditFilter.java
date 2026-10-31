package com.quanlynhakhoa.audit;

import java.time.LocalDate;
import java.util.Set;

/** Explicit local calendar filters; the DAO translates Vietnam dates to UTC boundaries. */
public record AuditFilter(LocalDate from, LocalDate to, String keyword, String result) {
    private static final Set<String> RESULTS = Set.of("SUCCESS", "DENIED", "FAILED");

    public AuditFilter {
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
    }
}
