package com.quanlynhakhoa.model.taikhoan;

import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.model.nhanvien.NhanChucDanh;


/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class NhanVienDuDieuKien {
    private final String maNhanVien;
    private final String hoTen;
    private final String chucDanh;
    private final VaiTroHeThong vaiTro;

    public NhanVienDuDieuKien(String maNhanVien, String hoTen, String chucDanh, VaiTroHeThong vaiTro) {
        if (maNhanVien == null || !maNhanVien.matches("[A-Z0-9]{1,10}")) {
        throw new IllegalArgumentException("Mã nhân viên không hợp lệ.");
        }
        if (hoTen == null || hoTen.isBlank()) {
        throw new IllegalArgumentException("Nhân viên phải có họ tên.");
        }
        if (vaiTro != roleForJob(chucDanh)) {
        throw new IllegalArgumentException("Vai trò không khớp chức danh nhân viên.");
        }
        this.maNhanVien = maNhanVien;
        this.hoTen = hoTen;
        this.chucDanh = chucDanh;
        this.vaiTro = vaiTro;
    }

    public String employeeId() { return maNhanVien; }
    public String fullName() { return hoTen; }
    public String jobTitle() { return chucDanh; }
    public VaiTroHeThong role() { return vaiTro; }



    public static VaiTroHeThong roleForJob(String chucDanh) {
        if (chucDanh == null) throw new IllegalArgumentException("Chức danh không hợp lệ.");
        return switch (chucDanh) {
            case "LT" -> VaiTroHeThong.RECEPTIONIST;
            case "BS" -> VaiTroHeThong.DOCTOR;
            case "YT" -> VaiTroHeThong.NURSE;
            case "QL" -> VaiTroHeThong.CLINIC_MANAGER;
            case "KHO" -> VaiTroHeThong.INVENTORY_MANAGER;
            default -> throw new IllegalArgumentException("Không thể cấp tài khoản cho chức danh này.");
        };
    }

    public String displayLabel() {
        return maNhanVien + " — " + hoTen + " (" + NhanChucDanh.label(chucDanh) + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof NhanVienDuDieuKien value)) return false;
        return java.util.Objects.equals(maNhanVien, value.maNhanVien)
                && java.util.Objects.equals(hoTen, value.hoTen)
                && java.util.Objects.equals(chucDanh, value.chucDanh)
                && java.util.Objects.equals(vaiTro, value.vaiTro);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(maNhanVien);
        result = 31 * result + java.util.Objects.hashCode(hoTen);
        result = 31 * result + java.util.Objects.hashCode(chucDanh);
        result = 31 * result + java.util.Objects.hashCode(vaiTro);
        return result;
    }

    @Override
    public String toString() {
        return "EligibleEmployee[" + "employeeId=" + maNhanVien + ", " + "fullName=" + hoTen + ", " + "jobTitle=" + chucDanh + ", " + "role=" + vaiTro + "]";
    }
}
