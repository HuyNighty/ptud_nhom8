-- Add the two tables used by SupplyRequestDao, which are absent from V1-V5.
-- Additive and repeatable; existing rows are preserved. Local development only.
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;
IF LEN(DB_NAME()) <> 15 OR DB_NAME() COLLATE Latin1_General_100_BIN2
    NOT LIKE N'N08Dev[_][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]'
    THROW 51110, 'Expected a local N08Dev database.', 1;
IF OBJECT_ID('dbo.PhieuXuatKho', 'U') IS NULL
    THROW 51111, 'Install bootstrap V1-V5 first.', 1;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @lockResult int;
    EXEC @lockResult = sys.sp_getapplock @Resource = N'N08_SCHEMA_V6',
        @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 5000;
    IF @lockResult < 0 THROW 51112, 'Supply request migration is locked.', 1;

    IF OBJECT_ID('dbo.YeuCauCapPhat', 'U') IS NULL
    CREATE TABLE dbo.YeuCauCapPhat (
        maYeuCau varchar(10) NOT NULL CONSTRAINT PK_YeuCauCapPhat PRIMARY KEY,
        thoiGianTao datetime2(0) NOT NULL CONSTRAINT DF_YCCP_ThoiGian DEFAULT SYSUTCDATETIME(),
        maNhanVienYeuCau varchar(10) NOT NULL,
        maTaiKhoanTao varchar(10) NOT NULL,
        lyDo nvarchar(300) NOT NULL,
        trangThai varchar(20) NOT NULL CONSTRAINT DF_YCCP_TrangThai DEFAULT 'CHO_DUYET',
        maTaiKhoanXuLy varchar(10) NULL,
        thoiGianXuLy datetime2(0) NULL,
        ghiChuXuLy nvarchar(500) NULL,
        maPhieuXuat varchar(10) NULL,
        CONSTRAINT FK_YCCP_NhanVien FOREIGN KEY (maNhanVienYeuCau) REFERENCES dbo.NhanVien(maNhanVien),
        CONSTRAINT FK_YCCP_NguoiTao FOREIGN KEY (maTaiKhoanTao) REFERENCES dbo.TaiKhoan(maTaiKhoan),
        CONSTRAINT FK_YCCP_NguoiXuLy FOREIGN KEY (maTaiKhoanXuLy) REFERENCES dbo.TaiKhoan(maTaiKhoan),
        CONSTRAINT FK_YCCP_PhieuXuat FOREIGN KEY (maPhieuXuat) REFERENCES dbo.PhieuXuatKho(maPhieuXuat),
        CONSTRAINT CK_YCCP_TrangThai CHECK (trangThai IN ('CHO_DUYET','DA_DUYET','TU_CHOI','DA_CAP_PHAT')),
        CONSTRAINT CK_YCCP_KhongTuDuyet CHECK (maTaiKhoanXuLy IS NULL OR maTaiKhoanXuLy <> maTaiKhoanTao),
        CONSTRAINT CK_YCCP_XuLy CHECK (trangThai = 'CHO_DUYET' OR (maTaiKhoanXuLy IS NOT NULL AND thoiGianXuLy IS NOT NULL)),
        CONSTRAINT CK_YCCP_CapPhat CHECK (trangThai <> 'DA_CAP_PHAT' OR maPhieuXuat IS NOT NULL)
    );

    IF OBJECT_ID('dbo.ChiTietYeuCauCapPhat', 'U') IS NULL
    CREATE TABLE dbo.ChiTietYeuCauCapPhat (
        maYeuCau varchar(10) NOT NULL,
        soThuTu smallint NOT NULL,
        maThuoc varchar(10) NULL,
        maVatTu varchar(10) NULL,
        soLuongYeuCau int NOT NULL,
        maLoCapPhat varchar(10) NULL,
        soLuongCapPhat int NOT NULL CONSTRAINT DF_CTYCCP_SoLuong DEFAULT 0,
        CONSTRAINT PK_ChiTietYeuCauCapPhat PRIMARY KEY (maYeuCau, soThuTu),
        CONSTRAINT FK_CTYCCP_YeuCau FOREIGN KEY (maYeuCau) REFERENCES dbo.YeuCauCapPhat(maYeuCau),
        CONSTRAINT FK_CTYCCP_Thuoc FOREIGN KEY (maThuoc) REFERENCES dbo.Thuoc(maThuoc),
        CONSTRAINT FK_CTYCCP_VatTu FOREIGN KEY (maVatTu) REFERENCES dbo.VatTu(maVatTu),
        CONSTRAINT FK_CTYCCP_LoHang FOREIGN KEY (maLoCapPhat) REFERENCES dbo.LoHang(maLo),
        CONSTRAINT CK_CTYCCP_ThuTu CHECK (soThuTu > 0),
        CONSTRAINT CK_CTYCCP_MotLoai CHECK ((maThuoc IS NOT NULL AND maVatTu IS NULL) OR (maThuoc IS NULL AND maVatTu IS NOT NULL)),
        CONSTRAINT CK_CTYCCP_SoLuong CHECK (soLuongYeuCau > 0 AND soLuongCapPhat >= 0 AND soLuongCapPhat <= soLuongYeuCau),
        CONSTRAINT CK_CTYCCP_CapPhat CHECK ((soLuongCapPhat = 0 AND maLoCapPhat IS NULL) OR (soLuongCapPhat > 0 AND maLoCapPhat IS NOT NULL))
    );
    COMMIT TRANSACTION;
    PRINT 'PASS: supply request header and lines are available.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
