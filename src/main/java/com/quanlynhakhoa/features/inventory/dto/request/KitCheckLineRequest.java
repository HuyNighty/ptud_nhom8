package com.quanlynhakhoa.features.inventory.dto.request;

import java.time.LocalDate;

/** Kết quả đếm thực tế một mặt hàng khi kiểm tra hộp; {@code hanDung} = hạn sớm nhất trong hộp. */
public record KitCheckLineRequest(Integer maChiTiet, Integer soLuongThucTe, LocalDate hanDung) {
}
