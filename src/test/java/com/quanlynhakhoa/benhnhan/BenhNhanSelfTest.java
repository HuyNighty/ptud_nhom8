package com.quanlynhakhoa.benhnhan;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.benhnhan.controller.BenhNhanController;
import com.quanlynhakhoa.benhnhan.service.BenhNhanService;
import com.quanlynhakhoa.benhnhan.service.KetNoiCsdl;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.DesktopSession;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Self-test không cần SQL Server: phiên giả lập + kết nối giả (không ghi dữ liệu).
 * Chạy: java -cp target/classes;target/test-classes com.quanlynhakhoa.benhnhan.BenhNhanSelfTest
 */
public final class BenhNhanSelfTest {
    private static int dat;
    private static int truot;

    /** Kết nối giả: đếm số lần mở, luôn ném lỗi để không bao giờ chạm DB thật. */
    private static final class KetNoiGia implements KetNoiCsdl {
        final AtomicInteger soLanMo = new AtomicInteger();

        @Override
        public java.sql.Connection moKetNoi() throws SQLException {
            soLanMo.incrementAndGet();
            throw new SQLException("KET_NOI_GIA", "08000");
        }
    }

    public static void main(String[] args) {
        kiemTraPhanQuyenDoc();
        kiemTraDuLieuTimKiem();
        kiemTraMaBenhNhan();
        kiemTraThongDiepLoi();
        System.out.println("BenhNhanSelfTest: " + dat + " PASS, " + truot + " FAIL");
        if (truot > 0) System.exit(1);
    }

    private static void kiemTraPhanQuyenDoc() {
        for (Role vaiTro : Role.values()) {
            boolean duocDoc = vaiTro == Role.RECEPTIONIST || vaiTro == Role.DOCTOR
                    || vaiTro == Role.SYSTEM_ADMIN;
            KetNoiGia ketNoi = new KetNoiGia();
            BenhNhanService service = new BenhNhanService(phien(vaiTro), ketNoi);
            Throwable loi = chay(() -> service.timKiem("Lan", null));
            if (duocDoc) {
                xacNhan(loi instanceof SQLException && ketNoi.soLanMo.get() == 1,
                        vaiTro + " được tìm kiếm (đi tới bước mở kết nối)");
            } else {
                xacNhan(loi instanceof AccessDeniedException && ketNoi.soLanMo.get() == 0,
                        vaiTro + " bị chặn trước khi mở kết nối");
            }
        }
        DesktopSession daDangXuat = phien(Role.RECEPTIONIST);
        daDangXuat.logout();
        KetNoiGia ketNoi = new KetNoiGia();
        Throwable loi = chay(() -> new BenhNhanService(daDangXuat, ketNoi).timKiem("", null));
        xacNhan(loi instanceof AccessDeniedException && ketNoi.soLanMo.get() == 0,
                "phiên đã đăng xuất bị chặn");
    }

    private static void kiemTraDuLieuTimKiem() {
        BenhNhanService service = new BenhNhanService(phien(Role.RECEPTIONIST), new KetNoiGia());
        Throwable loi = chay(() -> service.timKiem("x".repeat(101), null));
        xacNhan(loi instanceof IllegalArgumentException, "từ khóa > 100 ký tự bị từ chối");

        loi = chay(() -> service.timKiem("0909 123 4567", null));
        xacNhan(loi instanceof IllegalArgumentException
                        && loi.getMessage().startsWith("Số điện thoại không hợp lệ"),
                "SĐT 11 số bị báo sai định dạng");

        loi = chay(() -> service.timKiem("0909 123", null));
        xacNhan(loi instanceof SQLException, "SĐT một phần (7 số) được phép tìm");
    }

    private static void kiemTraMaBenhNhan() {
        xacNhan("BN000001".equals(BenhNhanService.kiemTraMaBenhNhan(" bn000001 ")),
                "mã bn000001 được chuẩn hóa thành BN000001");
        Throwable loi = chay(() -> BenhNhanService.kiemTraMaBenhNhan("BN1"));
        xacNhan(loi instanceof IllegalArgumentException, "mã BN1 bị từ chối");
    }

    private static void kiemTraThongDiepLoi() {
        xacNhan(BenhNhanController.thongDiepLoi(new AccessDeniedException()).contains("không có quyền"),
                "lỗi quyền → câu tiếng Việt");
        String sql = BenhNhanController.thongDiepLoi(new SQLException("SELECT * FROM BenhNhan", "S0001", 208));
        xacNhan(!sql.contains("SELECT") && sql.contains("S0001"), "lỗi SQL không lộ câu truy vấn");
    }

    // ---------------------------------------------------------------------

    @FunctionalInterface
    private interface ViecCoLoi {
        void chay() throws Exception;
    }

    private static Throwable chay(ViecCoLoi viec) {
        try {
            viec.chay();
            return null;
        } catch (Throwable loi) {
            return loi;
        }
    }

    static DesktopSession phien(Role vaiTro) {
        return DesktopSession.fromSuccessfulLogin(new AuthDecision(true, "SYNTHETIC", vaiTro, false));
    }

    static void xacNhan(boolean dieuKien, String moTa) {
        if (dieuKien) {
            dat++;
            System.out.println("[PASS] " + moTa);
        } else {
            truot++;
            System.out.println("[FAIL] " + moTa);
        }
    }
}
