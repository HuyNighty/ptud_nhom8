
-- N08 - Flyway V4
-- Audit Log Foundation
-- SQL Server 2022

ALTER TABLE dbo.PhienDangNhap
ADD CONSTRAINT UQ_PhienDangNhap_SessionAccount
UNIQUE (sessionId, maTaiKhoan);

CREATE TABLE dbo.NhatKyThaoTac (
    maNhatKy BIGINT IDENTITY(1,1) NOT NULL,

    thoiGian DATETIME2(3) NOT NULL
        CONSTRAINT DF_NhatKy_ThoiGian
        DEFAULT SYSUTCDATETIME(),

    loaiTacNhan VARCHAR(16)
        COLLATE Latin1_General_100_BIN2 NOT NULL,

    maTaiKhoan VARCHAR(10) NULL,
    sessionId UNIQUEIDENTIFIER NULL,

    dinhDanhTacNhan NVARCHAR(128) NULL,

    chucNang NVARCHAR(50) NOT NULL,
    hanhDong NVARCHAR(200) NOT NULL,

    ketQua VARCHAR(16)
        COLLATE Latin1_General_100_BIN2 NOT NULL,

    doiTuong VARCHAR(50) NULL,
    maDoiTuong VARCHAR(64) NULL,

    noiDung NVARCHAR(1000) NULL,

    maNguoiChiuTrachNhiem VARCHAR(10) NULL,
    correlationId UNIQUEIDENTIFIER NULL,

    CONSTRAINT PK_NhatKyThaoTac
        PRIMARY KEY (maNhatKy),

    CONSTRAINT FK_NhatKy_TaiKhoan
        FOREIGN KEY (maTaiKhoan)
        REFERENCES dbo.TaiKhoan(maTaiKhoan),

    CONSTRAINT FK_NhatKy_SessionAccount
        FOREIGN KEY (sessionId, maTaiKhoan)
        REFERENCES dbo.PhienDangNhap(
            sessionId, maTaiKhoan
        ),

    CONSTRAINT FK_NhatKy_NguoiChiuTrachNhiem
        FOREIGN KEY (maNguoiChiuTrachNhiem)
        REFERENCES dbo.NhanVien(maNhanVien),

    CONSTRAINT CK_NhatKy_KetQua
        CHECK (
            ketQua IN (
                'SUCCESS',
                'DENIED',
                'FAILED'
            )
        ),

    CONSTRAINT CK_NhatKy_TacNhan
        CHECK (
            (
                loaiTacNhan = 'ACCOUNT'
                AND maTaiKhoan IS NOT NULL
                AND dinhDanhTacNhan IS NULL
            )
            OR
            (
                loaiTacNhan = 'ANONYMOUS'
                AND maTaiKhoan IS NULL
                AND sessionId IS NULL
                AND dinhDanhTacNhan IS NULL
            )
            OR
            (
                loaiTacNhan IN (
                    'SYSTEM',
                    'MAINTENANCE'
                )
                AND maTaiKhoan IS NULL
                AND sessionId IS NULL
                AND dinhDanhTacNhan IS NOT NULL
                AND LEN(TRIM(dinhDanhTacNhan)) > 0
            )
        ),

    CONSTRAINT CK_NhatKy_SessionActor
        CHECK (
            sessionId IS NULL
            OR loaiTacNhan = 'ACCOUNT'
        ),

    CONSTRAINT CK_NhatKy_ChucNang
        CHECK (
            LEN(TRIM(chucNang)) > 0
        ),

    CONSTRAINT CK_NhatKy_HanhDong
        CHECK (
            LEN(TRIM(hanhDong)) > 0
        ),

    CONSTRAINT CK_NhatKy_DoiTuong
        CHECK (
            maDoiTuong IS NULL
            OR (
                doiTuong IS NOT NULL
                AND LEN(TRIM(doiTuong)) > 0
            )
        )
);

CREATE INDEX IX_NhatKy_ThoiGian
ON dbo.NhatKyThaoTac (
    thoiGian DESC,
    maNhatKy DESC
);

CREATE INDEX IX_NhatKy_TaiKhoan
ON dbo.NhatKyThaoTac (
    maTaiKhoan,
    thoiGian DESC
)
WHERE maTaiKhoan IS NOT NULL;

CREATE INDEX IX_NhatKy_DoiTuong
ON dbo.NhatKyThaoTac (
    doiTuong,
    maDoiTuong,
    thoiGian DESC
)
WHERE doiTuong IS NOT NULL;

CREATE INDEX IX_NhatKy_Correlation
ON dbo.NhatKyThaoTac (
    correlationId
)
WHERE correlationId IS NOT NULL;

EXEC(N'
CREATE TRIGGER dbo.TR_NhatKyThaoTac_AppendOnly
ON dbo.NhatKyThaoTac
AFTER UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    THROW 51001,
        ''Audit records are append-only.'',
        1;
END
');
