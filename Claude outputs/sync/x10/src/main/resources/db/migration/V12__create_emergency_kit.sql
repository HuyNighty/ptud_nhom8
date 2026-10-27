-- N08 - Database Migration V12 (ĐỀ XUẤT — số version chờ Huy xác nhận trước khi merge)
-- Hộp thuốc cấp cứu (UC34) và vị trí lưu trữ cho theo dõi tồn kho (UC36)
-- SQL Server 2022
-- Managed exclusively by Flyway.
-- Hộp cấp cứu là danh mục cấu hình được: mỗi hộp có danh sách mặt hàng và số lượng tối thiểu.
-- Mục đạt khi số hiện có >= tối thiểu và hạn dùng còn hơn ngưỡng (mặc định 30 ngày); chu kỳ kiểm tra
-- mặc định 7 ngày — đều là tham số của từng hộp (THIET_KE_DU_LIEU.md, giả định 12).
-- Bổ sung / thay thế lấy hàng từ kho bằng phiếu xuất kho (PhieuXuatKho) nên tồn kho luôn có chứng từ;
-- nhật ký hộp chỉ tham chiếu phiếu xuất, không tự trừ tồn.

ALTER TABLE dbo.Thuoc
    ADD viTriLuuTru NVARCHAR(50) NULL;
GO

ALTER TABLE dbo.VatTu
    ADD viTriLuuTru NVARCHAR(50) NULL;
GO

CREATE TABLE dbo.HopCapCuu (
    maHop VARCHAR(10) NOT NULL,
    tenHop NVARCHAR(150) NOT NULL,

    loaiHop VARCHAR(20)
        COLLATE Latin1_General_100_BIN2 NOT NULL,

    viTri NVARCHAR(100) NOT NULL,

    chuKyKiemTraNgay INT NOT NULL
        CONSTRAINT DF_HopCapCuu_ChuKy DEFAULT 7,

    nguongHanDungNgay INT NOT NULL
        CONSTRAINT DF_HopCapCuu_NguongHan DEFAULT 30,

    ghiChu NVARCHAR(300) NULL,

    maNhanVienTao VARCHAR(10) NOT NULL,
    thoiDiemTao DATETIME2(0) NOT NULL,

    CONSTRAINT PK_HopCapCuu
        PRIMARY KEY (maHop),

    CONSTRAINT FK_HopCapCuu_NguoiTao
        FOREIGN KEY (maNhanVienTao) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT CK_HopCapCuu_Ma
        CHECK (
            maHop LIKE 'HTCC-[0-9][0-9]%'
            AND SUBSTRING(maHop, 6, 10) NOT LIKE '%[^0-9]%'
        ),

    CONSTRAINT CK_HopCapCuu_Ten
        CHECK (LEN(TRIM(tenHop)) > 0),

    CONSTRAINT CK_HopCapCuu_ViTri
        CHECK (LEN(TRIM(viTri)) > 0),

    CONSTRAINT CK_HopCapCuu_Loai
        CHECK (loaiHop IN ('CHONG_SOC', 'CO_BAN')),

    CONSTRAINT CK_HopCapCuu_ThamSo
        CHECK (chuKyKiemTraNgay BETWEEN 1 AND 365 AND nguongHanDungNgay BETWEEN 0 AND 365)
);

-- Thành phần hộp: đúng một trong maThuoc / maVatTu; soLuongHienCo và hanDung là số đang có trong hộp
-- (cập nhật khi kiểm tra, bổ sung, thay thế). hanDung = hạn sớm nhất của số đang có; NULL = không theo dõi hạn.
CREATE TABLE dbo.ChiTietHopCapCuu (
    maChiTiet INT IDENTITY(1, 1) NOT NULL,
    maHop VARCHAR(10) NOT NULL,
    maThuoc VARCHAR(10) NULL,
    maVatTu VARCHAR(10) NULL,
    soLuongToiThieu INT NOT NULL,
    soLuongHienCo INT NOT NULL
        CONSTRAINT DF_ChiTietHopCapCuu_HienCo DEFAULT 0,
    hanDung DATE NULL,

    CONSTRAINT PK_ChiTietHopCapCuu
        PRIMARY KEY (maChiTiet),

    CONSTRAINT FK_ChiTietHopCapCuu_Hop
        FOREIGN KEY (maHop) REFERENCES dbo.HopCapCuu (maHop),

    CONSTRAINT FK_ChiTietHopCapCuu_Thuoc
        FOREIGN KEY (maThuoc) REFERENCES dbo.Thuoc (maThuoc),

    CONSTRAINT FK_ChiTietHopCapCuu_VatTu
        FOREIGN KEY (maVatTu) REFERENCES dbo.VatTu (maVatTu),

    CONSTRAINT CK_ChiTietHopCapCuu_MotMatHang
        CHECK (
            (maThuoc IS NOT NULL AND maVatTu IS NULL)
            OR (maThuoc IS NULL AND maVatTu IS NOT NULL)
        ),

    CONSTRAINT CK_ChiTietHopCapCuu_SoLuong
        CHECK (soLuongToiThieu > 0 AND soLuongHienCo >= 0)
);

CREATE UNIQUE INDEX UQ_ChiTietHopCapCuu_Thuoc
    ON dbo.ChiTietHopCapCuu (maHop, maThuoc)
    WHERE maThuoc IS NOT NULL;

CREATE UNIQUE INDEX UQ_ChiTietHopCapCuu_VatTu
    ON dbo.ChiTietHopCapCuu (maHop, maVatTu)
    WHERE maVatTu IS NOT NULL;

-- Nhật ký hộp: KIEM_TRA có kết quả DAT / KHONG_DAT; BO_SUNG / THAY_THE bắt buộc gắn phiếu xuất kho.
CREATE TABLE dbo.NhatKyHopCapCuu (
    maNhatKy INT IDENTITY(1, 1) NOT NULL,
    maHop VARCHAR(10) NOT NULL,

    loai VARCHAR(20)
        COLLATE Latin1_General_100_BIN2 NOT NULL,

    ketQua VARCHAR(20)
        COLLATE Latin1_General_100_BIN2 NULL,

    maPhieuXuat VARCHAR(10) NULL,

    maNhanVien VARCHAR(10) NOT NULL,
    thoiDiem DATETIME2(0) NOT NULL,
    ghiChu NVARCHAR(300) NULL,

    CONSTRAINT PK_NhatKyHopCapCuu
        PRIMARY KEY (maNhatKy),

    CONSTRAINT FK_NhatKyHopCapCuu_Hop
        FOREIGN KEY (maHop) REFERENCES dbo.HopCapCuu (maHop),

    CONSTRAINT FK_NhatKyHopCapCuu_PhieuXuat
        FOREIGN KEY (maPhieuXuat) REFERENCES dbo.PhieuXuatKho (maPhieuXuat),

    CONSTRAINT FK_NhatKyHopCapCuu_NhanVien
        FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien (maNhanVien),

    CONSTRAINT CK_NhatKyHopCapCuu_Loai
        CHECK (
            (loai = 'KIEM_TRA' AND ketQua IN ('DAT', 'KHONG_DAT') AND maPhieuXuat IS NULL)
            OR (loai IN ('BO_SUNG', 'THAY_THE') AND ketQua IS NULL AND maPhieuXuat IS NOT NULL)
        )
);

CREATE INDEX IX_NhatKyHopCapCuu_Hop
    ON dbo.NhatKyHopCapCuu (maHop, thoiDiem DESC);

-- Chi tiết nhật ký lưu mã mặt hàng (không trỏ ChiTietHopCapCuu) để vẫn giữ lịch sử khi bỏ mặt hàng khỏi hộp.
CREATE TABLE dbo.ChiTietNhatKyHopCapCuu (
    maChiTiet INT IDENTITY(1, 1) NOT NULL,
    maNhatKy INT NOT NULL,
    maThuoc VARCHAR(10) NULL,
    maVatTu VARCHAR(10) NULL,
    soLuongTruoc INT NOT NULL,
    soLuongSau INT NOT NULL,
    hanDungSau DATE NULL,

    ketQua VARCHAR(20)
        COLLATE Latin1_General_100_BIN2 NULL,

    CONSTRAINT PK_ChiTietNhatKyHopCapCuu
        PRIMARY KEY (maChiTiet),

    CONSTRAINT FK_ChiTietNhatKyHopCapCuu_NhatKy
        FOREIGN KEY (maNhatKy) REFERENCES dbo.NhatKyHopCapCuu (maNhatKy),

    CONSTRAINT FK_ChiTietNhatKyHopCapCuu_Thuoc
        FOREIGN KEY (maThuoc) REFERENCES dbo.Thuoc (maThuoc),

    CONSTRAINT FK_ChiTietNhatKyHopCapCuu_VatTu
        FOREIGN KEY (maVatTu) REFERENCES dbo.VatTu (maVatTu),

    CONSTRAINT CK_ChiTietNhatKyHopCapCuu_MotMatHang
        CHECK (
            (maThuoc IS NOT NULL AND maVatTu IS NULL)
            OR (maThuoc IS NULL AND maVatTu IS NOT NULL)
        ),

    CONSTRAINT CK_ChiTietNhatKyHopCapCuu_SoLuong
        CHECK (soLuongTruoc >= 0 AND soLuongSau >= 0),

    CONSTRAINT CK_ChiTietNhatKyHopCapCuu_KetQua
        CHECK (ketQua IS NULL OR ketQua IN ('DAT', 'THIEU', 'CAN_HAN', 'HET_HAN'))
);
