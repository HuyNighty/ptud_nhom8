# M2 — Hợp đồng dữ liệu Bệnh nhân → Lịch hẹn

Trạng thái: **ĐỀ XUẤT, chờ Huy và người làm Lịch hẹn duyệt** (theo `TEAM_WORK_SPLIT`: tên API, DTO phải được duyệt trước khi dùng).

Căn cứ: `docs/tuan_cuoi/9_REPORT_ApplicationDevelopment.docx`
- UC004 bước 1: *"Chọn 'Đặt lịch', tìm và chọn bệnh nhân."*
- UC004 A3 / UC005 A2: *"Bệnh nhân chưa có hồ sơ: lễ tân tạo hồ sơ (UC003) rồi quay lại bước 1."*
- UC005 bước 1: tìm theo mã, tên, số điện thoại.
- BR10: kiểm tra lại dữ liệu ngay trước khi lưu.

## 1. Phần Lịch hẹn được dùng gì

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

1. Phần Lịch hẹn **không** viết SQL đọc / ghi `dbo.BenhNhan`, `dbo.HoSoBenhAn`; chỉ lưu `maBenhNhan`.
2. Phần Bệnh nhân giữ nguyên tên và ý nghĩa 2 hàm trên; muốn đổi phải báo trước.
3. Các quy tắc của lịch hẹn (BR02 không chồng giờ, BR03 chỉ đổi/hủy lịch "Đã đặt") do phần Lịch hẹn tự kiểm tra.

## 4. Cần thống nhất trước khi làm Lịch hẹn

| # | Vấn đề | Đề xuất |
|---|---|---|
| 1 | Tài khoản app (`TeamM1Permissions`) chỉ có **SELECT** trên `LichHen` → chưa lưu được lịch hẹn. | Huy cấp thêm INSERT, UPDATE trên `LichHen`. |
| 2 | Báo cáo dùng trạng thái **"Đã đặt"**, database V5 mặc định `CHO_XAC_NHAN` và có thêm `DA_XAC_NHAN`. | Chốt: "Đã đặt" = `CHO_XAC_NHAN` hay `DA_XAC_NHAN`. |
| 3 | UC004 A3: chưa có hồ sơ thì tạo hồ sơ (UC003) rồi quay lại. | Màn hình Lịch hẹn có nút "Thêm bệnh nhân" mở form Thêm của phần Bệnh nhân — cần thống nhất cách gọi (hiện form Thêm nằm trong `BenhNhanController`). |
| 4 | `TEAM_WORK_SPLIT` gợi ý package `patient/`, code hiện dùng `benhnhan/`. | Giữ `benhnhan/` (đã có sẵn), hoặc Huy quyết đổi. |
