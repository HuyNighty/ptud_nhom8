-- N08 - Database Migration V9 (ĐỀ XUẤT — số version chờ Huy xác nhận trước khi merge)
-- Kiểm kê kho: đợt kiểm kê và chi tiết theo lô (UC28)
-- SQL Server 2022
-- Managed exclusively by Flyway.
-- Khi tạo đợt, hệ thống chụp số tồn sổ sách của từng lô; người kiểm nhập số thực tế đếm được.
-- Người duyệt (hoàn tất) phải khác người lập. Khi duyệt, tồn mỗi lô được điều chỉnh theo
-- chênh lệch (thực tế − sổ sách) trên tồn hiện tại, trong cùng transaction, không âm tồn.

CREATE TABLE dbo.PhieuKiemKe (
    maKiemKe VARCHAR(10) NOT NULL,
    ngayKiemKe DATE NOT NULL,

    phamVi VARCHAR(10)
        COLLATE Latin1_General_100_BIN2 NOT NULL,

    trangThai VARCHAR(20)
        COLLATE Latin1_General_100_BIN2 NOT NULL
        CONSTRAINT DF_PhieuKiemKe_TrangThai
        DEFAULT 'CHO_XU_LY',

    ghiChu NVARCHAR(300) NULL,

    maNhanVienLap VARCHAR(10) NOT NULL,
    thoiDiemLap DATETIME2(0) NOT NULL,

    maNhanVienDuyet VARCHAR(10) NULL,
    thoiDiemDuyet DATETIME2(0) NULL,

    maNhanVienHuy VARCHAR(10) NULL,
    thoiDiemHuy DATETIME2(0) NULL,
    lyDoHuy NVARCHAR(300) NULL,

    CONSTRAINT PK_PhieuKiemKe
        PRIMARY KEY (maKiemKe),

    CONSTRAINT FK_PhieuKiemKe_NguoiLap
        FOREIGN KEY (maNhanVienLap) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT FK_PhieuKiemKe_NguoiDuyet
        FOREIGN KEY (maNhanVienDuyet) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT FK_PhieuKiemKe_NguoiHuy
        FOREIGN KEY (maNhanVienHuy) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT CK_PhieuKiemKe_Ma
        CHECK (
            maKiemKe LIKE 'KK[0-9][0-9][0-9]%'
            AND maKiemKe NOT LIKE 'KK%[^0-9]%'
        ),

    CONSTRAINT CK_PhieuKiemKe_PhamVi
        CHECK (phamVi IN ('TAT_CA', 'THUOC', 'VAT_TU')),

    CONSTRAINT CK_PhieuKiemKe_TrangThai
        CHECK (trangThai IN ('CHO_XU_LY', 'DA_HOAN_THANH', 'DA_HUY')),

    -- UC28: người duyệt khác người lập.
    CONSTRAINT CK_PhieuKiemKe_NguoiDuyetKhacNguoiLap
        CHECK (maNhanVienDuyet IS NULL OR maNhanVienDuyet <> maNhanVienLap),

    CONSTRAINT CK_PhieuKiemKe_DuLieuTrangThai
        CHECK (
            (trangThai = 'CHO_XU_LY'
                AND maNhanVienDuyet IS NULL AND thoiDiemDuyet IS NULL
                AND maNhanVienHuy IS NULL AND thoiDiemHuy IS NULL AND lyDoHuy IS NULL)
            OR
            (trangThai = 'DA_HOAN_THANH'
                AND maNhanVienDuyet IS NOT NULL AND thoiDiemDuyet IS NOT NULL
                AND maNhanVienHuy IS NULL AND thoiDiemHuy IS NULL AND lyDoHuy IS NULL)
            OR
            (trangThai = 'DA_HUY'
                AND maNhanVienDuyet IS NULL AND thoiDiemDuyet IS NULL
                AND maNhanVienHuy IS NOT NULL AND thoiDiemHuy IS NOT NULL
                AND lyDoHuy IS NOT NULL AND LEN(TRIM(lyDoHuy)) > 0)
        )
);

CREATE INDEX IX_PhieuKiemKe_NgayKiemKe
    ON dbo.PhieuKiemKe (ngayKiemKe DESC);

CREATE TABLE dbo.ChiTietKiemKe (
    maChiTiet INT IDENTITY(1, 1) NOT NULL,
    maKiemKe VARCHAR(10) NOT NULL,
    maLo INT NOT NULL,

    -- Tồn sổ sách tại lúc tạo đợt.
    soLuongSoSach INT NOT NULL,

    -- NULL = chưa đếm.
    soLuongThucTe INT NULL,

    -- Lý do chênh lệch (bắt buộc khi duyệt nếu thực tế khác sổ sách — kiểm tra ở Service).
    ghiChu NVARCHAR(200) NULL,

    CONSTRAINT PK_ChiTietKiemKe
        PRIMARY KEY (maChiTiet),

    CONSTRAINT FK_ChiTietKiemKe_Phieu
        FOREIGN KEY (maKiemKe) REFERENCES dbo.PhieuKiemKe (maKiemKe),

    CONSTRAINT FK_ChiTietKiemKe_Lo
        FOREIGN KEY (maLo) REFERENCES dbo.LoHang (maLo),

    CONSTRAINT UQ_ChiTietKiemKe_PhieuLo
        UNIQUE (maKiemKe, maLo),

    CONSTRAINT CK_ChiTietKiemKe_SoLuong
        CHECK (soLuongSoSach >= 0 AND (soLuongThucTe IS NULL OR soLuongThucTe >= 0))
);
