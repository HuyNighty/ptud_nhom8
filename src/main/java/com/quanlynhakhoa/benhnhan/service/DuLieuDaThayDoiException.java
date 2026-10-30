package com.quanlynhakhoa.benhnhan.service;

// Lỗi khi bấm Lưu mà dữ liệu trong database đã bị NGƯỜI KHÁC sửa sau lúc mình mở form.
// Theo BR10 và UC003 E2 (báo cáo docs/tuan_cuoi):
//   "hệ thống báo, tải lại dữ liệu mới nhất và không lưu" → không ghi đè lên thay đổi của người khác.
public class DuLieuDaThayDoiException extends RuntimeException {
    public DuLieuDaThayDoiException(String thongBao) {
        super(thongBao);
    }
}
