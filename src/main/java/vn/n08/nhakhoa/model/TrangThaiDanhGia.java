package vn.n08.nhakhoa.model;

/** DanhGiaNhanSu.trangThai. */
public enum TrangThaiDanhGia implements Labeled {
    NHAP("Bản nháp", Tone.AMBER),
    DA_GUI("Đã gửi", Tone.GREEN);

    private final String label;
    private final Tone tone;

    TrangThaiDanhGia(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static TrangThaiDanhGia of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
