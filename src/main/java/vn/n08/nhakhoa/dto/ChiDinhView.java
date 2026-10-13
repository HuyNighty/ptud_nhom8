package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.ChiDinhCanLamSang;

import java.math.BigDecimal;

/** Chỉ định cận lâm sàng kèm tên, giá dịch vụ và bác sĩ chỉ định. */
public record ChiDinhView(ChiDinhCanLamSang cd, String tenDichVu, BigDecimal donGia, String tenBacSi) {}
