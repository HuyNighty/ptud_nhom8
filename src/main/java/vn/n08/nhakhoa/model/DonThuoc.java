package vn.n08.nhakhoa.model;

import java.time.LocalDate;

/** Bảng DonThuoc — tối đa 1 đơn cho 1 phiếu khám. */
public record DonThuoc(
        String maDonThuoc,
        String maPhieuKham,
        String maBenhNhan,
        String maNhanVien,
        LocalDate ngayKe,
        String loiDan) {}
