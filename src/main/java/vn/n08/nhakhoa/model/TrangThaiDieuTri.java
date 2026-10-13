package vn.n08.nhakhoa.model;

/** ChiTietDieuTri.trangThai (hạng mục). */
public enum TrangThaiDieuTri implements Labeled {
    CHUA_THUC_HIEN("Chưa thực hiện", Tone.AMBER),
    DA_THUC_HIEN("Đã thực hiện", Tone.GREEN),
    DA_HUY("Đã hủy", Tone.RED);

    private final String label;
    private final Tone tone;

    TrangThaiDieuTri(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static TrangThaiDieuTri of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
