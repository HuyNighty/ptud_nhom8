package com.quanlynhakhoa.quantri.lichlamviec.entity;
import java.time.LocalDate;
public record PhanCong(String id, String nhanVienId, String tenNhanVien, CaLamViec ca,
        LocalDate ngay, String nhiemVu, String trangThai, String lyDoHuy) {
    public String trangThaiHienThi() { return switch (trangThai) {
        case "CHO_HIEU_LUC" -> "Chờ hiệu lực";
        case "DANG_THUC_HIEN" -> "Đang thực hiện";
        case "HOAN_THANH" -> "Hoàn thành";
        case "DA_HUY" -> "Đã hủy";
        default -> trangThai;
    }; }
}
