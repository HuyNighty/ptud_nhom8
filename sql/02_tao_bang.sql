/* =====================================================================
   N08 - Quản lý phòng khám nha khoa
   02_tao_bang.sql — Tạo bảng, khóa chính, khóa ngoại, ràng buộc, chỉ mục
   Hệ quản trị: Microsoft SQL Server 2019 trở lên
   Chạy SAU 01_tao_csdl.sql, trong database N08_NhaKhoa.

   Quy ước:
   - 26 bảng đầu tương ứng 26 lớp entity trong class diagram mới nhất.
   - Các bảng bổ sung được đánh dấu [BỔ SUNG] kèm lý do nghiệp vụ.
   - Tổng tiền, đã thanh toán, còn nợ, tiền thối, hết hạn: KHÔNG lưu cột,
     tính từ dữ liệu gốc (ChiTietHoaDon, ThanhToan, LoHang.hanSuDung).
   - LoHang.soLuongTon là số tồn được cập nhật trong cùng transaction với
     phiếu nhập / xuất / hoàn trả / điều chỉnh; view vw_DoiSoatTonKho
     (04_kiem_tra.sql) đối chiếu với lịch sử phiếu.
   ===================================================================== */
USE N08_NhaKhoa;
GO

/* ---------- Hạ tầng [BỔ SUNG]: bộ đếm sinh mã & tham số cấu hình ---------- */
CREATE TABLE BoDemMa (
    loai        VARCHAR(10)  NOT NULL CONSTRAINT PK_BoDemMa PRIMARY KEY,
    tienTo      VARCHAR(10)  NOT NULL,
    doDai       INT          NOT NULL,
    giaTri      INT          NOT NULL DEFAULT 0
);
GO
CREATE TABLE ThamSoHeThong (
    ten         VARCHAR(50)    NOT NULL CONSTRAINT PK_ThamSoHeThong PRIMARY KEY,
    giaTri      VARCHAR(100)   NOT NULL,
    moTa        NVARCHAR(300)  NULL
);
GO

/* ---------- Nhân sự ---------- */
CREATE TABLE NhanVien (
    maNhanVien    VARCHAR(10)    NOT NULL CONSTRAINT PK_NhanVien PRIMARY KEY,
    hoTen         NVARCHAR(100)  NOT NULL,
    soDienThoai   VARCHAR(15)    NOT NULL CONSTRAINT UQ_NhanVien_SDT UNIQUE,
    chucDanh      VARCHAR(10)    NOT NULL,
    trangThai     VARCHAR(20)    NOT NULL DEFAULT 'DANG_LAM_VIEC',
    ngaySinh      DATE           NULL,
    gioiTinh      VARCHAR(5)     NULL,
    email         VARCHAR(100)   NULL,
    ngayVaoLam    DATE           NULL,
    chuyenMon     NVARCHAR(200)  NULL,
    ngayNghiViec  DATE           NULL,
    CONSTRAINT CK_NhanVien_ChucDanh CHECK (chucDanh IN ('AD','LT','BS','YT','QL','KHO')),
    CONSTRAINT CK_NhanVien_TrangThai CHECK (trangThai IN ('DANG_LAM_VIEC','NGHI_VIEC')),
    CONSTRAINT CK_NhanVien_GioiTinh CHECK (gioiTinh IS NULL OR gioiTinh IN ('NAM','NU','KHAC')),
    CONSTRAINT CK_NhanVien_NghiViec CHECK ((trangThai = 'NGHI_VIEC' AND ngayNghiViec IS NOT NULL) OR (trangThai = 'DANG_LAM_VIEC' AND ngayNghiViec IS NULL))
);
GO
CREATE TABLE TaiKhoan (
    maTaiKhoan       VARCHAR(10)    NOT NULL CONSTRAINT PK_TaiKhoan PRIMARY KEY,
    tenDangNhap      VARCHAR(50)    NOT NULL CONSTRAINT UQ_TaiKhoan_Ten UNIQUE,
    matKhau          VARCHAR(200)   NOT NULL,
    trangThai        VARCHAR(20)    NOT NULL DEFAULT 'HOAT_DONG',
    vaiTro           VARCHAR(10)    NOT NULL,
    maNhanVien       VARCHAR(10)    NOT NULL CONSTRAINT UQ_TaiKhoan_NhanVien UNIQUE,
    soLanSai         INT            NOT NULL DEFAULT 0,
    lanDangNhapCuoi  DATETIME2(0)   NULL,
    lyDoKhoa         NVARCHAR(300)  NULL,
    CONSTRAINT FK_TaiKhoan_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT CK_TaiKhoan_TrangThai CHECK (trangThai IN ('HOAT_DONG','BI_KHOA','VO_HIEU')),
    CONSTRAINT CK_TaiKhoan_VaiTro CHECK (vaiTro IN ('AD','LT','BS','YT','QL','KHO')),
    CONSTRAINT CK_TaiKhoan_SoLanSai CHECK (soLanSai >= 0)
);
GO
CREATE TABLE CaLamViec (
    maCa        VARCHAR(10)    NOT NULL CONSTRAINT PK_CaLamViec PRIMARY KEY,
    tenCa       NVARCHAR(50)   NOT NULL,
    gioBatDau   TIME(0)        NOT NULL,
    gioKetThuc  TIME(0)        NOT NULL,
    soBacSi     INT            NOT NULL DEFAULT 0,
    soYTa       INT            NOT NULL DEFAULT 0,
    soLeTan     INT            NOT NULL DEFAULT 0,
    CONSTRAINT CK_CaLamViec_Gio CHECK (gioKetThuc > gioBatDau),
    CONSTRAINT CK_CaLamViec_DinhMuc CHECK (soBacSi >= 0 AND soYTa >= 0 AND soLeTan >= 0)
);
GO
CREATE TABLE PhanCong (
    maPhanCong     VARCHAR(10)    NOT NULL CONSTRAINT PK_PhanCong PRIMARY KEY,
    maNhanVien     VARCHAR(10)    NOT NULL,
    maCa           VARCHAR(10)    NOT NULL,
    ngayLam        DATE           NOT NULL,
    nhiemVu        NVARCHAR(200)  NOT NULL,
    trangThai      VARCHAR(20)    NOT NULL DEFAULT 'CHO_HIEU_LUC',
    lyDoHuy        NVARCHAR(300)  NULL,
    maPhanCongGoc  VARCHAR(10)    NULL,
    CONSTRAINT FK_PhanCong_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT FK_PhanCong_Ca FOREIGN KEY (maCa) REFERENCES CaLamViec(maCa),
    CONSTRAINT FK_PhanCong_Goc FOREIGN KEY (maPhanCongGoc) REFERENCES PhanCong(maPhanCong),
    CONSTRAINT CK_PhanCong_TrangThai CHECK (trangThai IN ('CHO_HIEU_LUC','DANG_THUC_HIEN','HOAN_THANH','DA_HUY')),
    CONSTRAINT CK_PhanCong_Huy CHECK (trangThai <> 'DA_HUY' OR lyDoHuy IS NOT NULL)
);
GO
CREATE TABLE BanGiaoCa (
    maBanGiao          VARCHAR(10)     NOT NULL CONSTRAINT PK_BanGiaoCa PRIMARY KEY,
    thoiGian           DATETIME2(0)    NOT NULL,
    noiDung            NVARCHAR(1000)  NOT NULL,
    trangThai          VARCHAR(20)     NOT NULL DEFAULT 'CHO_BAN_GIAO',
    ngay               DATE            NOT NULL,
    maNguoiGiao        VARCHAR(10)     NOT NULL,
    maNguoiNhan        VARCHAR(10)     NOT NULL,
    maCaGiao           VARCHAR(10)     NOT NULL,
    maCaNhan           VARCHAR(10)     NOT NULL,
    coChenhLech        BIT             NOT NULL DEFAULT 0,
    maTaiKhoanTao      VARCHAR(10)     NOT NULL,
    maTaiKhoanXacNhan  VARCHAR(10)     NULL,
    thoiGianXacNhan    DATETIME2(0)    NULL,
    ghiChuXacNhan      NVARCHAR(500)   NULL,
    maTaiKhoanHuy      VARCHAR(10)     NULL,
    thoiGianHuy        DATETIME2(0)    NULL,
    lyDoHuy            NVARCHAR(300)   NULL,
    CONSTRAINT FK_BanGiao_Giao FOREIGN KEY (maNguoiGiao) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT FK_BanGiao_Nhan FOREIGN KEY (maNguoiNhan) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT FK_BanGiao_CaGiao FOREIGN KEY (maCaGiao) REFERENCES CaLamViec(maCa),
    CONSTRAINT FK_BanGiao_CaNhan FOREIGN KEY (maCaNhan) REFERENCES CaLamViec(maCa),
    CONSTRAINT FK_BanGiao_TKTao FOREIGN KEY (maTaiKhoanTao) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT FK_BanGiao_TKXacNhan FOREIGN KEY (maTaiKhoanXacNhan) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT FK_BanGiao_TKHuy FOREIGN KEY (maTaiKhoanHuy) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT CK_BanGiao_TrangThai CHECK (trangThai IN ('CHO_BAN_GIAO','DA_BAN_GIAO','DA_HUY')),
    CONSTRAINT CK_BanGiao_KhacNguoi CHECK (maNguoiGiao <> maNguoiNhan),
    CONSTRAINT CK_BanGiao_KhongTuXacNhan CHECK (maTaiKhoanXacNhan IS NULL OR maTaiKhoanXacNhan <> maTaiKhoanTao),
    CONSTRAINT CK_BanGiao_XacNhan CHECK (trangThai <> 'DA_BAN_GIAO' OR (maTaiKhoanXacNhan IS NOT NULL AND thoiGianXacNhan IS NOT NULL)),
    CONSTRAINT CK_BanGiao_Huy CHECK (trangThai <> 'DA_HUY' OR lyDoHuy IS NOT NULL)
);
GO
CREATE TABLE DanhGiaNhanSu (
    maDanhGia       VARCHAR(10)     NOT NULL CONSTRAINT PK_DanhGiaNhanSu PRIMARY KEY,
    maNhanVien      VARCHAR(10)     NOT NULL,
    maNguoiDanhGia  VARCHAR(10)     NOT NULL,
    kyDanhGia       VARCHAR(10)     NOT NULL,
    ngayDanhGia     DATE            NOT NULL,
    diem            DECIMAL(3,2)    NULL,
    nhanXet         NVARCHAR(1000)  NULL,
    trangThai       VARCHAR(10)     NOT NULL DEFAULT 'NHAP',
    CONSTRAINT FK_DanhGia_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT FK_DanhGia_NguoiDanhGia FOREIGN KEY (maNguoiDanhGia) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT UQ_DanhGia_Ky UNIQUE (maNhanVien, kyDanhGia),
    CONSTRAINT CK_DanhGia_TrangThai CHECK (trangThai IN ('NHAP','DA_GUI')),
    CONSTRAINT CK_DanhGia_KhacNguoi CHECK (maNhanVien <> maNguoiDanhGia),
    CONSTRAINT CK_DanhGia_Diem CHECK (diem IS NULL OR (diem >= 1 AND diem <= 5))
);
GO
/* [BỔ SUNG] Lưu điểm từng tiêu chí của Figma QL-07; DanhGiaNhanSu.diem = trung bình */
CREATE TABLE ChiTietDanhGia (
    maDanhGia   VARCHAR(10)    NOT NULL,
    thuTu       INT            NOT NULL,
    tieuChi     NVARCHAR(100)  NOT NULL,
    diem        INT            NOT NULL,
    CONSTRAINT PK_ChiTietDanhGia PRIMARY KEY (maDanhGia, thuTu),
    CONSTRAINT FK_CTDanhGia_DanhGia FOREIGN KEY (maDanhGia) REFERENCES DanhGiaNhanSu(maDanhGia),
    CONSTRAINT CK_CTDanhGia_Diem CHECK (diem BETWEEN 1 AND 5)
);
GO

/* ---------- Bệnh nhân, lịch hẹn, khám ---------- */
CREATE TABLE BenhNhan (
    maBenhNhan     VARCHAR(10)    NOT NULL CONSTRAINT PK_BenhNhan PRIMARY KEY,
    hoTen          NVARCHAR(100)  NOT NULL,
    ngaySinh       DATE           NOT NULL,
    gioiTinh       VARCHAR(5)     NOT NULL,
    soDienThoai    VARCHAR(15)    NOT NULL,
    diaChi         NVARCHAR(255)  NULL,
    nguoiLienHe    NVARCHAR(200)  NULL,
    ngayTao        DATETIME2(0)   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    maTaiKhoanTao  VARCHAR(10)    NULL,
    CONSTRAINT FK_BenhNhan_TaiKhoan FOREIGN KEY (maTaiKhoanTao) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT CK_BenhNhan_GioiTinh CHECK (gioiTinh IN ('NAM','NU','KHAC'))
);
GO
CREATE INDEX IX_BenhNhan_SDT ON BenhNhan(soDienThoai);
GO
CREATE TABLE HoSoBenhAn (
    maHoSo      VARCHAR(10)    NOT NULL CONSTRAINT PK_HoSoBenhAn PRIMARY KEY,
    maBenhNhan  VARCHAR(10)    NOT NULL CONSTRAINT UQ_HoSo_BenhNhan UNIQUE,
    ngayLap     DATE           NOT NULL,
    tienSuBenh  NVARCHAR(500)  NULL,
    diUng       NVARCHAR(300)  NULL,
    CONSTRAINT FK_HoSo_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(maBenhNhan)
);
GO
CREATE TABLE DichVu (
    maDichVu      VARCHAR(10)    NOT NULL CONSTRAINT PK_DichVu PRIMARY KEY,
    tenDichVu     NVARCHAR(100)  NOT NULL,
    donGia        DECIMAL(18,0)  NOT NULL,
    moTa          NVARCHAR(300)  NULL,
    laCanLamSang  BIT            NOT NULL DEFAULT 0,
    dangApDung    BIT            NOT NULL DEFAULT 1,
    CONSTRAINT CK_DichVu_DonGia CHECK (donGia >= 0)
);
GO
CREATE TABLE KeHoachDieuTri (
    maKeHoach    VARCHAR(10)    NOT NULL CONSTRAINT PK_KeHoachDieuTri PRIMARY KEY,
    maHoSo       VARCHAR(10)    NOT NULL,
    maPhieuKham  VARCHAR(10)    NULL,
    ngayLap      DATE           NOT NULL,
    mucTieu      NVARCHAR(300)  NOT NULL,
    trangThai    VARCHAR(20)    NOT NULL DEFAULT 'DANG_THUC_HIEN',
    maNhanVien   VARCHAR(10)    NOT NULL,
    lyDoHuy      NVARCHAR(300)  NULL,
    CONSTRAINT FK_KeHoach_HoSo FOREIGN KEY (maHoSo) REFERENCES HoSoBenhAn(maHoSo),
    CONSTRAINT FK_KeHoach_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT CK_KeHoach_TrangThai CHECK (trangThai IN ('DANG_THUC_HIEN','HOAN_THANH','DA_HUY')),
    CONSTRAINT CK_KeHoach_Huy CHECK (trangThai <> 'DA_HUY' OR lyDoHuy IS NOT NULL)
);
GO
CREATE TABLE LichHen (
    maLichHen      VARCHAR(10)    NOT NULL CONSTRAINT PK_LichHen PRIMARY KEY,
    maBenhNhan     VARCHAR(10)    NOT NULL,
    maNhanVien     VARCHAR(10)    NOT NULL,
    thoiGianHen    DATETIME2(0)   NOT NULL,
    lyDoHen        NVARCHAR(300)  NULL,
    trangThai      VARCHAR(20)    NOT NULL DEFAULT 'CHO_XAC_NHAN',
    maLichHenGoc   VARCHAR(10)    NULL,
    maKeHoach      VARCHAR(10)    NULL,
    lyDoHuy        NVARCHAR(300)  NULL,
    maTaiKhoanTao  VARCHAR(10)    NULL,
    ngayTao        DATETIME2(0)   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_LichHen_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(maBenhNhan),
    CONSTRAINT FK_LichHen_BacSi FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT FK_LichHen_Goc FOREIGN KEY (maLichHenGoc) REFERENCES LichHen(maLichHen),
    CONSTRAINT FK_LichHen_KeHoach FOREIGN KEY (maKeHoach) REFERENCES KeHoachDieuTri(maKeHoach),
    CONSTRAINT FK_LichHen_TaiKhoan FOREIGN KEY (maTaiKhoanTao) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT CK_LichHen_TrangThai CHECK (trangThai IN ('CHO_XAC_NHAN','DA_XAC_NHAN','DA_DEN','HOAN_THANH','VANG_MAT','DA_HUY','DA_DOI_LICH')),
    CONSTRAINT CK_LichHen_Huy CHECK (trangThai <> 'DA_HUY' OR lyDoHuy IS NOT NULL)
);
GO
CREATE INDEX IX_LichHen_BacSi_ThoiGian ON LichHen(maNhanVien, thoiGianHen);
GO
CREATE INDEX IX_LichHen_ThoiGian ON LichHen(thoiGianHen);
GO
CREATE TABLE PhieuKham (
    maPhieuKham       VARCHAR(10)     NOT NULL CONSTRAINT PK_PhieuKham PRIMARY KEY,
    maBenhNhan        VARCHAR(10)     NOT NULL,
    maHoSo            VARCHAR(10)     NOT NULL,
    maNhanVien        VARCHAR(10)     NOT NULL,
    maLichHen         VARCHAR(10)     NULL,
    thoiGianTiepNhan  DATETIME2(0)    NOT NULL,
    trieuChung        NVARCHAR(500)   NULL,
    chanDoan          NVARCHAR(1000)  NULL,
    ketLuan           NVARCHAR(1000)  NULL,
    trangThai         VARCHAR(20)     NOT NULL DEFAULT 'CHO_KHAM',
    lyDoHuy           NVARCHAR(300)   NULL,
    CONSTRAINT FK_PhieuKham_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(maBenhNhan),
    CONSTRAINT FK_PhieuKham_HoSo FOREIGN KEY (maHoSo) REFERENCES HoSoBenhAn(maHoSo),
    CONSTRAINT FK_PhieuKham_BacSi FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT FK_PhieuKham_LichHen FOREIGN KEY (maLichHen) REFERENCES LichHen(maLichHen),
    CONSTRAINT CK_PhieuKham_TrangThai CHECK (trangThai IN ('CHO_KHAM','DANG_KHAM','DA_KET_LUAN','DA_HUY')),
    CONSTRAINT CK_PhieuKham_KetLuan CHECK (trangThai <> 'DA_KET_LUAN' OR chanDoan IS NOT NULL),
    CONSTRAINT CK_PhieuKham_Huy CHECK (trangThai <> 'DA_HUY' OR lyDoHuy IS NOT NULL)
);
GO
CREATE INDEX IX_PhieuKham_ThoiGian ON PhieuKham(thoiGianTiepNhan);
GO
ALTER TABLE KeHoachDieuTri ADD CONSTRAINT FK_KeHoach_PhieuKham FOREIGN KEY (maPhieuKham) REFERENCES PhieuKham(maPhieuKham);
GO
CREATE TABLE ChiTietDieuTri (
    maChiTiet           INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ChiTietDieuTri PRIMARY KEY,
    maKeHoach           VARCHAR(10)    NOT NULL,
    maDichVu            VARCHAR(10)    NOT NULL,
    viTriRang           NVARCHAR(50)   NOT NULL,
    ngayDuKien          DATE           NOT NULL,
    ngayThucHien        DATE           NULL,
    ketQua              NVARCHAR(500)  NULL,
    trangThai           VARCHAR(20)    NOT NULL DEFAULT 'CHUA_THUC_HIEN',
    maNhanVienThucHien  VARCHAR(10)    NULL,
    ghiChu              NVARCHAR(300)  NULL,
    CONSTRAINT FK_CTDieuTri_KeHoach FOREIGN KEY (maKeHoach) REFERENCES KeHoachDieuTri(maKeHoach),
    CONSTRAINT FK_CTDieuTri_DichVu FOREIGN KEY (maDichVu) REFERENCES DichVu(maDichVu),
    CONSTRAINT FK_CTDieuTri_BacSi FOREIGN KEY (maNhanVienThucHien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT CK_CTDieuTri_TrangThai CHECK (trangThai IN ('CHUA_THUC_HIEN','DA_THUC_HIEN','DA_HUY')),
    CONSTRAINT CK_CTDieuTri_ThucHien CHECK (trangThai <> 'DA_THUC_HIEN' OR (ngayThucHien IS NOT NULL AND ketQua IS NOT NULL AND maNhanVienThucHien IS NOT NULL))
);
GO
/* [BỔ SUNG] UC11 Lập phiếu chỉ định cận lâm sàng — hình không có lớp lưu chỉ định & kết quả */
CREATE TABLE ChiDinhCanLamSang (
    maChiDinh        VARCHAR(10)    NOT NULL CONSTRAINT PK_ChiDinhCanLamSang PRIMARY KEY,
    maPhieuKham      VARCHAR(10)    NOT NULL,
    maDichVu         VARCHAR(10)    NOT NULL,
    yeuCau           NVARCHAR(300)  NOT NULL,
    ketQua           NVARCHAR(1000) NULL,
    maNhanVien       VARCHAR(10)    NOT NULL,
    thoiGianChiDinh  DATETIME2(0)   NOT NULL,
    thoiGianKetQua   DATETIME2(0)   NULL,
    trangThai        VARCHAR(20)    NOT NULL DEFAULT 'CHO_THUC_HIEN',
    CONSTRAINT FK_CLS_PhieuKham FOREIGN KEY (maPhieuKham) REFERENCES PhieuKham(maPhieuKham),
    CONSTRAINT FK_CLS_DichVu FOREIGN KEY (maDichVu) REFERENCES DichVu(maDichVu),
    CONSTRAINT FK_CLS_BacSi FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT CK_CLS_TrangThai CHECK (trangThai IN ('CHO_THUC_HIEN','DA_CO_KET_QUA')),
    CONSTRAINT CK_CLS_KetQua CHECK (trangThai <> 'DA_CO_KET_QUA' OR ketQua IS NOT NULL)
);
GO

/* ---------- Thuốc, vật tư, kho ---------- */
CREATE TABLE Thuoc (
    maThuoc          VARCHAR(10)    NOT NULL CONSTRAINT PK_Thuoc PRIMARY KEY,
    tenThuoc         NVARCHAR(150)  NOT NULL,
    donViTinh        NVARCHAR(30)   NOT NULL,
    kiemSoatDacBiet  BIT            NOT NULL DEFAULT 0,
    hamLuong         NVARCHAR(50)   NULL,
    hoatChat         NVARCHAR(150)  NULL,
    dinhMucToiThieu  INT            NOT NULL DEFAULT 0,
    daDatHang        BIT            NOT NULL DEFAULT 0,
    CONSTRAINT CK_Thuoc_DinhMuc CHECK (dinhMucToiThieu >= 0)
);
GO
CREATE TABLE VatTu (
    maVatTu          VARCHAR(10)    NOT NULL CONSTRAINT PK_VatTu PRIMARY KEY,
    tenVatTu         NVARCHAR(150)  NOT NULL,
    donViTinh        NVARCHAR(30)   NOT NULL,
    moTa             NVARCHAR(300)  NULL,
    dinhMucToiThieu  INT            NOT NULL DEFAULT 0,
    daDatHang        BIT            NOT NULL DEFAULT 0,
    CONSTRAINT CK_VatTu_DinhMuc CHECK (dinhMucToiThieu >= 0)
);
GO
CREATE TABLE DonThuoc (
    maDonThuoc   VARCHAR(10)    NOT NULL CONSTRAINT PK_DonThuoc PRIMARY KEY,
    maPhieuKham  VARCHAR(10)    NOT NULL CONSTRAINT UQ_DonThuoc_PhieuKham UNIQUE,
    maBenhNhan   VARCHAR(10)    NOT NULL,
    maNhanVien   VARCHAR(10)    NOT NULL,
    ngayKe       DATE           NOT NULL,
    loiDan       NVARCHAR(500)  NULL,
    CONSTRAINT FK_DonThuoc_PhieuKham FOREIGN KEY (maPhieuKham) REFERENCES PhieuKham(maPhieuKham),
    CONSTRAINT FK_DonThuoc_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(maBenhNhan),
    CONSTRAINT FK_DonThuoc_BacSi FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien)
);
GO
CREATE TABLE ChiTietDonThuoc (
    maDonThuoc  VARCHAR(10)    NOT NULL,
    maThuoc     VARCHAR(10)    NOT NULL,
    soLuong     INT            NOT NULL,
    lieuDung    NVARCHAR(100)  NOT NULL,
    cachDung    NVARCHAR(200)  NOT NULL,
    soNgayDung  INT            NOT NULL,
    CONSTRAINT PK_ChiTietDonThuoc PRIMARY KEY (maDonThuoc, maThuoc),
    CONSTRAINT FK_CTDonThuoc_Don FOREIGN KEY (maDonThuoc) REFERENCES DonThuoc(maDonThuoc),
    CONSTRAINT FK_CTDonThuoc_Thuoc FOREIGN KEY (maThuoc) REFERENCES Thuoc(maThuoc),
    CONSTRAINT CK_CTDonThuoc_SoLuong CHECK (soLuong > 0 AND soNgayDung > 0)
);
GO
CREATE TABLE NhaCungCap (
    maNhaCungCap   VARCHAR(10)    NOT NULL CONSTRAINT PK_NhaCungCap PRIMARY KEY,
    tenNhaCungCap  NVARCHAR(150)  NOT NULL,
    diaChi         NVARCHAR(255)  NULL,
    soDienThoai    VARCHAR(20)    NOT NULL,
    email          VARCHAR(100)   NULL,
    trangThai      VARCHAR(20)    NOT NULL DEFAULT 'DANG_HOP_TAC',
    CONSTRAINT CK_NCC_TrangThai CHECK (trangThai IN ('DANG_HOP_TAC','NGUNG_HOP_TAC'))
);
GO
CREATE TABLE LoHang (
    maLo          VARCHAR(10)  NOT NULL CONSTRAINT PK_LoHang PRIMARY KEY,
    soLo          VARCHAR(30)  NOT NULL,
    ngaySanXuat   DATE         NULL,
    hanSuDung     DATE         NULL,
    maThuoc       VARCHAR(10)  NULL,
    maVatTu       VARCHAR(10)  NULL,
    maNhaCungCap  VARCHAR(10)  NOT NULL,
    soLuongTon    INT          NOT NULL DEFAULT 0,
    CONSTRAINT FK_LoHang_Thuoc FOREIGN KEY (maThuoc) REFERENCES Thuoc(maThuoc),
    CONSTRAINT FK_LoHang_VatTu FOREIGN KEY (maVatTu) REFERENCES VatTu(maVatTu),
    CONSTRAINT FK_LoHang_NCC FOREIGN KEY (maNhaCungCap) REFERENCES NhaCungCap(maNhaCungCap),
    CONSTRAINT CK_LoHang_MotLoai CHECK ((maThuoc IS NOT NULL AND maVatTu IS NULL) OR (maThuoc IS NULL AND maVatTu IS NOT NULL)),
    CONSTRAINT CK_LoHang_Ton CHECK (soLuongTon >= 0),
    CONSTRAINT CK_LoHang_Han CHECK (hanSuDung IS NULL OR ngaySanXuat IS NULL OR hanSuDung > ngaySanXuat)
);
GO
CREATE INDEX IX_LoHang_Thuoc ON LoHang(maThuoc);
GO
CREATE INDEX IX_LoHang_VatTu ON LoHang(maVatTu);
GO
CREATE TABLE PhieuXuatKho (
    maPhieuXuat  VARCHAR(10)    NOT NULL CONSTRAINT PK_PhieuXuatKho PRIMARY KEY,
    ngayXuat     DATETIME2(0)   NOT NULL,
    boPhanNhan   NVARCHAR(100)  NOT NULL,
    nguoiNhan    NVARCHAR(100)  NULL,
    lyDo         NVARCHAR(300)  NULL,
    trangThai    VARCHAR(20)    NOT NULL DEFAULT 'HOAN_TAT',
    maNhanVien   VARCHAR(10)    NOT NULL,
    CONSTRAINT FK_PhieuXuat_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT CK_PhieuXuat_TrangThai CHECK (trangThai IN ('HOAN_TAT'))
);
GO
CREATE TABLE ChiTietPhieuXuat (
    maPhieuXuat  VARCHAR(10)  NOT NULL,
    maLo         VARCHAR(10)  NOT NULL,
    soLuong      INT          NOT NULL,
    CONSTRAINT PK_ChiTietPhieuXuat PRIMARY KEY (maPhieuXuat, maLo),
    CONSTRAINT FK_CTPX_Phieu FOREIGN KEY (maPhieuXuat) REFERENCES PhieuXuatKho(maPhieuXuat),
    CONSTRAINT FK_CTPX_Lo FOREIGN KEY (maLo) REFERENCES LoHang(maLo),
    CONSTRAINT CK_CTPX_SoLuong CHECK (soLuong > 0)
);
GO
CREATE TABLE PhieuNhapKho (
    maPhieuNhap     VARCHAR(10)    NOT NULL CONSTRAINT PK_PhieuNhapKho PRIMARY KEY,
    ngayNhap        DATETIME2(0)   NOT NULL,
    trangThai       VARCHAR(20)    NOT NULL DEFAULT 'HOAN_TAT',
    loaiNhap        VARCHAR(10)    NOT NULL DEFAULT 'NHAP_NCC',
    maNhaCungCap    VARCHAR(10)    NULL,
    maPhieuXuatGoc  VARCHAR(10)    NULL,
    maNhanVien      VARCHAR(10)    NOT NULL,
    ghiChu          NVARCHAR(300)  NULL,
    CONSTRAINT FK_PhieuNhap_NCC FOREIGN KEY (maNhaCungCap) REFERENCES NhaCungCap(maNhaCungCap),
    CONSTRAINT FK_PhieuNhap_XuatGoc FOREIGN KEY (maPhieuXuatGoc) REFERENCES PhieuXuatKho(maPhieuXuat),
    CONSTRAINT FK_PhieuNhap_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT CK_PhieuNhap_TrangThai CHECK (trangThai IN ('HOAN_TAT')),
    CONSTRAINT CK_PhieuNhap_Loai CHECK ((loaiNhap = 'NHAP_NCC' AND maNhaCungCap IS NOT NULL AND maPhieuXuatGoc IS NULL) OR (loaiNhap = 'HOAN_TRA' AND maPhieuXuatGoc IS NOT NULL))
);
GO
CREATE TABLE ChiTietPhieuNhap (
    maPhieuNhap  VARCHAR(10)    NOT NULL,
    maLo         VARCHAR(10)    NOT NULL,
    soLuong      INT            NOT NULL,
    donGiaNhap   DECIMAL(18,0)  NOT NULL DEFAULT 0,
    CONSTRAINT PK_ChiTietPhieuNhap PRIMARY KEY (maPhieuNhap, maLo),
    CONSTRAINT FK_CTPN_Phieu FOREIGN KEY (maPhieuNhap) REFERENCES PhieuNhapKho(maPhieuNhap),
    CONSTRAINT FK_CTPN_Lo FOREIGN KEY (maLo) REFERENCES LoHang(maLo),
    CONSTRAINT CK_CTPN_SoLuong CHECK (soLuong > 0 AND donGiaNhap >= 0)
);
GO
/* [BỔ SUNG] UC28 Kiểm kê + UC35 Hủy/hao hụt + Figma QL-10 (duyệt điều chỉnh) */
CREATE TABLE PhieuDieuChinhKho (
    maPhieu          VARCHAR(10)    NOT NULL CONSTRAINT PK_PhieuDieuChinhKho PRIMARY KEY,
    loai             VARCHAR(10)    NOT NULL,
    maTaiKhoanTao    VARCHAR(10)    NOT NULL,
    thoiGianTao      DATETIME2(0)   NOT NULL,
    lyDo             NVARCHAR(500)  NOT NULL,
    trangThai        VARCHAR(10)    NOT NULL DEFAULT 'CHO_DUYET',
    maTaiKhoanDuyet  VARCHAR(10)    NULL,
    thoiGianDuyet    DATETIME2(0)   NULL,
    ghiChuDuyet      NVARCHAR(500)  NULL,
    CONSTRAINT FK_PDC_TKTao FOREIGN KEY (maTaiKhoanTao) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT FK_PDC_TKDuyet FOREIGN KEY (maTaiKhoanDuyet) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT CK_PDC_Loai CHECK (loai IN ('KIEM_KE','HU_HONG','HET_HAN')),
    CONSTRAINT CK_PDC_TrangThai CHECK (trangThai IN ('CHO_DUYET','DA_DUYET','TU_CHOI')),
    CONSTRAINT CK_PDC_KhongTuDuyet CHECK (maTaiKhoanDuyet IS NULL OR maTaiKhoanDuyet <> maTaiKhoanTao),
    CONSTRAINT CK_PDC_TuChoi CHECK (trangThai <> 'TU_CHOI' OR ghiChuDuyet IS NOT NULL),
    CONSTRAINT CK_PDC_DaXuLy CHECK (trangThai = 'CHO_DUYET' OR maTaiKhoanDuyet IS NOT NULL)
);
GO
CREATE TABLE ChiTietDieuChinhKho (
    maPhieu          VARCHAR(10)  NOT NULL,
    maLo             VARCHAR(10)  NOT NULL,
    soLuongHeThong   INT          NOT NULL,
    soLuongThucTe    INT          NOT NULL,
    CONSTRAINT PK_ChiTietDieuChinhKho PRIMARY KEY (maPhieu, maLo),
    CONSTRAINT FK_CTDC_Phieu FOREIGN KEY (maPhieu) REFERENCES PhieuDieuChinhKho(maPhieu),
    CONSTRAINT FK_CTDC_Lo FOREIGN KEY (maLo) REFERENCES LoHang(maLo),
    CONSTRAINT CK_CTDC_SoLuong CHECK (soLuongHeThong >= 0 AND soLuongThucTe >= 0 AND soLuongThucTe <> soLuongHeThong)
);
GO
/* [BỔ SUNG] UC34 Kiểm tra hộp thuốc cấp cứu — danh mục cấu hình được */
CREATE TABLE DanhMucCapCuu (
    maMuc              VARCHAR(10)    NOT NULL CONSTRAINT PK_DanhMucCapCuu PRIMARY KEY,
    tenMuc             NVARCHAR(150)  NOT NULL,
    donViTinh          NVARCHAR(30)   NOT NULL,
    soLuongToiThieu    INT            NOT NULL,
    soLuongHienCo      INT            NOT NULL DEFAULT 0,
    hanSuDungGanNhat   DATE           NULL,
    maThuoc            VARCHAR(10)    NULL,
    maVatTu            VARCHAR(10)    NULL,
    dangApDung         BIT            NOT NULL DEFAULT 1,
    laDuLieuMau        BIT            NOT NULL DEFAULT 0,
    ghiChu             NVARCHAR(300)  NULL,
    CONSTRAINT FK_DMCC_Thuoc FOREIGN KEY (maThuoc) REFERENCES Thuoc(maThuoc),
    CONSTRAINT FK_DMCC_VatTu FOREIGN KEY (maVatTu) REFERENCES VatTu(maVatTu),
    CONSTRAINT CK_DMCC_SoLuong CHECK (soLuongToiThieu > 0 AND soLuongHienCo >= 0),
    CONSTRAINT CK_DMCC_LienKet CHECK (maThuoc IS NULL OR maVatTu IS NULL)
);
GO
CREATE TABLE KiemTraCapCuu (
    maKiemTra   VARCHAR(10)    NOT NULL CONSTRAINT PK_KiemTraCapCuu PRIMARY KEY,
    thoiGian    DATETIME2(0)   NOT NULL,
    maNhanVien  VARCHAR(10)    NOT NULL,
    ketQua      VARCHAR(15)    NOT NULL,
    ghiChu      NVARCHAR(500)  NULL,
    CONSTRAINT FK_KTCC_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT CK_KTCC_KetQua CHECK (ketQua IN ('DAT','CAN_BO_SUNG'))
);
GO
CREATE TABLE ChiTietKiemTraCapCuu (
    maKiemTra      VARCHAR(10)  NOT NULL,
    maMuc          VARCHAR(10)  NOT NULL,
    soLuongThucTe  INT          NOT NULL,
    hanSuDung      DATE         NULL,
    dat            BIT          NOT NULL,
    CONSTRAINT PK_ChiTietKiemTraCapCuu PRIMARY KEY (maKiemTra, maMuc),
    CONSTRAINT FK_CTKTCC_KiemTra FOREIGN KEY (maKiemTra) REFERENCES KiemTraCapCuu(maKiemTra),
    CONSTRAINT FK_CTKTCC_Muc FOREIGN KEY (maMuc) REFERENCES DanhMucCapCuu(maMuc),
    CONSTRAINT CK_CTKTCC_SoLuong CHECK (soLuongThucTe >= 0)
);
GO

/* ---------- Hóa đơn, thanh toán ---------- */
CREATE TABLE HoaDon (
    maHoaDon     VARCHAR(10)   NOT NULL CONSTRAINT PK_HoaDon PRIMARY KEY,
    maNhanVien   VARCHAR(10)   NOT NULL,
    maBenhNhan   VARCHAR(10)   NOT NULL,
    maPhieuKham  VARCHAR(10)   NULL,
    ngayLap      DATETIME2(0)  NOT NULL,
    trangThai    VARCHAR(20)   NOT NULL DEFAULT 'CHUA_THANH_TOAN',
    CONSTRAINT FK_HoaDon_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT FK_HoaDon_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(maBenhNhan),
    CONSTRAINT FK_HoaDon_PhieuKham FOREIGN KEY (maPhieuKham) REFERENCES PhieuKham(maPhieuKham),
    CONSTRAINT CK_HoaDon_TrangThai CHECK (trangThai IN ('CHUA_THANH_TOAN','THANH_TOAN_MOT_PHAN','DA_THANH_TOAN','DA_HUY'))
);
GO
CREATE INDEX IX_HoaDon_BenhNhan ON HoaDon(maBenhNhan);
GO
CREATE TABLE ChiTietHoaDon (
    maChiTiet     INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ChiTietHoaDon PRIMARY KEY,
    maHoaDon      VARCHAR(10)    NOT NULL,
    maDichVu      VARCHAR(10)    NULL,
    maThuoc       VARCHAR(10)    NULL,
    maVatTu       VARCHAR(10)    NULL,
    soLuong       INT            NOT NULL,
    donGiaApDung  DECIMAL(18,0)  NOT NULL,
    CONSTRAINT FK_CTHD_HoaDon FOREIGN KEY (maHoaDon) REFERENCES HoaDon(maHoaDon),
    CONSTRAINT FK_CTHD_DichVu FOREIGN KEY (maDichVu) REFERENCES DichVu(maDichVu),
    CONSTRAINT FK_CTHD_Thuoc FOREIGN KEY (maThuoc) REFERENCES Thuoc(maThuoc),
    CONSTRAINT FK_CTHD_VatTu FOREIGN KEY (maVatTu) REFERENCES VatTu(maVatTu),
    CONSTRAINT CK_CTHD_MotLoai CHECK ((maDichVu IS NOT NULL AND maThuoc IS NULL AND maVatTu IS NULL) OR (maDichVu IS NULL AND maThuoc IS NOT NULL AND maVatTu IS NULL) OR (maDichVu IS NULL AND maThuoc IS NULL AND maVatTu IS NOT NULL)),
    CONSTRAINT CK_CTHD_SoLuong CHECK (soLuong > 0 AND donGiaApDung >= 0)
);
GO
CREATE TABLE ThanhToan (
    maThanhToan    VARCHAR(10)    NOT NULL CONSTRAINT PK_ThanhToan PRIMARY KEY,
    maHoaDon       VARCHAR(10)    NOT NULL,
    thoiGian       DATETIME2(0)   NOT NULL,
    soTien         DECIMAL(18,0)  NOT NULL,
    phuongThuc     VARCHAR(15)    NOT NULL,
    maGiaoDich     VARCHAR(50)    NULL,
    tienKhachDua   DECIMAL(18,0)  NULL,
    trangThai      VARCHAR(15)    NOT NULL DEFAULT 'THANH_CONG',
    maNhanVien     VARCHAR(10)    NOT NULL,
    maTaiKhoanHuy  VARCHAR(10)    NULL,
    thoiGianHuy    DATETIME2(0)   NULL,
    lyDoHuy        NVARCHAR(300)  NULL,
    CONSTRAINT FK_ThanhToan_HoaDon FOREIGN KEY (maHoaDon) REFERENCES HoaDon(maHoaDon),
    CONSTRAINT FK_ThanhToan_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT FK_ThanhToan_TKHuy FOREIGN KEY (maTaiKhoanHuy) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT CK_ThanhToan_SoTien CHECK (soTien > 0),
    CONSTRAINT CK_ThanhToan_PhuongThuc CHECK (phuongThuc IN ('TIEN_MAT','CHUYEN_KHOAN','THE_POS')),
    CONSTRAINT CK_ThanhToan_MaGD CHECK (phuongThuc = 'TIEN_MAT' OR maGiaoDich IS NOT NULL),
    CONSTRAINT CK_ThanhToan_TienMat CHECK (phuongThuc <> 'TIEN_MAT' OR tienKhachDua >= soTien),
    CONSTRAINT CK_ThanhToan_TrangThai CHECK (trangThai IN ('THANH_CONG','DA_HUY')),
    CONSTRAINT CK_ThanhToan_Huy CHECK (trangThai <> 'DA_HUY' OR (lyDoHuy IS NOT NULL AND maTaiKhoanHuy IS NOT NULL))
);
GO
CREATE INDEX IX_ThanhToan_HoaDon ON ThanhToan(maHoaDon);
GO
/* [BỔ SUNG] Figma QL-08/09: yêu cầu hủy hóa đơn → duyệt; lưu người tạo / người duyệt */
CREATE TABLE YeuCauHuyHoaDon (
    maYeuCau         VARCHAR(10)    NOT NULL CONSTRAINT PK_YeuCauHuyHoaDon PRIMARY KEY,
    maHoaDon         VARCHAR(10)    NOT NULL,
    maTaiKhoanTao    VARCHAR(10)    NOT NULL,
    thoiGianTao      DATETIME2(0)   NOT NULL,
    lyDo             NVARCHAR(500)  NOT NULL,
    trangThai        VARCHAR(10)    NOT NULL DEFAULT 'CHO_DUYET',
    maTaiKhoanDuyet  VARCHAR(10)    NULL,
    thoiGianDuyet    DATETIME2(0)   NULL,
    lyDoDuyet        NVARCHAR(500)  NULL,
    CONSTRAINT FK_YCHuy_HoaDon FOREIGN KEY (maHoaDon) REFERENCES HoaDon(maHoaDon),
    CONSTRAINT FK_YCHuy_TKTao FOREIGN KEY (maTaiKhoanTao) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT FK_YCHuy_TKDuyet FOREIGN KEY (maTaiKhoanDuyet) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT CK_YCHuy_TrangThai CHECK (trangThai IN ('CHO_DUYET','DA_DUYET','TU_CHOI')),
    CONSTRAINT CK_YCHuy_KhongTuDuyet CHECK (maTaiKhoanDuyet IS NULL OR maTaiKhoanDuyet <> maTaiKhoanTao),
    CONSTRAINT CK_YCHuy_DaXuLy CHECK (trangThai = 'CHO_DUYET' OR (maTaiKhoanDuyet IS NOT NULL AND lyDoDuyet IS NOT NULL))
);
GO

/* ---------- [BỔ SUNG] Nhật ký thao tác: ai làm gì, ai chịu trách nhiệm ---------- */
CREATE TABLE NhatKyThaoTac (
    maNhatKy                BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_NhatKyThaoTac PRIMARY KEY,
    thoiGian                DATETIME2(0)    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    maTaiKhoan              VARCHAR(10)     NULL,
    chucNang                NVARCHAR(50)    NOT NULL,
    hanhDong                NVARCHAR(200)   NOT NULL,
    doiTuong                VARCHAR(50)     NULL,
    noiDung                 NVARCHAR(1000)  NULL,
    maNguoiChiuTrachNhiem   VARCHAR(10)     NULL,
    CONSTRAINT FK_NhatKy_TaiKhoan FOREIGN KEY (maTaiKhoan) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT FK_NhatKy_NguoiChiuTN FOREIGN KEY (maNguoiChiuTrachNhiem) REFERENCES NhanVien(maNhanVien)
);
GO
CREATE INDEX IX_NhatKy_DoiTuong ON NhatKyThaoTac(doiTuong);
GO
CREATE INDEX IX_NhatKy_ThoiGian ON NhatKyThaoTac(thoiGian);
GO

/* ---------- Chỉ mục lọc chỉ dùng trên SQL Server (Derby không hỗ trợ) ---------- */
/* Một nhân viên không có 2 phân công còn hiệu lực trong cùng ca, cùng ngày */
CREATE UNIQUE INDEX UX_PhanCong_HieuLuc ON PhanCong(maNhanVien, ngayLam, maCa) WHERE trangThai <> 'DA_HUY';
GO
/* Một hóa đơn chỉ có một yêu cầu hủy đang chờ duyệt */
CREATE UNIQUE INDEX UX_YCHuy_ChoDuyet ON YeuCauHuyHoaDon(maHoaDon) WHERE trangThai = 'CHO_DUYET';
GO
/* Một lượt khám chỉ có một hóa đơn còn hiệu lực */
CREATE UNIQUE INDEX UX_HoaDon_PhieuKham ON HoaDon(maPhieuKham) WHERE maPhieuKham IS NOT NULL AND trangThai <> 'DA_HUY';
GO
/* Một bác sĩ không có 2 lịch hẹn đang mở cùng thời điểm */
CREATE UNIQUE INDEX UX_LichHen_KhungGio ON LichHen(maNhanVien, thoiGianHen) WHERE trangThai IN ('CHO_XAC_NHAN','DA_XAC_NHAN','DA_DEN');
GO
