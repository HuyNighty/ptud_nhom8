package com.quanlynhakhoa.features.inventory.dto.request;

import com.quanlynhakhoa.features.inventory.enums.StocktakeScope;
import java.time.LocalDate;

/** Tạo đợt kiểm kê. Phạm vi chỉ dùng khi tạo; khi sửa đợt chỉ đổi ngày và ghi chú. */
public record StocktakeRequest(LocalDate ngayKiemKe, StocktakeScope phamVi, String ghiChu) {
}
