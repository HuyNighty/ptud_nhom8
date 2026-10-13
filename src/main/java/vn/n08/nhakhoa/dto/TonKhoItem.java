package vn.n08.nhakhoa.dto;

import java.time.LocalDate;

/**
 * Tồn kho tổng hợp của một thuốc / vật tư (UC36): cộng từ các lô.
 * tonKhaDung chỉ tính lô chưa hết hạn; tonHetHan là số còn nằm trong lô đã hết hạn (chờ lập phiếu hủy).
 */
public record TonKhoItem(String ma, String ten, boolean laThuoc, String donViTinh, String moTa, boolean kiemSoatDacBiet,
                         int dinhMucToiThieu, boolean daDatHang, int tonKhaDung, int tonHetHan, int soLo, LocalDate hsdGanNhat) {
    public boolean duoiDinhMuc() { return tonKhaDung < dinhMucToiThieu; }
    public String loai() { return laThuoc ? "Thuốc" : "Vật tư"; }
}
