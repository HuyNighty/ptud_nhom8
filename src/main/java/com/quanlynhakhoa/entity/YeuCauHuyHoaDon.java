package com.quanlynhakhoa.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.quanlynhakhoa.enums.*;

/** Dữ liệu nghiệp vụ; giao dịch và chuyển trạng thái được điều phối tại Service. */
public final class YeuCauHuyHoaDon {
    private final String maYeuCau;
    private final String maHoaDon;
    private final String maTaiKhoanTao;
    private final LocalDateTime thoiGianTao;
    private final String lyDo;
    private final TrangThaiYeuCauHuyHoaDon trangThai;
    private final String maTaiKhoanDuyet;
    private final LocalDateTime thoiGianDuyet;
    private final String lyDoDuyet;

    public YeuCauHuyHoaDon(String maYeuCau, String maHoaDon, String maTaiKhoanTao, LocalDateTime thoiGianTao, String lyDo, TrangThaiYeuCauHuyHoaDon trangThai, String maTaiKhoanDuyet, LocalDateTime thoiGianDuyet, String lyDoDuyet) {
        this.maYeuCau = maYeuCau;
        this.maHoaDon = maHoaDon;
        this.maTaiKhoanTao = maTaiKhoanTao;
        this.thoiGianTao = thoiGianTao;
        this.lyDo = lyDo;
        this.trangThai = trangThai;
        this.maTaiKhoanDuyet = maTaiKhoanDuyet;
        this.thoiGianDuyet = thoiGianDuyet;
        this.lyDoDuyet = lyDoDuyet;
    }

    public String getMaYeuCau() { return maYeuCau; }
    public String getMaHoaDon() { return maHoaDon; }
    public String getMaTaiKhoanTao() { return maTaiKhoanTao; }
    public LocalDateTime getThoiGianTao() { return thoiGianTao; }
    public String getLyDo() { return lyDo; }
    public TrangThaiYeuCauHuyHoaDon getTrangThai() { return trangThai; }
    public String getMaTaiKhoanDuyet() { return maTaiKhoanDuyet; }
    public LocalDateTime getThoiGianDuyet() { return thoiGianDuyet; }
    public String getLyDoDuyet() { return lyDoDuyet; }
}
