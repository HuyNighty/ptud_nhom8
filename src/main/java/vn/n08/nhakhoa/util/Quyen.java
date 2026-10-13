package vn.n08.nhakhoa.util;

import vn.n08.nhakhoa.model.VaiTro;

import java.util.EnumSet;
import java.util.Set;

/**
 * Quyền theo vai trò (TaiKhoan.vaiTro). Admin (AD) có mọi quyền.
 * Bảng này tổng hợp từ 38 use case; class diagram mới không có lớp VaiTro/Quyen
 * nên quyền được cài trong mã thay vì bảng.
 */
public enum Quyen {
    TONG_QUAN(VaiTro.LT, VaiTro.BS, VaiTro.YT, VaiTro.QL, VaiTro.KHO),
    TIEP_NHAN(VaiTro.LT),                       // UC01, UC02
    LICH_HEN(VaiTro.LT),                        // UC03, UC06, UC07
    BENH_NHAN_XEM(VaiTro.LT, VaiTro.BS, VaiTro.QL), // UC09, UC23
    BENH_NHAN_TAO(VaiTro.LT, VaiTro.QL),        // UC08 — lễ tân tạo hồ sơ chính thức trực tiếp
    BENH_NHAN_SUA(VaiTro.LT, VaiTro.QL),        // sửa thông tin hành chính
    TIEN_SU(VaiTro.BS),                         // HoSoBenhAn.capNhatTienSu
    KHAM(VaiTro.BS),                            // UC10, UC11, UC16
    KE_HOACH(VaiTro.BS),                        // UC12, UC13, UC15
    TAI_KHAM(VaiTro.BS, VaiTro.LT),             // UC14
    THU_NGAN(VaiTro.LT),                        // UC04, UC05
    BANG_GIA(VaiTro.QL),                        // DichVu.capNhatDonGia (class diagram)
    HUY_GIAO_DICH(VaiTro.QL),                   // hủy lần thu nhập sai
    YEU_CAU_HUY(VaiTro.LT),                     // gửi yêu cầu hủy hóa đơn
    DUYET_HUY(VaiTro.QL),                       // Figma QL-08/09
    NHAN_VIEN(VaiTro.QL),                       // UC22, UC26
    PHAN_CONG(VaiTro.QL),                       // UC24, CaLamViec.capNhatCa
    LICH_LAM_VIEC(VaiTro.BS, VaiTro.YT, VaiTro.QL, VaiTro.LT, VaiTro.KHO), // UC17, UC18, UC20
    BAN_GIAO(VaiTro.YT, VaiTro.QL, VaiTro.LT, VaiTro.BS), // UC19
    DANH_GIA(VaiTro.QL),                        // UC25
    KHO(VaiTro.KHO),                            // UC27, UC29–UC33, UC36, UC37
    KIEM_KE(VaiTro.KHO),                        // UC28, UC35 (lập phiếu)
    DUYET_KHO(VaiTro.QL),                       // Figma QL-10
    CAP_CUU(VaiTro.KHO),                        // UC34
    TAI_KHOAN(VaiTro.QL),                       // UC21
    NHAT_KY(VaiTro.QL);

    private final Set<VaiTro> roles;

    Quyen(VaiTro... r) {
        this.roles = r.length == 0 ? EnumSet.noneOf(VaiTro.class) : EnumSet.of(r[0], r);
    }

    public boolean allows(VaiTro v) {
        return v == VaiTro.AD || roles.contains(v);
    }
}
