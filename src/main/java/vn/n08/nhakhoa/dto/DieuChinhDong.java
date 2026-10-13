package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.ChiTietDieuChinhKho;

import java.time.LocalDate;

/** Dòng điều chỉnh kèm thông tin lô và tồn hiện tại (để phát hiện tồn đã đổi trước khi duyệt). */
public record DieuChinhDong(ChiTietDieuChinhKho ct, String soLo, String tenHang, LocalDate hanSuDung, int tonHienTai) {}
