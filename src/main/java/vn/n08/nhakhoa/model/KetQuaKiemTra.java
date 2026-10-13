package vn.n08.nhakhoa.model;

/** KiemTraCapCuu.ketQua. */
public enum KetQuaKiemTra implements Labeled {
    DAT("Đạt", Tone.GREEN),
    CAN_BO_SUNG("Cần bổ sung", Tone.RED);

    private final String label;
    private final Tone tone;

    KetQuaKiemTra(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static KetQuaKiemTra of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
