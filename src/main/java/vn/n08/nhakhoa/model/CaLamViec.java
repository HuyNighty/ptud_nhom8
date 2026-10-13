package vn.n08.nhakhoa.model;

import java.time.LocalTime;

/** Bảng CaLamViec — ca và định mức nhân sự tối thiểu. */
public record CaLamViec(
        String maCa,
        String tenCa,
        LocalTime gioBatDau,
        LocalTime gioKetThuc,
        int soBacSi,
        int soYTa,
        int soLeTan) {
    public int dinhMuc(VaiTro v) {
        return switch (v) { case BS -> soBacSi; case YT -> soYTa; case LT -> soLeTan; default -> 0; };
    }
    @Override public String toString() { return tenCa + " (" + gioBatDau + "–" + gioKetThuc + ")"; }
}
