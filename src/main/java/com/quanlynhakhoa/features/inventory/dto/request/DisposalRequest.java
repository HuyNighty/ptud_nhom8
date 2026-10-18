package com.quanlynhakhoa.features.inventory.dto.request;

import com.quanlynhakhoa.features.inventory.enums.DisposalReason;
import java.time.LocalDate;
import java.util.List;

/** Lập / sửa phiếu hủy. {@code moTa}: mô tả cụ thể, không bắt buộc. */
public record DisposalRequest(LocalDate ngayHuy, DisposalReason lyDo, String moTa, List<DisposalLineRequest> dong) {
}
