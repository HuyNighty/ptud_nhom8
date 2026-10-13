package vn.n08.nhakhoa.model;

import java.time.LocalDate;

/** [Bổ sung] Mục trong hộp cấp cứu — cấu hình được; laDuLieuMau = dữ liệu mẫu, không phải danh mục chuyên môn chính thức. */
public record DanhMucCapCuu(
        String maMuc,
        String tenMuc,
        String donViTinh,
        int soLuongToiThieu,
        int soLuongHienCo,
        LocalDate hanSuDungGanNhat,
        String maThuoc,
        String maVatTu,
        boolean dangApDung,
        boolean laDuLieuMau,
        String ghiChu) {}
