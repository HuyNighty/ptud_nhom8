package vn.n08.nhakhoa.model;

import java.time.LocalDate;

/** Bảng LoHang — lô của đúng một thuốc hoặc một vật tư. */
public record LoHang(
        String maLo,
        String soLo,
        LocalDate ngaySanXuat,
        LocalDate hanSuDung,
        String maThuoc,
        String maVatTu,
        String maNhaCungCap,
        int soLuongTon) {
    public boolean daHetHan(LocalDate homNay) { return hanSuDung != null && !hanSuDung.isAfter(homNay); }
    public boolean sapHetHan(LocalDate homNay, int soNgay) { return hanSuDung != null && !daHetHan(homNay) && !hanSuDung.isAfter(homNay.plusDays(soNgay)); }
    public String maHang() { return maThuoc != null ? maThuoc : maVatTu; }
}
