package vn.n08.nhakhoa.dto;

import java.time.LocalDate;

/** Dữ liệu lập biên bản giao nhận ca (UC19). */
public record BanGiaoForm(LocalDate ngay, String maNguoiGiao, String maCaGiao, String maNguoiNhan, String maCaNhan,
                          String noiDung, boolean coChenhLech) {}
