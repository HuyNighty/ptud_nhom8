package com.quanlynhakhoa.model.vaitro;


















/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DongVaiTro {
    private final String code;
    private final String name;
    private final boolean system;
    private final boolean active;

    public DongVaiTro(String code, String name, boolean system, boolean active) {
        this.code = code;
        this.name = name;
        this.system = system;
        this.active = active;
    }

    public String code() { return code; }
    public String name() { return name; }
    public boolean system() { return system; }
    public boolean active() { return active; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongVaiTro value)) return false;
        return java.util.Objects.equals(code, value.code)
                && java.util.Objects.equals(name, value.name)
                && system == value.system
                && active == value.active;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(code);
        result = 31 * result + java.util.Objects.hashCode(name);
        result = 31 * result + Boolean.hashCode(system);
        result = 31 * result + Boolean.hashCode(active);
        return result;
    }

    @Override
    public String toString() {
        return "RoleRow[" + "code=" + code + ", " + "name=" + name + ", " + "system=" + system + ", " + "active=" + active + "]";
    }
}
