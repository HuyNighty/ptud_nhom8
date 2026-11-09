package com.quanlynhakhoa.model.nhatky;

import java.time.LocalDate;
import java.util.Set;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class BoLocNhatKy {
    private final LocalDate tuNgay;
    private final LocalDate denNgay;
    private final String tuKhoa;
    private final String ketQua;

    public BoLocNhatKy(LocalDate tuNgay, LocalDate denNgay, String tuKhoa, String ketQua) {
        tuKhoa = tuKhoa == null ? "" : tuKhoa.trim();
        ketQua = ketQua == null || ketQua.isBlank() ? null : ketQua.trim();
        if (tuKhoa.length() > 80) {
        throw new IllegalArgumentException("Từ khóa tối đa 80 ký tự.");
        }
        if (tuNgay != null && denNgay != null && tuNgay.isAfter(denNgay)) {
        throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
        }
        if (ketQua != null && !RESULTS.contains(ketQua)) {
        throw new IllegalArgumentException("Bộ lọc kết quả không hợp lệ.");
        }
        this.tuNgay = tuNgay;
        this.denNgay = denNgay;
        this.tuKhoa = tuKhoa;
        this.ketQua = ketQua;
    }

    public LocalDate from() { return tuNgay; }
    public LocalDate to() { return denNgay; }
    public String keyword() { return tuKhoa; }
    public String result() { return ketQua; }

    private static final Set<String> RESULTS = Set.of("SUCCESS", "DENIED", "FAILED");



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BoLocNhatKy value)) return false;
        return java.util.Objects.equals(tuNgay, value.tuNgay)
                && java.util.Objects.equals(denNgay, value.denNgay)
                && java.util.Objects.equals(tuKhoa, value.tuKhoa)
                && java.util.Objects.equals(ketQua, value.ketQua);
    }

    @Override
    public int hashCode() {
        int ketQua = 0;
        ketQua = 31 * ketQua + java.util.Objects.hashCode(this.tuNgay);
        ketQua = 31 * ketQua + java.util.Objects.hashCode(this.denNgay);
        ketQua = 31 * ketQua + java.util.Objects.hashCode(this.tuKhoa);
        ketQua = 31 * ketQua + java.util.Objects.hashCode(this.ketQua);
        return ketQua;
    }

    @Override
    public String toString() {
        return "AuditFilter[" + "from=" + tuNgay + ", " + "to=" + denNgay + ", " + "keyword=" + tuKhoa + ", " + "result=" + ketQua + "]";
    }
}
