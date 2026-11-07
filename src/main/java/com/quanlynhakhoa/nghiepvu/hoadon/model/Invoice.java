package com.quanlynhakhoa.nghiepvu.hoadon.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Invoice(String maHoaDon, String maNhanVien, String maBenhNhan, String tenBenhNhan,
                     String maPhieuKham, LocalDateTime ngayLap, InvoiceStatus trangThai,
                     BigDecimal tongTien, BigDecimal daThu, BigDecimal congNoBenhNhan,
                     InvoiceCancellationRequest.TrangThai cancellationStatus) {
    public Invoice(String maHoaDon, String maNhanVien, String maBenhNhan, String tenBenhNhan,
                   String maPhieuKham, LocalDateTime ngayLap, InvoiceStatus trangThai,
                   BigDecimal tongTien, BigDecimal daThu, BigDecimal congNoBenhNhan) {
        this(maHoaDon, maNhanVien, maBenhNhan, tenBenhNhan, maPhieuKham, ngayLap,
                trangThai, tongTien, daThu, congNoBenhNhan, null);
    }
    public boolean cancellationPending() {
        return cancellationStatus == InvoiceCancellationRequest.TrangThai.CHO_DUYET;
    }
    public String cancellationLabel() {
        if (cancellationStatus == null) return "Chưa yêu cầu";
        return switch (cancellationStatus) {
            case CHO_DUYET -> "Chờ duyệt hủy";
            case DA_DUYET -> "Đã duyệt hủy";
            case TU_CHOI -> "Đã từ chối hủy";
        };
    }
    public BigDecimal conNo() { return tongTien.subtract(daThu); }
    public BigDecimal noTheoDoi() { return trangThai == InvoiceStatus.DA_HUY ? BigDecimal.ZERO : conNo(); }
    public String canhBao() {
        if (daThu.compareTo(tongTien) > 0) return "BẤT NHẤT: đã thu vượt tổng hóa đơn";
        if (trangThai == InvoiceStatus.DA_HUY) return "ĐÃ HỦY — không cộng công nợ theo dõi";
        InvoiceStatus expected = daThu.signum() == 0 ? InvoiceStatus.CHUA_THANH_TOAN
                : conNo().signum() == 0 ? InvoiceStatus.DA_THANH_TOAN : InvoiceStatus.THANH_TOAN_MOT_PHAN;
        return expected == trangThai ? "" : "BẤT NHẤT: trạng thái khác số tiền; cần đối soát";
    }
}
