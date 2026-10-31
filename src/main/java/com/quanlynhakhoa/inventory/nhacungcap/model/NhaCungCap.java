package com.quanlynhakhoa.inventory.nhacungcap.model;

/** Model ánh xạ một nhà cung cấp trong dbo.NhaCungCap. */
public class NhaCungCap {
    public static final String DANG_HOP_TAC = "DANG_HOP_TAC";
    public static final String NGUNG_HOP_TAC = "NGUNG_HOP_TAC";

    private String maNhaCungCap;
    private String tenNhaCungCap;
    private String diaChi;
    private String soDienThoai;
    private String email;
    private String trangThai;

    public NhaCungCap() { }

    public NhaCungCap(String maNhaCungCap, String tenNhaCungCap, String diaChi,
                      String soDienThoai, String email, String trangThai) {
        this.maNhaCungCap = maNhaCungCap;
        this.tenNhaCungCap = tenNhaCungCap;
        this.diaChi = diaChi;
        this.soDienThoai = soDienThoai;
        this.email = email;
        this.trangThai = trangThai;
    }

    public String getMaNhaCungCap() { return maNhaCungCap; }
    public void setMaNhaCungCap(String maNhaCungCap) { this.maNhaCungCap = maNhaCungCap; }
    public String getTenNhaCungCap() { return tenNhaCungCap; }
    public void setTenNhaCungCap(String tenNhaCungCap) { this.tenNhaCungCap = tenNhaCungCap; }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public String getTrangThaiHienThi() {
        return DANG_HOP_TAC.equals(trangThai) ? "Đang hợp tác" : "Ngừng hợp tác";
    }
}
