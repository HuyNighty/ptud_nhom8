package com.quanlynhakhoa.entity;

import java.time.LocalDateTime;

/** Dữ liệu nghiệp vụ theo schema hiện có; Service/DAO điều phối lưu và chuyển trạng thái. */
public final class PhieuXuatKho {
    private final String maPhieuXuat;
    private final LocalDateTime ngayXuat;
    private final String boPhanNhan;
    private final String nguoiNhan;
    private final String lyDo;
    private final String trangThai;
    private final String maNhanVien;

    public PhieuXuatKho(String maPhieuXuat, LocalDateTime ngayXuat, String boPhanNhan, String nguoiNhan, String lyDo, String trangThai, String maNhanVien) {
        this.maPhieuXuat = maPhieuXuat;
        this.ngayXuat = ngayXuat;
        this.boPhanNhan = boPhanNhan;
        this.nguoiNhan = nguoiNhan;
        this.lyDo = lyDo;
        this.trangThai = trangThai;
        this.maNhanVien = maNhanVien;
    }

    public String getMaPhieuXuat() { return maPhieuXuat; }
    public LocalDateTime getNgayXuat() { return ngayXuat; }
    public String getBoPhanNhan() { return boPhanNhan; }
    public String getNguoiNhan() { return nguoiNhan; }
    public String getLyDo() { return lyDo; }
    public String getTrangThai() { return trangThai; }
    public String getMaNhanVien() { return maNhanVien; }
}
