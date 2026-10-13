package vn.n08.nhakhoa.model;

import java.time.LocalDateTime;

/** Bảng LichHen. Đổi lịch không sửa bản ghi cũ: bản cũ → DA_DOI_LICH, bản mới trỏ maLichHenGoc. */
public record LichHen(
        String maLichHen,
        String maBenhNhan,
        String maNhanVien,
        LocalDateTime thoiGianHen,
        String lyDoHen,
        TrangThaiLichHen trangThai,
        String maLichHenGoc,
        String maKeHoach,
        String lyDoHuy,
        String maTaiKhoanTao,
        LocalDateTime ngayTao) {
    public boolean dangMo() { return trangThai == TrangThaiLichHen.CHO_XAC_NHAN || trangThai == TrangThaiLichHen.DA_XAC_NHAN; }
}
