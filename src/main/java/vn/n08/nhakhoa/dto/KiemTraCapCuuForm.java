package vn.n08.nhakhoa.dto;

import java.time.LocalDate;
import java.util.List;

/** Kết quả kiểm tra thực tế từng mục hộp cấp cứu. */
public record KiemTraCapCuuForm(String ghiChu, List<Dong> dong) {
    /** Số lượng và hạn dùng gần nhất đếm được của một mục. */
    public record Dong(String maMuc, int soLuongThucTe, LocalDate hanSuDung) {}
}
