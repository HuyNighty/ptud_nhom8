package com.quanlynhakhoa.model.kho;

import java.time.LocalDate;
import com.quanlynhakhoa.entity.LoHang;
import java.time.temporal.ChronoUnit;

/** Model chỉ đọc lô thuốc hoặc vật tư; số tồn chỉ thay đổi qua chứng từ kho. */
public class ThongTinLoHang {
    private final LoHang loHang;
    public static final String LOAI_TAT_CA = "ALL";
    public static final String LOAI_THUOC = "THUOC";
    public static final String LOAI_VAT_TU = "VATTU";
    public static final String HAN_TAT_CA = "ALL";
    public static final String HAN_HET_HAN = "HET_HAN";
    public static final String HAN_CON_HAN = "CON_HAN";

    private String tenHang;
    private String tenNhaCungCap;

    public ThongTinLoHang() { loHang = new LoHang(); }

    public ThongTinLoHang(String maLo, String soLo, LocalDate ngaySanXuat, LocalDate hanSuDung,
                  String maThuoc, String maVatTu, String tenHang,
                  String maNhaCungCap, String tenNhaCungCap, int soLuongTon) {
        this.loHang = new LoHang(maLo, soLo, ngaySanXuat, hanSuDung, maThuoc, maVatTu, maNhaCungCap, soLuongTon);





        this.tenHang = tenHang;

        this.tenNhaCungCap = tenNhaCungCap;

    }

    public String getMaLo() { return loHang.getMaLo(); }
    public void setMaLo(String maLo) { loHang.setMaLo(maLo); }
    public String getSoLo() { return loHang.getSoLo(); }
    public void setSoLo(String soLo) { loHang.setSoLo(soLo); }
    public LocalDate getNgaySanXuat() { return loHang.getNgaySanXuat(); }
    public void setNgaySanXuat(LocalDate ngaySanXuat) { loHang.setNgaySanXuat(ngaySanXuat); }
    public LocalDate getHanSuDung() { return loHang.getHanSuDung(); }
    public void setHanSuDung(LocalDate hanSuDung) { loHang.setHanSuDung(hanSuDung); }
    public String getMaThuoc() { return loHang.getMaThuoc(); }
    public void setMaThuoc(String maThuoc) { loHang.setMaThuoc(maThuoc); }
    public String getMaVatTu() { return loHang.getMaVatTu(); }
    public void setMaVatTu(String maVatTu) { loHang.setMaVatTu(maVatTu); }
    public String getTenHang() { return tenHang; }
    public void setTenHang(String tenHang) { this.tenHang = tenHang; }
    public String getMaNhaCungCap() { return loHang.getMaNhaCungCap(); }
    public void setMaNhaCungCap(String maNhaCungCap) { loHang.setMaNhaCungCap(maNhaCungCap); }
    public String getTenNhaCungCap() { return tenNhaCungCap; }
    public void setTenNhaCungCap(String tenNhaCungCap) { this.tenNhaCungCap = tenNhaCungCap; }
    public int getSoLuongTon() { return loHang.getSoLuongTon(); }
    public void setSoLuongTon(int soLuongTon) { loHang.setSoLuongTon(soLuongTon); }

    public String getLoaiHangHienThi() { return loHang.getMaThuoc() != null ? "Thuốc" : "Vật tư"; }

    public boolean daHetHan(LocalDate ngay) {
        return loHang.getHanSuDung() != null && loHang.getHanSuDung().isBefore(ngay);
    }

    public String getTinhTrangHan() {
        if (loHang.getHanSuDung() == null) return "Không có HSD";
        return daHetHan(LocalDate.now()) ? "Đã hết hạn" : "Còn hạn";
    }

    public long getSoNgayConLai(LocalDate ngay) {
        return loHang.getHanSuDung() == null ? Long.MAX_VALUE : ChronoUnit.DAYS.between(ngay, loHang.getHanSuDung());
    }
}
