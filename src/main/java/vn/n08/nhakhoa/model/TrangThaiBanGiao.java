package vn.n08.nhakhoa.model;

/** BanGiaoCa.trangThai. */
public enum TrangThaiBanGiao implements Labeled {
    CHO_BAN_GIAO("Chờ xác nhận", Tone.AMBER),
    DA_BAN_GIAO("Đã bàn giao", Tone.GREEN),
    DA_HUY("Đã hủy", Tone.RED);

    private final String label;
    private final Tone tone;

    TrangThaiBanGiao(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static TrangThaiBanGiao of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
