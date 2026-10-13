package vn.n08.nhakhoa.dto;

import java.util.List;

/** Màn hình tiếp nhận trong ngày: lịch hẹn chờ check-in và hàng chờ phiếu khám. */
public record TiepNhanData(List<LichHenView> lichHenCho, List<PhieuKhamView> phieuKham) {}
