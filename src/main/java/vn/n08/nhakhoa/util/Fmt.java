package vn.n08.nhakhoa.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** Định dạng và đọc tiền, ngày giờ theo kiểu Việt Nam. */
public final class Fmt {
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");
    public static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] THU = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ nhật"};
    private static final String[] THU_NGAN = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private Fmt() {}

    public static String money(BigDecimal v) {
        if (v == null) return "—";
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ROOT);
        s.setGroupingSeparator('.');
        DecimalFormat f = new DecimalFormat("#,##0", s);
        String r = f.format(v.abs());
        return (v.signum() < 0 ? "−" : "") + r + " ₫";
    }

    /** Đọc số tiền người dùng nhập: "1.200.000", "1200000", "1,2tr" không hỗ trợ. */
    public static BigDecimal parseMoney(String s, String field, String label) {
        String digits = s == null ? "" : s.replaceAll("[.\\s₫,đ]", "");
        if (digits.isEmpty() || !digits.matches("\\d{1,15}")) throw new BusinessException(field, label + " phải là số nguyên dương (VD: 500.000).");
        return new BigDecimal(digits);
    }

    public static int parseInt(String s, String field, String label) {
        String t = s == null ? "" : s.trim().replace(".", "");
        if (!t.matches("-?\\d{1,9}")) throw new BusinessException(field, label + " phải là số nguyên.");
        return Integer.parseInt(t);
    }

    public static String date(LocalDate d) { return d == null ? "—" : d.format(DATE); }
    public static String dateTime(LocalDateTime d) { return d == null ? "—" : d.format(DATE_TIME); }
    public static String shortDt(LocalDateTime d) { return d == null ? "—" : d.format(SHORT); }
    public static String time(LocalTime t) { return t == null ? "—" : t.format(TIME); }
    public static String time(LocalDateTime t) { return t == null ? "—" : t.format(TIME); }

    public static String weekday(LocalDate d) { return THU[d.getDayOfWeek().getValue() - 1]; }
    public static String weekdayShort(LocalDate d) { return THU_NGAN[d.getDayOfWeek().getValue() - 1]; }
    public static String longDate(LocalDate d) { return weekday(d) + ", " + date(d); }

    /** Đọc ngày dd/MM/yyyy, báo lỗi theo ô nhập. */
    public static LocalDate parseDate(String s, String field, String label) {
        if (s == null || s.isBlank()) throw new BusinessException(field, "Nhập " + label.toLowerCase() + " (dd/mm/yyyy).");
        try {
            return LocalDate.parse(s.trim(), DATE);
        } catch (DateTimeParseException e) {
            throw new BusinessException(field, label + " không hợp lệ, nhập theo dạng dd/mm/yyyy.");
        }
    }

    public static LocalDate parseDateOrNull(String s, String field, String label) {
        return s == null || s.isBlank() ? null : parseDate(s, field, label);
    }

    public static LocalTime parseTime(String s, String field, String label) {
        try {
            return LocalTime.parse(s.trim(), TIME);
        } catch (Exception e) {
            throw new BusinessException(field, label + " không hợp lệ (HH:mm).");
        }
    }

    public static String nz(String s) { return s == null || s.isBlank() ? "—" : s; }
}
