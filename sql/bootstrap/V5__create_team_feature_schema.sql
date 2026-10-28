-- N08 M0-SCHEMA-01: team feature schema; 34 tables + 12 indexes + 1 FK.
-- Source: legacy git blob 6d76952b4a61f7391ecabb06ed0f1d502b646453
-- Adaptation: omitted obsolete NhanVien, TaiKhoan, NhatKyThaoTac and audit indexes.
-- Existing V1-V4 installs five trusted infrastructure tables. No data / seed / GRANT.
-- Run ONLY inside a fresh local N08Dev_<8 lower hex> database after V1-V4.
-- Uses one transaction and refuses to execute in any other database.
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() IS NULL OR LEN(DB_NAME()) <> 15 OR DB_NAME() COLLATE Latin1_General_100_BIN2 NOT LIKE N'N08Dev[_][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f][0-9a-f]'
    THROW 51070, 'STOP: only N08Dev_<8 hex> development databases accepted.', 1;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @lockResult INT;
    EXEC @lockResult = sys.sp_getapplock @Resource=N'N08_M0_SCHEMA_V5', @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=0;
    IF @lockResult < 0 THROW 51071, 'STOP: schema bootstrap is locked.', 1;
    IF OBJECT_ID(N'dbo.NhanVien', N'U') IS NULL THROW 51072, 'STOP: V1-V4 infrastructure schema missing.', 1;
    IF OBJECT_ID(N'dbo.TaiKhoan', N'U') IS NULL THROW 51072, 'STOP: V1-V4 infrastructure schema missing.', 1;
    IF OBJECT_ID(N'dbo.PhienDangNhap', N'U') IS NULL THROW 51072, 'STOP: V1-V4 infrastructure schema missing.', 1;
    IF OBJECT_ID(N'dbo.RefreshToken', N'U') IS NULL THROW 51072, 'STOP: V1-V4 infrastructure schema missing.', 1;
    IF OBJECT_ID(N'dbo.NhatKyThaoTac', N'U') IS NULL THROW 51072, 'STOP: V1-V4 infrastructure schema missing.', 1;
    IF COL_LENGTH('dbo.TaiKhoan','matKhauHash') IS NULL OR COL_LENGTH('dbo.NhatKyThaoTac','loaiTacNhan') IS NULL
        THROW 51073, 'STOP: incompatible old authentication/audit schema.', 1;
    IF EXISTS (SELECT 1 FROM sys.tables WHERE schema_id=SCHEMA_ID(N'dbo')
        AND name NOT IN ('NhanVien','TaiKhoan','PhienDangNhap','RefreshToken','NhatKyThaoTac'))
        THROW 51074, 'STOP: additional/partial schema exists; refusing to overwrite or rerun.', 1;
    -- Original DDL order is preserved, including FK after PhieuKham creation.
    EXEC sys.sp_executesql N'CREATE TABLE dbo.BoDemMa (
    loai        VARCHAR(10)  NOT NULL CONSTRAINT PK_BoDemMa PRIMARY KEY,
    tienTo      VARCHAR(10)  NOT NULL,
    doDai       INT          NOT NULL,
    giaTri      INT          NOT NULL DEFAULT 0
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.ThamSoHeThong (
    ten         VARCHAR(50)    NOT NULL CONSTRAINT PK_ThamSoHeThong PRIMARY KEY,
    giaTri      VARCHAR(100)   NOT NULL,
    moTa        NVARCHAR(300)  NULL
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.CaLamViec (
    maCa        VARCHAR(10)    NOT NULL CONSTRAINT PK_CaLamViec PRIMARY KEY,
    tenCa       NVARCHAR(50)   NOT NULL,
    gioBatDau   TIME(0)        NOT NULL,
    gioKetThuc  TIME(0)        NOT NULL,
    soBacSi     INT            NOT NULL DEFAULT 0,
    soYTa       INT            NOT NULL DEFAULT 0,
    soLeTan     INT            NOT NULL DEFAULT 0,
    CONSTRAINT CK_CaLamViec_Gio CHECK (gioKetThuc > gioBatDau),
    CONSTRAINT CK_CaLamViec_DinhMuc CHECK (soBacSi >= 0 AND soYTa >= 0 AND soLeTan >= 0)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.PhanCong (
    maPhanCong     VARCHAR(10)    NOT NULL CONSTRAINT PK_PhanCong PRIMARY KEY,
    maNhanVien     VARCHAR(10)    NOT NULL,
    maCa           VARCHAR(10)    NOT NULL,
    ngayLam        DATE           NOT NULL,
    nhiemVu        NVARCHAR(200)  NOT NULL,
    trangThai      VARCHAR(20)    NOT NULL DEFAULT ''CHO_HIEU_LUC'',
    lyDoHuy        NVARCHAR(300)  NULL,
    maPhanCongGoc  VARCHAR(10)    NULL,
    CONSTRAINT FK_PhanCong_NhanVien FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT FK_PhanCong_Ca FOREIGN KEY (maCa) REFERENCES dbo.CaLamViec(maCa),
    CONSTRAINT FK_PhanCong_Goc FOREIGN KEY (maPhanCongGoc) REFERENCES dbo.PhanCong(maPhanCong),
    CONSTRAINT CK_PhanCong_TrangThai CHECK (trangThai IN (''CHO_HIEU_LUC'',''DANG_THUC_HIEN'',''HOAN_THANH'',''DA_HUY'')),
    CONSTRAINT CK_PhanCong_Huy CHECK (trangThai <> ''DA_HUY'' OR lyDoHuy IS NOT NULL)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.BanGiaoCa (
    maBanGiao          VARCHAR(10)     NOT NULL CONSTRAINT PK_BanGiaoCa PRIMARY KEY,
    thoiGian           DATETIME2(0)    NOT NULL,
    noiDung            NVARCHAR(1000)  NOT NULL,
    trangThai          VARCHAR(20)     NOT NULL DEFAULT ''CHO_BAN_GIAO'',
    ngay               DATE            NOT NULL,
    maNguoiGiao        VARCHAR(10)     NOT NULL,
    maNguoiNhan        VARCHAR(10)     NOT NULL,
    maCaGiao           VARCHAR(10)     NOT NULL,
    maCaNhan           VARCHAR(10)     NOT NULL,
    coChenhLech        BIT             NOT NULL DEFAULT 0,
    maTaiKhoanTao      VARCHAR(10)     NOT NULL,
    maTaiKhoanXacNhan  VARCHAR(10)     NULL,
    thoiGianXacNhan    DATETIME2(0)    NULL,
    ghiChuXacNhan      NVARCHAR(500)   NULL,
    maTaiKhoanHuy      VARCHAR(10)     NULL,
    thoiGianHuy        DATETIME2(0)    NULL,
    lyDoHuy            NVARCHAR(300)   NULL,
    CONSTRAINT FK_BanGiao_Giao FOREIGN KEY (maNguoiGiao) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT FK_BanGiao_Nhan FOREIGN KEY (maNguoiNhan) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT FK_BanGiao_CaGiao FOREIGN KEY (maCaGiao) REFERENCES dbo.CaLamViec(maCa),
    CONSTRAINT FK_BanGiao_CaNhan FOREIGN KEY (maCaNhan) REFERENCES dbo.CaLamViec(maCa),
    CONSTRAINT FK_BanGiao_TKTao FOREIGN KEY (maTaiKhoanTao) REFERENCES dbo.TaiKhoan(maTaiKhoan),
    CONSTRAINT FK_BanGiao_TKXacNhan FOREIGN KEY (maTaiKhoanXacNhan) REFERENCES dbo.TaiKhoan(maTaiKhoan),
    CONSTRAINT FK_BanGiao_TKHuy FOREIGN KEY (maTaiKhoanHuy) REFERENCES dbo.TaiKhoan(maTaiKhoan),
    CONSTRAINT CK_BanGiao_TrangThai CHECK (trangThai IN (''CHO_BAN_GIAO'',''DA_BAN_GIAO'',''DA_HUY'')),
    CONSTRAINT CK_BanGiao_KhacNguoi CHECK (maNguoiGiao <> maNguoiNhan),
    CONSTRAINT CK_BanGiao_KhongTuXacNhan CHECK (maTaiKhoanXacNhan IS NULL OR maTaiKhoanXacNhan <> maTaiKhoanTao),
    CONSTRAINT CK_BanGiao_XacNhan CHECK (trangThai <> ''DA_BAN_GIAO'' OR (maTaiKhoanXacNhan IS NOT NULL AND thoiGianXacNhan IS NOT NULL)),
    CONSTRAINT CK_BanGiao_Huy CHECK (trangThai <> ''DA_HUY'' OR lyDoHuy IS NOT NULL)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.DanhGiaNhanSu (
    maDanhGia       VARCHAR(10)     NOT NULL CONSTRAINT PK_DanhGiaNhanSu PRIMARY KEY,
    maNhanVien      VARCHAR(10)     NOT NULL,
    maNguoiDanhGia  VARCHAR(10)     NOT NULL,
    kyDanhGia       VARCHAR(10)     NOT NULL,
    ngayDanhGia     DATE            NOT NULL,
    diem            DECIMAL(3,2)    NULL,
    nhanXet         NVARCHAR(1000)  NULL,
    trangThai       VARCHAR(10)     NOT NULL DEFAULT ''NHAP'',
    CONSTRAINT FK_DanhGia_NhanVien FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT FK_DanhGia_NguoiDanhGia FOREIGN KEY (maNguoiDanhGia) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT UQ_DanhGia_Ky UNIQUE (maNhanVien, kyDanhGia),
    CONSTRAINT CK_DanhGia_TrangThai CHECK (trangThai IN (''NHAP'',''DA_GUI'')),
    CONSTRAINT CK_DanhGia_KhacNguoi CHECK (maNhanVien <> maNguoiDanhGia),
    CONSTRAINT CK_DanhGia_Diem CHECK (diem IS NULL OR (diem >= 1 AND diem <= 5))
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.ChiTietDanhGia (
    maDanhGia   VARCHAR(10)    NOT NULL,
    thuTu       INT            NOT NULL,
    tieuChi     NVARCHAR(100)  NOT NULL,
    diem        INT            NOT NULL,
    CONSTRAINT PK_ChiTietDanhGia PRIMARY KEY (maDanhGia, thuTu),
    CONSTRAINT FK_CTDanhGia_DanhGia FOREIGN KEY (maDanhGia) REFERENCES dbo.DanhGiaNhanSu(maDanhGia),
    CONSTRAINT CK_CTDanhGia_Diem CHECK (diem BETWEEN 1 AND 5)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.BenhNhan (
    maBenhNhan     VARCHAR(10)    NOT NULL CONSTRAINT PK_BenhNhan PRIMARY KEY,
    hoTen          NVARCHAR(100)  NOT NULL,
    ngaySinh       DATE           NOT NULL,
    gioiTinh       VARCHAR(5)     NOT NULL,
    soDienThoai    VARCHAR(15)    NOT NULL,
    diaChi         NVARCHAR(255)  NULL,
    nguoiLienHe    NVARCHAR(200)  NULL,
    ngayTao        DATETIME2(0)   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    maTaiKhoanTao  VARCHAR(10)    NULL,
    CONSTRAINT FK_BenhNhan_TaiKhoan FOREIGN KEY (maTaiKhoanTao) REFERENCES dbo.TaiKhoan(maTaiKhoan),
    CONSTRAINT CK_BenhNhan_GioiTinh CHECK (gioiTinh IN (''NAM'',''NU'',''KHAC''))
);';
    EXEC sys.sp_executesql N'CREATE INDEX IX_BenhNhan_SDT ON dbo.BenhNhan(soDienThoai);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.HoSoBenhAn (
    maHoSo      VARCHAR(10)    NOT NULL CONSTRAINT PK_HoSoBenhAn PRIMARY KEY,
    maBenhNhan  VARCHAR(10)    NOT NULL CONSTRAINT UQ_HoSo_BenhNhan UNIQUE,
    ngayLap     DATE           NOT NULL,
    tienSuBenh  NVARCHAR(500)  NULL,
    diUng       NVARCHAR(300)  NULL,
    CONSTRAINT FK_HoSo_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES dbo.BenhNhan(maBenhNhan)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.DichVu (
    maDichVu      VARCHAR(10)    NOT NULL CONSTRAINT PK_DichVu PRIMARY KEY,
    tenDichVu     NVARCHAR(100)  NOT NULL,
    donGia        DECIMAL(18,0)  NOT NULL,
    moTa          NVARCHAR(300)  NULL,
    laCanLamSang  BIT            NOT NULL DEFAULT 0,
    dangApDung    BIT            NOT NULL DEFAULT 1,
    CONSTRAINT CK_DichVu_DonGia CHECK (donGia >= 0)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.KeHoachDieuTri (
    maKeHoach    VARCHAR(10)    NOT NULL CONSTRAINT PK_KeHoachDieuTri PRIMARY KEY,
    maHoSo       VARCHAR(10)    NOT NULL,
    maPhieuKham  VARCHAR(10)    NULL,
    ngayLap      DATE           NOT NULL,
    mucTieu      NVARCHAR(300)  NOT NULL,
    trangThai    VARCHAR(20)    NOT NULL DEFAULT ''DANG_THUC_HIEN'',
    maNhanVien   VARCHAR(10)    NOT NULL,
    lyDoHuy      NVARCHAR(300)  NULL,
    CONSTRAINT FK_KeHoach_HoSo FOREIGN KEY (maHoSo) REFERENCES dbo.HoSoBenhAn(maHoSo),
    CONSTRAINT FK_KeHoach_NhanVien FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT CK_KeHoach_TrangThai CHECK (trangThai IN (''DANG_THUC_HIEN'',''HOAN_THANH'',''DA_HUY'')),
    CONSTRAINT CK_KeHoach_Huy CHECK (trangThai <> ''DA_HUY'' OR lyDoHuy IS NOT NULL)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.LichHen (
    maLichHen      VARCHAR(10)    NOT NULL CONSTRAINT PK_LichHen PRIMARY KEY,
    maBenhNhan     VARCHAR(10)    NOT NULL,
    maNhanVien     VARCHAR(10)    NOT NULL,
    thoiGianHen    DATETIME2(0)   NOT NULL,
    lyDoHen        NVARCHAR(300)  NULL,
    trangThai      VARCHAR(20)    NOT NULL DEFAULT ''CHO_XAC_NHAN'',
    maLichHenGoc   VARCHAR(10)    NULL,
    maKeHoach      VARCHAR(10)    NULL,
    lyDoHuy        NVARCHAR(300)  NULL,
    maTaiKhoanTao  VARCHAR(10)    NULL,
    ngayTao        DATETIME2(0)   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_LichHen_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES dbo.BenhNhan(maBenhNhan),
    CONSTRAINT FK_LichHen_BacSi FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT FK_LichHen_Goc FOREIGN KEY (maLichHenGoc) REFERENCES dbo.LichHen(maLichHen),
    CONSTRAINT FK_LichHen_KeHoach FOREIGN KEY (maKeHoach) REFERENCES dbo.KeHoachDieuTri(maKeHoach),
    CONSTRAINT FK_LichHen_TaiKhoan FOREIGN KEY (maTaiKhoanTao) REFERENCES dbo.TaiKhoan(maTaiKhoan),
    CONSTRAINT CK_LichHen_TrangThai CHECK (trangThai IN (''CHO_XAC_NHAN'',''DA_XAC_NHAN'',''DA_DEN'',''HOAN_THANH'',''VANG_MAT'',''DA_HUY'',''DA_DOI_LICH'')),
    CONSTRAINT CK_LichHen_Huy CHECK (trangThai <> ''DA_HUY'' OR lyDoHuy IS NOT NULL)
);';
    EXEC sys.sp_executesql N'CREATE INDEX IX_LichHen_BacSi_ThoiGian ON dbo.LichHen(maNhanVien, thoiGianHen);';
    EXEC sys.sp_executesql N'CREATE INDEX IX_LichHen_ThoiGian ON dbo.LichHen(thoiGianHen);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.PhieuKham (
    maPhieuKham       VARCHAR(10)     NOT NULL CONSTRAINT PK_PhieuKham PRIMARY KEY,
    maBenhNhan        VARCHAR(10)     NOT NULL,
    maHoSo            VARCHAR(10)     NOT NULL,
    maNhanVien        VARCHAR(10)     NOT NULL,
    maLichHen         VARCHAR(10)     NULL,
    thoiGianTiepNhan  DATETIME2(0)    NOT NULL,
    trieuChung        NVARCHAR(500)   NULL,
    chanDoan          NVARCHAR(1000)  NULL,
    ketLuan           NVARCHAR(1000)  NULL,
    trangThai         VARCHAR(20)     NOT NULL DEFAULT ''CHO_KHAM'',
    lyDoHuy           NVARCHAR(300)   NULL,
    CONSTRAINT FK_PhieuKham_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES dbo.BenhNhan(maBenhNhan),
    CONSTRAINT FK_PhieuKham_HoSo FOREIGN KEY (maHoSo) REFERENCES dbo.HoSoBenhAn(maHoSo),
    CONSTRAINT FK_PhieuKham_BacSi FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT FK_PhieuKham_LichHen FOREIGN KEY (maLichHen) REFERENCES dbo.LichHen(maLichHen),
    CONSTRAINT CK_PhieuKham_TrangThai CHECK (trangThai IN (''CHO_KHAM'',''DANG_KHAM'',''DA_KET_LUAN'',''DA_HUY'')),
    CONSTRAINT CK_PhieuKham_KetLuan CHECK (trangThai <> ''DA_KET_LUAN'' OR chanDoan IS NOT NULL),
    CONSTRAINT CK_PhieuKham_Huy CHECK (trangThai <> ''DA_HUY'' OR lyDoHuy IS NOT NULL)
);';
    EXEC sys.sp_executesql N'CREATE INDEX IX_PhieuKham_ThoiGian ON dbo.PhieuKham(thoiGianTiepNhan);';
    EXEC sys.sp_executesql N'ALTER TABLE dbo.KeHoachDieuTri ADD CONSTRAINT FK_KeHoach_PhieuKham FOREIGN KEY (maPhieuKham) REFERENCES dbo.PhieuKham(maPhieuKham);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.ChiTietDieuTri (
    maChiTiet           INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ChiTietDieuTri PRIMARY KEY,
    maKeHoach           VARCHAR(10)    NOT NULL,
    maDichVu            VARCHAR(10)    NOT NULL,
    viTriRang           NVARCHAR(50)   NOT NULL,
    ngayDuKien          DATE           NOT NULL,
    ngayThucHien        DATE           NULL,
    ketQua              NVARCHAR(500)  NULL,
    trangThai           VARCHAR(20)    NOT NULL DEFAULT ''CHUA_THUC_HIEN'',
    maNhanVienThucHien  VARCHAR(10)    NULL,
    ghiChu              NVARCHAR(300)  NULL,
    CONSTRAINT FK_CTDieuTri_KeHoach FOREIGN KEY (maKeHoach) REFERENCES dbo.KeHoachDieuTri(maKeHoach),
    CONSTRAINT FK_CTDieuTri_DichVu FOREIGN KEY (maDichVu) REFERENCES dbo.DichVu(maDichVu),
    CONSTRAINT FK_CTDieuTri_BacSi FOREIGN KEY (maNhanVienThucHien) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT CK_CTDieuTri_TrangThai CHECK (trangThai IN (''CHUA_THUC_HIEN'',''DA_THUC_HIEN'',''DA_HUY'')),
    CONSTRAINT CK_CTDieuTri_ThucHien CHECK (trangThai <> ''DA_THUC_HIEN'' OR (ngayThucHien IS NOT NULL AND ketQua IS NOT NULL AND maNhanVienThucHien IS NOT NULL))
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.ChiDinhCanLamSang (
    maChiDinh        VARCHAR(10)    NOT NULL CONSTRAINT PK_ChiDinhCanLamSang PRIMARY KEY,
    maPhieuKham      VARCHAR(10)    NOT NULL,
    maDichVu         VARCHAR(10)    NOT NULL,
    yeuCau           NVARCHAR(300)  NOT NULL,
    ketQua           NVARCHAR(1000) NULL,
    maNhanVien       VARCHAR(10)    NOT NULL,
    thoiGianChiDinh  DATETIME2(0)   NOT NULL,
    thoiGianKetQua   DATETIME2(0)   NULL,
    trangThai        VARCHAR(20)    NOT NULL DEFAULT ''CHO_THUC_HIEN'',
    CONSTRAINT FK_CLS_PhieuKham FOREIGN KEY (maPhieuKham) REFERENCES dbo.PhieuKham(maPhieuKham),
    CONSTRAINT FK_CLS_DichVu FOREIGN KEY (maDichVu) REFERENCES dbo.DichVu(maDichVu),
    CONSTRAINT FK_CLS_BacSi FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT CK_CLS_TrangThai CHECK (trangThai IN (''CHO_THUC_HIEN'',''DA_CO_KET_QUA'')),
    CONSTRAINT CK_CLS_KetQua CHECK (trangThai <> ''DA_CO_KET_QUA'' OR ketQua IS NOT NULL)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.Thuoc (
    maThuoc          VARCHAR(10)    NOT NULL CONSTRAINT PK_Thuoc PRIMARY KEY,
    tenThuoc         NVARCHAR(150)  NOT NULL,
    donViTinh        NVARCHAR(30)   NOT NULL,
    kiemSoatDacBiet  BIT            NOT NULL DEFAULT 0,
    hamLuong         NVARCHAR(50)   NULL,
    hoatChat         NVARCHAR(150)  NULL,
    dinhMucToiThieu  INT            NOT NULL DEFAULT 0,
    daDatHang        BIT            NOT NULL DEFAULT 0,
    CONSTRAINT CK_Thuoc_DinhMuc CHECK (dinhMucToiThieu >= 0)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.VatTu (
    maVatTu          VARCHAR(10)    NOT NULL CONSTRAINT PK_VatTu PRIMARY KEY,
    tenVatTu         NVARCHAR(150)  NOT NULL,
    donViTinh        NVARCHAR(30)   NOT NULL,
    moTa             NVARCHAR(300)  NULL,
    dinhMucToiThieu  INT            NOT NULL DEFAULT 0,
    daDatHang        BIT            NOT NULL DEFAULT 0,
    CONSTRAINT CK_VatTu_DinhMuc CHECK (dinhMucToiThieu >= 0)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.DonThuoc (
    maDonThuoc   VARCHAR(10)    NOT NULL CONSTRAINT PK_DonThuoc PRIMARY KEY,
    maPhieuKham  VARCHAR(10)    NOT NULL CONSTRAINT UQ_DonThuoc_PhieuKham UNIQUE,
    maBenhNhan   VARCHAR(10)    NOT NULL,
    maNhanVien   VARCHAR(10)    NOT NULL,
    ngayKe       DATE           NOT NULL,
    loiDan       NVARCHAR(500)  NULL,
    CONSTRAINT FK_DonThuoc_PhieuKham FOREIGN KEY (maPhieuKham) REFERENCES dbo.PhieuKham(maPhieuKham),
    CONSTRAINT FK_DonThuoc_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES dbo.BenhNhan(maBenhNhan),
    CONSTRAINT FK_DonThuoc_BacSi FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien(maNhanVien)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.ChiTietDonThuoc (
    maDonThuoc  VARCHAR(10)    NOT NULL,
    maThuoc     VARCHAR(10)    NOT NULL,
    soLuong     INT            NOT NULL,
    lieuDung    NVARCHAR(100)  NOT NULL,
    cachDung    NVARCHAR(200)  NOT NULL,
    soNgayDung  INT            NOT NULL,
    CONSTRAINT PK_ChiTietDonThuoc PRIMARY KEY (maDonThuoc, maThuoc),
    CONSTRAINT FK_CTDonThuoc_Don FOREIGN KEY (maDonThuoc) REFERENCES dbo.DonThuoc(maDonThuoc),
    CONSTRAINT FK_CTDonThuoc_Thuoc FOREIGN KEY (maThuoc) REFERENCES dbo.Thuoc(maThuoc),
    CONSTRAINT CK_CTDonThuoc_SoLuong CHECK (soLuong > 0 AND soNgayDung > 0)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.NhaCungCap (
    maNhaCungCap   VARCHAR(10)    NOT NULL CONSTRAINT PK_NhaCungCap PRIMARY KEY,
    tenNhaCungCap  NVARCHAR(150)  NOT NULL,
    diaChi         NVARCHAR(255)  NULL,
    soDienThoai    VARCHAR(20)    NOT NULL,
    email          VARCHAR(100)   NULL,
    trangThai      VARCHAR(20)    NOT NULL DEFAULT ''DANG_HOP_TAC'',
    CONSTRAINT CK_NCC_TrangThai CHECK (trangThai IN (''DANG_HOP_TAC'',''NGUNG_HOP_TAC''))
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.LoHang (
    maLo          VARCHAR(10)  NOT NULL CONSTRAINT PK_LoHang PRIMARY KEY,
    soLo          VARCHAR(30)  NOT NULL,
    ngaySanXuat   DATE         NULL,
    hanSuDung     DATE         NULL,
    maThuoc       VARCHAR(10)  NULL,
    maVatTu       VARCHAR(10)  NULL,
    maNhaCungCap  VARCHAR(10)  NOT NULL,
    soLuongTon    INT          NOT NULL DEFAULT 0,
    CONSTRAINT FK_LoHang_Thuoc FOREIGN KEY (maThuoc) REFERENCES dbo.Thuoc(maThuoc),
    CONSTRAINT FK_LoHang_VatTu FOREIGN KEY (maVatTu) REFERENCES dbo.VatTu(maVatTu),
    CONSTRAINT FK_LoHang_NCC FOREIGN KEY (maNhaCungCap) REFERENCES dbo.NhaCungCap(maNhaCungCap),
    CONSTRAINT CK_LoHang_MotLoai CHECK ((maThuoc IS NOT NULL AND maVatTu IS NULL) OR (maThuoc IS NULL AND maVatTu IS NOT NULL)),
    CONSTRAINT CK_LoHang_Ton CHECK (soLuongTon >= 0),
    CONSTRAINT CK_LoHang_Han CHECK (hanSuDung IS NULL OR ngaySanXuat IS NULL OR hanSuDung > ngaySanXuat)
);';
    EXEC sys.sp_executesql N'CREATE INDEX IX_LoHang_Thuoc ON dbo.LoHang(maThuoc);';
    EXEC sys.sp_executesql N'CREATE INDEX IX_LoHang_VatTu ON dbo.LoHang(maVatTu);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.PhieuXuatKho (
    maPhieuXuat  VARCHAR(10)    NOT NULL CONSTRAINT PK_PhieuXuatKho PRIMARY KEY,
    ngayXuat     DATETIME2(0)   NOT NULL,
    boPhanNhan   NVARCHAR(100)  NOT NULL,
    nguoiNhan    NVARCHAR(100)  NULL,
    lyDo         NVARCHAR(300)  NULL,
    trangThai    VARCHAR(20)    NOT NULL DEFAULT ''HOAN_TAT'',
    maNhanVien   VARCHAR(10)    NOT NULL,
    CONSTRAINT FK_PhieuXuat_NhanVien FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT CK_PhieuXuat_TrangThai CHECK (trangThai IN (''HOAN_TAT''))
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.ChiTietPhieuXuat (
    maPhieuXuat  VARCHAR(10)  NOT NULL,
    maLo         VARCHAR(10)  NOT NULL,
    soLuong      INT          NOT NULL,
    CONSTRAINT PK_ChiTietPhieuXuat PRIMARY KEY (maPhieuXuat, maLo),
    CONSTRAINT FK_CTPX_Phieu FOREIGN KEY (maPhieuXuat) REFERENCES dbo.PhieuXuatKho(maPhieuXuat),
    CONSTRAINT FK_CTPX_Lo FOREIGN KEY (maLo) REFERENCES dbo.LoHang(maLo),
    CONSTRAINT CK_CTPX_SoLuong CHECK (soLuong > 0)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.PhieuNhapKho (
    maPhieuNhap     VARCHAR(10)    NOT NULL CONSTRAINT PK_PhieuNhapKho PRIMARY KEY,
    ngayNhap        DATETIME2(0)   NOT NULL,
    trangThai       VARCHAR(20)    NOT NULL DEFAULT ''HOAN_TAT'',
    loaiNhap        VARCHAR(10)    NOT NULL DEFAULT ''NHAP_NCC'',
    maNhaCungCap    VARCHAR(10)    NULL,
    maPhieuXuatGoc  VARCHAR(10)    NULL,
    maNhanVien      VARCHAR(10)    NOT NULL,
    ghiChu          NVARCHAR(300)  NULL,
    CONSTRAINT FK_PhieuNhap_NCC FOREIGN KEY (maNhaCungCap) REFERENCES dbo.NhaCungCap(maNhaCungCap),
    CONSTRAINT FK_PhieuNhap_XuatGoc FOREIGN KEY (maPhieuXuatGoc) REFERENCES dbo.PhieuXuatKho(maPhieuXuat),
    CONSTRAINT FK_PhieuNhap_NhanVien FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT CK_PhieuNhap_TrangThai CHECK (trangThai IN (''HOAN_TAT'')),
    CONSTRAINT CK_PhieuNhap_Loai CHECK ((loaiNhap = ''NHAP_NCC'' AND maNhaCungCap IS NOT NULL AND maPhieuXuatGoc IS NULL) OR (loaiNhap = ''HOAN_TRA'' AND maPhieuXuatGoc IS NOT NULL))
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.ChiTietPhieuNhap (
    maPhieuNhap  VARCHAR(10)    NOT NULL,
    maLo         VARCHAR(10)    NOT NULL,
    soLuong      INT            NOT NULL,
    donGiaNhap   DECIMAL(18,0)  NOT NULL DEFAULT 0,
    CONSTRAINT PK_ChiTietPhieuNhap PRIMARY KEY (maPhieuNhap, maLo),
    CONSTRAINT FK_CTPN_Phieu FOREIGN KEY (maPhieuNhap) REFERENCES dbo.PhieuNhapKho(maPhieuNhap),
    CONSTRAINT FK_CTPN_Lo FOREIGN KEY (maLo) REFERENCES dbo.LoHang(maLo),
    CONSTRAINT CK_CTPN_SoLuong CHECK (soLuong > 0 AND donGiaNhap >= 0)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.PhieuDieuChinhKho (
    maPhieu          VARCHAR(10)    NOT NULL CONSTRAINT PK_PhieuDieuChinhKho PRIMARY KEY,
    loai             VARCHAR(10)    NOT NULL,
    maTaiKhoanTao    VARCHAR(10)    NOT NULL,
    thoiGianTao      DATETIME2(0)   NOT NULL,
    lyDo             NVARCHAR(500)  NOT NULL,
    trangThai        VARCHAR(10)    NOT NULL DEFAULT ''CHO_DUYET'',
    maTaiKhoanDuyet  VARCHAR(10)    NULL,
    thoiGianDuyet    DATETIME2(0)   NULL,
    ghiChuDuyet      NVARCHAR(500)  NULL,
    CONSTRAINT FK_PDC_TKTao FOREIGN KEY (maTaiKhoanTao) REFERENCES dbo.TaiKhoan(maTaiKhoan),
    CONSTRAINT FK_PDC_TKDuyet FOREIGN KEY (maTaiKhoanDuyet) REFERENCES dbo.TaiKhoan(maTaiKhoan),
    CONSTRAINT CK_PDC_Loai CHECK (loai IN (''KIEM_KE'',''HU_HONG'',''HET_HAN'')),
    CONSTRAINT CK_PDC_TrangThai CHECK (trangThai IN (''CHO_DUYET'',''DA_DUYET'',''TU_CHOI'')),
    CONSTRAINT CK_PDC_KhongTuDuyet CHECK (maTaiKhoanDuyet IS NULL OR maTaiKhoanDuyet <> maTaiKhoanTao),
    CONSTRAINT CK_PDC_TuChoi CHECK (trangThai <> ''TU_CHOI'' OR ghiChuDuyet IS NOT NULL),
    CONSTRAINT CK_PDC_DaXuLy CHECK (trangThai = ''CHO_DUYET'' OR maTaiKhoanDuyet IS NOT NULL)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.ChiTietDieuChinhKho (
    maPhieu          VARCHAR(10)  NOT NULL,
    maLo             VARCHAR(10)  NOT NULL,
    soLuongHeThong   INT          NOT NULL,
    soLuongThucTe    INT          NOT NULL,
    CONSTRAINT PK_ChiTietDieuChinhKho PRIMARY KEY (maPhieu, maLo),
    CONSTRAINT FK_CTDC_Phieu FOREIGN KEY (maPhieu) REFERENCES dbo.PhieuDieuChinhKho(maPhieu),
    CONSTRAINT FK_CTDC_Lo FOREIGN KEY (maLo) REFERENCES dbo.LoHang(maLo),
    CONSTRAINT CK_CTDC_SoLuong CHECK (soLuongHeThong >= 0 AND soLuongThucTe >= 0 AND soLuongThucTe <> soLuongHeThong)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.DanhMucCapCuu (
    maMuc              VARCHAR(10)    NOT NULL CONSTRAINT PK_DanhMucCapCuu PRIMARY KEY,
    tenMuc             NVARCHAR(150)  NOT NULL,
    donViTinh          NVARCHAR(30)   NOT NULL,
    soLuongToiThieu    INT            NOT NULL,
    soLuongHienCo      INT            NOT NULL DEFAULT 0,
    hanSuDungGanNhat   DATE           NULL,
    maThuoc            VARCHAR(10)    NULL,
    maVatTu            VARCHAR(10)    NULL,
    dangApDung         BIT            NOT NULL DEFAULT 1,
    laDuLieuMau        BIT            NOT NULL DEFAULT 0,
    ghiChu             NVARCHAR(300)  NULL,
    CONSTRAINT FK_DMCC_Thuoc FOREIGN KEY (maThuoc) REFERENCES dbo.Thuoc(maThuoc),
    CONSTRAINT FK_DMCC_VatTu FOREIGN KEY (maVatTu) REFERENCES dbo.VatTu(maVatTu),
    CONSTRAINT CK_DMCC_SoLuong CHECK (soLuongToiThieu > 0 AND soLuongHienCo >= 0),
    CONSTRAINT CK_DMCC_LienKet CHECK (maThuoc IS NULL OR maVatTu IS NULL)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.KiemTraCapCuu (
    maKiemTra   VARCHAR(10)    NOT NULL CONSTRAINT PK_KiemTraCapCuu PRIMARY KEY,
    thoiGian    DATETIME2(0)   NOT NULL,
    maNhanVien  VARCHAR(10)    NOT NULL,
    ketQua      VARCHAR(15)    NOT NULL,
    ghiChu      NVARCHAR(500)  NULL,
    CONSTRAINT FK_KTCC_NhanVien FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT CK_KTCC_KetQua CHECK (ketQua IN (''DAT'',''CAN_BO_SUNG''))
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.ChiTietKiemTraCapCuu (
    maKiemTra      VARCHAR(10)  NOT NULL,
    maMuc          VARCHAR(10)  NOT NULL,
    soLuongThucTe  INT          NOT NULL,
    hanSuDung      DATE         NULL,
    dat            BIT          NOT NULL,
    CONSTRAINT PK_ChiTietKiemTraCapCuu PRIMARY KEY (maKiemTra, maMuc),
    CONSTRAINT FK_CTKTCC_KiemTra FOREIGN KEY (maKiemTra) REFERENCES dbo.KiemTraCapCuu(maKiemTra),
    CONSTRAINT FK_CTKTCC_Muc FOREIGN KEY (maMuc) REFERENCES dbo.DanhMucCapCuu(maMuc),
    CONSTRAINT CK_CTKTCC_SoLuong CHECK (soLuongThucTe >= 0)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.HoaDon (
    maHoaDon     VARCHAR(10)   NOT NULL CONSTRAINT PK_HoaDon PRIMARY KEY,
    maNhanVien   VARCHAR(10)   NOT NULL,
    maBenhNhan   VARCHAR(10)   NOT NULL,
    maPhieuKham  VARCHAR(10)   NULL,
    ngayLap      DATETIME2(0)  NOT NULL,
    trangThai    VARCHAR(20)   NOT NULL DEFAULT ''CHUA_THANH_TOAN'',
    CONSTRAINT FK_HoaDon_NhanVien FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT FK_HoaDon_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES dbo.BenhNhan(maBenhNhan),
    CONSTRAINT FK_HoaDon_PhieuKham FOREIGN KEY (maPhieuKham) REFERENCES dbo.PhieuKham(maPhieuKham),
    CONSTRAINT CK_HoaDon_TrangThai CHECK (trangThai IN (''CHUA_THANH_TOAN'',''THANH_TOAN_MOT_PHAN'',''DA_THANH_TOAN'',''DA_HUY''))
);';
    EXEC sys.sp_executesql N'CREATE INDEX IX_HoaDon_BenhNhan ON dbo.HoaDon(maBenhNhan);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.ChiTietHoaDon (
    maChiTiet     INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ChiTietHoaDon PRIMARY KEY,
    maHoaDon      VARCHAR(10)    NOT NULL,
    maDichVu      VARCHAR(10)    NULL,
    maThuoc       VARCHAR(10)    NULL,
    maVatTu       VARCHAR(10)    NULL,
    soLuong       INT            NOT NULL,
    donGiaApDung  DECIMAL(18,0)  NOT NULL,
    CONSTRAINT FK_CTHD_HoaDon FOREIGN KEY (maHoaDon) REFERENCES dbo.HoaDon(maHoaDon),
    CONSTRAINT FK_CTHD_DichVu FOREIGN KEY (maDichVu) REFERENCES dbo.DichVu(maDichVu),
    CONSTRAINT FK_CTHD_Thuoc FOREIGN KEY (maThuoc) REFERENCES dbo.Thuoc(maThuoc),
    CONSTRAINT FK_CTHD_VatTu FOREIGN KEY (maVatTu) REFERENCES dbo.VatTu(maVatTu),
    CONSTRAINT CK_CTHD_MotLoai CHECK ((maDichVu IS NOT NULL AND maThuoc IS NULL AND maVatTu IS NULL) OR (maDichVu IS NULL AND maThuoc IS NOT NULL AND maVatTu IS NULL) OR (maDichVu IS NULL AND maThuoc IS NULL AND maVatTu IS NOT NULL)),
    CONSTRAINT CK_CTHD_SoLuong CHECK (soLuong > 0 AND donGiaApDung >= 0)
);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.ThanhToan (
    maThanhToan    VARCHAR(10)    NOT NULL CONSTRAINT PK_ThanhToan PRIMARY KEY,
    maHoaDon       VARCHAR(10)    NOT NULL,
    thoiGian       DATETIME2(0)   NOT NULL,
    soTien         DECIMAL(18,0)  NOT NULL,
    phuongThuc     VARCHAR(15)    NOT NULL,
    maGiaoDich     VARCHAR(50)    NULL,
    tienKhachDua   DECIMAL(18,0)  NULL,
    trangThai      VARCHAR(15)    NOT NULL DEFAULT ''THANH_CONG'',
    maNhanVien     VARCHAR(10)    NOT NULL,
    maTaiKhoanHuy  VARCHAR(10)    NULL,
    thoiGianHuy    DATETIME2(0)   NULL,
    lyDoHuy        NVARCHAR(300)  NULL,
    CONSTRAINT FK_ThanhToan_HoaDon FOREIGN KEY (maHoaDon) REFERENCES dbo.HoaDon(maHoaDon),
    CONSTRAINT FK_ThanhToan_NhanVien FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT FK_ThanhToan_TKHuy FOREIGN KEY (maTaiKhoanHuy) REFERENCES dbo.TaiKhoan(maTaiKhoan),
    CONSTRAINT CK_ThanhToan_SoTien CHECK (soTien > 0),
    CONSTRAINT CK_ThanhToan_PhuongThuc CHECK (phuongThuc IN (''TIEN_MAT'',''CHUYEN_KHOAN'',''THE_POS'')),
    CONSTRAINT CK_ThanhToan_MaGD CHECK (phuongThuc = ''TIEN_MAT'' OR maGiaoDich IS NOT NULL),
    CONSTRAINT CK_ThanhToan_TienMat CHECK (phuongThuc <> ''TIEN_MAT'' OR tienKhachDua >= soTien),
    CONSTRAINT CK_ThanhToan_TrangThai CHECK (trangThai IN (''THANH_CONG'',''DA_HUY'')),
    CONSTRAINT CK_ThanhToan_Huy CHECK (trangThai <> ''DA_HUY'' OR (lyDoHuy IS NOT NULL AND maTaiKhoanHuy IS NOT NULL))
);';
    EXEC sys.sp_executesql N'CREATE INDEX IX_ThanhToan_HoaDon ON dbo.ThanhToan(maHoaDon);';
    EXEC sys.sp_executesql N'CREATE TABLE dbo.YeuCauHuyHoaDon (
    maYeuCau         VARCHAR(10)    NOT NULL CONSTRAINT PK_YeuCauHuyHoaDon PRIMARY KEY,
    maHoaDon         VARCHAR(10)    NOT NULL,
    maTaiKhoanTao    VARCHAR(10)    NOT NULL,
    thoiGianTao      DATETIME2(0)   NOT NULL,
    lyDo             NVARCHAR(500)  NOT NULL,
    trangThai        VARCHAR(10)    NOT NULL DEFAULT ''CHO_DUYET'',
    maTaiKhoanDuyet  VARCHAR(10)    NULL,
    thoiGianDuyet    DATETIME2(0)   NULL,
    lyDoDuyet        NVARCHAR(500)  NULL,
    CONSTRAINT FK_YCHuy_HoaDon FOREIGN KEY (maHoaDon) REFERENCES dbo.HoaDon(maHoaDon),
    CONSTRAINT FK_YCHuy_TKTao FOREIGN KEY (maTaiKhoanTao) REFERENCES dbo.TaiKhoan(maTaiKhoan),
    CONSTRAINT FK_YCHuy_TKDuyet FOREIGN KEY (maTaiKhoanDuyet) REFERENCES dbo.TaiKhoan(maTaiKhoan),
    CONSTRAINT CK_YCHuy_TrangThai CHECK (trangThai IN (''CHO_DUYET'',''DA_DUYET'',''TU_CHOI'')),
    CONSTRAINT CK_YCHuy_KhongTuDuyet CHECK (maTaiKhoanDuyet IS NULL OR maTaiKhoanDuyet <> maTaiKhoanTao),
    CONSTRAINT CK_YCHuy_DaXuLy CHECK (trangThai = ''CHO_DUYET'' OR (maTaiKhoanDuyet IS NOT NULL AND lyDoDuyet IS NOT NULL))
);';
    EXEC sys.sp_executesql N'CREATE UNIQUE INDEX UX_PhanCong_HieuLuc ON dbo.PhanCong(maNhanVien, ngayLam, maCa) WHERE trangThai <> ''DA_HUY'';';
    EXEC sys.sp_executesql N'CREATE UNIQUE INDEX UX_YCHuy_ChoDuyet ON dbo.YeuCauHuyHoaDon(maHoaDon) WHERE trangThai = ''CHO_DUYET'';';
    EXEC sys.sp_executesql N'CREATE UNIQUE INDEX UX_HoaDon_PhieuKham ON dbo.HoaDon(maPhieuKham) WHERE maPhieuKham IS NOT NULL AND trangThai <> ''DA_HUY'';';
    EXEC sys.sp_executesql N'CREATE UNIQUE INDEX UX_LichHen_KhungGio ON dbo.LichHen(maNhanVien, thoiGianHen) WHERE trangThai IN (''CHO_XAC_NHAN'',''DA_XAC_NHAN'',''DA_DEN'');';
    IF (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'dbo')) <> 39
        THROW 51075, 'STOP: unexpected number of final tables.', 1;
    COMMIT TRANSACTION;
    PRINT 'PASS: 34 business tables added; 39 dbo tables total. No demo patient records seeded.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
