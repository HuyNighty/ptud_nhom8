package vn.n08.nhakhoa.model;

import java.time.LocalDateTime;

/** [Bổ sung] UC11 — chỉ định cận lâm sàng và kết quả. */
public record ChiDinhCanLamSang(
        String maChiDinh,
        String maPhieuKham,
        String maDichVu,
        String yeuCau,
        String ketQua,
        String maNhanVien,
        LocalDateTime thoiGianChiDinh,
        LocalDateTime thoiGianKetQua,
        TrangThaiCLS trangThai) {}
