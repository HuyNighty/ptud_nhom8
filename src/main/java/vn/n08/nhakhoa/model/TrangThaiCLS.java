package vn.n08.nhakhoa.model;

/** ChiDinhCanLamSang.trangThai. */
public enum TrangThaiCLS implements Labeled {
    CHO_THUC_HIEN("Chờ kết quả", Tone.AMBER),
    DA_CO_KET_QUA("Đã có kết quả", Tone.GREEN);

    private final String label;
    private final Tone tone;

    TrangThaiCLS(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static TrangThaiCLS of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
