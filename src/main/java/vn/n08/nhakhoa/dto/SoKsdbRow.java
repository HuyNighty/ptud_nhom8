package vn.n08.nhakhoa.dto;

import java.time.LocalDateTime;

/** Một dòng trong sổ theo dõi thuốc kiểm soát đặc biệt (UC33). soLuong âm = giảm tồn; dòng kê đơn không làm đổi tồn. */
public record SoKsdbRow(LocalDateTime thoiGian, String loai, String chungTu, String maThuoc, String tenThuoc, String soLo,
                        int soLuong, String nguoiThucHien, String ghiChu) {}
