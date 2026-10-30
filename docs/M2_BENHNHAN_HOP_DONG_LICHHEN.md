# M2 — Hợp đồng dữ liệu Bệnh nhân → Lịch hẹn

Trạng thái: **ĐỀ XUẤT, chờ Huy và người làm Lịch hẹn duyệt** (theo `TEAM_WORK_SPLIT`: tên API, DTO phải được duyệt trước khi dùng).

Căn cứ: `docs/tuan_cuoi/9_REPORT_ApplicationDevelopment.docx`
- UC004 bước 1: *"Chọn 'Đặt lịch', tìm và chọn bệnh nhân."*
- UC004 A3 / UC005 A2: *"Bệnh nhân chưa có hồ sơ: lễ tân tạo hồ sơ (UC003) rồi quay lại bước 1."*
- UC005 bước 1: tìm theo mã, tên, số điện thoại.
- BR10: kiểm tra lại dữ liệu ngay trước khi lưu.

## 1. Hợp đồng: phần Lịch hẹn được dùng gì

Package `com.quanlynhakhoa.benhnhan.hopdong`:

| File | Vai trò |
|---|---|
| `TraCuuBenhNhan` | interface — hợp đồng, phần Lịch hẹn chỉ gọi 2 hàm này |
| `BenhNhanTomTat` | dữ liệu tóm tắt 1 bệnh nhân (chỉ đọc) |
| `TraCuuBenhNhanService` | lớp triển khai, dùng lại `BenhNhanService` |

### Hàm

| Hàm | Dùng ở | Kết quả |
|---|---|---|
| `ArrayList<BenhNhanTomTat> timBenhNhan(String tuKhoa)` | UC004 bước 1, UC005 bước 1 | Tìm theo mã / họ tên / SĐT. Rỗng → bệnh nhân mới nhất. Tối đa 200, mới nhất ở trên. Không có → danh sách rỗng. |
| `BenhNhanTomTat layTheoMa(String maBenhNhan)` | Ngay trước khi lưu lịch hẹn (BR10); mở lại lịch cũ | Không có → `null`. Mã rỗng → `IllegalArgumentException`. |

Cả 2 hàm: kiểm tra quyền `PATIENT_READ` (không có → `AccessDeniedException`), đọc database nên **phải gọi trong `SwingWorker`**.

### Dữ liệu `BenhNhanTomTat`

| Getter | Kiểu | Ghi chú |
|---|---|---|
| `getMaBenhNhan()` | String | `BN000001`… — lưu vào `LichHen.maBenhNhan` |
| `getHoTen()` | String | |
| `getNgaySinh()` | LocalDate | |
| `getGioiTinh()` / `getGioiTinhHienThi()` | String | `NAM`/`NU`/`KHAC` → "Nam"/"Nữ"/"Khác" |
| `getSoDienThoai()` | String | 10 chữ số |
| `toString()` | String | `BN000001 - Nguyễn Thị Lan - 0909123456` (để bỏ thẳng vào JComboBox) |

Không có tiền sử bệnh, dị ứng, địa chỉ: Lịch hẹn không cần.

## 2. Ví dụ dùng trong Controller của Lịch hẹn

```java
TraCuuBenhNhan traCuuBenhNhan = new TraCuuBenhNhanService(phienDangNhap);

// Bấm "Tìm bệnh nhân"
String tuKhoa = view.getTxtTimBenhNhan().getText();
new SwingWorker<ArrayList<BenhNhanTomTat>, Void>() {
    @Override
    protected ArrayList<BenhNhanTomTat> doInBackground() throws Exception {
        return traCuuBenhNhan.timBenhNhan(tuKhoa);
    }

    @Override
    protected void done() {
        try {
            view.hienDanhSachBenhNhan(get());
        } catch (Exception loi) {
            view.hienLoi("Không tìm được bệnh nhân.");
        }
    }
}.execute();

// Trong Service của Lịch hẹn, ngay trước INSERT LichHen:
BenhNhanTomTat benhNhan = traCuuBenhNhan.layTheoMa(maBenhNhanDaChon);
if (benhNhan == null) {
    throw new IllegalArgumentException("Bệnh nhân không còn tồn tại, hãy chọn lại.");
}
```

## 3. Quy ước

1. Phần Lịch hẹn **không ghi** vào `dbo.BenhNhan`, `dbo.HoSoBenhAn`; chỉ lưu `maBenhNhan` vào `LichHen`.
2. Tìm / kiểm tra bệnh nhân khi đặt lịch: **chỉ qua `TraCuuBenhNhan`**.
   Riêng câu SQL danh sách lịch hẹn được `JOIN dbo.BenhNhan` (chỉ đọc) để hiện tên + SĐT trên bảng.
3. Phần Bệnh nhân giữ nguyên tên và ý nghĩa 2 hàm trên; muốn đổi phải báo trước.
4. Các quy tắc của lịch hẹn (BR02 không chồng giờ + trong ca, BR03 chỉ đổi/hủy lịch "Đã đặt") do phần Lịch hẹn tự kiểm tra.

## 4. Phần Lịch hẹn đã làm (package `com.quanlynhakhoa.lichhen`)

| File | Nội dung |
|---|---|
| `model/LichHen`, `model/BacSi` | dữ liệu lịch hẹn, bác sĩ |
| `dao/LichHenDao` | toàn bộ SQL (dùng lại `BenhNhanDao.taoCauLenh`, `doiKyTuDacBietThanhKyTuThuong`) |
| `service/LichHenService` | quyền `APPOINTMENT_READ` / `APPOINTMENT_MANAGE`, BR02, BR03, BR10, transaction, nhật ký |
| `controller/LichHenController` | sự kiện + SwingWorker |
| `view/LichHenPanel` | menu Lịch hẹn → Danh sách lịch hẹn: North = lọc + Đặt lịch; Center = bảng; East = lịch đang chọn + Tiếp đón / Đổi / Hủy |
| `view/LichHenDialog` | form Đặt lịch / Đổi lịch (dùng lại `ChonNgayDialog`, `BieuTuong`) |

Quyết định đã chọn (Huy có thể đổi):

- Lịch mới lưu `CHO_XAC_NHAN`. "Đã đặt" = `CHO_XAC_NHAN` hoặc `DA_XAC_NHAN` (cả 2 đổi / hủy được).
- Đổi lịch: lịch cũ → `DA_DOI_LICH`, tạo lịch mới có `maLichHenGoc` = lịch cũ (giữ lịch sử).
- Hủy: `DA_HUY` + `lyDoHuy` bắt buộc.
- **Tiếp đón bệnh nhân** (UC005, chỉ cho lịch "Đã đặt" của hôm nay, quyền `PATIENT_CHECK_IN`): trong 1 transaction
  kiểm tra BR09 (không có lượt khám `CHO_KHAM` / `DANG_KHAM`), tạo `PhieuKham` mã `PK000001…` trạng thái `CHO_KHAM`
  (gắn `maHoSo`, bác sĩ, `maLichHen`), lịch hẹn → `DA_DEN`, ghi nhật ký.
  Chưa làm: số thứ tự (V5 không có cột), kiểm tra bác sĩ đang trong ca (UC005 E2), tiếp đón khách không hẹn trước.
  **Cần báo Thịnh**: đây là chỗ đầu tiên tạo `PhieuKham`.
- Mỗi lịch hẹn **30 phút cố định** (xem mục 5).
- Giờ trống = các mốc 30 phút nằm trọn trong ca (`PhanCong` + `CaLamViec`, bỏ ca `DA_HUY`), trừ giờ đã qua và giờ chồng lịch khác (bỏ lịch `DA_HUY`, `DA_DOI_LICH`).
- Lúc lưu: kiểm tra lại trong transaction với `UPDLOCK, HOLDLOCK`; không còn trống → báo "Khung giờ không còn trống" và tải lại giờ trống (UC004 E1).

## 5. Cần Huy làm / chốt

| # | Vấn đề | Đề xuất |
|---|---|---|
| 1 | Tài khoản app chỉ có **SELECT** trên `LichHen` → không lưu được. | Thêm vào `TeamM1Permissions`: `LichHen` SELECT, INSERT, UPDATE. |
| 2 | Tài khoản app **chưa có SELECT** trên `PhanCong`, `CaLamViec` → không xem được giờ trống (BR02). | Thêm SELECT cho 2 bảng này. |
| 3 | Báo cáo cho chọn thời lượng **30 / 60 / 90 phút**, `LichHen` V5 **không có cột thời lượng / giờ kết thúc**. | Migration mới thêm `thoiLuongPhut INT NOT NULL DEFAULT 30`; khi có cột thì Lịch hẹn thêm ô chọn thời lượng. |
| 4 | Database dev chưa có dữ liệu `CaLamViec` / `PhanCong` → mọi bác sĩ đều "không có ca". | Thêm dữ liệu mẫu ca làm việc cho dev. |
| 5 | UC004 A3: chưa có hồ sơ → tạo hồ sơ rồi quay lại. | Hiện tại form báo "hãy tạo hồ sơ ở màn hình Bệnh nhân rồi đặt lịch lại". |
| 6 | Đăng ký màn hình: `FeatureBootstrap` thêm `.register(Module.APPOINTMENTS, LichHenPanel::new)`. Menu "Đặt lịch hẹn" đang `pending` trong `MainFrame` → có thể bỏ, vì nút "+ Đặt lịch hẹn" nằm trong màn hình danh sách. | Huy đăng ký (file Core). |
