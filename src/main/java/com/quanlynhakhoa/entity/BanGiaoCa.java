package com.quanlynhakhoa.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Dữ liệu nghiệp vụ theo schema hiện có; Service/DAO điều phối lưu và chuyển trạng thái. */
public final class BanGiaoCa {
    private final String maBanGiao;
    private final LocalDateTime thoiGian;
    private final String noiDung;
    private final String trangThai;
    private final LocalDate ngay;
    private final String maNguoiGiao;
    private final String maNguoiNhan;
    private final String maCaGiao;
    private final String maCaNhan;
    private final boolean coChenhLech;
    private final String maTaiKhoanTao;
    private final String maTaiKhoanXacNhan;
    private final LocalDateTime thoiGianXacNhan;
    private final String ghiChuXacNhan;
    private final String maTaiKhoanHuy;
    private final LocalDateTime thoiGianHuy;
    private final String lyDoHuy;

    public BanGiaoCa(String maBanGiao, LocalDateTime thoiGian, String noiDung, String trangThai, LocalDate ngay, String maNguoiGiao, String maNguoiNhan, String maCaGiao, String maCaNhan, boolean coChenhLech, String maTaiKhoanTao, String maTaiKhoanXacNhan, LocalDateTime thoiGianXacNhan, String ghiChuXacNhan, String maTaiKhoanHuy, LocalDateTime thoiGianHuy, String lyDoHuy) {
        this.maBanGiao = maBanGiao;
        this.thoiGian = thoiGian;
        this.noiDung = noiDung;
        this.trangThai = trangThai;
        this.ngay = ngay;
        this.maNguoiGiao = maNguoiGiao;
        this.maNguoiNhan = maNguoiNhan;
        this.maCaGiao = maCaGiao;
        this.maCaNhan = maCaNhan;
        this.coChenhLech = coChenhLech;
        this.maTaiKhoanTao = maTaiKhoanTao;
        this.maTaiKhoanXacNhan = maTaiKhoanXacNhan;
        this.thoiGianXacNhan = thoiGianXacNhan;
        this.ghiChuXacNhan = ghiChuXacNhan;
        this.maTaiKhoanHuy = maTaiKhoanHuy;
        this.thoiGianHuy = thoiGianHuy;
        this.lyDoHuy = lyDoHuy;
    }

    public String getMaBanGiao() { return maBanGiao; }
    public LocalDateTime getThoiGian() { return thoiGian; }
    public String getNoiDung() { return noiDung; }
    public String getTrangThai() { return trangThai; }
    public LocalDate getNgay() { return ngay; }
    public String getMaNguoiGiao() { return maNguoiGiao; }
    public String getMaNguoiNhan() { return maNguoiNhan; }
    public String getMaCaGiao() { return maCaGiao; }
    public String getMaCaNhan() { return maCaNhan; }
    public boolean isCoChenhLech() { return coChenhLech; }
    public String getMaTaiKhoanTao() { return maTaiKhoanTao; }
    public String getMaTaiKhoanXacNhan() { return maTaiKhoanXacNhan; }
    public LocalDateTime getThoiGianXacNhan() { return thoiGianXacNhan; }
    public String getGhiChuXacNhan() { return ghiChuXacNhan; }
    public String getMaTaiKhoanHuy() { return maTaiKhoanHuy; }
    public LocalDateTime getThoiGianHuy() { return thoiGianHuy; }
    public String getLyDoHuy() { return lyDoHuy; }
}
