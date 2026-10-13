package vn.n08.nhakhoa;

import java.util.ArrayList;
import java.util.List;

/** Bộ đếm kiểm thử tối giản (không dùng JUnit để chạy được chỉ với javac/java). */
public final class Kiem {
    private static int pass;
    private static final List<String> FAIL = new ArrayList<>();
    private static String nhom = "";

    private Kiem() {}

    public static void nhom(String ten) {
        nhom = ten;
        System.out.println("\n== " + ten);
    }

    public static void dung(boolean ok, String moTa) {
        if (ok) {
            pass++;
            System.out.println("  [ĐẠT] " + moTa);
        } else {
            FAIL.add(nhom + " — " + moTa);
            System.out.println("  [LỖI] " + moTa);
        }
    }

    public interface Viec { void chay() throws Exception; }

    /** Thao tác phải bị chặn bằng BusinessException; in thông điệp nhận được. */
    public static void biChan(Viec v, String moTa) {
        try {
            v.chay();
            dung(false, moTa + " (không bị chặn)");
        } catch (vn.n08.nhakhoa.util.BusinessException e) {
            dung(true, moTa + " → \"" + e.getMessage() + "\"");
        } catch (Exception e) {
            dung(false, moTa + " (lỗi khác: " + e + ")");
        }
    }

    public static void chay(Viec v, String moTa) {
        try {
            v.chay();
            dung(true, moTa);
        } catch (Exception e) {
            dung(false, moTa + " → " + e);
            e.printStackTrace(System.out);
        }
    }

    public static int tongKet() {
        System.out.println("\n==================== KẾT QUẢ: " + pass + " đạt, " + FAIL.size() + " lỗi");
        FAIL.forEach(f -> System.out.println("  LỖI: " + f));
        return FAIL.size();
    }
}
