
-- N08 - Database Migration V1
-- Workforce + Identity Foundation
-- SQL Server 2022
-- Managed exclusively by Flyway.

CREATE TABLE dbo.NhanVien (
    maNhanVien VARCHAR(10) NOT NULL,
    hoTen NVARCHAR(100) NOT NULL,
    soDienThoai VARCHAR(15) NOT NULL,

    chucDanh VARCHAR(10)
        COLLATE Latin1_General_100_BIN2 NOT NULL,

    trangThai VARCHAR(20)
        COLLATE Latin1_General_100_BIN2 NOT NULL
        CONSTRAINT DF_NhanVien_TrangThai
        DEFAULT 'DANG_LAM_VIEC',

    ngaySinh DATE NULL,
    gioiTinh VARCHAR(5) NULL,
    email VARCHAR(100) NULL,
    ngayVaoLam DATE NULL,
    chuyenMon NVARCHAR(200) NULL,
    ngayNghiViec DATE NULL,

    CONSTRAINT PK_NhanVien
        PRIMARY KEY (maNhanVien),

    CONSTRAINT UQ_NhanVien_SoDienThoai
        UNIQUE (soDienThoai),

    CONSTRAINT CK_NhanVien_HoTen
        CHECK (LEN(TRIM(hoTen)) > 0),

    CONSTRAINT CK_NhanVien_ChucDanh
        CHECK (
            chucDanh IN ('AD', 'LT', 'BS', 'YT', 'QL', 'KHO')
        ),

    CONSTRAINT CK_NhanVien_TrangThai
        CHECK (
            trangThai IN ('DANG_LAM_VIEC', 'NGHI_VIEC')
        ),

    CONSTRAINT CK_NhanVien_GioiTinh
        CHECK (
            gioiTinh IS NULL
            OR gioiTinh IN ('NAM', 'NU', 'KHAC')
        ),

    CONSTRAINT CK_NhanVien_NghiViec
        CHECK (
            (trangThai = 'DANG_LAM_VIEC'
                AND ngayNghiViec IS NULL)
            OR
            (trangThai = 'NGHI_VIEC'
                AND ngayNghiViec IS NOT NULL)
        )
);

CREATE TABLE dbo.TaiKhoan (
    maTaiKhoan VARCHAR(10) NOT NULL,
    maNhanVien VARCHAR(10) NULL,

    tenDangNhap VARCHAR(50)
        COLLATE Latin1_General_100_CI_AS NOT NULL,

    tenHienThi NVARCHAR(100) NOT NULL,
    matKhauHash VARCHAR(255) NOT NULL,

    vaiTro VARCHAR(32)
        COLLATE Latin1_General_100_BIN2 NOT NULL,

    trangThai VARCHAR(20)
        COLLATE Latin1_General_100_BIN2 NOT NULL
        CONSTRAINT DF_TaiKhoan_TrangThai
        DEFAULT 'HOAT_DONG',

    soLanSai INT NOT NULL
        CONSTRAINT DF_TaiKhoan_SoLanSai DEFAULT 0,

    lanSaiDau DATETIME2(3) NULL,
    khoaLuc DATETIME2(3) NULL,
    khoaDen DATETIME2(3) NULL,

    nguonKhoa VARCHAR(20)
        COLLATE Latin1_General_100_BIN2 NULL,

    lyDoKhoa NVARCHAR(300) NULL,

    phaiDoiMatKhau BIT NOT NULL
        CONSTRAINT DF_TaiKhoan_PhaiDoiMatKhau DEFAULT 0,

    matKhauCapNhatLuc DATETIME2(3) NOT NULL,
    lanDangNhapCuoi DATETIME2(3) NULL,

    taoLuc DATETIME2(3) NOT NULL
        CONSTRAINT DF_TaiKhoan_TaoLuc
        DEFAULT SYSUTCDATETIME(),

    capNhatLuc DATETIME2(3) NOT NULL
        CONSTRAINT DF_TaiKhoan_CapNhatLuc
        DEFAULT SYSUTCDATETIME(),

    voHieuLuc DATETIME2(3) NULL,
    lyDoVoHieu NVARCHAR(300) NULL,

    CONSTRAINT PK_TaiKhoan
        PRIMARY KEY (maTaiKhoan),

    CONSTRAINT FK_TaiKhoan_NhanVien
        FOREIGN KEY (maNhanVien)
        REFERENCES dbo.NhanVien(maNhanVien),

    CONSTRAINT UQ_TaiKhoan_TenDangNhap
        UNIQUE (tenDangNhap),

    CONSTRAINT CK_TaiKhoan_TenDangNhap
        CHECK (
            DATALENGTH(tenDangNhap) > 0
            AND DATALENGTH(tenDangNhap) =
                DATALENGTH(TRIM(tenDangNhap))
        ),

    CONSTRAINT CK_TaiKhoan_TenHienThi
        CHECK (LEN(TRIM(tenHienThi)) > 0),

    CONSTRAINT CK_TaiKhoan_MatKhauHash
        CHECK (LEN(TRIM(matKhauHash)) > 0),

    CONSTRAINT CK_TaiKhoan_VaiTro
        CHECK (
            vaiTro IN (
                'RECEPTIONIST',
                'DOCTOR',
                'NURSE',
                'CLINIC_MANAGER',
                'INVENTORY_MANAGER',
                'SYSTEM_ADMIN'
            )
        ),

    CONSTRAINT CK_TaiKhoan_NhanVien
        CHECK (
            maNhanVien IS NOT NULL
            OR vaiTro = 'SYSTEM_ADMIN'
        ),

    CONSTRAINT CK_TaiKhoan_TrangThai
        CHECK (
            trangThai IN (
                'HOAT_DONG',
                'BI_KHOA',
                'VO_HIEU'
            )
        ),

    CONSTRAINT CK_TaiKhoan_SoLanSai
        CHECK (
            (soLanSai = 0 AND lanSaiDau IS NULL)
            OR
            (soLanSai > 0 AND lanSaiDau IS NOT NULL)
        ),

    CONSTRAINT CK_TaiKhoan_LockState
        CHECK (
            (
                trangThai = 'HOAT_DONG'
                AND khoaLuc IS NULL
                AND khoaDen IS NULL
                AND nguonKhoa IS NULL
                AND lyDoKhoa IS NULL
                AND voHieuLuc IS NULL
                AND lyDoVoHieu IS NULL
            )
            OR
            (
                trangThai = 'BI_KHOA'
                AND khoaLuc IS NOT NULL
                AND lyDoKhoa IS NOT NULL
                AND voHieuLuc IS NULL
                AND lyDoVoHieu IS NULL
                AND (
                    (
                        nguonKhoa = 'FAILED_LOGIN'
                        AND khoaDen IS NOT NULL
                        AND khoaDen > khoaLuc
                    )
                    OR
                    (
                        nguonKhoa = 'ADMIN_ACTION'
                        AND khoaDen IS NULL
                    )
                )
            )
            OR
            (
                trangThai = 'VO_HIEU'
                AND voHieuLuc IS NOT NULL
                AND lyDoVoHieu IS NOT NULL
                AND khoaLuc IS NULL
                AND khoaDen IS NULL
                AND nguonKhoa IS NULL
                AND lyDoKhoa IS NULL
            )
        )
);

CREATE UNIQUE INDEX UX_TaiKhoan_NhanVien_NotNull
ON dbo.TaiKhoan (maNhanVien)
WHERE maNhanVien IS NOT NULL;
