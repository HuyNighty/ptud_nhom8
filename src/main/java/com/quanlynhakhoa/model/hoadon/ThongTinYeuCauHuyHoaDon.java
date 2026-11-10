package com.quanlynhakhoa.model.hoadon;

import com.quanlynhakhoa.enums.TrangThaiYeuCauHuyHoaDon;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class ThongTinYeuCauHuyHoaDon {
    private final String maYeuCau;
    private final String maHoaDon;
    private final String maBenhNhan;
    private final String tenBenhNhan;
    private final BigDecimal tongTien;
    private final BigDecimal daThu;
    private final String maTaiKhoanTao;
    private final LocalDateTime thoiGianTao;
    private final String lyDo;
    private final TrangThaiYeuCauHuyHoaDon trangThai;
    private final String maTaiKhoanDuyet;
    private final LocalDateTime thoiGianDuyet;
    private final String lyDoDuyet;

    public ThongTinYeuCauHuyHoaDon(String maYeuCau, String maHoaDon, String maBenhNhan, String tenBenhNhan, BigDecimal tongTien, BigDecimal daThu, String maTaiKhoanTao, LocalDateTime thoiGianTao, String lyDo, TrangThaiYeuCauHuyHoaDon trangThai, String maTaiKhoanDuyet, LocalDateTime thoiGianDuyet, String lyDoDuyet) {
        this.maYeuCau = maYeuCau;
        this.maHoaDon = maHoaDon;
        this.maBenhNhan = maBenhNhan;
        this.tenBenhNhan = tenBenhNhan;
        this.tongTien = tongTien;
        this.daThu = daThu;
        this.maTaiKhoanTao = maTaiKhoanTao;
        this.thoiGianTao = thoiGianTao;
        this.lyDo = lyDo;
        this.trangThai = trangThai;
        this.maTaiKhoanDuyet = maTaiKhoanDuyet;
        this.thoiGianDuyet = thoiGianDuyet;
        this.lyDoDuyet = lyDoDuyet;
    }

    public String maYeuCau() { return maYeuCau; }
    public String maHoaDon() { return maHoaDon; }
    public String maBenhNhan() { return maBenhNhan; }
    public String tenBenhNhan() { return tenBenhNhan; }
    public BigDecimal tongTien() { return tongTien; }
    public BigDecimal daThu() { return daThu; }
    public String maTaiKhoanTao() { return maTaiKhoanTao; }
    public LocalDateTime thoiGianTao() { return thoiGianTao; }
    public String lyDo() { return lyDo; }
    public TrangThaiYeuCauHuyHoaDon trangThai() { return trangThai; }
    public String maTaiKhoanDuyet() { return maTaiKhoanDuyet; }
    public LocalDateTime thoiGianDuyet() { return thoiGianDuyet; }
    public String lyDoDuyet() { return lyDoDuyet; }


    public BigDecimal conNo(){return tongTien.subtract(daThu);}

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinYeuCauHuyHoaDon value)) return false;
        return java.util.Objects.equals(maYeuCau, value.maYeuCau)
                && java.util.Objects.equals(maHoaDon, value.maHoaDon)
                && java.util.Objects.equals(maBenhNhan, value.maBenhNhan)
                && java.util.Objects.equals(tenBenhNhan, value.tenBenhNhan)
                && java.util.Objects.equals(tongTien, value.tongTien)
                && java.util.Objects.equals(daThu, value.daThu)
                && java.util.Objects.equals(maTaiKhoanTao, value.maTaiKhoanTao)
                && java.util.Objects.equals(thoiGianTao, value.thoiGianTao)
                && java.util.Objects.equals(lyDo, value.lyDo)
                && java.util.Objects.equals(trangThai, value.trangThai)
                && java.util.Objects.equals(maTaiKhoanDuyet, value.maTaiKhoanDuyet)
                && java.util.Objects.equals(thoiGianDuyet, value.thoiGianDuyet)
                && java.util.Objects.equals(lyDoDuyet, value.lyDoDuyet);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maYeuCau);
        result = 31 * result + java.util.Objects.hashCode(maHoaDon);
        result = 31 * result + java.util.Objects.hashCode(maBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(tenBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(tongTien);
        result = 31 * result + java.util.Objects.hashCode(daThu);
        result = 31 * result + java.util.Objects.hashCode(maTaiKhoanTao);
        result = 31 * result + java.util.Objects.hashCode(thoiGianTao);
        result = 31 * result + java.util.Objects.hashCode(lyDo);
        result = 31 * result + java.util.Objects.hashCode(trangThai);
        result = 31 * result + java.util.Objects.hashCode(maTaiKhoanDuyet);
        result = 31 * result + java.util.Objects.hashCode(thoiGianDuyet);
        result = 31 * result + java.util.Objects.hashCode(lyDoDuyet);
        return result;
    }

    @Override
    public String toString() {
        return "InvoiceCancellationRequest[" + "maYeuCau=" + maYeuCau + ", " + "maHoaDon=" + maHoaDon + ", " + "maBenhNhan=" + maBenhNhan + ", " + "tenBenhNhan=" + tenBenhNhan + ", " + "tongTien=" + tongTien + ", " + "daThu=" + daThu + ", " + "maTaiKhoanTao=" + maTaiKhoanTao + ", " + "thoiGianTao=" + thoiGianTao + ", " + "lyDo=" + lyDo + ", " + "trangThai=" + trangThai + ", " + "maTaiKhoanDuyet=" + maTaiKhoanDuyet + ", " + "thoiGianDuyet=" + thoiGianDuyet + ", " + "lyDoDuyet=" + lyDoDuyet + "]";
    }
}
