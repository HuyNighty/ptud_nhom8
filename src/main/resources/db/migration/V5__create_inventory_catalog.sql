-- N08 - Database Migration V5 (ĐỀ XUẤT — số version chờ Huy xác nhận trước khi merge)
-- Inventory catalog: danh mục Thuốc và Vật tư (M1)
-- SQL Server 2022
-- Managed exclusively by Flyway.
-- Nguồn cột: sql/02_tao_bang.sql (prototype đã duyệt) + docs/THIET_KE_DU_LIEU.md mục 3.
-- LoHang / Phiếu kho thuộc M2, không tạo ở đây.

CREATE TABLE dbo.Thuoc (
    maThuoc VARCHAR(10) NOT NULL,
    tenThuoc NVARCHAR(150) NOT NULL,
    donViTinh NVARCHAR(30) NOT NULL,

    kiemSoatDacBiet BIT NOT NULL
        CONSTRAINT DF_Thuoc_KiemSoatDacBiet
        DEFAULT 0,

    hamLuong NVARCHAR(50) NULL,
    hoatChat NVARCHAR(150) NULL,

    dinhMucToiThieu INT NOT NULL
        CONSTRAINT DF_Thuoc_DinhMucToiThieu
        DEFAULT 0,

    daDatHang BIT NOT NULL
        CONSTRAINT DF_Thuoc_DaDatHang
        DEFAULT 0,

    CONSTRAINT PK_Thuoc
        PRIMARY KEY (maThuoc),

    CONSTRAINT CK_Thuoc_Ma
        CHECK (
            maThuoc LIKE 'TH[0-9][0-9][0-9]%'
            AND maThuoc NOT LIKE 'TH%[^0-9]%'
        ),

    CONSTRAINT CK_Thuoc_Ten
        CHECK (LEN(TRIM(tenThuoc)) > 0),

    CONSTRAINT CK_Thuoc_DonViTinh
        CHECK (LEN(TRIM(donViTinh)) > 0),

    CONSTRAINT CK_Thuoc_DinhMuc
        CHECK (dinhMucToiThieu >= 0)
);

CREATE TABLE dbo.VatTu (
    maVatTu VARCHAR(10) NOT NULL,
    tenVatTu NVARCHAR(150) NOT NULL,
    donViTinh NVARCHAR(30) NOT NULL,
    moTa NVARCHAR(300) NULL,

    dinhMucToiThieu INT NOT NULL
        CONSTRAINT DF_VatTu_DinhMucToiThieu
        DEFAULT 0,

    daDatHang BIT NOT NULL
        CONSTRAINT DF_VatTu_DaDatHang
        DEFAULT 0,

    CONSTRAINT PK_VatTu
        PRIMARY KEY (maVatTu),

    CONSTRAINT CK_VatTu_Ma
        CHECK (
            maVatTu LIKE 'VT[0-9][0-9][0-9]%'
            AND maVatTu NOT LIKE 'VT%[^0-9]%'
        ),

    CONSTRAINT CK_VatTu_Ten
        CHECK (LEN(TRIM(tenVatTu)) > 0),

    CONSTRAINT CK_VatTu_DonViTinh
        CHECK (LEN(TRIM(donViTinh)) > 0),

    CONSTRAINT CK_VatTu_DinhMuc
        CHECK (dinhMucToiThieu >= 0)
);

CREATE INDEX IX_Thuoc_TenThuoc ON dbo.Thuoc (tenThuoc);
CREATE INDEX IX_VatTu_TenVatTu ON dbo.VatTu (tenVatTu);
