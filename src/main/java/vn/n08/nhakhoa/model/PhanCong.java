package vn.n08.nhakhoa.model;

import java.time.LocalDate;

/** Bảng PhanCong. Thay người = hủy bản cũ (có lý do) + tạo bản mới trỏ maPhanCongGoc. */
public record PhanCong(
        String maPhanCong,
        String maNhanVien,
        String maCa,
        LocalDate ngayLam,
        String nhiemVu,
        TrangThaiPhanCong trangThai,
        String lyDoHuy,
        String maPhanCongGoc) {
    public boolean conHieuLuc() { return trangThai == TrangThaiPhanCong.CHO_HIEU_LUC || trangThai == TrangThaiPhanCong.DANG_THUC_HIEN; }
}
