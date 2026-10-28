-- DEV-ONLY: thêm 2 nhân viên kho để chạy thử không gian Kho trên SQL Server local.
-- Không phải migration Flyway, không commit vào db/migration. Chạy được nhiều lần.
-- Tên dùng NCHAR() để không phụ thuộc bảng mã khi chạy qua sqlcmd/PowerShell.
USE N08Dental;
DECLARE @mot NVARCHAR(100) = N'Kho Th' + NCHAR(7917) + N' M' + NCHAR(7897) + N't';
DECLARE @hai NVARCHAR(100) = N'Kho Th' + NCHAR(7917) + N' Hai';
IF NOT EXISTS (SELECT 1 FROM dbo.NhanVien WHERE maNhanVien = 'NV9001')
    INSERT INTO dbo.NhanVien (maNhanVien, hoTen, soDienThoai, chucDanh, trangThai)
    VALUES ('NV9001', @mot, '0900009001', 'KHO', 'DANG_LAM_VIEC');
ELSE
    UPDATE dbo.NhanVien SET hoTen = @mot WHERE maNhanVien = 'NV9001';
IF NOT EXISTS (SELECT 1 FROM dbo.NhanVien WHERE maNhanVien = 'NV9002')
    INSERT INTO dbo.NhanVien (maNhanVien, hoTen, soDienThoai, chucDanh, trangThai)
    VALUES ('NV9002', @hai, '0900009002', 'KHO', 'DANG_LAM_VIEC');
ELSE
    UPDATE dbo.NhanVien SET hoTen = @hai WHERE maNhanVien = 'NV9002';
SELECT maNhanVien, hoTen, chucDanh FROM dbo.NhanVien;
