package com.quanlynhakhoa.model.hoadon;

import com.quanlynhakhoa.nghiepvu.hoadon.entity.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class ThongTinHoaDon {
    private final String maHoaDon;
    private final String maNhanVien;
    private final String maBenhNhan;
    private final String tenBenhNhan;
    private final String maPhieuKham;
    private final LocalDateTime ngayLap;
    private final InvoiceStatus trangThai;
    private final BigDecimal tongTien;
    private final BigDecimal daThu;
    private final BigDecimal congNoBenhNhan;
    private final ThongTinYeuCauHuyHoaDon.TrangThai cancellationStatus;

    public ThongTinHoaDon(String maHoaDon, String maNhanVien, String maBenhNhan, String tenBenhNhan, String maPhieuKham, LocalDateTime ngayLap, InvoiceStatus trangThai, BigDecimal tongTien, BigDecimal daThu, BigDecimal congNoBenhNhan, ThongTinYeuCauHuyHoaDon.TrangThai cancellationStatus) {
        this.maHoaDon = maHoaDon;
        this.maNhanVien = maNhanVien;
        this.maBenhNhan = maBenhNhan;
        this.tenBenhNhan = tenBenhNhan;
        this.maPhieuKham = maPhieuKham;
        this.ngayLap = ngayLap;
        this.trangThai = trangThai;
        this.tongTien = tongTien;
        this.daThu = daThu;
        this.congNoBenhNhan = congNoBenhNhan;
        this.cancellationStatus = cancellationStatus;
    }

    public String maHoaDon() { return maHoaDon; }
    public String maNhanVien() { return maNhanVien; }
    public String maBenhNhan() { return maBenhNhan; }
    public String tenBenhNhan() { return tenBenhNhan; }
    public String maPhieuKham() { return maPhieuKham; }
    public LocalDateTime ngayLap() { return ngayLap; }
    public InvoiceStatus trangThai() { return trangThai; }
    public BigDecimal tongTien() { return tongTien; }
    public BigDecimal daThu() { return daThu; }
    public BigDecimal congNoBenhNhan() { return congNoBenhNhan; }
    public ThongTinYeuCauHuyHoaDon.TrangThai cancellationStatus() { return cancellationStatus; }

    public ThongTinHoaDon(String maHoaDon, String maNhanVien, String maBenhNhan, String tenBenhNhan,
                   String maPhieuKham, LocalDateTime ngayLap, InvoiceStatus trangThai,
                   BigDecimal tongTien, BigDecimal daThu, BigDecimal congNoBenhNhan) {
        this(maHoaDon, maNhanVien, maBenhNhan, tenBenhNhan, maPhieuKham, ngayLap,
                trangThai, tongTien, daThu, congNoBenhNhan, null);
    }
    public boolean cancellationPending() {
        return cancellationStatus == ThongTinYeuCauHuyHoaDon.TrangThai.CHO_DUYET;
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
        InvoiceStatus expected = conNo().signum() == 0 ? InvoiceStatus.DA_THANH_TOAN : daThu.signum() == 0 ? InvoiceStatus.CHUA_THANH_TOAN
                : conNo().signum() == 0 ? InvoiceStatus.DA_THANH_TOAN : InvoiceStatus.THANH_TOAN_MOT_PHAN;
        return expected == trangThai ? "" : "BẤT NHẤT: trạng thái khác số tiền; cần đối soát";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinHoaDon value)) return false;
        return java.util.Objects.equals(maHoaDon, value.maHoaDon)
                && java.util.Objects.equals(maNhanVien, value.maNhanVien)
                && java.util.Objects.equals(maBenhNhan, value.maBenhNhan)
                && java.util.Objects.equals(tenBenhNhan, value.tenBenhNhan)
                && java.util.Objects.equals(maPhieuKham, value.maPhieuKham)
                && java.util.Objects.equals(ngayLap, value.ngayLap)
                && java.util.Objects.equals(trangThai, value.trangThai)
                && java.util.Objects.equals(tongTien, value.tongTien)
                && java.util.Objects.equals(daThu, value.daThu)
                && java.util.Objects.equals(congNoBenhNhan, value.congNoBenhNhan)
                && java.util.Objects.equals(cancellationStatus, value.cancellationStatus);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maHoaDon);
        result = 31 * result + java.util.Objects.hashCode(maNhanVien);
        result = 31 * result + java.util.Objects.hashCode(maBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(tenBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(maPhieuKham);
        result = 31 * result + java.util.Objects.hashCode(ngayLap);
        result = 31 * result + java.util.Objects.hashCode(trangThai);
        result = 31 * result + java.util.Objects.hashCode(tongTien);
        result = 31 * result + java.util.Objects.hashCode(daThu);
        result = 31 * result + java.util.Objects.hashCode(congNoBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(cancellationStatus);
        return result;
    }

    @Override
    public String toString() {
        return "Invoice[" + "maHoaDon=" + maHoaDon + ", " + "maNhanVien=" + maNhanVien + ", " + "maBenhNhan=" + maBenhNhan + ", " + "tenBenhNhan=" + tenBenhNhan + ", " + "maPhieuKham=" + maPhieuKham + ", " + "ngayLap=" + ngayLap + ", " + "trangThai=" + trangThai + ", " + "tongTien=" + tongTien + ", " + "daThu=" + daThu + ", " + "congNoBenhNhan=" + congNoBenhNhan + ", " + "cancellationStatus=" + cancellationStatus + "]";
    }
}
