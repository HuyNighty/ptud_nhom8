package vn.n08.nhakhoa.model;

import java.time.LocalDateTime;

/** Bảng HoaDon. Tổng tiền / đã trả / còn nợ không lưu cột mà tính từ ChiTietHoaDon và ThanhToan. */
public record HoaDon(
        String maHoaDon,
        String maNhanVien,
        String maBenhNhan,
        String maPhieuKham,
        LocalDateTime ngayLap,
        TrangThaiHoaDon trangThai) {}
