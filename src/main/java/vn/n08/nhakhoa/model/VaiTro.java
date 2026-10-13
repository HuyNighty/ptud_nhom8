package vn.n08.nhakhoa.model;

/** Vai trò / chức danh (TaiKhoan.vaiTro, NhanVien.chucDanh). */
public enum VaiTro implements Labeled {
    AD("Quản trị (Admin)", Tone.PURPLE),
    LT("Lễ tân", Tone.BLUE),
    BS("Bác sĩ", Tone.TEAL),
    YT("Y tá", Tone.GREEN),
    QL("QL phòng khám", Tone.ORANGE),
    KHO("QL kho", Tone.AMBER);

    private final String label;
    private final Tone tone;

    VaiTro(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    @Override public String label() { return label; }
    @Override public Tone tone() { return tone; }
    @Override public String toString() { return label; }

    /** Đọc giá trị lưu trong CSDL; null nếu cột NULL. */
    public static VaiTro of(String code) {
        return code == null ? null : valueOf(code.trim());
    }
}
