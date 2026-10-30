package com.quanlynhakhoa.billing.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HoaDon(String maHoaDon, String maNhanVien, String maBenhNhan, String tenBenhNhan,
                     String maPhieuKham, LocalDateTime ngayLap, TrangThaiHoaDon trangThai,
                     BigDecimal tongTien, BigDecimal daThu, BigDecimal congNoBenhNhan) {
    public BigDecimal conNo() { return tongTien.subtract(daThu); }
    public BigDecimal noTheoDoi() { return trangThai == TrangThaiHoaDon.DA_HUY ? BigDecimal.ZERO : conNo(); }
    public String canhBao() {
        if (daThu.compareTo(tongTien) > 0) return "BẤT NHẤT: đã thu vượt tổng hóa đơn";
        if (trangThai == TrangThaiHoaDon.DA_HUY) return "ĐÃ HỦY — không cộng công nợ theo dõi";
        TrangThaiHoaDon expected = daThu.signum() == 0 ? TrangThaiHoaDon.CHUA_THANH_TOAN
                : conNo().signum() == 0 ? TrangThaiHoaDon.DA_THANH_TOAN : TrangThaiHoaDon.THANH_TOAN_MOT_PHAN;
        return expected == trangThai ? "" : "BẤT NHẤT: trạng thái khác số tiền; cần đối soát";
    }
}
