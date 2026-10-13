package vn.n08.nhakhoa.model;

/** Bảng NhaCungCap. */
public record NhaCungCap(
        String maNhaCungCap,
        String tenNhaCungCap,
        String diaChi,
        String soDienThoai,
        String email,
        TrangThaiNCC trangThai) {
    @Override public String toString() { return tenNhaCungCap; }
}
