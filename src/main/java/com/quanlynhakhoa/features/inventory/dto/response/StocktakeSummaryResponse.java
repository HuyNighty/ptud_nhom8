package com.quanlynhakhoa.features.inventory.dto.response;

/** Thẻ thống kê màn Kiểm kê: tổng đợt trong năm, đang kiểm kê, đã duyệt khớp / có lệch. */
public record StocktakeSummaryResponse(
        long tongTrongNam,
        long dangKiemKe,
        long khop,
        long lech,
        long daHuy) {
}
