package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.PhanCong;
import vn.n08.nhakhoa.model.VaiTro;

import java.time.LocalTime;

/** Phân công kèm tên nhân viên, chức danh và giờ ca. */
public record PhanCongView(PhanCong pc, String hoTen, VaiTro chucDanh, String tenCa, LocalTime gioBatDau, LocalTime gioKetThuc) {}
