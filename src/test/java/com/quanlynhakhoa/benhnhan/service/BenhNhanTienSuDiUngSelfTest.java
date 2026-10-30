package com.quanlynhakhoa.benhnhan.service;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.DesktopSession;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

/** No DB: tiền sử bệnh + dị ứng là thông tin bệnh nhân, kiểm tra trong kiemTraDuLieu. */
public final class BenhNhanTienSuDiUngSelfTest {
    private static int checks;

    private static void check(boolean b, String message) {
        checks++;
        if (!b) throw new AssertionError(message);
    }

    // Bệnh nhân hợp lệ ở mọi trường khác, chỉ thay đổi tiền sử / dị ứng
    private static BenhNhan benhNhan(String tienSu, String diUng) {
        BenhNhan bn = new BenhNhan(null, "Nguyễn Thị Lan", LocalDate.of(1998, 5, 12), "NU",
                "0909123456", null, null, null);
        bn.setTienSuBenh(tienSu);
        bn.setDiUng(diUng);
        return bn;
    }

    private static String loi(BenhNhanService service, BenhNhan bn) {
        try {
            service.kiemTraDuLieu(bn);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    public static void main(String[] args) {
        AtomicInteger opened = new AtomicInteger();
        BenhNhanService service = new BenhNhanService(
                DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "SYNTHETIC", Role.RECEPTIONIST, false)),
                () -> {
                    opened.incrementAndGet();
                    throw new SQLException("Should not open");
                });

        // Để trống → lưu NULL; có chữ → bỏ khoảng trắng 2 đầu
        BenhNhan trong = benhNhan("   ", "");
        check(loi(service, trong) == null, "blank values accepted");
        check(trong.getTienSuBenh() == null && trong.getDiUng() == null, "blank values become null");

        BenhNhan coChu = benhNhan("  Cao huyết áp  ", " Penicillin ");
        check(loi(service, coChu) == null, "normal values accepted");
        check("Cao huyết áp".equals(coChu.getTienSuBenh()), "history trimmed");
        check("Penicillin".equals(coChu.getDiUng()), "allergy trimmed");

        // Đúng giới hạn → nhận; vượt 1 ký tự → báo lỗi có ghi số ký tự
        check(loi(service, benhNhan("a".repeat(BenhNhanService.TIEN_SU_TOI_DA), null)) == null, "500 chars ok");
        check(loi(service, benhNhan(null, "a".repeat(BenhNhanService.DI_UNG_TOI_DA))) == null, "300 chars ok");
        String loiTienSu = loi(service, benhNhan("a".repeat(501), null));
        check(loiTienSu != null && loiTienSu.contains("500") && loiTienSu.contains("501"), "501-char history rejected");
        String loiDiUng = loi(service, benhNhan(null, "a".repeat(301)));
        check(loiDiUng != null && loiDiUng.contains("300"), "301-char allergy rejected");

        // Kiểm tra dữ liệu không đụng database
        check(opened.get() == 0, "validation never opens SQL");
        System.out.println("PASS: BENH NHAN TIEN SU / DI UNG (" + checks + " checks; no DB, no GUI)");
    }
}
