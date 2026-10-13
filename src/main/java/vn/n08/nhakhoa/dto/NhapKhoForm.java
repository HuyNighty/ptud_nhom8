package vn.n08.nhakhoa.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Phiếu nhập từ nhà cung cấp (UC30). */
public record NhapKhoForm(String maNhaCungCap, String ghiChu, List<Dong> dong) {
    /** Một dòng nhập theo lô. laThuoc = true nếu maHang là mã thuốc. */
    public record Dong(boolean laThuoc, String maHang, String soLo, LocalDate ngaySanXuat, LocalDate hanSuDung, int soLuong, BigDecimal donGia) {}
}
