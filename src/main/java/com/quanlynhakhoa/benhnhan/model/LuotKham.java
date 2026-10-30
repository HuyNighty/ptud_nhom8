package com.quanlynhakhoa.benhnhan.model;

import java.time.LocalDateTime;

// Lớp LuotKham: 1 lần khám của bệnh nhân (đọc từ bảng dbo.PhieuKham, kèm tên bác sĩ từ dbo.NhanVien)
// Chỉ dùng để XEM hồ sơ bệnh án; việc tạo / sửa phiếu khám thuộc phần Khám & điều trị.
public class LuotKham {
    private String maPhieuKham;
    private LocalDateTime thoiGianTiepNhan;
    private String tenBacSi;
    private String trieuChung;
    private String chanDoan;
    private String ketLuan;
    private String trangThai;     // CHO_KHAM, DANG_KHAM, DA_KET_LUAN, DA_HUY (giống database)

    // Hàm khởi tạo rỗng
    public LuotKham() {
    }

    // Hàm khởi tạo đầy đủ
    public LuotKham(String maPhieuKham, LocalDateTime thoiGianTiepNhan, String tenBacSi,
                    String trieuChung, String chanDoan, String ketLuan, String trangThai) {
        this.maPhieuKham = maPhieuKham;
        this.thoiGianTiepNhan = thoiGianTiepNhan;
        this.tenBacSi = tenBacSi;
        this.trieuChung = trieuChung;
        this.chanDoan = chanDoan;
        this.ketLuan = ketLuan;
        this.trangThai = trangThai;
    }

    public String getMaPhieuKham() {
        return maPhieuKham;
    }

    public void setMaPhieuKham(String maPhieuKham) {
        this.maPhieuKham = maPhieuKham;
    }

    public LocalDateTime getThoiGianTiepNhan() {
        return thoiGianTiepNhan;
    }

    public void setThoiGianTiepNhan(LocalDateTime thoiGianTiepNhan) {
        this.thoiGianTiepNhan = thoiGianTiepNhan;
    }

    public String getTenBacSi() {
        return tenBacSi;
    }

    public void setTenBacSi(String tenBacSi) {
        this.tenBacSi = tenBacSi;
    }

    public String getTrieuChung() {
        return trieuChung;
    }

    public void setTrieuChung(String trieuChung) {
        this.trieuChung = trieuChung;
    }

    public String getChanDoan() {
        return chanDoan;
    }

    public void setChanDoan(String chanDoan) {
        this.chanDoan = chanDoan;
    }

    public String getKetLuan() {
        return ketLuan;
    }

    public void setKetLuan(String ketLuan) {
        this.ketLuan = ketLuan;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }

    // Đổi mã trạng thái trong database sang chữ để hiển thị.
    // Dùng đúng tên trạng thái trong báo cáo: Chờ khám → Đang khám → Hoàn tất; Chờ khám / Đang khám → Bỏ khám.
    //   DA_KET_LUAN (database) = "Hoàn tất" (báo cáo)
    //   DA_HUY      (database) = "Bỏ khám"  (báo cáo)
    public String getTrangThaiHienThi() {
        if ("CHO_KHAM".equals(trangThai)) {
            return "Chờ khám";
        }
        if ("DANG_KHAM".equals(trangThai)) {
            return "Đang khám";
        }
        if ("DA_KET_LUAN".equals(trangThai)) {
            return "Hoàn tất";
        }
        if ("DA_HUY".equals(trangThai)) {
            return "Bỏ khám";
        }
        return trangThai;
    }
}
