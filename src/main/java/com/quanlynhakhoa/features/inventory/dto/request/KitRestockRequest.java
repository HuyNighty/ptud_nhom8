package com.quanlynhakhoa.features.inventory.dto.request;

import com.quanlynhakhoa.features.inventory.enums.KitLogType;
import java.util.List;

/** Bổ sung (cộng thêm) hoặc thay thế (lấy hàng cũ ra, thay bằng hàng mới) từ kho. */
public record KitRestockRequest(KitLogType loai, List<KitRestockLineRequest> dong, String ghiChu) {
}
