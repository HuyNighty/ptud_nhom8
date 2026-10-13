package vn.n08.nhakhoa.model;

/** TaiKhoan.trangThai: BI_KHOA là khóa tạm (mở lại được), VO_HIEU là vô hiệu vĩnh viễn. */
public enum TrangThaiTaiKhoan implements Labeled {
    HOAT_DONG("Hoạt động", Tone.GREEN),
    BI_KHOA("Bị khóa", Tone.AMBER),
    VO_HIEU("Vô hiệu", Tone.GRAY);

    private final String label;
    private final Tone tone;

    TrangThaiTaiKhoan(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static TrangThaiTaiKhoan of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
