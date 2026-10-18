-- N08 - Flyway V5
-- Clinical examination feature: independent service catalog.
-- V1-V4 are already applied and must remain unchanged.

CREATE TABLE dbo.DichVu (
    maDichVu VARCHAR(10) COLLATE Latin1_General_100_BIN2 NOT NULL,
    tenDichVu NVARCHAR(100) NOT NULL,
    moTa NVARCHAR(500) NULL,
    donGia DECIMAL(12,2) NOT NULL,
    laCanLamSang BIT NOT NULL
        CONSTRAINT DF_DichVu_LaCanLamSang DEFAULT 0,
    dangApDung BIT NOT NULL
        CONSTRAINT DF_DichVu_DangApDung DEFAULT 1,
    taoLuc DATETIME2(3) NOT NULL
        CONSTRAINT DF_DichVu_TaoLuc DEFAULT SYSUTCDATETIME(),
    capNhatLuc DATETIME2(3) NOT NULL
        CONSTRAINT DF_DichVu_CapNhatLuc DEFAULT SYSUTCDATETIME(),

    CONSTRAINT PK_DichVu PRIMARY KEY (maDichVu),
    CONSTRAINT UQ_DichVu_TenDichVu UNIQUE (tenDichVu),
    CONSTRAINT CK_DichVu_MaDichVu_NotBlank
        CHECK (LEN(TRIM(maDichVu)) > 0),
    CONSTRAINT CK_DichVu_TenDichVu_NotBlank
        CHECK (LEN(TRIM(tenDichVu)) > 0),
    CONSTRAINT CK_DichVu_DonGia_NonNegative
        CHECK (donGia >= 0)
);

CREATE INDEX IX_DichVu_DangApDung_TenDichVu
ON dbo.DichVu (dangApDung, tenDichVu);
