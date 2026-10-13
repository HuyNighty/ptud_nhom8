package vn.n08.nhakhoa.model;

/** Trạng thái chung của yêu cầu cần người khác duyệt (YeuCauHuyHoaDon, PhieuDieuChinhKho). */
public enum TrangThaiDuyet implements Labeled {
    CHO_DUYET("Chờ duyệt", Tone.AMBER),
    DA_DUYET("Đã duyệt", Tone.GREEN),
    TU_CHOI("Từ chối", Tone.RED);

    private final String label;
    private final Tone tone;

    TrangThaiDuyet(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static TrangThaiDuyet of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
