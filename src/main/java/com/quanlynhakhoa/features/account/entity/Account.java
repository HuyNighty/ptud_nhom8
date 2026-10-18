
package com.quanlynhakhoa.features.account.entity;

import com.quanlynhakhoa.features.account.enums.AccountStatus;
import com.quanlynhakhoa.features.account.enums.LockSource;
import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.shared.exception.BusinessException;
import com.quanlynhakhoa.shared.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity(name = "TaiKhoanJpaEntity")
@Table(name = "TaiKhoan", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @Column(name = "maTaiKhoan", length = 10, nullable = false)
    private String maTaiKhoan;

    @Column(name = "maNhanVien", length = 10)
    private String maNhanVien;

    @Column(name = "tenDangNhap", length = 50, nullable = false)
    private String tenDangNhap;

    @Column(name = "tenHienThi", length = 100, nullable = false)
    private String tenHienThi;

    @Column(name = "matKhauHash", length = 255, nullable = false)
    private String matKhauHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "vaiTro", length = 32, nullable = false)
    private Role vaiTro;

    @Enumerated(EnumType.STRING)
    @Column(name = "trangThai", length = 20, nullable = false)
    private AccountStatus trangThai;

    @Column(name = "soLanSai", nullable = false)
    private int soLanSai;

    @Column(name = "lanSaiDau")
    private LocalDateTime lanSaiDau;

    @Column(name = "khoaLuc")
    private LocalDateTime khoaLuc;

    @Column(name = "khoaDen")
    private LocalDateTime khoaDen;

    @Enumerated(EnumType.STRING)
    @Column(name = "nguonKhoa", length = 20)
    private LockSource nguonKhoa;

    @Column(name = "lyDoKhoa", length = 300)
    private String lyDoKhoa;

    @Column(name = "phaiDoiMatKhau", nullable = false)
    private boolean phaiDoiMatKhau;

    @Column(name = "matKhauCapNhatLuc", nullable = false)
    private LocalDateTime matKhauCapNhatLuc;

    @Column(name = "lanDangNhapCuoi")
    private LocalDateTime lanDangNhapCuoi;

    @Column(name = "taoLuc", nullable = false)
    private LocalDateTime taoLuc;

    @Column(name = "capNhatLuc", nullable = false)
    private LocalDateTime capNhatLuc;

    @Column(name = "voHieuLuc")
    private LocalDateTime voHieuLuc;

    @Column(name = "lyDoVoHieu", length = 300)
    private String lyDoVoHieu;

    public void recordFailedPasswordAttempt(LocalDateTime nowUtc) {

        if (trangThai != AccountStatus.HOAT_DONG) {
            throw new BusinessException(ErrorCode.ACCOUNT_INVALID_STATE);
        }

        if (lanSaiDau == null ||
                !nowUtc.isBefore(lanSaiDau.plusMinutes(15))) {

            soLanSai = 1;
            lanSaiDau = nowUtc;

        } else {
            soLanSai++;
        }

        if (soLanSai >= 5) {
            trangThai = AccountStatus.BI_KHOA;

            khoaLuc = nowUtc;
            khoaDen = nowUtc.plusMinutes(15);

            nguonKhoa = LockSource.FAILED_LOGIN;
            lyDoKhoa = "Khóa tự động do đăng nhập sai 5 lần";
        }

        capNhatLuc = nowUtc;
    }

    public boolean unlockExpiredAutomaticLock(LocalDateTime nowUtc) {

        if (trangThai != AccountStatus.BI_KHOA) {
            return false;
        }

        if (nguonKhoa != LockSource.FAILED_LOGIN) {
            return false;
        }

        if (khoaDen == null || nowUtc.isBefore(khoaDen)) {
            return false;
        }

        trangThai = AccountStatus.HOAT_DONG;

        soLanSai = 0;
        lanSaiDau = null;

        khoaLuc = null;
        khoaDen = null;
        nguonKhoa = null;
        lyDoKhoa = null;

        capNhatLuc = nowUtc;

        return true;
    }

    public void recordSuccessfulLogin(LocalDateTime nowUtc) {

        if (trangThai != AccountStatus.HOAT_DONG) {
            throw new BusinessException(
                    ErrorCode.ACCOUNT_INVALID_STATE
            );
        }

        soLanSai = 0;
        lanSaiDau = null;

        lanDangNhapCuoi = nowUtc;
        capNhatLuc = nowUtc;
    }
}
