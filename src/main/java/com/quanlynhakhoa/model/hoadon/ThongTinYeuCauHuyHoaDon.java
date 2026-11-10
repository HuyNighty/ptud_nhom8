package com.quanlynhakhoa.model.hoadon;

import com.quanlynhakhoa.entity.YeuCauHuyHoaDon;

import com.quanlynhakhoa.enums.TrangThaiYeuCauHuyHoaDon;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class ThongTinYeuCauHuyHoaDon {
    private final YeuCauHuyHoaDon yeuCauHuyHoaDon;
    private final String maBenhNhan;
    private final String tenBenhNhan;
    private final BigDecimal tongTien;
    private final BigDecimal daThu;

    public ThongTinYeuCauHuyHoaDon(String maYeuCau, String maHoaDon, String maBenhNhan, String tenBenhNhan, BigDecimal tongTien, BigDecimal daThu, String maTaiKhoanTao, LocalDateTime thoiGianTao, String lyDo, TrangThaiYeuCauHuyHoaDon trangThai, String maTaiKhoanDuyet, LocalDateTime thoiGianDuyet, String lyDoDuyet) {
        this.yeuCauHuyHoaDon = new YeuCauHuyHoaDon(maYeuCau, maHoaDon, maTaiKhoanTao, thoiGianTao, lyDo, trangThai, maTaiKhoanDuyet, thoiGianDuyet, lyDoDuyet);

        this.maBenhNhan = maBenhNhan;
        this.tenBenhNhan = tenBenhNhan;
        this.tongTien = tongTien;
        this.daThu = daThu;







    }

    public String maYeuCau() { return yeuCauHuyHoaDon.getMaYeuCau(); }
    public String maHoaDon() { return yeuCauHuyHoaDon.getMaHoaDon(); }
    public String maBenhNhan() { return maBenhNhan; }
    public String tenBenhNhan() { return tenBenhNhan; }
    public BigDecimal tongTien() { return tongTien; }
    public BigDecimal daThu() { return daThu; }
    public String maTaiKhoanTao() { return yeuCauHuyHoaDon.getMaTaiKhoanTao(); }
    public LocalDateTime thoiGianTao() { return yeuCauHuyHoaDon.getThoiGianTao(); }
    public String lyDo() { return yeuCauHuyHoaDon.getLyDo(); }
    public TrangThaiYeuCauHuyHoaDon trangThai() { return yeuCauHuyHoaDon.getTrangThai(); }
    public String maTaiKhoanDuyet() { return yeuCauHuyHoaDon.getMaTaiKhoanDuyet(); }
    public LocalDateTime thoiGianDuyet() { return yeuCauHuyHoaDon.getThoiGianDuyet(); }
    public String lyDoDuyet() { return yeuCauHuyHoaDon.getLyDoDuyet(); }


    public BigDecimal conNo(){return tongTien.subtract(daThu);}

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinYeuCauHuyHoaDon value)) return false;
        return java.util.Objects.equals(yeuCauHuyHoaDon.getMaYeuCau(), value.yeuCauHuyHoaDon.getMaYeuCau())
                && java.util.Objects.equals(yeuCauHuyHoaDon.getMaHoaDon(), value.yeuCauHuyHoaDon.getMaHoaDon())
                && java.util.Objects.equals(maBenhNhan, value.maBenhNhan)
                && java.util.Objects.equals(tenBenhNhan, value.tenBenhNhan)
                && java.util.Objects.equals(tongTien, value.tongTien)
                && java.util.Objects.equals(daThu, value.daThu)
                && java.util.Objects.equals(yeuCauHuyHoaDon.getMaTaiKhoanTao(), value.yeuCauHuyHoaDon.getMaTaiKhoanTao())
                && java.util.Objects.equals(yeuCauHuyHoaDon.getThoiGianTao(), value.yeuCauHuyHoaDon.getThoiGianTao())
                && java.util.Objects.equals(yeuCauHuyHoaDon.getLyDo(), value.yeuCauHuyHoaDon.getLyDo())
                && java.util.Objects.equals(yeuCauHuyHoaDon.getTrangThai(), value.yeuCauHuyHoaDon.getTrangThai())
                && java.util.Objects.equals(yeuCauHuyHoaDon.getMaTaiKhoanDuyet(), value.yeuCauHuyHoaDon.getMaTaiKhoanDuyet())
                && java.util.Objects.equals(yeuCauHuyHoaDon.getThoiGianDuyet(), value.yeuCauHuyHoaDon.getThoiGianDuyet())
                && java.util.Objects.equals(yeuCauHuyHoaDon.getLyDoDuyet(), value.yeuCauHuyHoaDon.getLyDoDuyet());
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(yeuCauHuyHoaDon.getMaYeuCau());
        result = 31 * result + java.util.Objects.hashCode(yeuCauHuyHoaDon.getMaHoaDon());
        result = 31 * result + java.util.Objects.hashCode(maBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(tenBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(tongTien);
        result = 31 * result + java.util.Objects.hashCode(daThu);
        result = 31 * result + java.util.Objects.hashCode(yeuCauHuyHoaDon.getMaTaiKhoanTao());
        result = 31 * result + java.util.Objects.hashCode(yeuCauHuyHoaDon.getThoiGianTao());
        result = 31 * result + java.util.Objects.hashCode(yeuCauHuyHoaDon.getLyDo());
        result = 31 * result + java.util.Objects.hashCode(yeuCauHuyHoaDon.getTrangThai());
        result = 31 * result + java.util.Objects.hashCode(yeuCauHuyHoaDon.getMaTaiKhoanDuyet());
        result = 31 * result + java.util.Objects.hashCode(yeuCauHuyHoaDon.getThoiGianDuyet());
        result = 31 * result + java.util.Objects.hashCode(yeuCauHuyHoaDon.getLyDoDuyet());
        return result;
    }

    @Override
    public String toString() {
        return "InvoiceCancellationRequest[" + "maYeuCau=" + yeuCauHuyHoaDon.getMaYeuCau() + ", " + "maHoaDon=" + yeuCauHuyHoaDon.getMaHoaDon() + ", " + "maBenhNhan=" + maBenhNhan + ", " + "tenBenhNhan=" + tenBenhNhan + ", " + "tongTien=" + tongTien + ", " + "daThu=" + daThu + ", " + "maTaiKhoanTao=" + yeuCauHuyHoaDon.getMaTaiKhoanTao() + ", " + "thoiGianTao=" + yeuCauHuyHoaDon.getThoiGianTao() + ", " + "lyDo=" + yeuCauHuyHoaDon.getLyDo() + ", " + "trangThai=" + yeuCauHuyHoaDon.getTrangThai() + ", " + "maTaiKhoanDuyet=" + yeuCauHuyHoaDon.getMaTaiKhoanDuyet() + ", " + "thoiGianDuyet=" + yeuCauHuyHoaDon.getThoiGianDuyet() + ", " + "lyDoDuyet=" + yeuCauHuyHoaDon.getLyDoDuyet() + "]";
    }

    public YeuCauHuyHoaDon getYeuCauHuyHoaDon() { return yeuCauHuyHoaDon; }
}
