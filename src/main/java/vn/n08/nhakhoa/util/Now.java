package vn.n08.nhakhoa.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Thời điểm hiện tại theo giờ máy chạy ứng dụng, làm tròn đến giây (khớp DATETIME2(0)). */
public final class Now {
    private Now() {}

    public static LocalDateTime now() { return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS); }
    public static LocalDate today() { return LocalDate.now(); }
    public static LocalDateTime startOf(LocalDate d) { return d.atStartOfDay(); }
    public static LocalDateTime endOf(LocalDate d) { return d.plusDays(1).atStartOfDay(); }
}
