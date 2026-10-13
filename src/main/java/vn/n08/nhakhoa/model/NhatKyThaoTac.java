package vn.n08.nhakhoa.model;

import java.time.LocalDateTime;

/** [Bổ sung] Nhật ký: tài khoản thực hiện và người chịu trách nhiệm chuyên môn (nếu khác). */
public record NhatKyThaoTac(
        long maNhatKy,
        LocalDateTime thoiGian,
        String maTaiKhoan,
        String chucNang,
        String hanhDong,
        String doiTuong,
        String noiDung,
        String maNguoiChiuTrachNhiem) {}
