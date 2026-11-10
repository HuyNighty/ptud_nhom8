package com.quanlynhakhoa.model.hoadon;

import com.quanlynhakhoa.enums.TrangThaiYeuCauHuyHoaDon;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
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
    private final TrangThaiHoaDon trangThai;
    private final BigDecimal tongTien;
    private final BigDecimal daThu;
    private final BigDecimal congNoBenhNhan;
    private final TrangThaiYeuCauHuyHoaDon trangThaiYeuCauHuy;

    public ThongTinHoaDon(String maHoaDon, String maNhanVien, String maBenhNhan, String tenBenhNhan, String maPhieuKham, LocalDateTime ngayLap, TrangThaiHoaDon trangThai, BigDecimal tongTien, BigDecimal daThu, BigDecimal congNoBenhNhan, TrangThaiYeuCauHuyHoaDon trangThaiYeuCauHuy) {
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
        this.trangThaiYeuCauHuy = trangThaiYeuCauHuy;
    }

    public String maHoaDon() { return maHoaDon; }
    public String maNhanVien() { return maNhanVien; }
    public String maBenhNhan() { return maBenhNhan; }
    public String tenBenhNhan() { return tenBenhNhan; }
    public String maPhieuKham() { return maPhieuKham; }
    public LocalDateTime ngayLap() { return ngayLap; }
    public TrangThaiHoaDon trangThai() { return trangThai; }
    public BigDecimal tongTien() { return tongTien; }
    public BigDecimal daThu() { return daThu; }
    public BigDecimal congNoBenhNhan() { return congNoBenhNhan; }
    public TrangThaiYeuCauHuyHoaDon cancellationStatus() { return trangThaiYeuCauHuy; }

    public ThongTinHoaDon(String maHoaDon, String maNhanVien, String maBenhNhan, String tenBenhNhan,
                   String maPhieuKham, LocalDateTime ngayLap, TrangThaiHoaDon trangThai,
                   BigDecimal tongTien, BigDecimal daThu, BigDecimal congNoBenhNhan) {
        this(maHoaDon, maNhanVien, maBenhNhan, tenBenhNhan, maPhieuKham, ngayLap,
                trangThai, tongTien, daThu, congNoBenhNhan, null);
    }
    public boolean cancellationPending() {
        return trangThaiYeuCauHuy == TrangThaiYeuCauHuyHoaDon.CHO_DUYET;
    }
    public String cancellationLabel() {
        if (trangThaiYeuCauHuy == null) return "Chưa yêu cầu";
        return switch (trangThaiYeuCauHuy) {
            case CHO_DUYET -> "Chờ duyệt hủy";
            case DA_DUYET -> "Đã duyệt hủy";
            case TU_CHOI -> "Đã từ chối hủy";
        };
    }
    public BigDecimal conNo() { return tongTien.subtract(daThu); }
    public BigDecimal noTheoDoi() { return trangThai == TrangThaiHoaDon.DA_HUY ? BigDecimal.ZERO : conNo(); }
    public String canhBao() {
        if (daThu.compareTo(tongTien) > 0) return "BẤT NHẤT: đã thu vượt tổng hóa đơn";
        if (trangThai == TrangThaiHoaDon.DA_HUY) return "ĐÃ HỦY — không cộng công nợ theo dõi";
        TrangThaiHoaDon expected = conNo().signum() == 0 ? TrangThaiHoaDon.DA_THANH_TOAN : daThu.signum() == 0 ? TrangThaiHoaDon.CHUA_THANH_TOAN
                : conNo().signum() == 0 ? TrangThaiHoaDon.DA_THANH_TOAN : TrangThaiHoaDon.THANH_TOAN_MOT_PHAN;
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
                && java.util.Objects.equals(trangThaiYeuCauHuy, value.trangThaiYeuCauHuy);
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
        result = 31 * result + java.util.Objects.hashCode(trangThaiYeuCauHuy);
        return result;
    }

    @Override
    public String toString() {
        return "Invoice[" + "maHoaDon=" + maHoaDon + ", " + "maNhanVien=" + maNhanVien + ", " + "maBenhNhan=" + maBenhNhan + ", " + "tenBenhNhan=" + tenBenhNhan + ", " + "maPhieuKham=" + maPhieuKham + ", " + "ngayLap=" + ngayLap + ", " + "trangThai=" + trangThai + ", " + "tongTien=" + tongTien + ", " + "daThu=" + daThu + ", " + "congNoBenhNhan=" + congNoBenhNhan + ", " + "cancellationStatus=" + trangThaiYeuCauHuy + "]";
    }
}
