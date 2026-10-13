package vn.n08.nhakhoa.model;

import java.time.LocalDateTime;

/** [Bổ sung] Một lần kiểm tra hộp cấp cứu. */
public record KiemTraCapCuu(
        String maKiemTra,
        LocalDateTime thoiGian,
        String maNhanVien,
        KetQuaKiemTra ketQua,
        String ghiChu) {}
