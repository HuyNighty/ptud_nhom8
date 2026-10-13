package vn.n08.nhakhoa.dto;

import java.time.LocalDateTime;

/** Một phiếu kho trong danh sách chung: loai = NHAP_NCC, HOAN_TRA hoặc XUAT. */
public record PhieuKhoView(String ma, String loai, LocalDateTime ngay, String doiTac, String nguoiLap, int soDong, int tongSoLuong,
                           String ghiChu, String maPhieuXuatGoc) {
    public String loaiLabel() {
        return switch (loai) { case "NHAP_NCC" -> "Nhập NCC"; case "HOAN_TRA" -> "Hoàn trả"; default -> "Xuất kho"; };
    }
}
