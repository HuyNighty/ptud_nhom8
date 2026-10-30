package com.quanlynhakhoa.billing.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record YeuCauHuyHoaDon(String maYeuCau,String maHoaDon,String maBenhNhan,String tenBenhNhan,
        BigDecimal tongTien,BigDecimal daThu,String maTaiKhoanTao,LocalDateTime thoiGianTao,
        String lyDo,TrangThai trangThai,String maTaiKhoanDuyet,LocalDateTime thoiGianDuyet,String lyDoDuyet) {
    public enum TrangThai {
        CHO_DUYET("Chờ duyệt"),DA_DUYET("Đã duyệt"),TU_CHOI("Từ chối");
        private final String label;TrangThai(String label){this.label=label;}
        @Override public String toString(){return label;}
    }
    public BigDecimal conNo(){return tongTien.subtract(daThu);}
}
