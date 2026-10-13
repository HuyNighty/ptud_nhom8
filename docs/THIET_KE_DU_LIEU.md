# Thiết kế dữ liệu và ánh xạ — Giai đoạn 2

Cơ sở: **class diagram mới nhất (26 lớp)** + prototype Admin v0.3 đã duyệt + các quyết định nghiệp vụ đã xác nhận.
Nguyên tắc: không giả định "1 lớp = 1 bảng"; tách **entity** (khớp bảng, `model/`), **DTO** (dữ liệu nhập `…Form`,
dữ liệu hiển thị ghép bảng `…View`, `dto/`), **DAO** (JDBC, `dao/`), **Service** (nghiệp vụ, `service/`),
**view/controller** (Swing).

## 1. Lớp trong class diagram → bảng → mã Java

| Lớp (class diagram) | Bảng | Entity | DAO | Service chính |
|---|---|---|---|---|
| NhanVien | NhanVien | `NhanVien` | NhanVienDao | NhanVienService |
| TaiKhoan | TaiKhoan | `TaiKhoan` | TaiKhoanDao | AuthService, TaiKhoanService |
| CaLamViec | CaLamViec | `CaLamViec` | CaLamViecDao | PhanCongService |
| PhanCong | PhanCong | `PhanCong` | PhanCongDao | PhanCongService |
| BanGiaoCa | BanGiaoCa | `BanGiaoCa` | BanGiaoDao | BanGiaoService |
| DanhGiaNhanSu | DanhGiaNhanSu | `DanhGiaNhanSu` | DanhGiaDao | DanhGiaService |
| BenhNhan | BenhNhan | `BenhNhan` | BenhNhanDao | BenhNhanService |
| HoSoBenhAn | HoSoBenhAn | `HoSoBenhAn` | BenhNhanDao | BenhNhanService |
| DichVu | DichVu | `DichVu` | DichVuDao | DichVuService |
| KeHoachDieuTri | KeHoachDieuTri | `KeHoachDieuTri` | KeHoachDao | KeHoachService |
| ChiTietDieuTri | ChiTietDieuTri | `ChiTietDieuTri` | KeHoachDao | KeHoachService |
| LichHen | LichHen | `LichHen` | LichHenDao | LichHenService, TiepNhanService |
| PhieuKham | PhieuKham | `PhieuKham` | PhieuKhamDao | TiepNhanService, KhamService |
| DonThuoc | DonThuoc | `DonThuoc` | DonThuocDao | KhamService |
| ChiTietDonThuoc | ChiTietDonThuoc | `ChiTietDonThuoc` | DonThuocDao | KhamService |
| Thuoc | Thuoc | `Thuoc` | HangHoaDao | KhoService |
| VatTu | VatTu | `VatTu` | HangHoaDao | KhoService |
| NhaCungCap | NhaCungCap | `NhaCungCap` | NhaCungCapDao | NhaCungCapService |
| LoHang | LoHang | `LoHang` | LoHangDao | KhoService, DieuChinhKhoService |
| PhieuNhapKho | PhieuNhapKho | `PhieuNhapKho` | PhieuKhoDao | KhoService |
| ChiTietPhieuNhap | ChiTietPhieuNhap | `ChiTietPhieuNhap` | PhieuKhoDao | KhoService |
| PhieuXuatKho | PhieuXuatKho | `PhieuXuatKho` | PhieuKhoDao | KhoService |
| ChiTietPhieuXuat | ChiTietPhieuXuat | `ChiTietPhieuXuat` | PhieuKhoDao | KhoService |
| HoaDon | HoaDon | `HoaDon` | HoaDonDao | HoaDonService |
| ChiTietHoaDon | ChiTietHoaDon | `ChiTietHoaDon` | HoaDonDao | HoaDonService |
| ThanhToan | ThanhToan | `ThanhToan` | ThanhToanDao | HoaDonService |

Thuộc tính kiểu đối tượng trong class diagram (VD: `PhieuKham.maBenhNhan : BenhNhan`) được lưu thành **khóa ngoại**
(`maBenhNhan VARCHAR(10) FK`); entity Java giữ mã, còn dữ liệu ghép (tên bệnh nhân, tên bác sĩ…) nằm trong DTO `…View`.

## 2. Bảng bổ sung (11) và lý do

| Bảng | Vì sao cần | Nguồn yêu cầu |
|---|---|---|
| ChiTietDanhGia | Lưu điểm 5 tiêu chí; `DanhGiaNhanSu.diem` = trung bình | UC25, Figma QL-07 |
| ChiDinhCanLamSang | Lưu chỉ định và kết quả cận lâm sàng; class diagram không có lớp tương ứng | UC11 |
| PhieuDieuChinhKho + ChiTietDieuChinhKho | Một loại phiếu chung cho kiểm kê / hư hỏng / hết hạn, có người lập – người duyệt | UC28, UC35, Figma QL-10 |
| DanhMucCapCuu | Danh mục hộp cấp cứu **cấu hình được** (cờ `laDuLieuMau` cho dữ liệu mẫu) | UC34 |
| KiemTraCapCuu + ChiTietKiemTraCapCuu | Lịch sử từng lần kiểm tra và kết quả từng mục | UC34 |
| YeuCauHuyHoaDon | Yêu cầu hủy → duyệt bởi tài khoản khác; lưu người tạo / người duyệt / lý do | Figma QL-08/09 |
| NhatKyThaoTac | Ai làm gì (tài khoản thật) và ai chịu trách nhiệm chuyên môn | Quyết định "không mạo danh" |
| BoDemMa | Sinh mã nghiệp vụ tuần tự an toàn khi nhiều người dùng (khóa dòng trong transaction) | Kỹ thuật |
| ThamSoHeThong | Tham số cấu hình: số lần sai mật khẩu, số ca tối đa/ngày, ngưỡng tái khám, ngưỡng HSD, khung giờ hẹn | Kỹ thuật / giả định |

Hai bảng hạ tầng (`BoDemMa`, `ThamSoHeThong`) không có entity riêng — truy cập qua `MaDao`, `ThamSoDao`.

## 3. Cột bổ sung vào bảng của class diagram

| Bảng | Cột thêm | Lý do |
|---|---|---|
| NhanVien | ngaySinh, gioiTinh, email, ngayVaoLam, chuyenMon, ngayNghiViec | Hồ sơ nhân sự UC26; thủ tục nghỉ việc |
| TaiKhoan | maNhanVien (UNIQUE), soLanSai, lanDangNhapCuoi, lyDoKhoa | Khóa sau N lần sai (UC38), phân biệt khóa / vô hiệu |
| CaLamViec | soBacSi, soYTa, soLeTan | Định mức để cảnh báo ca thiếu người |
| PhanCong | maNhanVien, maCa, lyDoHuy, maPhanCongGoc | Quan hệ trong diagram; hủy có lý do; thay người giữ lịch sử |
| BanGiaoCa | ngay, người giao/nhận, ca giao/nhận, coChenhLech, tài khoản tạo/xác nhận/hủy + thời gian, ghi chú | Xác nhận khác người lập, lịch sử xử lý |
| DanhGiaNhanSu | maNhanVien, maNguoiDanhGia, kyDanhGia, trangThai | Đánh giá theo kỳ, nháp / đã gửi |
| BenhNhan | nguoiLienHe, ngayTao, maTaiKhoanTao | Truy vết tạo hồ sơ |
| DichVu | laCanLamSang, dangApDung | Lọc dịch vụ CLS; ngừng áp dụng mà không xóa |
| KeHoachDieuTri | maPhieuKham, lyDoHuy | Kế hoạch lập từ lượt khám; hủy có lý do |
| ChiTietDieuTri | maChiTiet (IDENTITY), maNhanVienThucHien, ghiChu | Bác sĩ thực hiện từng hạng mục |
| LichHen | maLichHenGoc, maKeHoach, lyDoHuy, maTaiKhoanTao, ngayTao | Đổi lịch giữ bản cũ (Q&A #4); lịch tái khám gắn kế hoạch |
| PhieuKham | maHoSo, maLichHen, lyDoHuy | Liên kết hồ sơ / lịch hẹn |
| DonThuoc | maPhieuKham (UNIQUE) | Mỗi lượt khám tối đa một đơn |
| Thuoc / VatTu | dinhMucToiThieu, daDatHang | UC36 cảnh báo định mức, đánh dấu đã đặt hàng |
| LoHang | maThuoc / maVatTu (đúng một), maNhaCungCap, soLuongTon | Lô của thuốc **hoặc** vật tư |
| PhieuNhapKho | loaiNhap, maNhaCungCap, maPhieuXuatGoc, maNhanVien, ghiChu | Nhập NCC và hoàn trả (UC32) dùng chung bảng |
| PhieuXuatKho | nguoiNhan, lyDo, maNhanVien | Thuốc KSĐB cần người nhận |
| ChiTietHoaDon | maChiTiet (IDENTITY), maThuoc, maVatTu | Một dòng là dịch vụ **hoặc** thuốc **hoặc** vật tư; `donGiaApDung` giữ giá lúc lập |
| HoaDon | maPhieuKham | Hóa đơn của lượt khám |
| ThanhToan | tienKhachDua, maNhanVien, tài khoản / thời gian / lý do hủy | Mỗi lần thu một phương thức (thay Q&A #8), tiền thối, hủy lần thu giữ lịch sử |

## 4. Phương thức trong class diagram → nơi cài đặt

| Phương thức | Cài đặt |
|---|---|
| `NhanVien.capNhatThongTin()` | `NhanVienService.capNhat` |
| `TaiKhoan.khoa()`, `moKhoa()` | `TaiKhoanService.khoa`, `moKhoa` (+ `voHieu`, `datLaiMatKhau`, `doiVaiTro`) |
| `CaLamViec.capNhatCa()` | `PhanCongService.capNhatCa` |
| `PhanCong.capNhatNhiemVu()` | `PhanCongService.suaNhiemVu` (+ `batDau`, `hoanTat`, `huy`, `thayNguoi`) |
| `BanGiaoCa.xacNhanNhanCa()` | `BanGiaoService.xacNhan` |
| `DanhGiaNhanSu.capNhatDanhGia()` | `DanhGiaService.luu` |
| `BenhNhan.capNhatThongTin()` | `BenhNhanService.capNhat` |
| `HoSoBenhAn.capNhatTienSu()` | `BenhNhanService.capNhatTienSu` |
| `DichVu.capNhatDonGia(giaMoi)` | `DichVuService.luu` |
| `KeHoachDieuTri.capNhatKeHoach()`, `hoanThanh()` | `KeHoachService.themHangMuc / doiNgayDuKien / huyHangMuc / huyKeHoach`, `hoanThanh` |
| `ChiTietDieuTri.ghiNhanKetQua()`, `capNhatTienDo()` | `KeHoachService.ghiKetQua`, `doiNgayDuKien` |
| `LichHen.doiLich(thoiGianMoi)`, `huyLich()` | `LichHenService.doiLich` (tạo bản mới, bản cũ `DA_DOI_LICH`), `huy` |
| `PhieuKham.tiepNhan()`, `ghiNhanKetQua()` | `TiepNhanService.tiepNhanTrucTiep / checkIn`, `KhamService.luuKetQua / ketLuan` |
| `DonThuoc.themChiTiet()`, `ChiTietDonThuoc.capNhatLieuDung()` | `KhamService.keDon` |
| `Thuoc.capNhatThongTin()`, `VatTu.capNhatThongTon()` | `KhoService.luuThuoc`, `luuVatTu` |
| `LoHang.kiemTraHetHan()` | `LoHang.daHetHan(ngay)` (record) |
| `PhieuNhapKho.taoPhieuNhap()`, `PhieuXuatKho.taoPhieuXuat()` | `KhoService.nhapKho / hoanTra`, `xuatKho` |
| `ChiTietHoaDon.tinhThanhTien()` | `ChiTietHoaDon.thanhTien()` |
| `HoaDon.tinhTongTien()`, `tinhConNo()` | Truy vấn con trong `HoaDonDao` → `HoaDonView.tongTien()`, `conNo()` |
| `ThanhToan.xacNhan()` | `HoaDonService.thuTien` |
| `toString()` (NhaCungCap, ChiTietPhieuNhap, ChiTietPhieuXuat) | `toString` của record / DTO hiển thị |

## 5. Giá trị suy ra — không lưu cột

| Giá trị | Tính từ |
|---|---|
| Tổng tiền hóa đơn | `SUM(soLuong × donGiaApDung)` của ChiTietHoaDon |
| Đã thanh toán / còn nợ | `SUM(soTien)` các lần thu `THANH_CONG`; còn nợ = tổng − đã thu |
| Tiền thối | `tienKhachDua − soTien` (chỉ tiền mặt) |
| Công nợ bệnh nhân | Tổng còn nợ các hóa đơn chưa hủy |
| Chi phí dự kiến kế hoạch | `SUM(DichVu.donGia)` các hạng mục chưa hủy |
| Hết hạn / sắp hết hạn | `LoHang.hanSuDung` so với ngày hiện tại và ngưỡng cấu hình |
| Tồn khả dụng | Tổng `soLuongTon` các lô chưa hết hạn |

`HoaDon.trangThai` có lưu (để lọc nhanh) nhưng **chỉ do Service tính lại** sau mỗi lần thu / hủy lần thu;
`04_kiem_tra.sql` có view đối chiếu trạng thái với số đã thu. `LoHang.soLuongTon` cũng được lưu và chỉ thay đổi
trong cùng transaction với phiếu; `vw_DoiSoatTonKho` đối chiếu với lịch sử phiếu.

## 6. Ràng buộc chính trong CSDL

- **Không tự duyệt**: `CK_YCHuy_KhongTuDuyet`, `CK_PDC_KhongTuDuyet`, `CK_BanGiao_KhongTuXacNhan`
  (người duyệt/xác nhận ≠ người tạo), thêm điều kiện trong câu `UPDATE` của DAO và kiểm tra ở Service.
- **Đúng một loại**: `CK_LoHang_MotLoai`, `CK_CTHD_MotLoai`.
- **Trạng thái cần lý do / dữ liệu**: hủy phải có lý do; `DA_KET_LUAN` cần chẩn đoán; hạng mục `DA_THUC_HIEN` cần
  ngày, kết quả, bác sĩ; `NGHI_VIEC` ⇔ có `ngayNghiViec`; CK/POS cần mã giao dịch; tiền mặt cần tiền khách đưa ≥ số thu.
- **Không âm**: `CK_LoHang_Ton`, số lượng > 0 ở mọi chi tiết phiếu.
- **Chỉ mục lọc (SQL Server)**: `UX_LichHen_KhungGio` (bác sĩ không có 2 lịch mở cùng giờ), `UX_HoaDon_PhieuKham`
  (một lượt khám một hóa đơn còn hiệu lực), `UX_YCHuy_ChoDuyet`, `UX_PhanCong_HieuLuc`. Service kiểm tra cùng
  điều kiện trước khi ghi để có thông báo rõ ràng.

## 7. Trạng thái (khớp State Machine tuần 7)

| Đối tượng | Trạng thái |
|---|---|
| LichHen | CHO_XAC_NHAN → DA_XAC_NHAN → DA_DEN → HOAN_THANH; VANG_MAT; DA_HUY (có lý do); DA_DOI_LICH |
| PhieuKham | CHO_KHAM → DANG_KHAM → DA_KET_LUAN; DA_HUY (chỉ khi chờ khám) |
| KeHoachDieuTri | DANG_THUC_HIEN → HOAN_THANH / DA_HUY · Hạng mục: CHUA_THUC_HIEN → DA_THUC_HIEN / DA_HUY |
| HoaDon | CHUA_THANH_TOAN ↔ THANH_TOAN_MOT_PHAN ↔ DA_THANH_TOAN; DA_HUY (qua yêu cầu được duyệt) |
| ThanhToan | THANH_CONG → DA_HUY (hủy lần thu nhập sai, giữ lịch sử) |
| YeuCauHuyHoaDon, PhieuDieuChinhKho | CHO_DUYET → DA_DUYET / TU_CHOI |
| NhanVien / TaiKhoan | DANG_LAM_VIEC → NGHI_VIEC · HOAT_DONG ↔ BI_KHOA; → VO_HIEU (vĩnh viễn) |
| PhanCong | CHO_HIEU_LUC → DANG_THUC_HIEN → HOAN_THANH; DA_HUY (có lý do) |
| BanGiaoCa | CHO_BAN_GIAO → DA_BAN_GIAO / DA_HUY |

## 8. Use case → màn hình → Service

| UC | Tên | Màn hình | Service |
|---|---|---|---|
| UC01 | Tìm kiếm bệnh nhân | Tiếp nhận, Hồ sơ BN | BenhNhanService.tim |
| UC02 | Tiếp nhận bệnh nhân | Tiếp nhận | TiepNhanService |
| UC03 | Quản lý lịch hẹn | Lịch hẹn | LichHenService.tao / xacNhan / danhDauVang |
| UC04 | Quản lý thanh toán | Hóa đơn & thanh toán | HoaDonService.lap / thuTien / huyGiaoDich |
| UC05 | Theo dõi công nợ | Hóa đơn → tab Công nợ | HoaDonService.congNo |
| UC06 | Hủy lịch hẹn | Lịch hẹn | LichHenService.huy |
| UC07 | Thay đổi lịch hẹn | Lịch hẹn | LichHenService.doiLich |
| UC08 | Tạo hồ sơ bệnh nhân | Tiếp nhận, Hồ sơ BN | BenhNhanService.tao |
| UC09 | Xem hồ sơ bệnh nhân | Hồ sơ BN | BenhNhanService.chiTiet |
| UC10 | Ghi nhận kết quả khám | Phiếu khám | KhamService.batDauKham / luuKetQua / ketLuan |
| UC11 | Lập phiếu chỉ định CLS | Phiếu khám | KhamService.chiDinh / nhapKetQuaCls |
| UC12 | Lập kế hoạch điều trị | Kế hoạch, Phiếu khám | KeHoachService.lap |
| UC13 | Cập nhật quá trình điều trị | Kế hoạch | KeHoachService.themHangMuc / doiNgayDuKien / huyHangMuc / huyKeHoach |
| UC14 | Quản lý tái khám | Tái khám | KeHoachService.danhSachTaiKham + LichHenService.tao |
| UC15 | Ghi nhận kết quả điều trị | Kế hoạch | KeHoachService.ghiKetQua / hoanThanh |
| UC16 | Kê đơn | Phiếu khám | KhamService.keDon |
| UC17, UC20 | Xem lịch làm việc | Phân công → Lịch theo nhân viên / Lịch tuần | PhanCongService.lich |
| UC18 | Xem nhiệm vụ được phân công | Phân công → Nhiệm vụ theo ngày | PhanCongService.batDau / hoanTat |
| UC19 | Giao nhận ca | Bàn giao ca | BanGiaoService |
| UC21 | Quản lý tài khoản và phân quyền | Tài khoản & phân quyền | TaiKhoanService |
| UC22, UC26 | Quản lý nhân viên / hồ sơ nhân sự | Nhân viên | NhanVienService |
| UC23 | Quản lý hồ sơ bệnh nhân | Hồ sơ BN | BenhNhanService.capNhat / capNhatTienSu |
| UC24 | Quản lý lịch làm việc | Phân công | PhanCongService.them / huy / thayNguoi / saoChepTuanTruoc / capNhatCa |
| UC25 | Đánh giá nhân sự | Đánh giá nhân sự | DanhGiaService |
| UC27 | Tìm kiếm vật tư | Thuốc & vật tư | KhoService.tonKho |
| UC28 | Kiểm kê kho | Kiểm kê & hao hụt | DieuChinhKhoService (loại KIEM_KE) |
| UC29 | Quản lý vật tư | Thuốc & vật tư | KhoService.luuThuoc / luuVatTu |
| UC30 | Quản lý nhập kho | Nhập / xuất / hoàn trả, Nhà cung cấp | KhoService.nhapKho, NhaCungCapService |
| UC31 | Quản lý xuất kho | Nhập / xuất / hoàn trả | KhoService.goiYFefo / xuatKho |
| UC32 | Quản lý hoàn trả kho | Nhập / xuất / hoàn trả | KhoService.hoanTra |
| UC33 | Thuốc kiểm soát đặc biệt | Thuốc & vật tư → Sổ KSĐB; xuất kho; kê đơn | KhoService.soKiemSoatDacBiet, xuatKho, KhamService.keDon |
| UC34 | Kiểm tra hộp thuốc cấp cứu | Hộp cấp cứu | CapCuuService |
| UC35 | Quản lý hủy / hao hụt | Kiểm kê & hao hụt | DieuChinhKhoService (HU_HONG, HET_HAN) |
| UC36 | Theo dõi tồn kho | Thuốc & vật tư, Tổng quan | KhoService.tonKho / danhDauDaDatHang |
| UC37 | Theo dõi hạn sử dụng | Thuốc & vật tư → Hạn sử dụng | KhoService.canhBaoHanDung |
| UC38 | Đăng nhập | Đăng nhập | AuthService.login |

Ngoài UC: Duyệt hủy hóa đơn (Figma QL-08/09), Bảng giá dịch vụ, Nhà cung cấp, Nhật ký thao tác, Tổng quan (QL-01).

## 9. Giả định (đã dùng trong prototype v0.3 hoặc bổ sung khi cài đặt)

1. Admin là nhân viên `NV0000` (chức danh `AD`); không làm thủ tục nghỉ việc cho tài khoản quản trị; chỉ quản trị
   mới cấp / sửa vai trò quản trị.
2. Một nhân viên tối đa **2 ca/ngày** (tham số `SO_CA_TOI_DA_NGAY`); khóa tự động sau **5** lần sai (`SO_LAN_SAI_TOI_DA`).
3. Tái khám = hạng mục chưa thực hiện của kế hoạch đang thực hiện, đến hạn trong **14 ngày** hoặc đã quá hạn.
4. Khung giờ hẹn lấy từ `KHUNG_GIO_HEN`; lịch hẹn mới bắt đầu ở "Chờ xác nhận" (kể cả lịch tạo do đổi lịch).
5. Bác sĩ chịu trách nhiệm của lượt khám được gán khi **bắt đầu khám**; sau đó mọi thao tác trên phiếu ghi người chịu
   trách nhiệm là bác sĩ đó. Tài khoản bác sĩ chỉ thao tác phiếu / kế hoạch của mình.
6. Kết luận lượt khám cần chẩn đoán, kết luận và mọi chỉ định CLS đã có kết quả; kết luận làm lịch hẹn gắn kèm
   → "Hoàn thành". Hủy phiếu chờ khám làm lịch hẹn gắn kèm → "Đã hủy" cùng lý do.
7. Kê đơn không trừ kho (cấp phát thuốc ngoài phạm vi); thuốc trong hóa đơn nhập đơn giá vì bảng Thuoc không có giá bán.
8. Không duyệt hủy hóa đơn khi còn lần thu thành công — phải hủy (hoàn) các lần thu trước; trong lúc chờ duyệt hủy
   không thu thêm.
9. Mã giao dịch CK/POS không được trùng với một lần thu thành công khác cùng phương thức.
10. Chức danh nhân viên không đổi sau khi tạo (vai trò tài khoản đổi được ở màn hình Tài khoản).
11. Thủ tục nghỉ việc kiểm tra cả phiếu khám chưa kết thúc của bác sĩ (ngoài phân công, lịch hẹn, bàn giao).
12. Hộp cấp cứu: mục đạt khi SL ≥ tối thiểu và HSD còn hơn **30 ngày**; chu kỳ kiểm tra **7 ngày** — đều là tham số;
    6 mục cài sẵn là dữ liệu mẫu, không phải danh mục chuyên môn chính thức. Không thêm quy tắc lâm sàng ngoài tài liệu.
13. Nhập lô trùng (cùng mặt hàng, số lô, NCC) được cộng dồn nếu cùng hạn dùng; khác hạn thì báo lỗi.
14. Phiếu điều chỉnh: loại "Hết hạn" chỉ cho lô đã hết hạn; hư hỏng / hết hạn chỉ giảm tồn; một lô không nằm trong
    hai phiếu chờ duyệt; khi duyệt, nếu tồn đã đổi từ lúc lập phiếu thì chặn.
15. Đánh giá theo quý (`yyyy-Qn`), chỉ kỳ hiện tại và 3 kỳ trước; không tự đánh giá.
