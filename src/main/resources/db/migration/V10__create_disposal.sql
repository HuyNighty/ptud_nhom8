-- N08 - Database Migration V10 (ĐỀ XUẤT — số version chờ Huy xác nhận trước khi merge)
-- Hủy / hao hụt: phiếu hủy và chi tiết theo lô (UC35)
-- SQL Server 2022
-- Managed exclusively by Flyway.
-- Ghi nhận hàng hư hỏng, hết hạn, mất mát. Tồn chỉ giảm khi phiếu được duyệt, người duyệt khác người lập
-- (người yêu cầu). Giá trị hủy tính theo giá nhập gần nhất của từng lô, chốt khi lập/sửa phiếu.

CREATE TABLE dbo.PhieuHuy (
    maPhieuHuy VARCHAR(10) NOT NULL,
    ngayHuy DATE NOT NULL,

    lyDo VARCHAR(20)
        COLLATE Latin1_General_100_BIN2 NOT NULL,

    -- Mô tả cụ thể (vd. "Hỏng hóc do đổ vỡ"); NULL = dùng nhãn lý do.
    moTa NVARCHAR(200) NULL,

    trangThai VARCHAR(20)
        COLLATE Latin1_General_100_BIN2 NOT NULL
        CONSTRAINT DF_PhieuHuy_TrangThai
        DEFAULT 'CHO_XU_LY',

    maNhanVienLap VARCHAR(10) NOT NULL,
    thoiDiemLap DATETIME2(0) NOT NULL,

    maNhanVienDuyet VARCHAR(10) NULL,
    thoiDiemDuyet DATETIME2(0) NULL,

    maNhanVienHuy VARCHAR(10) NULL,
    thoiDiemHuy DATETIME2(0) NULL,
    lyDoHuy NVARCHAR(300) NULL,

    CONSTRAINT PK_PhieuHuy
        PRIMARY KEY (maPhieuHuy),

    CONSTRAINT FK_PhieuHuy_NguoiLap
        FOREIGN KEY (maNhanVienLap) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT FK_PhieuHuy_NguoiDuyet
        FOREIGN KEY (maNhanVienDuyet) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT FK_PhieuHuy_NguoiHuy
        FOREIGN KEY (maNhanVienHuy) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT CK_PhieuHuy_Ma
        CHECK (
            maPhieuHuy LIKE 'HH[0-9][0-9][0-9]%'
            AND maPhieuHuy NOT LIKE 'HH%[^0-9]%'
        ),

    CONSTRAINT CK_PhieuHuy_LyDo
        CHECK (lyDo IN ('QUA_HAN', 'HONG_HOC', 'HAO_HUT', 'KHAC')),

    CONSTRAINT CK_PhieuHuy_NguoiDuyetKhacNguoiLap
        CHECK (maNhanVienDuyet IS NULL OR maNhanVienDuyet <> maNhanVienLap),

    CONSTRAINT CK_PhieuHuy_TrangThai
        CHECK (trangThai IN ('CHO_XU_LY', 'DA_HOAN_THANH', 'DA_HUY')),

    CONSTRAINT CK_PhieuHuy_DuLieuTrangThai
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

CREATE INDEX IX_PhieuHuy_NgayHuy
    ON dbo.PhieuHuy (ngayHuy DESC);

CREATE TABLE dbo.ChiTietPhieuHuy (
    maChiTiet INT IDENTITY(1, 1) NOT NULL,
    maPhieuHuy VARCHAR(10) NOT NULL,
    maLo INT NOT NULL,
    soLuong INT NOT NULL,
    donGia DECIMAL(18, 0) NOT NULL,

    CONSTRAINT PK_ChiTietPhieuHuy
        PRIMARY KEY (maChiTiet),

    CONSTRAINT FK_ChiTietPhieuHuy_Phieu
        FOREIGN KEY (maPhieuHuy) REFERENCES dbo.PhieuHuy (maPhieuHuy),

    CONSTRAINT FK_ChiTietPhieuHuy_Lo
        FOREIGN KEY (maLo) REFERENCES dbo.LoHang (maLo),

    CONSTRAINT UQ_ChiTietPhieuHuy_PhieuLo
        UNIQUE (maPhieuHuy, maLo),

    CONSTRAINT CK_ChiTietPhieuHuy_SoLuong
        CHECK (soLuong > 0 AND donGia >= 0)
);
