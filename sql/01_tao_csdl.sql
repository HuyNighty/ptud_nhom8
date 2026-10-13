/* =====================================================================
   01_tao_csdl.sql — Tạo database N08_NhaKhoa trên Microsoft SQL Server
   Chạy bằng SQL Server Management Studio (SSMS) hoặc sqlcmd với quyền
   tạo database (VD: sa hoặc tài khoản thuộc dbcreator).

   CẢNH BÁO: script XÓA database N08_NhaKhoa nếu đã tồn tại.
   Collation Vietnamese_CI_AS giúp sắp xếp/tìm kiếm tiếng Việt không phân
   biệt hoa thường.
   ===================================================================== */
USE master;
GO
IF DB_ID(N'N08_NhaKhoa') IS NOT NULL
BEGIN
    ALTER DATABASE N08_NhaKhoa SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE N08_NhaKhoa;
END
GO
CREATE DATABASE N08_NhaKhoa COLLATE Vietnamese_CI_AS;
GO
ALTER DATABASE N08_NhaKhoa SET READ_COMMITTED_SNAPSHOT ON;
GO

/* ---------------------------------------------------------------------
   (Tùy chọn) Tạo login riêng cho ứng dụng thay vì dùng sa.
   Bỏ chú thích, ĐỔI MẬT KHẨU rồi chạy. Không dùng mật khẩu mẫu này.
   ---------------------------------------------------------------------
CREATE LOGIN n08_app WITH PASSWORD = N'<Đặt_mật_khẩu_mạnh_tại_đây>', CHECK_POLICY = ON;
GO
USE N08_NhaKhoa;
GO
CREATE USER n08_app FOR LOGIN n08_app;
ALTER ROLE db_datareader ADD MEMBER n08_app;
ALTER ROLE db_datawriter ADD MEMBER n08_app;
GO
*/
