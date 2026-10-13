package vn.n08.nhakhoa.util;

import java.util.regex.Pattern;

/** Kiểm tra dữ liệu đầu vào dùng chung. Mỗi hàm ném BusinessException kèm mã ô nhập. */
public final class Validator {
    private static final Pattern PHONE = Pattern.compile("^0\\d{9}$");
    private static final Pattern LANDLINE = Pattern.compile("^0\\d{9,10}$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9._]{3,30}$");

    private Validator() {}

    public static String required(String v, String field, String label) {
        if (v == null || v.isBlank()) throw new BusinessException(field, "Nhập " + label.toLowerCase() + ".");
        return v.trim();
    }

    public static String maxLen(String v, int max, String field, String label) {
        if (v != null && v.length() > max) throw new BusinessException(field, label + " tối đa " + max + " ký tự.");
        return v;
    }

    public static String optional(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** Số di động Việt Nam: 10 số, bắt đầu bằng 0. Cho phép người dùng nhập có khoảng trắng hoặc dấu chấm. */
    public static String phone(String v, String field) {
        String s = normalizePhone(v);
        if (!PHONE.matcher(s).matches()) throw new BusinessException(field, "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.");
        return s;
    }

    /** Số điện thoại cố định hoặc di động (10–11 số). */
    public static String anyPhone(String v, String field) {
        String s = normalizePhone(v);
        if (!LANDLINE.matcher(s).matches()) throw new BusinessException(field, "Số điện thoại gồm 10–11 chữ số, bắt đầu bằng 0.");
        return s;
    }

    public static String normalizePhone(String v) {
        return v == null ? "" : v.replaceAll("[\\s.\\-]", "");
    }

    public static String email(String v, String field) {
        String s = optional(v);
        if (s != null && !EMAIL.matcher(s).matches()) throw new BusinessException(field, "Email không hợp lệ.");
        return s;
    }

    public static String username(String v, String field) {
        String s = v == null ? "" : v.trim();
        if (!USERNAME.matcher(s).matches()) throw new BusinessException(field, "Tên đăng nhập 3–30 ký tự: chữ thường không dấu, số, dấu chấm hoặc gạch dưới.");
        return s;
    }

    public static void positive(int v, String field, String label) {
        if (v <= 0) throw new BusinessException(field, label + " phải lớn hơn 0.");
    }

    public static void check(boolean ok, String field, String message) {
        if (!ok) throw new BusinessException(field, message);
    }

    public static void check(boolean ok, String message) {
        if (!ok) throw new BusinessException(message);
    }
}
