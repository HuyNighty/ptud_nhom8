package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.BanGiaoCa;

/** Biên bản bàn giao kèm tên người, tên ca và tên đăng nhập của người tạo / xác nhận / hủy. */
public record BanGiaoView(BanGiaoCa bg, String tenNguoiGiao, String tenNguoiNhan, String tenCaGiao, String tenCaNhan,
                          String tkTao, String tkXacNhan, String tkHuy) {}
