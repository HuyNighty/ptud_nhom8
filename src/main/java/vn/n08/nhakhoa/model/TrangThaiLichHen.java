package vn.n08.nhakhoa.model;

/** LichHen.trangThai — State Machine tuần 7. */
public enum TrangThaiLichHen implements Labeled {
    CHO_XAC_NHAN("Chờ xác nhận", Tone.AMBER),
    DA_XAC_NHAN("Đã xác nhận", Tone.BLUE),
    DA_DEN("Đã đến", Tone.TEAL),
    HOAN_THANH("Hoàn thành", Tone.GREEN),
    VANG_MAT("Vắng mặt", Tone.GRAY),
    DA_HUY("Đã hủy", Tone.RED),
    DA_DOI_LICH("Đã đổi lịch", Tone.PURPLE);

    private final String label;
    private final Tone tone;

    TrangThaiLichHen(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static TrangThaiLichHen of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
