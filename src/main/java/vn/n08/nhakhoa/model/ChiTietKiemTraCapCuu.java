package vn.n08.nhakhoa.model;

import java.time.LocalDate;

/** [Bổ sung] Kết quả từng mục trong một lần kiểm tra. */
public record ChiTietKiemTraCapCuu(
        String maKiemTra,
        String maMuc,
        int soLuongThucTe,
        LocalDate hanSuDung,
        boolean dat) {}
