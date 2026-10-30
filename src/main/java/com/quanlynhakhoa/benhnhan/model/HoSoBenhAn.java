package com.quanlynhakhoa.benhnhan.model;

import java.time.LocalDate;
import java.util.ArrayList;

// Lớp HoSoBenhAn: 1 dòng trong bảng dbo.HoSoBenhAn + danh sách các lần khám của bệnh nhân.
// Mỗi bệnh nhân có đúng 1 hồ sơ bệnh án (maBenhNhan là UNIQUE trong database).
// Theo báo cáo (thuật ngữ 22, UC009): hồ sơ bệnh án = lịch sử khám, chẩn đoán, điều trị.
public class HoSoBenhAn {
    private String maHoSo;
    private String maBenhNhan;
    private LocalDate ngayLap;
    private String tienSuBenh;    // tối đa 500 ký tự, có thể để trống
    private String diUng;         // tối đa 300 ký tự, có thể để trống
    private ArrayList<LuotKham> dsLuotKham = new ArrayList<>();   // các lần khám, mới nhất ở đầu

    // Hàm khởi tạo rỗng
    public HoSoBenhAn() {
    }

    // Hàm khởi tạo chỉ có mã
    public HoSoBenhAn(String maHoSo) {
        this.maHoSo = maHoSo;
    }

    // Hàm khởi tạo đầy đủ
    public HoSoBenhAn(String maHoSo, String maBenhNhan, LocalDate ngayLap,
                      String tienSuBenh, String diUng) {
        this.maHoSo = maHoSo;
        this.maBenhNhan = maBenhNhan;
        this.ngayLap = ngayLap;
        this.tienSuBenh = tienSuBenh;
        this.diUng = diUng;
    }

    public String getMaHoSo() {
        return maHoSo;
    }

    public void setMaHoSo(String maHoSo) {
        this.maHoSo = maHoSo;
    }

    public String getMaBenhNhan() {
        return maBenhNhan;
    }

    public void setMaBenhNhan(String maBenhNhan) {
        this.maBenhNhan = maBenhNhan;
    }

    public LocalDate getNgayLap() {
        return ngayLap;
    }

    public void setNgayLap(LocalDate ngayLap) {
        this.ngayLap = ngayLap;
    }

    public String getTienSuBenh() {
        return tienSuBenh;
    }

    public void setTienSuBenh(String tienSuBenh) {
        this.tienSuBenh = tienSuBenh;
    }

    public String getDiUng() {
        return diUng;
    }

    public void setDiUng(String diUng) {
        this.diUng = diUng;
    }

    public ArrayList<LuotKham> getDsLuotKham() {
        return dsLuotKham;
    }

    public void setDsLuotKham(ArrayList<LuotKham> dsLuotKham) {
        this.dsLuotKham = dsLuotKham;
    }
}
