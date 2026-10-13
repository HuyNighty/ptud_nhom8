CREATE TABLE BoDemMa (
    loai        VARCHAR(10)  NOT NULL CONSTRAINT PK_BoDemMa PRIMARY KEY,
    tienTo      VARCHAR(10)  NOT NULL,
    doDai       INT          NOT NULL,
    giaTri      INT          NOT NULL DEFAULT 0
);
CREATE TABLE ThamSoHeThong (
    ten         VARCHAR(50)    NOT NULL CONSTRAINT PK_ThamSoHeThong PRIMARY KEY,
    giaTri      VARCHAR(100)   NOT NULL,
    moTa        VARCHAR(300)
);
CREATE TABLE NhanVien (
    maNhanVien    VARCHAR(10)    NOT NULL CONSTRAINT PK_NhanVien PRIMARY KEY,
    hoTen         VARCHAR(100)  NOT NULL,
    soDienThoai   VARCHAR(15)    NOT NULL CONSTRAINT UQ_NhanVien_SDT UNIQUE,
    chucDanh      VARCHAR(10)    NOT NULL,
    trangThai     VARCHAR(20)    NOT NULL DEFAULT 'DANG_LAM_VIEC',
    ngaySinh      DATE,
    gioiTinh      VARCHAR(5),
    email         VARCHAR(100),
    ngayVaoLam    DATE,
    chuyenMon     VARCHAR(200),
    ngayNghiViec  DATE,
    CONSTRAINT CK_NhanVien_ChucDanh CHECK (chucDanh IN ('AD','LT','BS','YT','QL','KHO')),
    CONSTRAINT CK_NhanVien_TrangThai CHECK (trangThai IN ('DANG_LAM_VIEC','NGHI_VIEC')),
    CONSTRAINT CK_NhanVien_GioiTinh CHECK (gioiTinh IS NULL OR gioiTinh IN ('NAM','NU','KHAC')),
    CONSTRAINT CK_NhanVien_NghiViec CHECK ((trangThai = 'NGHI_VIEC' AND ngayNghiViec IS NOT NULL) OR (trangThai = 'DANG_LAM_VIEC' AND ngayNghiViec IS NULL))
);
CREATE TABLE TaiKhoan (
    maTaiKhoan       VARCHAR(10)    NOT NULL CONSTRAINT PK_TaiKhoan PRIMARY KEY,
    tenDangNhap      VARCHAR(50)    NOT NULL CONSTRAINT UQ_TaiKhoan_Ten UNIQUE,
    matKhau          VARCHAR(200)   NOT NULL,
    trangThai        VARCHAR(20)    NOT NULL DEFAULT 'HOAT_DONG',
    vaiTro           VARCHAR(10)    NOT NULL,
    maNhanVien       VARCHAR(10)    NOT NULL CONSTRAINT UQ_TaiKhoan_NhanVien UNIQUE,
    soLanSai         INT            NOT NULL DEFAULT 0,
    lanDangNhapCuoi  TIMESTAMP,
    lyDoKhoa         VARCHAR(300),
    CONSTRAINT FK_TaiKhoan_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT CK_TaiKhoan_TrangThai CHECK (trangThai IN ('HOAT_DONG','BI_KHOA','VO_HIEU')),
    CONSTRAINT CK_TaiKhoan_VaiTro CHECK (vaiTro IN ('AD','LT','BS','YT','QL','KHO')),
    CONSTRAINT CK_TaiKhoan_SoLanSai CHECK (soLanSai >= 0)
);
CREATE TABLE CaLamViec (
    maCa        VARCHAR(10)    NOT NULL CONSTRAINT PK_CaLamViec PRIMARY KEY,
    tenCa       VARCHAR(50)   NOT NULL,
    gioBatDau   TIME        NOT NULL,
    gioKetThuc  TIME        NOT NULL,
    soBacSi     INT            NOT NULL DEFAULT 0,
    soYTa       INT            NOT NULL DEFAULT 0,
    soLeTan     INT            NOT NULL DEFAULT 0,
    CONSTRAINT CK_CaLamViec_Gio CHECK (gioKetThuc > gioBatDau),
    CONSTRAINT CK_CaLamViec_DinhMuc CHECK (soBacSi >= 0 AND soYTa >= 0 AND soLeTan >= 0)
);
CREATE TABLE PhanCong (
    maPhanCong     VARCHAR(10)    NOT NULL CONSTRAINT PK_PhanCong PRIMARY KEY,
    maNhanVien     VARCHAR(10)    NOT NULL,
    maCa           VARCHAR(10)    NOT NULL,
    ngayLam        DATE           NOT NULL,
    nhiemVu        VARCHAR(200)  NOT NULL,
    trangThai      VARCHAR(20)    NOT NULL DEFAULT 'CHO_HIEU_LUC',
    lyDoHuy        VARCHAR(300),
    maPhanCongGoc  VARCHAR(10),
    CONSTRAINT FK_PhanCong_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT FK_PhanCong_Ca FOREIGN KEY (maCa) REFERENCES CaLamViec(maCa),
    CONSTRAINT FK_PhanCong_Goc FOREIGN KEY (maPhanCongGoc) REFERENCES PhanCong(maPhanCong),
    CONSTRAINT CK_PhanCong_TrangThai CHECK (trangThai IN ('CHO_HIEU_LUC','DANG_THUC_HIEN','HOAN_THANH','DA_HUY')),
    CONSTRAINT CK_PhanCong_Huy CHECK (trangThai <> 'DA_HUY' OR lyDoHuy IS NOT NULL)
);
CREATE TABLE BanGiaoCa (
    maBanGiao          VARCHAR(10)     NOT NULL CONSTRAINT PK_BanGiaoCa PRIMARY KEY,
    thoiGian           TIMESTAMP    NOT NULL,
    noiDung            VARCHAR(1000)  NOT NULL,
    trangThai          VARCHAR(20)     NOT NULL DEFAULT 'CHO_BAN_GIAO',
    ngay               DATE            NOT NULL,
    maNguoiGiao        VARCHAR(10)     NOT NULL,
    maNguoiNhan        VARCHAR(10)     NOT NULL,
    maCaGiao           VARCHAR(10)     NOT NULL,
    maCaNhan           VARCHAR(10)     NOT NULL,
    coChenhLech        SMALLINT             NOT NULL DEFAULT 0,
    maTaiKhoanTao      VARCHAR(10)     NOT NULL,
    maTaiKhoanXacNhan  VARCHAR(10),
    thoiGianXacNhan    TIMESTAMP,
    ghiChuXacNhan      VARCHAR(500),
    maTaiKhoanHuy      VARCHAR(10),
    thoiGianHuy        TIMESTAMP,
    lyDoHuy            VARCHAR(300),
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
CREATE TABLE DanhGiaNhanSu (
    maDanhGia       VARCHAR(10)     NOT NULL CONSTRAINT PK_DanhGiaNhanSu PRIMARY KEY,
    maNhanVien      VARCHAR(10)     NOT NULL,
    maNguoiDanhGia  VARCHAR(10)     NOT NULL,
    kyDanhGia       VARCHAR(10)     NOT NULL,
    ngayDanhGia     DATE            NOT NULL,
    diem            DECIMAL(3,2),
    nhanXet         VARCHAR(1000),
    trangThai       VARCHAR(10)     NOT NULL DEFAULT 'NHAP',
    CONSTRAINT FK_DanhGia_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT FK_DanhGia_NguoiDanhGia FOREIGN KEY (maNguoiDanhGia) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT UQ_DanhGia_Ky UNIQUE (maNhanVien, kyDanhGia),
    CONSTRAINT CK_DanhGia_TrangThai CHECK (trangThai IN ('NHAP','DA_GUI')),
    CONSTRAINT CK_DanhGia_KhacNguoi CHECK (maNhanVien <> maNguoiDanhGia),
    CONSTRAINT CK_DanhGia_Diem CHECK (diem IS NULL OR (diem >= 1 AND diem <= 5))
);
CREATE TABLE ChiTietDanhGia (
    maDanhGia   VARCHAR(10)    NOT NULL,
    thuTu       INT            NOT NULL,
    tieuChi     VARCHAR(100)  NOT NULL,
    diem        INT            NOT NULL,
    CONSTRAINT PK_ChiTietDanhGia PRIMARY KEY (maDanhGia, thuTu),
    CONSTRAINT FK_CTDanhGia_DanhGia FOREIGN KEY (maDanhGia) REFERENCES DanhGiaNhanSu(maDanhGia),
    CONSTRAINT CK_CTDanhGia_Diem CHECK (diem BETWEEN 1 AND 5)
);
CREATE TABLE BenhNhan (
    maBenhNhan     VARCHAR(10)    NOT NULL CONSTRAINT PK_BenhNhan PRIMARY KEY,
    hoTen          VARCHAR(100)  NOT NULL,
    ngaySinh       DATE           NOT NULL,
    gioiTinh       VARCHAR(5)     NOT NULL,
    soDienThoai    VARCHAR(15)    NOT NULL,
    diaChi         VARCHAR(255),
    nguoiLienHe    VARCHAR(200),
    ngayTao        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    maTaiKhoanTao  VARCHAR(10),
    CONSTRAINT FK_BenhNhan_TaiKhoan FOREIGN KEY (maTaiKhoanTao) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT CK_BenhNhan_GioiTinh CHECK (gioiTinh IN ('NAM','NU','KHAC'))
);
CREATE INDEX IX_BenhNhan_SDT ON BenhNhan(soDienThoai);
CREATE TABLE HoSoBenhAn (
    maHoSo      VARCHAR(10)    NOT NULL CONSTRAINT PK_HoSoBenhAn PRIMARY KEY,
    maBenhNhan  VARCHAR(10)    NOT NULL CONSTRAINT UQ_HoSo_BenhNhan UNIQUE,
    ngayLap     DATE           NOT NULL,
    tienSuBenh  VARCHAR(500),
    diUng       VARCHAR(300),
    CONSTRAINT FK_HoSo_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(maBenhNhan)
);
CREATE TABLE DichVu (
    maDichVu      VARCHAR(10)    NOT NULL CONSTRAINT PK_DichVu PRIMARY KEY,
    tenDichVu     VARCHAR(100)  NOT NULL,
    donGia        DECIMAL(18,0)  NOT NULL,
    moTa          VARCHAR(300),
    laCanLamSang  SMALLINT            NOT NULL DEFAULT 0,
    dangApDung    SMALLINT            NOT NULL DEFAULT 1,
    CONSTRAINT CK_DichVu_DonGia CHECK (donGia >= 0)
);
CREATE TABLE KeHoachDieuTri (
    maKeHoach    VARCHAR(10)    NOT NULL CONSTRAINT PK_KeHoachDieuTri PRIMARY KEY,
    maHoSo       VARCHAR(10)    NOT NULL,
    maPhieuKham  VARCHAR(10),
    ngayLap      DATE           NOT NULL,
    mucTieu      VARCHAR(300)  NOT NULL,
    trangThai    VARCHAR(20)    NOT NULL DEFAULT 'DANG_THUC_HIEN',
    maNhanVien   VARCHAR(10)    NOT NULL,
    lyDoHuy      VARCHAR(300),
    CONSTRAINT FK_KeHoach_HoSo FOREIGN KEY (maHoSo) REFERENCES HoSoBenhAn(maHoSo),
    CONSTRAINT FK_KeHoach_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT CK_KeHoach_TrangThai CHECK (trangThai IN ('DANG_THUC_HIEN','HOAN_THANH','DA_HUY')),
    CONSTRAINT CK_KeHoach_Huy CHECK (trangThai <> 'DA_HUY' OR lyDoHuy IS NOT NULL)
);
CREATE TABLE LichHen (
    maLichHen      VARCHAR(10)    NOT NULL CONSTRAINT PK_LichHen PRIMARY KEY,
    maBenhNhan     VARCHAR(10)    NOT NULL,
    maNhanVien     VARCHAR(10)    NOT NULL,
    thoiGianHen    TIMESTAMP   NOT NULL,
    lyDoHen        VARCHAR(300),
    trangThai      VARCHAR(20)    NOT NULL DEFAULT 'CHO_XAC_NHAN',
    maLichHenGoc   VARCHAR(10),
    maKeHoach      VARCHAR(10),
    lyDoHuy        VARCHAR(300),
    maTaiKhoanTao  VARCHAR(10),
    ngayTao        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_LichHen_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(maBenhNhan),
    CONSTRAINT FK_LichHen_BacSi FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT FK_LichHen_Goc FOREIGN KEY (maLichHenGoc) REFERENCES LichHen(maLichHen),
    CONSTRAINT FK_LichHen_KeHoach FOREIGN KEY (maKeHoach) REFERENCES KeHoachDieuTri(maKeHoach),
    CONSTRAINT FK_LichHen_TaiKhoan FOREIGN KEY (maTaiKhoanTao) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT CK_LichHen_TrangThai CHECK (trangThai IN ('CHO_XAC_NHAN','DA_XAC_NHAN','DA_DEN','HOAN_THANH','VANG_MAT','DA_HUY','DA_DOI_LICH')),
    CONSTRAINT CK_LichHen_Huy CHECK (trangThai <> 'DA_HUY' OR lyDoHuy IS NOT NULL)
);
CREATE INDEX IX_LichHen_BacSi_ThoiGian ON LichHen(maNhanVien, thoiGianHen);
CREATE INDEX IX_LichHen_ThoiGian ON LichHen(thoiGianHen);
CREATE TABLE PhieuKham (
    maPhieuKham       VARCHAR(10)     NOT NULL CONSTRAINT PK_PhieuKham PRIMARY KEY,
    maBenhNhan        VARCHAR(10)     NOT NULL,
    maHoSo            VARCHAR(10)     NOT NULL,
    maNhanVien        VARCHAR(10)     NOT NULL,
    maLichHen         VARCHAR(10),
    thoiGianTiepNhan  TIMESTAMP    NOT NULL,
    trieuChung        VARCHAR(500),
    chanDoan          VARCHAR(1000),
    ketLuan           VARCHAR(1000),
    trangThai         VARCHAR(20)     NOT NULL DEFAULT 'CHO_KHAM',
    lyDoHuy           VARCHAR(300),
    CONSTRAINT FK_PhieuKham_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(maBenhNhan),
    CONSTRAINT FK_PhieuKham_HoSo FOREIGN KEY (maHoSo) REFERENCES HoSoBenhAn(maHoSo),
    CONSTRAINT FK_PhieuKham_BacSi FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT FK_PhieuKham_LichHen FOREIGN KEY (maLichHen) REFERENCES LichHen(maLichHen),
    CONSTRAINT CK_PhieuKham_TrangThai CHECK (trangThai IN ('CHO_KHAM','DANG_KHAM','DA_KET_LUAN','DA_HUY')),
    CONSTRAINT CK_PhieuKham_KetLuan CHECK (trangThai <> 'DA_KET_LUAN' OR chanDoan IS NOT NULL),
    CONSTRAINT CK_PhieuKham_Huy CHECK (trangThai <> 'DA_HUY' OR lyDoHuy IS NOT NULL)
);
CREATE INDEX IX_PhieuKham_ThoiGian ON PhieuKham(thoiGianTiepNhan);
ALTER TABLE KeHoachDieuTri ADD CONSTRAINT FK_KeHoach_PhieuKham FOREIGN KEY (maPhieuKham) REFERENCES PhieuKham(maPhieuKham);
CREATE TABLE ChiTietDieuTri (
    maChiTiet           INT NOT NULL GENERATED BY DEFAULT AS IDENTITY CONSTRAINT PK_ChiTietDieuTri PRIMARY KEY,
    maKeHoach           VARCHAR(10)    NOT NULL,
    maDichVu            VARCHAR(10)    NOT NULL,
    viTriRang           VARCHAR(50)   NOT NULL,
    ngayDuKien          DATE           NOT NULL,
    ngayThucHien        DATE,
    ketQua              VARCHAR(500),
    trangThai           VARCHAR(20)    NOT NULL DEFAULT 'CHUA_THUC_HIEN',
    maNhanVienThucHien  VARCHAR(10),
    ghiChu              VARCHAR(300),
    CONSTRAINT FK_CTDieuTri_KeHoach FOREIGN KEY (maKeHoach) REFERENCES KeHoachDieuTri(maKeHoach),
    CONSTRAINT FK_CTDieuTri_DichVu FOREIGN KEY (maDichVu) REFERENCES DichVu(maDichVu),
    CONSTRAINT FK_CTDieuTri_BacSi FOREIGN KEY (maNhanVienThucHien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT CK_CTDieuTri_TrangThai CHECK (trangThai IN ('CHUA_THUC_HIEN','DA_THUC_HIEN','DA_HUY')),
    CONSTRAINT CK_CTDieuTri_ThucHien CHECK (trangThai <> 'DA_THUC_HIEN' OR (ngayThucHien IS NOT NULL AND ketQua IS NOT NULL AND maNhanVienThucHien IS NOT NULL))
);
CREATE TABLE ChiDinhCanLamSang (
    maChiDinh        VARCHAR(10)    NOT NULL CONSTRAINT PK_ChiDinhCanLamSang PRIMARY KEY,
    maPhieuKham      VARCHAR(10)    NOT NULL,
    maDichVu         VARCHAR(10)    NOT NULL,
    yeuCau           VARCHAR(300)  NOT NULL,
    ketQua           VARCHAR(1000),
    maNhanVien       VARCHAR(10)    NOT NULL,
    thoiGianChiDinh  TIMESTAMP   NOT NULL,
    thoiGianKetQua   TIMESTAMP,
    trangThai        VARCHAR(20)    NOT NULL DEFAULT 'CHO_THUC_HIEN',
    CONSTRAINT FK_CLS_PhieuKham FOREIGN KEY (maPhieuKham) REFERENCES PhieuKham(maPhieuKham),
    CONSTRAINT FK_CLS_DichVu FOREIGN KEY (maDichVu) REFERENCES DichVu(maDichVu),
    CONSTRAINT FK_CLS_BacSi FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT CK_CLS_TrangThai CHECK (trangThai IN ('CHO_THUC_HIEN','DA_CO_KET_QUA')),
    CONSTRAINT CK_CLS_KetQua CHECK (trangThai <> 'DA_CO_KET_QUA' OR ketQua IS NOT NULL)
);
CREATE TABLE Thuoc (
    maThuoc          VARCHAR(10)    NOT NULL CONSTRAINT PK_Thuoc PRIMARY KEY,
    tenThuoc         VARCHAR(150)  NOT NULL,
    donViTinh        VARCHAR(30)   NOT NULL,
    kiemSoatDacBiet  SMALLINT            NOT NULL DEFAULT 0,
    hamLuong         VARCHAR(50),
    hoatChat         VARCHAR(150),
    dinhMucToiThieu  INT            NOT NULL DEFAULT 0,
    daDatHang        SMALLINT            NOT NULL DEFAULT 0,
    CONSTRAINT CK_Thuoc_DinhMuc CHECK (dinhMucToiThieu >= 0)
);
CREATE TABLE VatTu (
    maVatTu          VARCHAR(10)    NOT NULL CONSTRAINT PK_VatTu PRIMARY KEY,
    tenVatTu         VARCHAR(150)  NOT NULL,
    donViTinh        VARCHAR(30)   NOT NULL,
    moTa             VARCHAR(300),
    dinhMucToiThieu  INT            NOT NULL DEFAULT 0,
    daDatHang        SMALLINT            NOT NULL DEFAULT 0,
    CONSTRAINT CK_VatTu_DinhMuc CHECK (dinhMucToiThieu >= 0)
);
CREATE TABLE DonThuoc (
    maDonThuoc   VARCHAR(10)    NOT NULL CONSTRAINT PK_DonThuoc PRIMARY KEY,
    maPhieuKham  VARCHAR(10)    NOT NULL CONSTRAINT UQ_DonThuoc_PhieuKham UNIQUE,
    maBenhNhan   VARCHAR(10)    NOT NULL,
    maNhanVien   VARCHAR(10)    NOT NULL,
    ngayKe       DATE           NOT NULL,
    loiDan       VARCHAR(500),
    CONSTRAINT FK_DonThuoc_PhieuKham FOREIGN KEY (maPhieuKham) REFERENCES PhieuKham(maPhieuKham),
    CONSTRAINT FK_DonThuoc_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(maBenhNhan),
    CONSTRAINT FK_DonThuoc_BacSi FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien)
);
CREATE TABLE ChiTietDonThuoc (
    maDonThuoc  VARCHAR(10)    NOT NULL,
    maThuoc     VARCHAR(10)    NOT NULL,
    soLuong     INT            NOT NULL,
    lieuDung    VARCHAR(100)  NOT NULL,
    cachDung    VARCHAR(200)  NOT NULL,
    soNgayDung  INT            NOT NULL,
    CONSTRAINT PK_ChiTietDonThuoc PRIMARY KEY (maDonThuoc, maThuoc),
    CONSTRAINT FK_CTDonThuoc_Don FOREIGN KEY (maDonThuoc) REFERENCES DonThuoc(maDonThuoc),
    CONSTRAINT FK_CTDonThuoc_Thuoc FOREIGN KEY (maThuoc) REFERENCES Thuoc(maThuoc),
    CONSTRAINT CK_CTDonThuoc_SoLuong CHECK (soLuong > 0 AND soNgayDung > 0)
);
CREATE TABLE NhaCungCap (
    maNhaCungCap   VARCHAR(10)    NOT NULL CONSTRAINT PK_NhaCungCap PRIMARY KEY,
    tenNhaCungCap  VARCHAR(150)  NOT NULL,
    diaChi         VARCHAR(255),
    soDienThoai    VARCHAR(20)    NOT NULL,
    email          VARCHAR(100),
    trangThai      VARCHAR(20)    NOT NULL DEFAULT 'DANG_HOP_TAC',
    CONSTRAINT CK_NCC_TrangThai CHECK (trangThai IN ('DANG_HOP_TAC','NGUNG_HOP_TAC'))
);
CREATE TABLE LoHang (
    maLo          VARCHAR(10)  NOT NULL CONSTRAINT PK_LoHang PRIMARY KEY,
    soLo          VARCHAR(30)  NOT NULL,
    ngaySanXuat   DATE,
    hanSuDung     DATE,
    maThuoc       VARCHAR(10),
    maVatTu       VARCHAR(10),
    maNhaCungCap  VARCHAR(10)  NOT NULL,
    soLuongTon    INT          NOT NULL DEFAULT 0,
    CONSTRAINT FK_LoHang_Thuoc FOREIGN KEY (maThuoc) REFERENCES Thuoc(maThuoc),
    CONSTRAINT FK_LoHang_VatTu FOREIGN KEY (maVatTu) REFERENCES VatTu(maVatTu),
    CONSTRAINT FK_LoHang_NCC FOREIGN KEY (maNhaCungCap) REFERENCES NhaCungCap(maNhaCungCap),
    CONSTRAINT CK_LoHang_MotLoai CHECK ((maThuoc IS NOT NULL AND maVatTu IS NULL) OR (maThuoc IS NULL AND maVatTu IS NOT NULL)),
    CONSTRAINT CK_LoHang_Ton CHECK (soLuongTon >= 0),
    CONSTRAINT CK_LoHang_Han CHECK (hanSuDung IS NULL OR ngaySanXuat IS NULL OR hanSuDung > ngaySanXuat)
);
CREATE INDEX IX_LoHang_Thuoc ON LoHang(maThuoc);
CREATE INDEX IX_LoHang_VatTu ON LoHang(maVatTu);
CREATE TABLE PhieuXuatKho (
    maPhieuXuat  VARCHAR(10)    NOT NULL CONSTRAINT PK_PhieuXuatKho PRIMARY KEY,
    ngayXuat     TIMESTAMP   NOT NULL,
    boPhanNhan   VARCHAR(100)  NOT NULL,
    nguoiNhan    VARCHAR(100),
    lyDo         VARCHAR(300),
    trangThai    VARCHAR(20)    NOT NULL DEFAULT 'HOAN_TAT',
    maNhanVien   VARCHAR(10)    NOT NULL,
    CONSTRAINT FK_PhieuXuat_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT CK_PhieuXuat_TrangThai CHECK (trangThai IN ('HOAN_TAT'))
);
CREATE TABLE ChiTietPhieuXuat (
    maPhieuXuat  VARCHAR(10)  NOT NULL,
    maLo         VARCHAR(10)  NOT NULL,
    soLuong      INT          NOT NULL,
    CONSTRAINT PK_ChiTietPhieuXuat PRIMARY KEY (maPhieuXuat, maLo),
    CONSTRAINT FK_CTPX_Phieu FOREIGN KEY (maPhieuXuat) REFERENCES PhieuXuatKho(maPhieuXuat),
    CONSTRAINT FK_CTPX_Lo FOREIGN KEY (maLo) REFERENCES LoHang(maLo),
    CONSTRAINT CK_CTPX_SoLuong CHECK (soLuong > 0)
);
CREATE TABLE PhieuNhapKho (
    maPhieuNhap     VARCHAR(10)    NOT NULL CONSTRAINT PK_PhieuNhapKho PRIMARY KEY,
    ngayNhap        TIMESTAMP   NOT NULL,
    trangThai       VARCHAR(20)    NOT NULL DEFAULT 'HOAN_TAT',
    loaiNhap        VARCHAR(10)    NOT NULL DEFAULT 'NHAP_NCC',
    maNhaCungCap    VARCHAR(10),
    maPhieuXuatGoc  VARCHAR(10),
    maNhanVien      VARCHAR(10)    NOT NULL,
    ghiChu          VARCHAR(300),
    CONSTRAINT FK_PhieuNhap_NCC FOREIGN KEY (maNhaCungCap) REFERENCES NhaCungCap(maNhaCungCap),
    CONSTRAINT FK_PhieuNhap_XuatGoc FOREIGN KEY (maPhieuXuatGoc) REFERENCES PhieuXuatKho(maPhieuXuat),
    CONSTRAINT FK_PhieuNhap_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT CK_PhieuNhap_TrangThai CHECK (trangThai IN ('HOAN_TAT')),
    CONSTRAINT CK_PhieuNhap_Loai CHECK ((loaiNhap = 'NHAP_NCC' AND maNhaCungCap IS NOT NULL AND maPhieuXuatGoc IS NULL) OR (loaiNhap = 'HOAN_TRA' AND maPhieuXuatGoc IS NOT NULL))
);
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
CREATE TABLE PhieuDieuChinhKho (
    maPhieu          VARCHAR(10)    NOT NULL CONSTRAINT PK_PhieuDieuChinhKho PRIMARY KEY,
    loai             VARCHAR(10)    NOT NULL,
    maTaiKhoanTao    VARCHAR(10)    NOT NULL,
    thoiGianTao      TIMESTAMP   NOT NULL,
    lyDo             VARCHAR(500)  NOT NULL,
    trangThai        VARCHAR(10)    NOT NULL DEFAULT 'CHO_DUYET',
    maTaiKhoanDuyet  VARCHAR(10),
    thoiGianDuyet    TIMESTAMP,
    ghiChuDuyet      VARCHAR(500),
    CONSTRAINT FK_PDC_TKTao FOREIGN KEY (maTaiKhoanTao) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT FK_PDC_TKDuyet FOREIGN KEY (maTaiKhoanDuyet) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT CK_PDC_Loai CHECK (loai IN ('KIEM_KE','HU_HONG','HET_HAN')),
    CONSTRAINT CK_PDC_TrangThai CHECK (trangThai IN ('CHO_DUYET','DA_DUYET','TU_CHOI')),
    CONSTRAINT CK_PDC_KhongTuDuyet CHECK (maTaiKhoanDuyet IS NULL OR maTaiKhoanDuyet <> maTaiKhoanTao),
    CONSTRAINT CK_PDC_TuChoi CHECK (trangThai <> 'TU_CHOI' OR ghiChuDuyet IS NOT NULL),
    CONSTRAINT CK_PDC_DaXuLy CHECK (trangThai = 'CHO_DUYET' OR maTaiKhoanDuyet IS NOT NULL)
);
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
CREATE TABLE DanhMucCapCuu (
    maMuc              VARCHAR(10)    NOT NULL CONSTRAINT PK_DanhMucCapCuu PRIMARY KEY,
    tenMuc             VARCHAR(150)  NOT NULL,
    donViTinh          VARCHAR(30)   NOT NULL,
    soLuongToiThieu    INT            NOT NULL,
    soLuongHienCo      INT            NOT NULL DEFAULT 0,
    hanSuDungGanNhat   DATE,
    maThuoc            VARCHAR(10),
    maVatTu            VARCHAR(10),
    dangApDung         SMALLINT            NOT NULL DEFAULT 1,
    laDuLieuMau        SMALLINT            NOT NULL DEFAULT 0,
    ghiChu             VARCHAR(300),
    CONSTRAINT FK_DMCC_Thuoc FOREIGN KEY (maThuoc) REFERENCES Thuoc(maThuoc),
    CONSTRAINT FK_DMCC_VatTu FOREIGN KEY (maVatTu) REFERENCES VatTu(maVatTu),
    CONSTRAINT CK_DMCC_SoLuong CHECK (soLuongToiThieu > 0 AND soLuongHienCo >= 0),
    CONSTRAINT CK_DMCC_LienKet CHECK (maThuoc IS NULL OR maVatTu IS NULL)
);
CREATE TABLE KiemTraCapCuu (
    maKiemTra   VARCHAR(10)    NOT NULL CONSTRAINT PK_KiemTraCapCuu PRIMARY KEY,
    thoiGian    TIMESTAMP   NOT NULL,
    maNhanVien  VARCHAR(10)    NOT NULL,
    ketQua      VARCHAR(15)    NOT NULL,
    ghiChu      VARCHAR(500),
    CONSTRAINT FK_KTCC_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT CK_KTCC_KetQua CHECK (ketQua IN ('DAT','CAN_BO_SUNG'))
);
CREATE TABLE ChiTietKiemTraCapCuu (
    maKiemTra      VARCHAR(10)  NOT NULL,
    maMuc          VARCHAR(10)  NOT NULL,
    soLuongThucTe  INT          NOT NULL,
    hanSuDung      DATE,
    dat            SMALLINT          NOT NULL,
    CONSTRAINT PK_ChiTietKiemTraCapCuu PRIMARY KEY (maKiemTra, maMuc),
    CONSTRAINT FK_CTKTCC_KiemTra FOREIGN KEY (maKiemTra) REFERENCES KiemTraCapCuu(maKiemTra),
    CONSTRAINT FK_CTKTCC_Muc FOREIGN KEY (maMuc) REFERENCES DanhMucCapCuu(maMuc),
    CONSTRAINT CK_CTKTCC_SoLuong CHECK (soLuongThucTe >= 0)
);
CREATE TABLE HoaDon (
    maHoaDon     VARCHAR(10)   NOT NULL CONSTRAINT PK_HoaDon PRIMARY KEY,
    maNhanVien   VARCHAR(10)   NOT NULL,
    maBenhNhan   VARCHAR(10)   NOT NULL,
    maPhieuKham  VARCHAR(10),
    ngayLap      TIMESTAMP  NOT NULL,
    trangThai    VARCHAR(20)   NOT NULL DEFAULT 'CHUA_THANH_TOAN',
    CONSTRAINT FK_HoaDon_NhanVien FOREIGN KEY (maNhanVien) REFERENCES NhanVien(maNhanVien),
    CONSTRAINT FK_HoaDon_BenhNhan FOREIGN KEY (maBenhNhan) REFERENCES BenhNhan(maBenhNhan),
    CONSTRAINT FK_HoaDon_PhieuKham FOREIGN KEY (maPhieuKham) REFERENCES PhieuKham(maPhieuKham),
    CONSTRAINT CK_HoaDon_TrangThai CHECK (trangThai IN ('CHUA_THANH_TOAN','THANH_TOAN_MOT_PHAN','DA_THANH_TOAN','DA_HUY'))
);
CREATE INDEX IX_HoaDon_BenhNhan ON HoaDon(maBenhNhan);
CREATE TABLE ChiTietHoaDon (
    maChiTiet     INT NOT NULL GENERATED BY DEFAULT AS IDENTITY CONSTRAINT PK_ChiTietHoaDon PRIMARY KEY,
    maHoaDon      VARCHAR(10)    NOT NULL,
    maDichVu      VARCHAR(10),
    maThuoc       VARCHAR(10),
    maVatTu       VARCHAR(10),
    soLuong       INT            NOT NULL,
    donGiaApDung  DECIMAL(18,0)  NOT NULL,
    CONSTRAINT FK_CTHD_HoaDon FOREIGN KEY (maHoaDon) REFERENCES HoaDon(maHoaDon),
    CONSTRAINT FK_CTHD_DichVu FOREIGN KEY (maDichVu) REFERENCES DichVu(maDichVu),
    CONSTRAINT FK_CTHD_Thuoc FOREIGN KEY (maThuoc) REFERENCES Thuoc(maThuoc),
    CONSTRAINT FK_CTHD_VatTu FOREIGN KEY (maVatTu) REFERENCES VatTu(maVatTu),
    CONSTRAINT CK_CTHD_MotLoai CHECK ((maDichVu IS NOT NULL AND maThuoc IS NULL AND maVatTu IS NULL) OR (maDichVu IS NULL AND maThuoc IS NOT NULL AND maVatTu IS NULL) OR (maDichVu IS NULL AND maThuoc IS NULL AND maVatTu IS NOT NULL)),
    CONSTRAINT CK_CTHD_SoLuong CHECK (soLuong > 0 AND donGiaApDung >= 0)
);
CREATE TABLE ThanhToan (
    maThanhToan    VARCHAR(10)    NOT NULL CONSTRAINT PK_ThanhToan PRIMARY KEY,
    maHoaDon       VARCHAR(10)    NOT NULL,
    thoiGian       TIMESTAMP   NOT NULL,
    soTien         DECIMAL(18,0)  NOT NULL,
    phuongThuc     VARCHAR(15)    NOT NULL,
    maGiaoDich     VARCHAR(50),
    tienKhachDua   DECIMAL(18,0),
    trangThai      VARCHAR(15)    NOT NULL DEFAULT 'THANH_CONG',
    maNhanVien     VARCHAR(10)    NOT NULL,
    maTaiKhoanHuy  VARCHAR(10),
    thoiGianHuy    TIMESTAMP,
    lyDoHuy        VARCHAR(300),
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
CREATE INDEX IX_ThanhToan_HoaDon ON ThanhToan(maHoaDon);
CREATE TABLE YeuCauHuyHoaDon (
    maYeuCau         VARCHAR(10)    NOT NULL CONSTRAINT PK_YeuCauHuyHoaDon PRIMARY KEY,
    maHoaDon         VARCHAR(10)    NOT NULL,
    maTaiKhoanTao    VARCHAR(10)    NOT NULL,
    thoiGianTao      TIMESTAMP   NOT NULL,
    lyDo             VARCHAR(500)  NOT NULL,
    trangThai        VARCHAR(10)    NOT NULL DEFAULT 'CHO_DUYET',
    maTaiKhoanDuyet  VARCHAR(10),
    thoiGianDuyet    TIMESTAMP,
    lyDoDuyet        VARCHAR(500),
    CONSTRAINT FK_YCHuy_HoaDon FOREIGN KEY (maHoaDon) REFERENCES HoaDon(maHoaDon),
    CONSTRAINT FK_YCHuy_TKTao FOREIGN KEY (maTaiKhoanTao) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT FK_YCHuy_TKDuyet FOREIGN KEY (maTaiKhoanDuyet) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT CK_YCHuy_TrangThai CHECK (trangThai IN ('CHO_DUYET','DA_DUYET','TU_CHOI')),
    CONSTRAINT CK_YCHuy_KhongTuDuyet CHECK (maTaiKhoanDuyet IS NULL OR maTaiKhoanDuyet <> maTaiKhoanTao),
    CONSTRAINT CK_YCHuy_DaXuLy CHECK (trangThai = 'CHO_DUYET' OR (maTaiKhoanDuyet IS NOT NULL AND lyDoDuyet IS NOT NULL))
);
CREATE TABLE NhatKyThaoTac (
    maNhatKy                BIGINT NOT NULL GENERATED BY DEFAULT AS IDENTITY CONSTRAINT PK_NhatKyThaoTac PRIMARY KEY,
    thoiGian                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    maTaiKhoan              VARCHAR(10),
    chucNang                VARCHAR(50)    NOT NULL,
    hanhDong                VARCHAR(200)   NOT NULL,
    doiTuong                VARCHAR(50),
    noiDung                 VARCHAR(1000),
    maNguoiChiuTrachNhiem   VARCHAR(10),
    CONSTRAINT FK_NhatKy_TaiKhoan FOREIGN KEY (maTaiKhoan) REFERENCES TaiKhoan(maTaiKhoan),
    CONSTRAINT FK_NhatKy_NguoiChiuTN FOREIGN KEY (maNguoiChiuTrachNhiem) REFERENCES NhanVien(maNhanVien)
);
CREATE INDEX IX_NhatKy_DoiTuong ON NhatKyThaoTac(doiTuong);
CREATE INDEX IX_NhatKy_ThoiGian ON NhatKyThaoTac(thoiGian);
