package vn.n08.nhakhoa.dto;

import java.util.List;

/** Phiếu xuất (UC31). maBacSiChiuTrachNhiem bắt buộc khi có thuốc kiểm soát đặc biệt. */
public record XuatKhoForm(String boPhanNhan, String nguoiNhan, String lyDo, String maBacSiChiuTrachNhiem, List<Dong> dong) {
    /** Một dòng xuất theo lô. */
    public record Dong(String maLo, int soLuong) {}
}
