
package com.quanlynhakhoa.modules.identity.infrastructure.persistence.entity;

import com.quanlynhakhoa.modules.identity.enums.AccountStatus;
import com.quanlynhakhoa.modules.identity.enums.LockSource;
import com.quanlynhakhoa.modules.identity.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TaiKhoan", schema = "dbo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaiKhoanJpaEntity {

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
}
