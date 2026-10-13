package vn.n08.nhakhoa.model;

/** PhieuKham.trangThai. */
public enum TrangThaiPhieuKham implements Labeled {
    CHO_KHAM("Chờ khám", Tone.AMBER),
    DANG_KHAM("Đang khám", Tone.BLUE),
    DA_KET_LUAN("Đã kết luận", Tone.GREEN),
    DA_HUY("Đã hủy", Tone.RED);

    private final String label;
    private final Tone tone;

    TrangThaiPhieuKham(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static TrangThaiPhieuKham of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
