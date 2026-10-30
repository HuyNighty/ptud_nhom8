package com.quanlynhakhoa.lichhen.model;

// Lớp BacSi: 1 nhân viên có chức danh bác sĩ (dbo.NhanVien, chucDanh = 'BS'),
// chỉ gồm mã và họ tên — đủ để chọn bác sĩ khi đặt lịch.
public class BacSi {
    private String maNhanVien;
    private String hoTen;

    public BacSi(String maNhanVien, String hoTen) {
        this.maNhanVien = maNhanVien;
        this.hoTen = hoTen;
    }

    public String getMaNhanVien() {
        return maNhanVien;
    }

    public String getHoTen() {
        return hoTen;
    }

    // Chữ hiển thị trong ô chọn bác sĩ (JComboBox). Ví dụ: "Trần Văn Nam (NV000003)"
    @Override
    public String toString() {
        return hoTen + " (" + maNhanVien + ")";
    }
}
