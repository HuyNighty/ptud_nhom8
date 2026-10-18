-- N08 - Database Migration V6 (ĐỀ XUẤT — số version chờ Huy xác nhận trước khi merge)
-- Nhập kho: nhà cung cấp, lô hàng, phiếu nhập và chi tiết phiếu nhập (UC30)
-- SQL Server 2022
-- Managed exclusively by Flyway.
-- Dựa trên sql/02_tao_bang.sql (prototype đã duyệt), bổ sung trạng thái CHO_XU_LY / DA_HUY theo Figma Nhập kho:
-- phiếu lưu thông tin lô ở từng dòng; chỉ khi "Nhập kho" mới tạo/cộng tồn LoHang (cùng transaction).
-- Hoàn trả kho (UC32, cần PhieuXuatKho) sẽ bổ sung bằng migration sau.

CREATE TABLE dbo.NhaCungCap (
    maNhaCungCap VARCHAR(10) NOT NULL,
    tenNhaCungCap NVARCHAR(150) NOT NULL,
    soDienThoai VARCHAR(20) NOT NULL,
    diaChi NVARCHAR(255) NULL,
    email VARCHAR(100) NULL,

    trangThai VARCHAR(20)
        COLLATE Latin1_General_100_BIN2 NOT NULL
        CONSTRAINT DF_NhaCungCap_TrangThai
        DEFAULT 'DANG_HOP_TAC',

    CONSTRAINT PK_NhaCungCap
        PRIMARY KEY (maNhaCungCap),

    CONSTRAINT CK_NhaCungCap_Ma
        CHECK (
            maNhaCungCap LIKE 'NCC[0-9][0-9][0-9]%'
            AND maNhaCungCap NOT LIKE 'NCC%[^0-9]%'
        ),

    CONSTRAINT CK_NhaCungCap_Ten
        CHECK (LEN(TRIM(tenNhaCungCap)) > 0),

    CONSTRAINT CK_NhaCungCap_TrangThai
        CHECK (trangThai IN ('DANG_HOP_TAC', 'NGUNG_HOP_TAC'))
);

CREATE TABLE dbo.LoHang (
    maLo INT IDENTITY(1, 1) NOT NULL,
    maThuoc VARCHAR(10) NULL,
    maVatTu VARCHAR(10) NULL,
    soLo VARCHAR(30) NOT NULL,
    hanSuDung DATE NULL,
    maNhaCungCap VARCHAR(10) NOT NULL,

    soLuongTon INT NOT NULL
        CONSTRAINT DF_LoHang_SoLuongTon
        DEFAULT 0,

    ngayNhapDau DATE NOT NULL,

    CONSTRAINT PK_LoHang
        PRIMARY KEY (maLo),

    CONSTRAINT FK_LoHang_Thuoc
        FOREIGN KEY (maThuoc) REFERENCES dbo.Thuoc (maThuoc),

    CONSTRAINT FK_LoHang_VatTu
        FOREIGN KEY (maVatTu) REFERENCES dbo.VatTu (maVatTu),

    CONSTRAINT FK_LoHang_NhaCungCap
        FOREIGN KEY (maNhaCungCap) REFERENCES dbo.NhaCungCap (maNhaCungCap),

    CONSTRAINT CK_LoHang_MotLoai
        CHECK (
            (maThuoc IS NOT NULL AND maVatTu IS NULL)
            OR (maThuoc IS NULL AND maVatTu IS NOT NULL)
        ),

    CONSTRAINT CK_LoHang_SoLo
        CHECK (LEN(TRIM(soLo)) > 0),

    CONSTRAINT CK_LoHang_Ton
        CHECK (soLuongTon >= 0)
);

-- Một số lô chỉ thuộc về một lô hàng của mỗi mặt hàng.
CREATE UNIQUE INDEX UX_LoHang_Thuoc_SoLo
    ON dbo.LoHang (maThuoc, soLo) WHERE maThuoc IS NOT NULL;

CREATE UNIQUE INDEX UX_LoHang_VatTu_SoLo
    ON dbo.LoHang (maVatTu, soLo) WHERE maVatTu IS NOT NULL;

CREATE INDEX IX_LoHang_HanSuDung
    ON dbo.LoHang (hanSuDung) WHERE soLuongTon > 0;

CREATE TABLE dbo.PhieuNhapKho (
    maPhieuNhap VARCHAR(10) NOT NULL,
    ngayNhap DATE NOT NULL,
    maNhaCungCap VARCHAR(10) NOT NULL,

    trangThai VARCHAR(20)
        COLLATE Latin1_General_100_BIN2 NOT NULL
        CONSTRAINT DF_PhieuNhapKho_TrangThai
        DEFAULT 'CHO_XU_LY',

    ghiChu NVARCHAR(300) NULL,

    maNhanVienLap VARCHAR(10) NOT NULL,
    thoiDiemLap DATETIME2(0) NOT NULL,

    maNhanVienNhapKho VARCHAR(10) NULL,
    thoiDiemNhapKho DATETIME2(0) NULL,

    maNhanVienHuy VARCHAR(10) NULL,
    thoiDiemHuy DATETIME2(0) NULL,
    lyDoHuy NVARCHAR(300) NULL,

    CONSTRAINT PK_PhieuNhapKho
        PRIMARY KEY (maPhieuNhap),

    CONSTRAINT FK_PhieuNhapKho_NhaCungCap
        FOREIGN KEY (maNhaCungCap) REFERENCES dbo.NhaCungCap (maNhaCungCap),

    CONSTRAINT FK_PhieuNhapKho_NguoiLap
        FOREIGN KEY (maNhanVienLap) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT FK_PhieuNhapKho_NguoiNhapKho
        FOREIGN KEY (maNhanVienNhapKho) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT FK_PhieuNhapKho_NguoiHuy
        FOREIGN KEY (maNhanVienHuy) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT CK_PhieuNhapKho_Ma
        CHECK (
            maPhieuNhap LIKE 'PN[0-9][0-9][0-9][0-9]%'
            AND maPhieuNhap NOT LIKE 'PN%[^0-9]%'
        ),

    CONSTRAINT CK_PhieuNhapKho_TrangThai
        CHECK (trangThai IN ('CHO_XU_LY', 'DA_HOAN_THANH', 'DA_HUY')),

    -- Dữ liệu đi kèm phải khớp trạng thái (hủy bắt buộc có lý do).
    CONSTRAINT CK_PhieuNhapKho_DuLieuTrangThai
        CHECK (
            (trangThai = 'CHO_XU_LY'
                AND maNhanVienNhapKho IS NULL AND thoiDiemNhapKho IS NULL
                AND maNhanVienHuy IS NULL AND thoiDiemHuy IS NULL AND lyDoHuy IS NULL)
            OR
            (trangThai = 'DA_HOAN_THANH'
                AND maNhanVienNhapKho IS NOT NULL AND thoiDiemNhapKho IS NOT NULL
                AND maNhanVienHuy IS NULL AND thoiDiemHuy IS NULL AND lyDoHuy IS NULL)
            OR
            (trangThai = 'DA_HUY'
                AND maNhanVienNhapKho IS NULL AND thoiDiemNhapKho IS NULL
                AND maNhanVienHuy IS NOT NULL AND thoiDiemHuy IS NOT NULL
                AND lyDoHuy IS NOT NULL AND LEN(TRIM(lyDoHuy)) > 0)
        )
);

CREATE INDEX IX_PhieuNhapKho_NgayNhap
    ON dbo.PhieuNhapKho (ngayNhap DESC);

CREATE TABLE dbo.ChiTietPhieuNhap (
    maChiTiet INT IDENTITY(1, 1) NOT NULL,
    maPhieuNhap VARCHAR(10) NOT NULL,
    maThuoc VARCHAR(10) NULL,
    maVatTu VARCHAR(10) NULL,
    soLo VARCHAR(30) NOT NULL,
    hanSuDung DATE NULL,
    soLuong INT NOT NULL,
    donGiaNhap DECIMAL(18, 0) NOT NULL,

    -- Lô được tạo/cộng tồn khi phiếu chuyển sang DA_HOAN_THANH.
    maLo INT NULL,

    CONSTRAINT PK_ChiTietPhieuNhap
        PRIMARY KEY (maChiTiet),

    CONSTRAINT FK_ChiTietPhieuNhap_Phieu
        FOREIGN KEY (maPhieuNhap) REFERENCES dbo.PhieuNhapKho (maPhieuNhap),

    CONSTRAINT FK_ChiTietPhieuNhap_Thuoc
        FOREIGN KEY (maThuoc) REFERENCES dbo.Thuoc (maThuoc),

    CONSTRAINT FK_ChiTietPhieuNhap_VatTu
        FOREIGN KEY (maVatTu) REFERENCES dbo.VatTu (maVatTu),

    CONSTRAINT FK_ChiTietPhieuNhap_Lo
        FOREIGN KEY (maLo) REFERENCES dbo.LoHang (maLo),

    CONSTRAINT CK_ChiTietPhieuNhap_MotLoai
        CHECK (
            (maThuoc IS NOT NULL AND maVatTu IS NULL)
            OR (maThuoc IS NULL AND maVatTu IS NOT NULL)
        ),

    CONSTRAINT CK_ChiTietPhieuNhap_SoLo
        CHECK (LEN(TRIM(soLo)) > 0),

    CONSTRAINT CK_ChiTietPhieuNhap_SoLuong
        CHECK (soLuong > 0 AND donGiaNhap >= 0)
);

CREATE INDEX IX_ChiTietPhieuNhap_Phieu
    ON dbo.ChiTietPhieuNhap (maPhieuNhap);
