-- N08 - Database Migration V8 (ĐỀ XUẤT — số version chờ Huy xác nhận trước khi merge)
-- Hoàn trả kho: phiếu hoàn trả gắn với phiếu xuất gốc, chi tiết theo lô (UC32)
-- SQL Server 2022
-- Managed exclusively by Flyway.
-- Hàng đã xuất được trả lại đúng lô đã xuất; tổng trả của một lô không vượt số đã xuất trên phiếu gốc
-- (kiểm tra ở Service, có khóa phiếu xuất gốc). Tồn chỉ thay đổi khi phiếu được "Duyệt";
-- hàng trả vì lỗi được ghi nhận nhưng không cộng lại vào tồn khả dụng.

CREATE TABLE dbo.PhieuHoanTra (
    maPhieuHoanTra VARCHAR(10) NOT NULL,
    ngayHoanTra DATE NOT NULL,
    maPhieuXuat VARCHAR(10) NOT NULL,

    lyDo VARCHAR(20)
        COLLATE Latin1_General_100_BIN2 NOT NULL,

    nguonHoanTra NVARCHAR(100) NOT NULL,
    nguoiHoanTra NVARCHAR(100) NOT NULL,

    trangThai VARCHAR(20)
        COLLATE Latin1_General_100_BIN2 NOT NULL
        CONSTRAINT DF_PhieuHoanTra_TrangThai
        DEFAULT 'CHO_XU_LY',

    ghiChu NVARCHAR(300) NULL,

    maNhanVienLap VARCHAR(10) NOT NULL,
    thoiDiemLap DATETIME2(0) NOT NULL,

    -- Người kiểm duyệt = người nhận lại hàng vào kho.
    maNhanVienDuyet VARCHAR(10) NULL,
    thoiDiemDuyet DATETIME2(0) NULL,

    maNhanVienHuy VARCHAR(10) NULL,
    thoiDiemHuy DATETIME2(0) NULL,
    lyDoHuy NVARCHAR(300) NULL,

    CONSTRAINT PK_PhieuHoanTra
        PRIMARY KEY (maPhieuHoanTra),

    CONSTRAINT FK_PhieuHoanTra_PhieuXuat
        FOREIGN KEY (maPhieuXuat) REFERENCES dbo.PhieuXuatKho (maPhieuXuat),

    CONSTRAINT FK_PhieuHoanTra_NguoiLap
        FOREIGN KEY (maNhanVienLap) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT FK_PhieuHoanTra_NguoiDuyet
        FOREIGN KEY (maNhanVienDuyet) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT FK_PhieuHoanTra_NguoiHuy
        FOREIGN KEY (maNhanVienHuy) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT CK_PhieuHoanTra_Ma
        CHECK (
            maPhieuHoanTra LIKE 'HT[0-9][0-9][0-9]%'
            AND maPhieuHoanTra NOT LIKE 'HT%[^0-9]%'
        ),

    CONSTRAINT CK_PhieuHoanTra_LyDo
        CHECK (lyDo IN ('DU_THUA', 'HANG_LOI', 'XUAT_NHAM', 'KHAC')),

    CONSTRAINT CK_PhieuHoanTra_Nguon
        CHECK (LEN(TRIM(nguonHoanTra)) > 0 AND LEN(TRIM(nguoiHoanTra)) > 0),

    CONSTRAINT CK_PhieuHoanTra_TrangThai
        CHECK (trangThai IN ('CHO_XU_LY', 'DA_HOAN_THANH', 'DA_HUY')),

    CONSTRAINT CK_PhieuHoanTra_DuLieuTrangThai
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

CREATE INDEX IX_PhieuHoanTra_NgayHoanTra
    ON dbo.PhieuHoanTra (ngayHoanTra DESC);

CREATE INDEX IX_PhieuHoanTra_PhieuXuat
    ON dbo.PhieuHoanTra (maPhieuXuat);

CREATE TABLE dbo.ChiTietPhieuHoanTra (
    maChiTiet INT IDENTITY(1, 1) NOT NULL,
    maPhieuHoanTra VARCHAR(10) NOT NULL,
    maLo INT NOT NULL,
    soLuong INT NOT NULL,

    -- Giá vốn lấy theo dòng xuất gốc của cùng lô.
    donGia DECIMAL(18, 0) NOT NULL,

    CONSTRAINT PK_ChiTietPhieuHoanTra
        PRIMARY KEY (maChiTiet),

    CONSTRAINT FK_ChiTietPhieuHoanTra_Phieu
        FOREIGN KEY (maPhieuHoanTra) REFERENCES dbo.PhieuHoanTra (maPhieuHoanTra),

    CONSTRAINT FK_ChiTietPhieuHoanTra_Lo
        FOREIGN KEY (maLo) REFERENCES dbo.LoHang (maLo),

    CONSTRAINT UQ_ChiTietPhieuHoanTra_PhieuLo
        UNIQUE (maPhieuHoanTra, maLo),

    CONSTRAINT CK_ChiTietPhieuHoanTra_SoLuong
        CHECK (soLuong > 0 AND donGia >= 0)
);
