package vn.n08.nhakhoa.dto;

import java.util.List;

/** Dữ liệu kê đơn: lời dặn và các dòng thuốc. */
public record DonThuocForm(String loiDan, List<Dong> dong) {
    /** Một dòng kê đơn. */
    public record Dong(String maThuoc, int soLuong, String lieuDung, String cachDung, int soNgayDung) {}
}
