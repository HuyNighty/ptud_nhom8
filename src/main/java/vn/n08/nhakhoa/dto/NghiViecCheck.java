package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.NhanVien;

import java.util.List;

/**
 * Điều kiện thủ tục nghỉ việc: các việc còn mở phải được xử lý hợp lệ (thay người / chuyển / hủy có lý do / xác nhận)
 * trước khi hoàn tất. Hệ thống không tự hủy hay xóa lịch sử.
 */
public record NghiViecCheck(NhanVien nv, List<PhanCongView> phanCong, List<LichHenView> lichHen, List<PhieuKhamView> phieuKham,
                            List<BanGiaoView> banGiao, boolean coTaiKhoan) {
    public boolean duDieuKien() {
        return phanCong.isEmpty() && lichHen.isEmpty() && phieuKham.isEmpty() && banGiao.isEmpty();
    }

    public int soViecCon() {
        return phanCong.size() + lichHen.size() + phieuKham.size() + banGiao.size();
    }
}
