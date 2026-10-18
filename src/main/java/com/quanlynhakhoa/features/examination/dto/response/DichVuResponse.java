package com.quanlynhakhoa.features.examination.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DichVuResponse(
        String maDichVu,
        String tenDichVu,
        String moTa,
        BigDecimal donGia,
        boolean laCanLamSang,
        boolean dangApDung,
        LocalDateTime taoLuc,
        LocalDateTime capNhatLuc
) {
}
