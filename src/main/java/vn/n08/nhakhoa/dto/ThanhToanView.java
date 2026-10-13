package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.ThanhToan;

/** Một lần thu kèm tên thu ngân và tài khoản đã hủy giao dịch (nếu có). */
public record ThanhToanView(ThanhToan tt, String tenThuNgan, String tkHuy) {}
