package com.quanlynhakhoa.benhnhan.service;

public class LoiNhapLieuException extends IllegalArgumentException {
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
