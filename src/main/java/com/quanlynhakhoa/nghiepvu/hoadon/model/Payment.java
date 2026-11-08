package com.quanlynhakhoa.nghiepvu.hoadon.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Payment(String maThanhToan, String maHoaDon, LocalDateTime thoiGian, BigDecimal soTien,
                        PhuongThuc phuongThuc, String maGiaoDich, BigDecimal tienKhachDua,
                        TrangThai trangThai, String maNhanVien, String maTaiKhoanHuy,
                        LocalDateTime thoiGianHuy, String lyDoHuy) {
    public enum TrangThai {
        THANH_CONG, DA_HUY, DA_HOAN_TIEN;
        @Override public String toString() { return switch(this){case THANH_CONG -> "Thành công";case DA_HUY -> "Hủy ghi nhận";case DA_HOAN_TIEN -> "Đã hoàn tiền";}; }
    }
    public enum PhuongThuc {
        TIEN_MAT, CHUYEN_KHOAN, THE_POS;
        @Override public String toString() {
            return switch (this) { case TIEN_MAT -> "Tiền mặt"; case CHUYEN_KHOAN -> "Chuyển khoản"; case THE_POS -> "Thẻ POS"; };
        }
    }
    public Payment {
        if (soTien == null || soTien.signum() <= 0 || soTien.stripTrailingZeros().scale() > 0
                || trangThai == null || phuongThuc == null) throw new IllegalArgumentException("Lần thu bất nhất.");
    }
}
