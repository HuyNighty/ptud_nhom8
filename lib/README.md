# Thư mục lib/ — chỉ dùng khi build KHÔNG qua Maven

Ứng dụng chỉ cần **một** thư viện ngoài để chạy: driver JDBC chính thức của Microsoft cho SQL Server.

- Nếu dùng Maven (`mvn package`), **không cần** thư mục này — Maven tự tải driver.
- Nếu build bằng `scripts/build.bat` / `scripts/build.sh` (chỉ dùng `javac`):
  1. Tải gói **Microsoft JDBC Driver for SQL Server** (bản 12.x) từ trang của Microsoft
     (learn.microsoft.com → "Download Microsoft JDBC Driver for SQL Server"), hoặc lấy file
     `mssql-jdbc-12.8.1.jre11.jar` từ Maven Central.
  2. Chép file `mssql-jdbc-*.jre11.jar` vào thư mục `lib/` này.

Chạy kiểm thử tự động bằng script (không qua Maven) cần thêm `derby-10.14.2.0.jar`
(Apache Derby — CSDL thay thế, chỉ dùng khi kiểm thử) đặt vào `lib/test/`.

Các file `.jar` không được đưa vào Git (xem `.gitignore`).
