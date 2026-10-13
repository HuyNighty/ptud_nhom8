package vn.n08.nhakhoa.dto;

import java.util.List;

/** Kế hoạch điều trị và các hạng mục. */
public record KeHoachChiTiet(KeHoachView keHoach, List<ChiTietDieuTriView> hangMuc) {}
