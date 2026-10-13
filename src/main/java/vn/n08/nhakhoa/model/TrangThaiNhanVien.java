package vn.n08.nhakhoa.model;

/** NhanVien.trangThai. */
public enum TrangThaiNhanVien implements Labeled {
    DANG_LAM_VIEC("Đang làm việc", Tone.GREEN),
    NGHI_VIEC("Đã nghỉ việc", Tone.GRAY);

    private final String label;
    private final Tone tone;

    TrangThaiNhanVien(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static TrangThaiNhanVien of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
