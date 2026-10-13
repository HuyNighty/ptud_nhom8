package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.KetQuaKiemTra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Số liệu màn hình Tổng quan (Figma QL-01). Phần nào tài khoản không có quyền xem thì null.
 * Mọi số đều tính tại thời điểm tải, không lưu.
 */
public record DashboardData(
        Map<String, Long> lichHenHomNay, Long choKham, Long dangKham, Long daKetLuan,
        Map<String, BigDecimal> doanhThuHomNay, BigDecimal tongCongNo, Long soHoaDonConNo,
        Long yeuCauHuyCho, Long dieuChinhCho, Long banGiaoCho,
        Long duoiDinhMuc, Long sapHetHan, Long daHetHanConTon,
        KetQuaKiemTra capCuuKetQua, LocalDateTime capCuuLanCuoi, Boolean capCuuQuaHan,
        Long taiKhoanBiKhoa, List<String> caThieuNguoi, Long taiKhamDenHan) {}
