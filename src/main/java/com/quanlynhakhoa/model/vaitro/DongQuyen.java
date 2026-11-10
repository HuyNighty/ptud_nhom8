package com.quanlynhakhoa.model.vaitro;


















/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DongQuyen {
    private final String code;
    private final String name;
    private final boolean builtIn;

    public DongQuyen(String code, String name, boolean builtIn) {
        this.code = code;
        this.name = name;
        this.builtIn = builtIn;
    }

    public String code() { return code; }
    public String name() { return name; }
    public boolean builtIn() { return builtIn; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongQuyen value)) return false;
        return java.util.Objects.equals(code, value.code)
                && java.util.Objects.equals(name, value.name)
                && builtIn == value.builtIn;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(code);
        result = 31 * result + java.util.Objects.hashCode(name);
        result = 31 * result + Boolean.hashCode(builtIn);
        return result;
    }

    @Override
    public String toString() {
        return "PermissionRow[" + "code=" + code + ", " + "name=" + name + ", " + "builtIn=" + builtIn + "]";
    }
}
