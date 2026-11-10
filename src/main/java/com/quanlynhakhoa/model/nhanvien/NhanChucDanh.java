package com.quanlynhakhoa.model.nhanvien;

import java.util.Map;

/** Nhãn trình bày chức danh; không thay đổi mã lưu trữ hoặc quyết định phân quyền. */
public final class NhanChucDanh {
    private static final Map<String, String> LABELS = Map.of(
            "AD", "Quản trị hệ thống", "LT", "Lễ tân", "BS", "Bác sĩ",
            "YT", "Y tá", "QL", "Quản lý phòng khám", "KHO", "Quản lý kho");
    private NhanChucDanh() { }
    public static String label(String code) {
        return code == null ? "—" : LABELS.getOrDefault(code, code);
    }
}
