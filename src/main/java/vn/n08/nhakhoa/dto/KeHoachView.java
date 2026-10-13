package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.KeHoachDieuTri;

import java.math.BigDecimal;

/** Kế hoạch điều trị kèm bệnh nhân, bác sĩ lập, tiến độ và chi phí dự kiến (tính từ hạng mục chưa hủy). */
public record KeHoachView(KeHoachDieuTri kh, String maBenhNhan, String tenBenhNhan, String tenBacSi,
                          int soHangMuc, int soDaThucHien, BigDecimal chiPhiDuKien) {}
