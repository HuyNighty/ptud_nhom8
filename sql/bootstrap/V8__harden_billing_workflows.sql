-- Additive migration. Backfilled historical names are the best available names at migration time.
SET XACT_ABORT ON;
IF DB_NAME() NOT LIKE 'N08Dev[_]%' THROW 51180, 'Requires isolated development database', 1;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.HoaDon','tenBenhNhanChot') IS NULL
    ALTER TABLE dbo.HoaDon ADD tenBenhNhanChot nvarchar(150) NULL,soDienThoaiChot varchar(20) NULL;
IF COL_LENGTH('dbo.ChiTietHoaDon','tenHangChot') IS NULL
    ALTER TABLE dbo.ChiTietHoaDon ADD tenHangChot nvarchar(200) NULL;
EXEC(N'UPDATE h SET tenBenhNhanChot=b.hoTen,soDienThoaiChot=b.soDienThoai FROM dbo.HoaDon h JOIN dbo.BenhNhan b ON b.maBenhNhan=h.maBenhNhan WHERE h.tenBenhNhanChot IS NULL');
EXEC(N'UPDATE c SET tenHangChot=COALESCE(d.tenDichVu,t.tenThuoc,v.tenVatTu) FROM dbo.ChiTietHoaDon c LEFT JOIN dbo.DichVu d ON d.maDichVu=c.maDichVu LEFT JOIN dbo.Thuoc t ON t.maThuoc=c.maThuoc LEFT JOIN dbo.VatTu v ON v.maVatTu=c.maVatTu WHERE c.tenHangChot IS NULL');
IF OBJECT_ID('dbo.LanThuHoaDon','U') IS NULL
CREATE TABLE dbo.LanThuHoaDon (
 maLanThu varchar(10) NOT NULL PRIMARY KEY,maHoaDon varchar(10) NOT NULL REFERENCES dbo.HoaDon(maHoaDon),
 maTaiKhoan varchar(10) NOT NULL REFERENCES dbo.TaiKhoan(maTaiKhoan),soTien decimal(18,0) NOT NULL CHECK(soTien>0),
 phuongThuc varchar(15) NOT NULL,maGiaoDich varchar(50) NULL,tienKhachDua decimal(18,0) NULL,
 trangThai varchar(15) NOT NULL DEFAULT 'CHO_XU_LY' CHECK(trangThai IN ('CHO_XU_LY','THANH_CONG','DA_HUY')),
 thoiGianTao datetime2(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
 maTaiKhoanXuLy varchar(10) NULL REFERENCES dbo.TaiKhoan(maTaiKhoan),lyDoXuLy nvarchar(300) NULL,
 CONSTRAINT CK_LanThu_PhuongThuc CHECK(phuongThuc IN ('TIEN_MAT','CHUYEN_KHOAN'))
);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.LanThuHoaDon') AND name='UX_LanThu_DangXuLy')
CREATE UNIQUE INDEX UX_LanThu_DangXuLy ON dbo.LanThuHoaDon(maHoaDon) WHERE trangThai='CHO_XU_LY';
IF OBJECT_ID('dbo.DieuChinhThanhToan','U') IS NULL
CREATE TABLE dbo.DieuChinhThanhToan (
 maThanhToan varchar(10) NOT NULL PRIMARY KEY REFERENCES dbo.ThanhToan(maThanhToan),
 loai varchar(15) NOT NULL CHECK(loai IN ('HUY_GHI_NHAN','HOAN_TIEN')),
 soTien decimal(18,0) NOT NULL CHECK(soTien>0),
 thoiGian datetime2(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
 maTaiKhoan varchar(10) NOT NULL REFERENCES dbo.TaiKhoan(maTaiKhoan),
 lyDo nvarchar(300) NOT NULL,thamChieu nvarchar(100) NOT NULL
);
-- Fill insert snapshots even for external importers using the existing schema.
EXEC(N'CREATE OR ALTER TRIGGER dbo.TR_HoaDon_ChotTen ON dbo.HoaDon AFTER INSERT AS BEGIN SET NOCOUNT ON;
UPDATE h SET tenBenhNhanChot=b.hoTen,soDienThoaiChot=b.soDienThoai FROM dbo.HoaDon h JOIN inserted i ON i.maHoaDon=h.maHoaDon JOIN dbo.BenhNhan b ON b.maBenhNhan=h.maBenhNhan; END');
EXEC(N'CREATE OR ALTER TRIGGER dbo.TR_ChiTietHoaDon_ChotTen ON dbo.ChiTietHoaDon AFTER INSERT AS BEGIN SET NOCOUNT ON;
UPDATE c SET tenHangChot=COALESCE(d.tenDichVu,t.tenThuoc,v.tenVatTu) FROM dbo.ChiTietHoaDon c JOIN inserted i ON i.maChiTiet=c.maChiTiet LEFT JOIN dbo.DichVu d ON d.maDichVu=c.maDichVu LEFT JOIN dbo.Thuoc t ON t.maThuoc=c.maThuoc LEFT JOIN dbo.VatTu v ON v.maVatTu=c.maVatTu;
UPDATE h SET trangThai=''DA_THANH_TOAN'' FROM dbo.HoaDon h WHERE h.trangThai=''CHUA_THANH_TOAN'' AND EXISTS(SELECT 1 FROM inserted i WHERE i.maHoaDon=h.maHoaDon) AND NOT EXISTS(SELECT 1 FROM dbo.ChiTietHoaDon c WHERE c.maHoaDon=h.maHoaDon AND c.donGiaApDung>0); END');
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.HoaDon') AND name='IX_HoaDon_NgayLap')
CREATE INDEX IX_HoaDon_NgayLap ON dbo.HoaDon(ngayLap DESC,maHoaDon) INCLUDE(maBenhNhan,maNhanVien,maPhieuKham,trangThai);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.ChiTietHoaDon') AND name='IX_CTHD_TongTien')
CREATE INDEX IX_CTHD_TongTien ON dbo.ChiTietHoaDon(maHoaDon) INCLUDE(soLuong,donGiaApDung);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.ThanhToan') AND name='IX_ThanhToan_TongThu')
CREATE INDEX IX_ThanhToan_TongThu ON dbo.ThanhToan(maHoaDon,trangThai) INCLUDE(soTien);
-- Repair zero-value existing documents without touching cancelled invoices or paid documents.
UPDATE h SET trangThai='DA_THANH_TOAN' FROM dbo.HoaDon h WHERE h.trangThai='CHUA_THANH_TOAN'
 AND EXISTS(SELECT 1 FROM dbo.ChiTietHoaDon c WHERE c.maHoaDon=h.maHoaDon)
 AND NOT EXISTS(SELECT 1 FROM dbo.ChiTietHoaDon c WHERE c.maHoaDon=h.maHoaDon AND c.donGiaApDung>0)
 AND NOT EXISTS(SELECT 1 FROM dbo.ThanhToan t WHERE t.maHoaDon=h.maHoaDon);
EXEC(N'CREATE OR ALTER TRIGGER dbo.TR_KeHoach_KhoaSauHoaDon ON dbo.KeHoachDieuTri AFTER INSERT AS
BEGIN
 SET NOCOUNT ON;
 IF EXISTS(SELECT 1 FROM inserted i JOIN dbo.PhieuKham p WITH (UPDLOCK,HOLDLOCK) ON p.maPhieuKham=i.maPhieuKham
           JOIN dbo.HoaDon h WITH (HOLDLOCK) ON h.maPhieuKham=p.maPhieuKham WHERE h.trangThai<>''DA_HUY'')
     THROW 51182, ''Visit already invoiced; create a new visit for additional treatment.'', 1;
END');
IF EXISTS(SELECT 1 FROM sys.check_constraints WHERE name='CK_ThanhToan_TrangThai')
ALTER TABLE dbo.ThanhToan DROP CONSTRAINT CK_ThanhToan_TrangThai;
ALTER TABLE dbo.ThanhToan ADD CONSTRAINT CK_ThanhToan_TrangThai CHECK(trangThai IN ('THANH_CONG','DA_HUY','DA_HOAN_TIEN'));
EXEC(N'CREATE OR ALTER VIEW dbo.DongTienHoaDon AS
SELECT maHoaDon,thoiGian,soTien FROM dbo.ThanhToan
UNION ALL
SELECT t.maHoaDon,t.thoiGianHuy,-t.soTien FROM dbo.ThanhToan t WHERE t.trangThai IN (''DA_HUY'',''DA_HOAN_TIEN'') AND t.thoiGianHuy IS NOT NULL');
COMMIT;
