package vn.n08.nhakhoa.model;

import java.time.LocalDateTime;

/** [Bổ sung] Phiếu kiểm kê / hủy / hao hụt cần người khác duyệt. */
public record PhieuDieuChinhKho(
        String maPhieu,
        LoaiDieuChinh loai,
        String maTaiKhoanTao,
        LocalDateTime thoiGianTao,
        String lyDo,
        TrangThaiDuyet trangThai,
        String maTaiKhoanDuyet,
        LocalDateTime thoiGianDuyet,
        String ghiChuDuyet) {}
