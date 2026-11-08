SET NOCOUNT ON;
SET XACT_ABORT ON;

IF DB_NAME() IS NULL
   OR LEN(DB_NAME()) <> 15
   OR LOWER(DB_NAME()) NOT LIKE N'n08dev[_][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]'
    THROW 51070, 'STOP: only N08Dev_<8 hex> development databases accepted.', 1;

BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @lockResult INT;
    EXEC @lockResult = sys.sp_getapplock @Resource=N'N08_M1_SCHEMA_V6',
         @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=0;
    IF @lockResult < 0 THROW 51071, 'STOP: schema bootstrap is locked.', 1;

    IF OBJECT_ID(N'dbo.NhanVien', N'U') IS NULL
       OR OBJECT_ID(N'dbo.TaiKhoan', N'U') IS NULL
       OR OBJECT_ID(N'dbo.PhieuXuatKho', N'U') IS NULL
       OR OBJECT_ID(N'dbo.DanhMucCapCuu', N'U') IS NULL
       OR OBJECT_ID(N'dbo.KiemTraCapCuu', N'U') IS NULL
        THROW 51072, 'STOP: V1-V5 infrastructure schema is missing.', 1;

    -- V5 already creates this shared table with ten/giaTri/moTa. Keep that contract.
    IF OBJECT_ID(N'dbo.ThamSoHeThong', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.ThamSoHeThong (
            ten         VARCHAR(50)    NOT NULL CONSTRAINT PK_ThamSoHeThong PRIMARY KEY,
            giaTri      VARCHAR(100)   NOT NULL,
            moTa        NVARCHAR(300)  NULL
        );
    END;
    IF COL_LENGTH(N'dbo.ThamSoHeThong', N'capNhatLuc') IS NULL
        ALTER TABLE dbo.ThamSoHeThong ADD capNhatLuc DATETIME2(0) NOT NULL
            CONSTRAINT DF_ThamSoHeThong_capNhatLuc DEFAULT SYSUTCDATETIME();

    IF OBJECT_ID(N'dbo.YeuCauCapPhat', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.YeuCauCapPhat (
            maYeuCau         VARCHAR(10)    NOT NULL CONSTRAINT PK_YeuCauCapPhat PRIMARY KEY,
            thoiGianTao      DATETIME2(0)   NOT NULL CONSTRAINT DF_YeuCauCapPhat_thoiGianTao DEFAULT SYSUTCDATETIME(),
            maNhanVienYeuCau VARCHAR(10)    NOT NULL,
            maTaiKhoanTao    VARCHAR(10)    NOT NULL,
            lyDo             NVARCHAR(300)  NOT NULL,
            trangThai        VARCHAR(20)    NOT NULL CONSTRAINT DF_YeuCauCapPhat_trangThai DEFAULT 'CHO_DUYET',
            maTaiKhoanXuLy   VARCHAR(10)    NULL,
            thoiGianXuLy     DATETIME2(0)   NULL,
            ghiChuXuLy       NVARCHAR(500)  NULL,
            maPhieuXuat      VARCHAR(10)    NULL,
            CONSTRAINT FK_YeuCauCapPhat_NhanVien FOREIGN KEY (maNhanVienYeuCau) REFERENCES dbo.NhanVien(maNhanVien),
            CONSTRAINT FK_YeuCauCapPhat_TaiKhoanTao FOREIGN KEY (maTaiKhoanTao) REFERENCES dbo.TaiKhoan(maTaiKhoan),
            CONSTRAINT FK_YeuCauCapPhat_TaiKhoanXuLy FOREIGN KEY (maTaiKhoanXuLy) REFERENCES dbo.TaiKhoan(maTaiKhoan),
            CONSTRAINT FK_YeuCauCapPhat_PhieuXuat FOREIGN KEY (maPhieuXuat) REFERENCES dbo.PhieuXuatKho(maPhieuXuat),
            CONSTRAINT CK_YeuCauCapPhat_TrangThai CHECK (trangThai IN ('CHO_DUYET','DA_DUYET','TU_CHOI','DA_CAP_PHAT')),
            CONSTRAINT CK_YeuCauCapPhat_KhongTuXuLy CHECK (maTaiKhoanXuLy IS NULL OR maTaiKhoanXuLy <> maTaiKhoanTao),
            CONSTRAINT CK_YeuCauCapPhat_TrangThaiXuLy CHECK (
                (trangThai='CHO_DUYET' AND maTaiKhoanXuLy IS NULL AND thoiGianXuLy IS NULL AND maPhieuXuat IS NULL)
                OR (trangThai IN ('DA_DUYET','TU_CHOI') AND maTaiKhoanXuLy IS NOT NULL AND thoiGianXuLy IS NOT NULL AND maPhieuXuat IS NULL)
                OR (trangThai='DA_CAP_PHAT' AND maTaiKhoanXuLy IS NOT NULL AND thoiGianXuLy IS NOT NULL AND maPhieuXuat IS NOT NULL)
            ),
            CONSTRAINT CK_YeuCauCapPhat_TuChoi CHECK (trangThai <> 'TU_CHOI' OR (ghiChuXuLy IS NOT NULL AND LEN(LTRIM(RTRIM(ghiChuXuLy))) > 0))
        );
    END;

    IF OBJECT_ID(N'dbo.ChiTietYeuCauCapPhat', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.ChiTietYeuCauCapPhat (
            maYeuCau       VARCHAR(10)  NOT NULL,
            soThuTu        SMALLINT     NOT NULL,
            maThuoc        VARCHAR(10)  NULL,
            maVatTu        VARCHAR(10)  NULL,
            soLuongYeuCau  INT          NOT NULL,
            maLoCapPhat    VARCHAR(10)  NULL,
            soLuongCapPhat INT          NOT NULL CONSTRAINT DF_CT_YeuCauCapPhat_soLuongCapPhat DEFAULT 0,
            CONSTRAINT PK_ChiTietYeuCauCapPhat PRIMARY KEY (maYeuCau, soThuTu),
            CONSTRAINT FK_CT_YeuCauCapPhat_YeuCau FOREIGN KEY (maYeuCau) REFERENCES dbo.YeuCauCapPhat(maYeuCau),
            CONSTRAINT FK_CT_YeuCauCapPhat_Thuoc FOREIGN KEY (maThuoc) REFERENCES dbo.Thuoc(maThuoc),
            CONSTRAINT FK_CT_YeuCauCapPhat_VatTu FOREIGN KEY (maVatTu) REFERENCES dbo.VatTu(maVatTu),
            CONSTRAINT FK_CT_YeuCauCapPhat_Lo FOREIGN KEY (maLoCapPhat) REFERENCES dbo.LoHang(maLo),
            CONSTRAINT CK_CT_YeuCauCapPhat_Dong CHECK (soThuTu > 0 AND soLuongYeuCau > 0 AND ((maThuoc IS NOT NULL AND maVatTu IS NULL) OR (maThuoc IS NULL AND maVatTu IS NOT NULL))),
            CONSTRAINT CK_CT_YeuCauCapPhat_CapPhat CHECK (soLuongCapPhat >= 0 AND soLuongCapPhat <= soLuongYeuCau AND ((maLoCapPhat IS NULL AND soLuongCapPhat = 0) OR (maLoCapPhat IS NOT NULL AND soLuongCapPhat > 0)))
        );
    END;

    IF NOT EXISTS (SELECT 1 FROM dbo.ThamSoHeThong WHERE ten='SO_CA_TOI_DA_NGAY')
        INSERT INTO dbo.ThamSoHeThong(ten,giaTri,moTa) VALUES ('SO_CA_TOI_DA_NGAY','2',N'Số ca tối đa một nhân viên trong ngày');
    IF NOT EXISTS (SELECT 1 FROM dbo.ThamSoHeThong WHERE ten='SO_NGAY_CANH_BAO_HSD')
        INSERT INTO dbo.ThamSoHeThong(ten,giaTri,moTa) VALUES ('SO_NGAY_CANH_BAO_HSD','30',N'Ngưỡng cảnh báo hạn sử dụng kho');
    IF NOT EXISTS (SELECT 1 FROM dbo.ThamSoHeThong WHERE ten='NGUONG_HSD_CAP_CUU_NGAY')
        INSERT INTO dbo.ThamSoHeThong(ten,giaTri,moTa) VALUES ('NGUONG_HSD_CAP_CUU_NGAY','30',N'Số ngày hạn dùng tối thiểu khi kiểm tra hộp cấp cứu');
    IF NOT EXISTS (SELECT 1 FROM dbo.ThamSoHeThong WHERE ten='CHU_KY_KIEM_TRA_CAP_CUU_NGAY')
        INSERT INTO dbo.ThamSoHeThong(ten,giaTri,moTa) VALUES ('CHU_KY_KIEM_TRA_CAP_CUU_NGAY','7',N'Chu kỳ kiểm tra hộp cấp cứu');

    IF NOT EXISTS (SELECT 1 FROM dbo.DanhMucCapCuu WHERE maMuc='MCC0001')
        INSERT INTO dbo.DanhMucCapCuu(maMuc,tenMuc,donViTinh,soLuongToiThieu,soLuongHienCo,hanSuDungGanNhat,laDuLieuMau,ghiChu)
        VALUES ('MCC0001',N'Adrenaline 1mg/ml',N'ống',1,1,DATEADD(day,365,CONVERT(date,SYSUTCDATETIME())),1,N'Dữ liệu minh họa, không phải danh mục chuyên môn chính thức');
    IF NOT EXISTS (SELECT 1 FROM dbo.DanhMucCapCuu WHERE maMuc='MCC0002')
        INSERT INTO dbo.DanhMucCapCuu(maMuc,tenMuc,donViTinh,soLuongToiThieu,soLuongHienCo,hanSuDungGanNhat,laDuLieuMau,ghiChu)
        VALUES ('MCC0002',N'Gạc vô khuẩn mẫu',N'gói',2,2,DATEADD(day,365,CONVERT(date,SYSUTCDATETIME())),1,N'Dữ liệu minh họa, không phải danh mục chuyên môn chính thức');
    IF NOT EXISTS (SELECT 1 FROM dbo.DanhMucCapCuu WHERE maMuc='MCC0003')
        INSERT INTO dbo.DanhMucCapCuu(maMuc,tenMuc,donViTinh,soLuongToiThieu,soLuongHienCo,hanSuDungGanNhat,laDuLieuMau,ghiChu)
        VALUES ('MCC0003',N'Băng cuộn mẫu',N'cuộn',2,2,DATEADD(day,365,CONVERT(date,SYSUTCDATETIME())),1,N'Dữ liệu minh họa, không phải danh mục chuyên môn chính thức');
    IF NOT EXISTS (SELECT 1 FROM dbo.DanhMucCapCuu WHERE maMuc='MCC0004')
        INSERT INTO dbo.DanhMucCapCuu(maMuc,tenMuc,donViTinh,soLuongToiThieu,soLuongHienCo,hanSuDungGanNhat,laDuLieuMau,ghiChu)
        VALUES ('MCC0004',N'Găng tay mẫu',N'đôi',4,4,NULL,1,N'Dữ liệu minh họa, không phải danh mục chuyên môn chính thức');
    IF NOT EXISTS (SELECT 1 FROM dbo.DanhMucCapCuu WHERE maMuc='MCC0005')
        INSERT INTO dbo.DanhMucCapCuu(maMuc,tenMuc,donViTinh,soLuongToiThieu,soLuongHienCo,hanSuDungGanNhat,laDuLieuMau,ghiChu)
        VALUES ('MCC0005',N'Nước muối sinh lý mẫu',N'chai',2,2,DATEADD(day,365,CONVERT(date,SYSUTCDATETIME())),1,N'Dữ liệu minh họa, không phải danh mục chuyên môn chính thức');
    IF NOT EXISTS (SELECT 1 FROM dbo.DanhMucCapCuu WHERE maMuc='MCC0006')
        INSERT INTO dbo.DanhMucCapCuu(maMuc,tenMuc,donViTinh,soLuongToiThieu,soLuongHienCo,hanSuDungGanNhat,laDuLieuMau,ghiChu)
        VALUES ('MCC0006',N'Kéo y tế mẫu',N'cái',1,1,NULL,1,N'Dữ liệu minh họa, không phải danh mục chuyên môn chính thức');

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.YeuCauCapPhat') AND name=N'IX_YeuCauCapPhat_TaiKhoan')
        CREATE INDEX IX_YeuCauCapPhat_TaiKhoan ON dbo.YeuCauCapPhat(maTaiKhoanTao,thoiGianTao DESC);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.YeuCauCapPhat') AND name=N'IX_YeuCauCapPhat_TrangThai')
        CREATE INDEX IX_YeuCauCapPhat_TrangThai ON dbo.YeuCauCapPhat(trangThai,thoiGianTao DESC);

--     IF (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'dbo')) <> 41
--         THROW 51076, 'STOP: expected 41 dbo tables after V6.', 1;

    COMMIT TRANSACTION;
    PRINT 'PASS: V6 installed supply-request schema, emergency-kit parameters and six flagged demo items; 41 dbo tables total.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
