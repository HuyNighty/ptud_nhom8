package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.ChiTietDieuTri;

import java.math.BigDecimal;

/** Hạng mục điều trị kèm tên, đơn giá dịch vụ và bác sĩ thực hiện. */
public record ChiTietDieuTriView(ChiTietDieuTri ct, String tenDichVu, BigDecimal donGia, String tenBacSiThucHien) {}
