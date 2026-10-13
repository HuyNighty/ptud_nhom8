package vn.n08.nhakhoa.model;

/** KeHoachDieuTri.trangThai. */
public enum TrangThaiKeHoach implements Labeled {
    DANG_THUC_HIEN("Đang thực hiện", Tone.BLUE),
    HOAN_THANH("Hoàn thành", Tone.GREEN),
    DA_HUY("Đã hủy", Tone.RED);

    private final String label;
    private final Tone tone;

    TrangThaiKeHoach(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static TrangThaiKeHoach of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
