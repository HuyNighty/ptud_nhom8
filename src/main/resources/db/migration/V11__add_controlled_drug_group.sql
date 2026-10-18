-- N08 - Database Migration V11 (ĐỀ XUẤT — số version chờ Huy xác nhận trước khi merge)
-- Thuốc kiểm soát đặc biệt: thêm nhóm kiểm soát cho thuốc (UC33)
-- SQL Server 2022
-- Managed exclusively by Flyway.
-- Nhóm: GAY_NGHIEN (gây nghiện), HUONG_THAN (hướng thần), TIEN_CHAT (tiền chất).
-- Chỉ thuốc kiemSoatDacBiet = 1 mới có nhóm; NULL = chưa phân loại. Không đổi dữ liệu có sẵn.

ALTER TABLE dbo.Thuoc
    ADD nhomKiemSoat VARCHAR(20)
        COLLATE Latin1_General_100_BIN2 NULL;
GO

ALTER TABLE dbo.Thuoc
    ADD CONSTRAINT CK_Thuoc_NhomKiemSoat
        CHECK (
            nhomKiemSoat IS NULL
            OR (kiemSoatDacBiet = 1 AND nhomKiemSoat IN ('GAY_NGHIEN', 'HUONG_THAN', 'TIEN_CHAT'))
        );
