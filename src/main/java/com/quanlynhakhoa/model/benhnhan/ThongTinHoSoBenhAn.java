package com.quanlynhakhoa.model.benhnhan;

import com.quanlynhakhoa.model.benhnhan.LuotKhamBenhNhan;

import java.time.LocalDate;
import com.quanlynhakhoa.entity.HoSoBenhAn;
import java.util.ArrayList;

public class ThongTinHoSoBenhAn {
    private final HoSoBenhAn hoSoBenhAn;
    private ArrayList<LuotKhamBenhNhan> danhSachLuotKham = new ArrayList<>();

    public ThongTinHoSoBenhAn() { this(null, null, null, null, null); }

    public ThongTinHoSoBenhAn(String maHoSo) { this(maHoSo, null, null, null, null); }

    public ThongTinHoSoBenhAn(String maHoSo, String maBenhNhan, LocalDate ngayLap,
                      String tienSuBenh, String diUng) {
        this.hoSoBenhAn = new HoSoBenhAn(maHoSo, maBenhNhan, ngayLap, tienSuBenh, diUng);

    }

    public String getMaHoSo() {
        return hoSoBenhAn.getMaHoSo();
    }

    public void setMaHoSo(String maHoSo) {
        hoSoBenhAn.setMaHoSo(maHoSo);
    }

    public String getMaBenhNhan() {
        return hoSoBenhAn.getMaBenhNhan();
    }

    public void setMaBenhNhan(String maBenhNhan) {
        hoSoBenhAn.setMaBenhNhan(maBenhNhan);
    }

    public LocalDate getNgayLap() {
        return hoSoBenhAn.getNgayLap();
    }

    public void setNgayLap(LocalDate ngayLap) {
        hoSoBenhAn.setNgayLap(ngayLap);
    }

    public String getTienSuBenh() {
        return hoSoBenhAn.getTienSuBenh();
    }

    public void setTienSuBenh(String tienSuBenh) {
        hoSoBenhAn.setTienSuBenh(tienSuBenh);
    }

    public String getDiUng() {
        return hoSoBenhAn.getDiUng();
    }

    public void setDiUng(String diUng) {
        hoSoBenhAn.setDiUng(diUng);
    }

    public ArrayList<LuotKhamBenhNhan> getDanhSachLuotKham() {
        return danhSachLuotKham;
    }

    public void setDanhSachLuotKham(ArrayList<LuotKhamBenhNhan> danhSachLuotKham) {
        this.danhSachLuotKham = danhSachLuotKham;
    }

}
