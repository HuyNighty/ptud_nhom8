package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.LichHen;

/** Lịch hẹn kèm tên bệnh nhân, SĐT và tên bác sĩ. */
public record LichHenView(LichHen lh, String tenBenhNhan, String soDienThoai, String tenBacSi) {}
