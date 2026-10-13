package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.PhieuDieuChinhKho;

/** Phiếu điều chỉnh kho kèm người tạo, người duyệt và tổng chênh lệch. */
public record DieuChinhView(PhieuDieuChinhKho p, String tkTao, String tenNguoiTao, String tkDuyet, String tenNguoiDuyet,
                            int soDong, int tongChenhLech) {}
