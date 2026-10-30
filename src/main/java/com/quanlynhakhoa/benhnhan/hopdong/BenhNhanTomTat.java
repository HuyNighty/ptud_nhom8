package com.quanlynhakhoa.benhnhan.hopdong;

import java.time.LocalDate;

// HỢP ĐỒNG DỮ LIỆU cho phần Lịch hẹn (UC004) và Tiếp đón (UC005):
// thông tin TÓM TẮT của 1 bệnh nhân, đủ để lễ tân nhận ra đúng người khi đặt lịch.
//
// Chỉ có getter, không có setter: phần Lịch hẹn chỉ ĐỌC thông tin bệnh nhân,
// muốn sửa bệnh nhân thì phải qua màn hình Bệnh nhân (UC003).
// Không chứa tiền sử bệnh / dị ứng (thông tin khám, Lịch hẹn không cần).
public class BenhNhanTomTat {
    private final String maBenhNhan;     // ví dụ BN000001 — Lịch hẹn lưu mã này vào cột LichHen.maBenhNhan
    private final String hoTen;
    private final LocalDate ngaySinh;
    private final String gioiTinh;       // NAM, NU (dữ liệu cũ có thể là KHAC)
    private final String soDienThoai;    // 10 chữ số, bắt đầu bằng 0

    public BenhNhanTomTat(String maBenhNhan, String hoTen, LocalDate ngaySinh,
                          String gioiTinh, String soDienThoai) {
        this.maBenhNhan = maBenhNhan;
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.gioiTinh = gioiTinh;
        this.soDienThoai = soDienThoai;
    }

    public String getMaBenhNhan() {
        return maBenhNhan;
    }

    public String getHoTen() {
        return hoTen;
    }

    public LocalDate getNgaySinh() {
        return ngaySinh;
    }

    public String getGioiTinh() {
        return gioiTinh;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    // Đổi mã giới tính sang chữ để hiển thị trên bảng
    public String getGioiTinhHienThi() {
        if ("NAM".equals(gioiTinh)) {
            return "Nam";
        }
        if ("NU".equals(gioiTinh)) {
            return "Nữ";
        }
        return "Khác";
    }

    // Chữ hiển thị trong ô chọn bệnh nhân (JComboBox) của màn hình Lịch hẹn.
    // Ví dụ: "BN000001 - Nguyễn Thị Lan - 0909123456"
    @Override
    public String toString() {
        return maBenhNhan + " - " + hoTen + " - " + soDienThoai;
    }
}
