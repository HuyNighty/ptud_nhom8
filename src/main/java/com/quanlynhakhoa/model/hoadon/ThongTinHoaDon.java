package com.quanlynhakhoa.model.hoadon;

import com.quanlynhakhoa.entity.HoaDon;

import com.quanlynhakhoa.enums.TrangThaiYeuCauHuyHoaDon;
import com.quanlynhakhoa.enums.TrangThaiHoaDon;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class ThongTinHoaDon {
    private final HoaDon hoaDon;
    private final String tenBenhNhan;
    private final BigDecimal tongTien;
    private final BigDecimal daThu;
    private final BigDecimal congNoBenhNhan;
    private final TrangThaiYeuCauHuyHoaDon trangThaiYeuCauHuy;

    public ThongTinHoaDon(String maHoaDon, String maNhanVien, String maBenhNhan, String tenBenhNhan, String maPhieuKham, LocalDateTime ngayLap, TrangThaiHoaDon trangThai, BigDecimal tongTien, BigDecimal daThu, BigDecimal congNoBenhNhan, TrangThaiYeuCauHuyHoaDon trangThaiYeuCauHuy) {
        this.hoaDon = new HoaDon(maHoaDon, maNhanVien, maBenhNhan, maPhieuKham, ngayLap, trangThai);


        this.tenBenhNhan = tenBenhNhan;



        this.tongTien = tongTien;
        this.daThu = daThu;
        this.congNoBenhNhan = congNoBenhNhan;
        this.trangThaiYeuCauHuy = trangThaiYeuCauHuy;
    }

    public String maHoaDon() { return hoaDon.getMaHoaDon(); }
    public String maNhanVien() { return hoaDon.getMaNhanVien(); }
    public String maBenhNhan() { return hoaDon.getMaBenhNhan(); }
    public String tenBenhNhan() { return tenBenhNhan; }
    public String maPhieuKham() { return hoaDon.getMaPhieuKham(); }
    public LocalDateTime ngayLap() { return hoaDon.getNgayLap(); }
    public TrangThaiHoaDon trangThai() { return hoaDon.getTrangThai(); }
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
    public BigDecimal noTheoDoi() { return hoaDon.getTrangThai() == TrangThaiHoaDon.DA_HUY ? BigDecimal.ZERO : conNo(); }
    public String canhBao() {
        if (daThu.compareTo(tongTien) > 0) return "BẤT NHẤT: đã thu vượt tổng hóa đơn";
        if (hoaDon.getTrangThai() == TrangThaiHoaDon.DA_HUY) return "ĐÃ HỦY — không cộng công nợ theo dõi";
        TrangThaiHoaDon expected = conNo().signum() == 0 ? TrangThaiHoaDon.DA_THANH_TOAN : daThu.signum() == 0 ? TrangThaiHoaDon.CHUA_THANH_TOAN
                : conNo().signum() == 0 ? TrangThaiHoaDon.DA_THANH_TOAN : TrangThaiHoaDon.THANH_TOAN_MOT_PHAN;
        return expected == hoaDon.getTrangThai() ? "" : "BẤT NHẤT: trạng thái khác số tiền; cần đối soát";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinHoaDon value)) return false;
        return java.util.Objects.equals(hoaDon.getMaHoaDon(), value.hoaDon.getMaHoaDon())
                && java.util.Objects.equals(hoaDon.getMaNhanVien(), value.hoaDon.getMaNhanVien())
                && java.util.Objects.equals(hoaDon.getMaBenhNhan(), value.hoaDon.getMaBenhNhan())
                && java.util.Objects.equals(tenBenhNhan, value.tenBenhNhan)
                && java.util.Objects.equals(hoaDon.getMaPhieuKham(), value.hoaDon.getMaPhieuKham())
                && java.util.Objects.equals(hoaDon.getNgayLap(), value.hoaDon.getNgayLap())
                && java.util.Objects.equals(hoaDon.getTrangThai(), value.hoaDon.getTrangThai())
                && java.util.Objects.equals(tongTien, value.tongTien)
                && java.util.Objects.equals(daThu, value.daThu)
                && java.util.Objects.equals(congNoBenhNhan, value.congNoBenhNhan)
                && java.util.Objects.equals(trangThaiYeuCauHuy, value.trangThaiYeuCauHuy);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(hoaDon.getMaHoaDon());
        result = 31 * result + java.util.Objects.hashCode(hoaDon.getMaNhanVien());
        result = 31 * result + java.util.Objects.hashCode(hoaDon.getMaBenhNhan());
        result = 31 * result + java.util.Objects.hashCode(tenBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(hoaDon.getMaPhieuKham());
        result = 31 * result + java.util.Objects.hashCode(hoaDon.getNgayLap());
        result = 31 * result + java.util.Objects.hashCode(hoaDon.getTrangThai());
        result = 31 * result + java.util.Objects.hashCode(tongTien);
        result = 31 * result + java.util.Objects.hashCode(daThu);
        result = 31 * result + java.util.Objects.hashCode(congNoBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(trangThaiYeuCauHuy);
        return result;
    }

    @Override
    public String toString() {
        return "Invoice[" + "maHoaDon=" + hoaDon.getMaHoaDon() + ", " + "maNhanVien=" + hoaDon.getMaNhanVien() + ", " + "maBenhNhan=" + hoaDon.getMaBenhNhan() + ", " + "tenBenhNhan=" + tenBenhNhan + ", " + "maPhieuKham=" + hoaDon.getMaPhieuKham() + ", " + "ngayLap=" + hoaDon.getNgayLap() + ", " + "trangThai=" + hoaDon.getTrangThai() + ", " + "tongTien=" + tongTien + ", " + "daThu=" + daThu + ", " + "congNoBenhNhan=" + congNoBenhNhan + ", " + "cancellationStatus=" + trangThaiYeuCauHuy + "]";
    }

    public HoaDon getHoaDon() { return hoaDon; }
}
