package vn.n08.nhakhoa.dto;

import java.util.List;

/** Chi tiết một lượt khám: phiếu, chỉ định CLS, đơn thuốc (có thể null). */
public record PhieuKhamChiTiet(PhieuKhamView phieu, List<ChiDinhView> chiDinh, DonThuocView donThuoc) {}
