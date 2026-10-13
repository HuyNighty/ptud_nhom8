package vn.n08.nhakhoa.dto;

import java.time.LocalDateTime;

/** Dòng nhật ký đã ghép tên đăng nhập và tên người chịu trách nhiệm. */
public record NhatKyView(long maNhatKy, LocalDateTime thoiGian, String tenDangNhap, String chucNang, String hanhDong,
                         String doiTuong, String noiDung, String maNguoiChiuTrachNhiem, String tenNguoiChiuTrachNhiem) {}
