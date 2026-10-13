package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.TrangThaiHoaDon;
import vn.n08.nhakhoa.model.YeuCauHuyHoaDon;

import java.math.BigDecimal;

/** Yêu cầu hủy hóa đơn kèm người tạo, người duyệt và tình trạng hóa đơn. */
public record YeuCauHuyView(YeuCauHuyHoaDon yc, String tkTao, String tenNguoiTao, String tkDuyet, String tenNguoiDuyet,
                            String tenBenhNhan, BigDecimal tongTien, BigDecimal daThanhToan, TrangThaiHoaDon trangThaiHoaDon) {}
