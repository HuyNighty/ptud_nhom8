package vn.n08.nhakhoa.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Một dòng chi tiết của phiếu nhập / xuất / hoàn trả (đã ghép thông tin lô). */
public record PhieuKhoDong(String maLo, String soLo, String maHang, String tenHang, String donViTinh, LocalDate hanSuDung,
                           int soLuong, BigDecimal donGia, boolean kiemSoatDacBiet) {}
