package com.quanlynhakhoa.features.inventory.dto.response;

import java.util.List;

/** Chi tiết thuốc kiểm soát đặc biệt: thông tin, trạng thái và các lô còn tồn (hạn sớm trước). */
public record ControlledDrugDetailResponse(ControlledDrugRowResponse thuoc, List<ControlledLotResponse> lo) {
}
