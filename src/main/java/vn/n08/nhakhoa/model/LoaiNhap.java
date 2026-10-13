package vn.n08.nhakhoa.model;

/** PhieuNhapKho.loaiNhap. */
public enum LoaiNhap implements Labeled {
    NHAP_NCC("Nhập từ NCC", Tone.BLUE),
    HOAN_TRA("Hoàn trả", Tone.TEAL);

    private final String label;
    private final Tone tone;

    LoaiNhap(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static LoaiNhap of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
