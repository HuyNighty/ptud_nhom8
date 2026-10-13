package vn.n08.nhakhoa.model;

import java.time.LocalDateTime;

/** [Bổ sung] Yêu cầu hủy hóa đơn — người duyệt phải khác người tạo. */
public record YeuCauHuyHoaDon(
        String maYeuCau,
        String maHoaDon,
        String maTaiKhoanTao,
        LocalDateTime thoiGianTao,
        String lyDo,
        TrangThaiDuyet trangThai,
        String maTaiKhoanDuyet,
        LocalDateTime thoiGianDuyet,
        String lyDoDuyet) {}
