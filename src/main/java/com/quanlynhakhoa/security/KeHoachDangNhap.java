package com.quanlynhakhoa.security;

import com.quanlynhakhoa.model.taikhoan.ThongTinTaiKhoan;
import com.quanlynhakhoa.enums.TrangThaiTaiKhoan;
import com.quanlynhakhoa.enums.NguonKhoaTaiKhoan;
import java.time.LocalDateTime;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class KeHoachDangNhap {
    private final boolean daXacThuc;
    private final boolean batBuocDoiMatKhau;
    private final boolean daThayDoiTrangThai;
    private final TrangThaiTaiKhoan trangThaiTiepTheo;
    private final int soLanSaiTiepTheo;
    private final LocalDateTime thoiGianSaiDauTiepTheo;
    private final LocalDateTime thoiGianKhoaTiepTheo;
    private final LocalDateTime khoaDenLucTiepTheo;
    private final NguonKhoaTaiKhoan nguonKhoaTiepTheo;
    private final String lyDoKhoaTiepTheo;
    private final LocalDateTime thoiGianDangNhapCuoiTiepTheo;
    private final LocalDateTime thoiGianCapNhatTiepTheo;

    public KeHoachDangNhap(boolean daXacThuc, boolean batBuocDoiMatKhau, boolean daThayDoiTrangThai, TrangThaiTaiKhoan trangThaiTiepTheo, int soLanSaiTiepTheo, LocalDateTime thoiGianSaiDauTiepTheo, LocalDateTime thoiGianKhoaTiepTheo, LocalDateTime khoaDenLucTiepTheo, NguonKhoaTaiKhoan nguonKhoaTiepTheo, String lyDoKhoaTiepTheo, LocalDateTime thoiGianDangNhapCuoiTiepTheo, LocalDateTime thoiGianCapNhatTiepTheo) {
        this.daXacThuc = daXacThuc;
        this.batBuocDoiMatKhau = batBuocDoiMatKhau;
        this.daThayDoiTrangThai = daThayDoiTrangThai;
        this.trangThaiTiepTheo = trangThaiTiepTheo;
        this.soLanSaiTiepTheo = soLanSaiTiepTheo;
        this.thoiGianSaiDauTiepTheo = thoiGianSaiDauTiepTheo;
        this.thoiGianKhoaTiepTheo = thoiGianKhoaTiepTheo;
        this.khoaDenLucTiepTheo = khoaDenLucTiepTheo;
        this.nguonKhoaTiepTheo = nguonKhoaTiepTheo;
        this.lyDoKhoaTiepTheo = lyDoKhoaTiepTheo;
        this.thoiGianDangNhapCuoiTiepTheo = thoiGianDangNhapCuoiTiepTheo;
        this.thoiGianCapNhatTiepTheo = thoiGianCapNhatTiepTheo;
    }

    public boolean authenticated() { return daXacThuc; }
    public boolean forcePasswordChange() { return batBuocDoiMatKhau; }
    public boolean stateChanged() { return daThayDoiTrangThai; }
    public TrangThaiTaiKhoan nextStatus() { return trangThaiTiepTheo; }
    public int nextFailedAttempts() { return soLanSaiTiepTheo; }
    public LocalDateTime nextFirstFailureAt() { return thoiGianSaiDauTiepTheo; }
    public LocalDateTime nextLockedAt() { return thoiGianKhoaTiepTheo; }
    public LocalDateTime nextLockedUntil() { return khoaDenLucTiepTheo; }
    public NguonKhoaTaiKhoan nextLockSource() { return nguonKhoaTiepTheo; }
    public String nextLockReason() { return lyDoKhoaTiepTheo; }
    public LocalDateTime nextLastLoginAt() { return thoiGianDangNhapCuoiTiepTheo; }
    public LocalDateTime nextUpdatedAt() { return thoiGianCapNhatTiepTheo; }

    public static KeHoachDangNhap rejectedUnknown() {
        return new KeHoachDangNhap(false, false, false, null, 0,
                null, null, null, null, null, null, null);
    }

    public static KeHoachDangNhap unchanged(ThongTinTaiKhoan account) {
        return new KeHoachDangNhap(false, false, false,
                account.status(), account.failedAttempts(), account.firstFailureAt(),
                account.lockedAt(), account.lockedUntil(), account.lockSource(),
                account.lockReason(), account.lastLoginAt(), account.updatedAt());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof KeHoachDangNhap value)) return false;
        return daXacThuc == value.daXacThuc
                && batBuocDoiMatKhau == value.batBuocDoiMatKhau
                && daThayDoiTrangThai == value.daThayDoiTrangThai
                && java.util.Objects.equals(trangThaiTiepTheo, value.trangThaiTiepTheo)
                && soLanSaiTiepTheo == value.soLanSaiTiepTheo
                && java.util.Objects.equals(thoiGianSaiDauTiepTheo, value.thoiGianSaiDauTiepTheo)
                && java.util.Objects.equals(thoiGianKhoaTiepTheo, value.thoiGianKhoaTiepTheo)
                && java.util.Objects.equals(khoaDenLucTiepTheo, value.khoaDenLucTiepTheo)
                && java.util.Objects.equals(nguonKhoaTiepTheo, value.nguonKhoaTiepTheo)
                && java.util.Objects.equals(lyDoKhoaTiepTheo, value.lyDoKhoaTiepTheo)
                && java.util.Objects.equals(thoiGianDangNhapCuoiTiepTheo, value.thoiGianDangNhapCuoiTiepTheo)
                && java.util.Objects.equals(thoiGianCapNhatTiepTheo, value.thoiGianCapNhatTiepTheo);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Boolean.hashCode(this.daXacThuc);
        result = 31 * result + Boolean.hashCode(this.batBuocDoiMatKhau);
        result = 31 * result + Boolean.hashCode(this.daThayDoiTrangThai);
        result = 31 * result + java.util.Objects.hashCode(this.trangThaiTiepTheo);
        result = 31 * result + Integer.hashCode(this.soLanSaiTiepTheo);
        result = 31 * result + java.util.Objects.hashCode(this.thoiGianSaiDauTiepTheo);
        result = 31 * result + java.util.Objects.hashCode(this.thoiGianKhoaTiepTheo);
        result = 31 * result + java.util.Objects.hashCode(this.khoaDenLucTiepTheo);
        result = 31 * result + java.util.Objects.hashCode(this.nguonKhoaTiepTheo);
        result = 31 * result + java.util.Objects.hashCode(this.lyDoKhoaTiepTheo);
        result = 31 * result + java.util.Objects.hashCode(this.thoiGianDangNhapCuoiTiepTheo);
        result = 31 * result + java.util.Objects.hashCode(this.thoiGianCapNhatTiepTheo);
        return result;
    }

    @Override
    public String toString() {
        return "LoginPlan[" + "authenticated=" + daXacThuc + ", " + "forcePasswordChange=" + batBuocDoiMatKhau + ", " + "stateChanged=" + daThayDoiTrangThai + ", " + "nextStatus=" + trangThaiTiepTheo + ", " + "nextFailedAttempts=" + soLanSaiTiepTheo + ", " + "nextFirstFailureAt=" + thoiGianSaiDauTiepTheo + ", " + "nextLockedAt=" + thoiGianKhoaTiepTheo + ", " + "nextLockedUntil=" + khoaDenLucTiepTheo + ", " + "nextLockSource=" + nguonKhoaTiepTheo + ", " + "nextLockReason=" + lyDoKhoaTiepTheo + ", " + "nextLastLoginAt=" + thoiGianDangNhapCuoiTiepTheo + ", " + "nextUpdatedAt=" + thoiGianCapNhatTiepTheo + "]";
    }
}
