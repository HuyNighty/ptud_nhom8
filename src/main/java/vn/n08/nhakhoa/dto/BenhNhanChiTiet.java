package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.BenhNhan;
import vn.n08.nhakhoa.model.HoSoBenhAn;

import java.util.List;

/** UC09 Xem hồ sơ bệnh nhân: hành chính, bệnh án, lượt khám, kế hoạch, lịch hẹn, hóa đơn. */
public record BenhNhanChiTiet(BenhNhan bn, HoSoBenhAn hoSo, List<PhieuKhamView> luotKham, List<KeHoachView> keHoach,
                              List<LichHenView> lichHen, List<HoaDonView> hoaDon) {}
