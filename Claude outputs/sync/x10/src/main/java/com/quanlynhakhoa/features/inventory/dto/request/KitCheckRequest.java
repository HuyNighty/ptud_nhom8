package com.quanlynhakhoa.features.inventory.dto.request;

import java.util.List;

/** Một lần kiểm tra hộp cấp cứu: phải có kết quả cho mọi mặt hàng của hộp. */
public record KitCheckRequest(List<KitCheckLineRequest> dong, String ghiChu) {
}
