-- N08 - Database Migration V7 (ĐỀ XUẤT — số version chờ Huy xác nhận trước khi merge)
-- Xuất kho: phiếu xuất và chi tiết theo lô (UC31)
-- SQL Server 2022
-- Managed exclusively by Flyway.
-- Dựa trên sql/02_tao_bang.sql (prototype: chi tiết phiếu xuất theo lô), bổ sung trạng thái
-- CHO_XU_LY / DA_HUY và lý do xuất theo Figma Xuất kho. Lô được gợi ý FEFO khi lập phiếu;
-- tồn chỉ bị trừ khi "Xuất kho" (cùng transaction, không âm tồn).

CREATE TABLE dbo.PhieuXuatKho (
    maPhieuXuat VARCHAR(10) NOT NULL,
    ngayXuat DATE NOT NULL,

    lyDoXuat VARCHAR(30)
        COLLATE Latin1_General_100_BIN2 NOT NULL,

    nguoiNhan NVARCHAR(100) NOT NULL,

    trangThai VARCHAR(20)
        COLLATE Latin1_General_100_BIN2 NOT NULL
        CONSTRAINT DF_PhieuXuatKho_TrangThai
        DEFAULT 'CHO_XU_LY',

    ghiChu NVARCHAR(300) NULL,

    maNhanVienLap VARCHAR(10) NOT NULL,
    thoiDiemLap DATETIME2(0) NOT NULL,

    maNhanVienXuatKho VARCHAR(10) NULL,
    thoiDiemXuatKho DATETIME2(0) NULL,

    maNhanVienHuy VARCHAR(10) NULL,
    thoiDiemHuy DATETIME2(0) NULL,
    lyDoHuy NVARCHAR(300) NULL,

    CONSTRAINT PK_PhieuXuatKho
        PRIMARY KEY (maPhieuXuat),

    CONSTRAINT FK_PhieuXuatKho_NguoiLap
        FOREIGN KEY (maNhanVienLap) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT FK_PhieuXuatKho_NguoiXuatKho
        FOREIGN KEY (maNhanVienXuatKho) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT FK_PhieuXuatKho_NguoiHuy
        FOREIGN KEY (maNhanVienHuy) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT CK_PhieuXuatKho_Ma
        CHECK (
            maPhieuXuat LIKE 'PX[0-9][0-9][0-9]%'
            AND maPhieuXuat NOT LIKE 'PX%[^0-9]%'
        ),

    CONSTRAINT CK_PhieuXuatKho_LyDo
        CHECK (lyDoXuat IN ('DIEU_TRI', 'PHUC_VU_PHONG_KHAM', 'HUY_HAO_HUT', 'TRA_NCC', 'KHAC')),

    CONSTRAINT CK_PhieuXuatKho_NguoiNhan
        CHECK (LEN(TRIM(nguoiNhan)) > 0),

    CONSTRAINT CK_PhieuXuatKho_TrangThai
        CHECK (trangThai IN ('CHO_XU_LY', 'DA_HOAN_THANH', 'DA_HUY')),

    CONSTRAINT CK_PhieuXuatKho_DuLieuTrangThai
        CHECK (
            (trangThai = 'CHO_XU_LY'
                AND maNhanVienXuatKho IS NULL AND thoiDiemXuatKho IS NULL
                AND maNhanVienHuy IS NULL AND thoiDiemHuy IS NULL AND lyDoHuy IS NULL)
            OR
            (trangThai = 'DA_HOAN_THANH'
                AND maNhanVienXuatKho IS NOT NULL AND thoiDiemXuatKho IS NOT NULL
                AND maNhanVienHuy IS NULL AND thoiDiemHuy IS NULL AND lyDoHuy IS NULL)
            OR
            (trangThai = 'DA_HUY'
                AND maNhanVienXuatKho IS NULL AND thoiDiemXuatKho IS NULL
                AND maNhanVienHuy IS NOT NULL AND thoiDiemHuy IS NOT NULL
                AND lyDoHuy IS NOT NULL AND LEN(TRIM(lyDoHuy)) > 0)
        )
);

CREATE INDEX IX_PhieuXuatKho_NgayXuat
    ON dbo.PhieuXuatKho (ngayXuat DESC);

CREATE TABLE dbo.ChiTietPhieuXuat (
    maChiTiet INT IDENTITY(1, 1) NOT NULL,
    maPhieuXuat VARCHAR(10) NOT NULL,
    maLo INT NOT NULL,
    soLuong INT NOT NULL,

    -- Giá vốn của lô tại thời điểm xuất (giá nhập gần nhất của lô); chỉ có khi đã xuất kho.
    donGiaXuat DECIMAL(18, 0) NULL,

    CONSTRAINT PK_ChiTietPhieuXuat
        PRIMARY KEY (maChiTiet),

    CONSTRAINT FK_ChiTietPhieuXuat_Phieu
        FOREIGN KEY (maPhieuXuat) REFERENCES dbo.PhieuXuatKho (maPhieuXuat),

    CONSTRAINT FK_ChiTietPhieuXuat_Lo
        FOREIGN KEY (maLo) REFERENCES dbo.LoHang (maLo),

    CONSTRAINT UQ_ChiTietPhieuXuat_PhieuLo
        UNIQUE (maPhieuXuat, maLo),

    CONSTRAINT CK_ChiTietPhieuXuat_SoLuong
        CHECK (soLuong > 0 AND (donGiaXuat IS NULL OR donGiaXuat >= 0))
);
