package com.quanlynhakhoa.benhnhan.service;

// Lỗi khi dữ liệu nhập ở 1 Ô CỤ THỂ trên form bị thiếu hoặc sai.
// Theo UC003 E1 (báo cáo docs/tuan_cuoi): "tô đỏ ô lỗi, quay lại bước 3".
//   → Service ném lỗi này kèm TÊN Ô bị sai, form dựa vào tên ô để tô đỏ đúng ô đó.
// Kế thừa IllegalArgumentException nên chỗ nào đang bắt IllegalArgumentException vẫn chạy như cũ.
public class LoiNhapLieuException extends IllegalArgumentException {
    // Tên các ô trên form Thêm / Sửa bệnh nhân
    public static final String O_HO_TEN = "hoTen";
    public static final String O_NGAY_SINH = "ngaySinh";
    public static final String O_GIOI_TINH = "gioiTinh";
    public static final String O_SO_DIEN_THOAI = "soDienThoai";
    public static final String O_DIA_CHI = "diaChi";
    public static final String O_NGUOI_LIEN_HE = "nguoiLienHe";
    public static final String O_TIEN_SU_BENH = "tienSuBenh";
    public static final String O_DI_UNG = "diUng";

    private final String tenO;

    public LoiNhapLieuException(String tenO, String thongBao) {
        super(thongBao);
        this.tenO = tenO;
    }

    public String getTenO() {
        return tenO;
    }
}
