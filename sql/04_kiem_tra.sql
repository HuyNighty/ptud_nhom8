/* =====================================================================
   04_kiem_tra.sql — View đối soát & truy vấn kiểm tra tính nhất quán
   (Tùy chọn, chạy sau 03). Ứng dụng không phụ thuộc các view này.
   ===================================================================== */
USE N08_NhaKhoa;
GO

/* Tổng tiền, đã thu, còn nợ của hóa đơn — tính từ dữ liệu gốc, không lưu cột */
CREATE OR ALTER VIEW vw_CongNoHoaDon AS
SELECT h.maHoaDon, h.maBenhNhan, h.trangThai,
       COALESCE(ct.tongTien, 0) AS tongTien,
       COALESCE(tt.daThu, 0)    AS daThu,
       CASE WHEN h.trangThai = 'DA_HUY' THEN 0 ELSE COALESCE(ct.tongTien, 0) - COALESCE(tt.daThu, 0) END AS conNo
FROM HoaDon h
LEFT JOIN (SELECT maHoaDon, SUM(soLuong * donGiaApDung) AS tongTien FROM ChiTietHoaDon GROUP BY maHoaDon) ct ON ct.maHoaDon = h.maHoaDon
LEFT JOIN (SELECT maHoaDon, SUM(soTien) AS daThu FROM ThanhToan WHERE trangThai = 'THANH_CONG' GROUP BY maHoaDon) tt ON tt.maHoaDon = h.maHoaDon;
GO

/* Đối soát tồn kho: số tồn đang lưu so với lịch sử phiếu */
CREATE OR ALTER VIEW vw_DoiSoatTonKho AS
SELECT l.maLo, l.soLo, l.soLuongTon,
       COALESCE(n.nhap, 0) - COALESCE(x.xuat, 0) + COALESCE(d.dieuChinh, 0) AS tonTheoLichSu,
       l.soLuongTon - (COALESCE(n.nhap, 0) - COALESCE(x.xuat, 0) + COALESCE(d.dieuChinh, 0)) AS chenhLech
FROM LoHang l
LEFT JOIN (SELECT maLo, SUM(soLuong) AS nhap FROM ChiTietPhieuNhap GROUP BY maLo) n ON n.maLo = l.maLo
LEFT JOIN (SELECT maLo, SUM(soLuong) AS xuat FROM ChiTietPhieuXuat GROUP BY maLo) x ON x.maLo = l.maLo
LEFT JOIN (SELECT c.maLo, SUM(c.soLuongThucTe - c.soLuongHeThong) AS dieuChinh
           FROM ChiTietDieuChinhKho c JOIN PhieuDieuChinhKho p ON p.maPhieu = c.maPhieu
           WHERE p.trangThai = 'DA_DUYET' GROUP BY c.maLo) d ON d.maLo = l.maLo;
GO

/* Trạng thái hóa đơn phải khớp với số đã thu */
CREATE OR ALTER VIEW vw_KiemTraTrangThaiHoaDon AS
SELECT v.*,
       CASE WHEN v.trangThai = 'DA_HUY' THEN 'DA_HUY'
            WHEN v.daThu = 0 THEN 'CHUA_THANH_TOAN'
            WHEN v.daThu >= v.tongTien THEN 'DA_THANH_TOAN'
            ELSE 'THANH_TOAN_MOT_PHAN' END AS trangThaiTheoTien
FROM vw_CongNoHoaDon v;
GO

/* ---------- Truy vấn kiểm tra: kết quả mong đợi = 0 dòng ---------- */
SELECT N'Lệch tồn kho' AS kiemTra, * FROM vw_DoiSoatTonKho WHERE chenhLech <> 0;
SELECT N'Sai trạng thái hóa đơn' AS kiemTra, * FROM vw_KiemTraTrangThaiHoaDon WHERE trangThai <> trangThaiTheoTien;
SELECT N'Tự duyệt yêu cầu hủy' AS kiemTra, * FROM YeuCauHuyHoaDon WHERE maTaiKhoanDuyet = maTaiKhoanTao;
SELECT N'Tự duyệt điều chỉnh kho' AS kiemTra, * FROM PhieuDieuChinhKho WHERE maTaiKhoanDuyet = maTaiKhoanTao;
SELECT N'Nhân viên nghỉ việc còn tài khoản hoạt động' AS kiemTra, nv.maNhanVien, tk.tenDangNhap, tk.trangThai
FROM NhanVien nv JOIN TaiKhoan tk ON tk.maNhanVien = nv.maNhanVien
WHERE nv.trangThai = 'NGHI_VIEC' AND tk.trangThai <> 'VO_HIEU';
SELECT N'Nhân viên nghỉ việc còn phân công chưa kết thúc' AS kiemTra, pc.*
FROM PhanCong pc JOIN NhanVien nv ON nv.maNhanVien = pc.maNhanVien
WHERE nv.trangThai = 'NGHI_VIEC' AND pc.trangThai IN ('CHO_HIEU_LUC','DANG_THUC_HIEN');
GO
