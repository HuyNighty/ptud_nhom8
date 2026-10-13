package vn.n08.nhakhoa.dto;

import java.time.LocalDate;

/** Một hạng mục khi lập/bổ sung kế hoạch điều trị. */
public record HangMucForm(String maDichVu, String viTriRang, LocalDate ngayDuKien) {}
