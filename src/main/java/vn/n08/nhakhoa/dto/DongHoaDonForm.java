package vn.n08.nhakhoa.dto;

import java.math.BigDecimal;

/** Một dòng khi lập hóa đơn. donGia chỉ dùng cho thuốc/vật tư; dịch vụ lấy giá từ bảng giá. */
public record DongHoaDonForm(Loai loai, String ma, int soLuong, BigDecimal donGia) {
    public enum Loai {
        DICH_VU("Dịch vụ"), THUOC("Thuốc"), VAT_TU("Vật tư");
        private final String label;
        Loai(String l) { label = l; }
        @Override public String toString() { return label; }
    }
}
