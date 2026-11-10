Bạn là lập trình viên Java Swing. Hãy code màn hình **Tổng quan (Dashboard) của Admin** cho phần mềm quản lý phòng khám nha khoa N08, đúng theo ảnh thiết kế `docs/menu-admin/ui-xanh-ngoc/01-tong-quan.png`, và tách phần giao diện thành **bộ component dùng lại được** cho các màn hình sau (Tiếp nhận, Lịch hẹn, Hồ sơ bệnh nhân…).

## 0. Trước khi code
- Đọc cấu trúc project hiện có (package, cách đặt tên, Service/DAO đang dùng) và làm theo quy ước đó. Không đổi code không liên quan.
- Công nghệ: **Java Swing thuần + JDBC + SQL Server**. Không dùng JavaFX, web, Hibernate/JPA, không thêm thư viện UI ngoài (không FlatLaf, MigLayout…).
- Mọi chữ hiển thị bằng tiếng Việt có dấu, file nguồn UTF-8.

## 1. Bộ UI dùng lại — package `...desktop.ui`
Tạo các lớp sau, mỗi lớp nhỏ, có Javadoc ngắn, không chứa nghiệp vụ:

| Lớp | Nhiệm vụ |
|---|---|
| `Theme` | Hằng số màu, font, khoảng cách, bo góc (bảng ở mục 2). Mọi component chỉ lấy màu/font từ đây, không hard-code. |
| `RoundedPanel` | JPanel bo góc (radius, màu nền, màu viền 1px) vẽ bằng `paintComponent` + antialias. Nền của mọi card. |
| `Card` | `RoundedPanel` có đầu khung: tiêu đề 15 đậm, badge số (tùy chọn), link/nút bên phải, nội dung ở CENTER. |
| `Buttons` | Factory: `primary(text)` nền xanh ngọc chữ trắng · `secondary(text)` nền trắng viền xám · `danger(text)` **nền đỏ chữ trắng** (dùng cho mọi nút Hủy/Xóa) · `link(text)` chữ xanh ngọc không viền · `small(text, icon)` nút nhỏ có mũi tên "›". Bo góc 9, cao 34 (nhỏ 28), hover đổi nền nhẹ, con trỏ bàn tay. |
| `Pill` / `Badge` | Nhãn bo tròn nền nhạt + chữ màu (ví dụ "2 chờ xác nhận", "5 hóa đơn còn nợ", số đỏ "9"). |
| `IconTile` | Ô vuông bo góc 11, nền nhạt, icon nét ở giữa (40×40 cho KPI, 32×32 cho dòng việc). |
| `Icons` | Icon vẽ nét bằng Java2D trên lưới 24×24 (lịch, răng, ví, hóa đơn, ống thuốc, hộp cấp cứu, lịch-x, tài liệu-x, clipboard-check, mũi tên đổi, đồng hồ lặp, hộp, ổ khóa, chuông, làm mới, info, check, cảnh báo). Màu truyền vào, không dùng file ảnh. Nếu project đã có `MenuIcon` thì mở rộng lớp đó. |
| `KpiCard` | IconTile + nhãn + số lớn + dòng phụ hoặc Pill; bấm được (gọi callback mở màn hình). |
| `TaskRow` | Một dòng việc: IconTile 32 + tiêu đề đậm + dòng phụ xám + nút nhỏ bên phải; hover nền `#F9FAFB`. |
| `PriorityGroupHeader` | Chấm màu + chữ IN HOA đậm 12 (CẦN XỬ LÝ NGAY / CHỜ DUYỆT / XÁC NHẬN / THEO DÕI). |
| `StackedBar` | Thanh ngang cao 12, bo 6, các đoạn theo tỉ lệ, khe 2px; kèm `Legend` lưới 4 cột (ô màu 8×8 + nhãn + số đậm). |
| `ShiftTile` | Ô ca làm: tên ca, "So với định mức", trạng thái ✓ Đủ người (xanh lá) hoặc ⚠ Y tá thiếu 1 (đỏ, viền + nền đỏ nhạt). |
| `StatTile` | Ô số nhỏ nền `#F9FAFB`: nhãn xám + số 20 đậm (màu cam/đỏ theo mức). |
| `AlertBar` | Thanh cảnh báo nền đỏ nhạt, icon + chữ đậm + chữ thường. |
| `StatusColors` | Map trạng thái → màu, dùng chung toàn app (mục 2). |
| `TopBar` | Breadcrumb `Quản trị / <màn hình>` · ô nhắc nền xanh ngọc nhạt · chuông có chấm đỏ · ô người dùng (avatar tròn "AD", tên, "admin · Quản trị", mũi tên xổ menu Đổi mật khẩu / Đăng xuất). |
| `ScreenHeader` | Tiêu đề 24 đậm + dòng phụ xám + vùng nút bên phải. |

Menu trái (`SidebarMenu`, 6 nhóm nghiệp vụ xổ xuống, icon trái, badge đỏ) nếu đã có trong project thì **dùng lại, chỉ đổi màu theo Theme**; nếu chưa có thì tạo theo ảnh.

## 2. Theme (tông xanh ngọc)
Font: `Segoe UI` (fallback `Dialog`). Cỡ: tiêu đề màn hình 24 đậm · số KPI 26 đậm · tiêu đề card 15 đậm · chữ thường 13 · chữ phụ 12.

| Token | Màu |
|---|---|
| Nền nội dung | `#EEF5F5` |
| Card | `#FFFFFF`, viền `#D5E3E4`, bo 14 |
| Chữ đậm / thường / phụ | `#111827` / `#1F2937` / `#6B7280` |
| Primary (xanh ngọc) | `#0F766E`, nền nhạt `#E6F4F2`, đậm `#0F3B45` |
| Sidebar | nền `#0F3B45`, mục chọn `#18606B`, hover `#15505B`, ô tìm `#0A2D35`, chữ `#E2E8F0`, chữ phụ `#8FB3B9`, icon `#A7C7CC`, nhấn `#5EEAD4`, logo gradient `#2DD4BF → #0F766E` |
| Đỏ | `#DC2626` / nền `#FEF2F2` |
| Cam | `#C2410C` / nền `#FFF7ED` |
| Xanh lá | `#15803D` / nền `#F0FDF4` |
| Tím | `#7C3AED` / nền `#F5F3FF` |
| Xám đậm | `#475569` / nền `#F1F5F9` |

Màu trạng thái lịch hẹn (dùng chung mọi màn hình): Chờ xác nhận `#F59E0B` · Đã xác nhận `#0E7490` · Đã đến `#8B5CF6` · Hoàn thành `#16A34A` · Vắng mặt `#475569` · Đã hủy `#DC2626` · Đã đổi lịch `#CBD5E1`.

Khoảng cách: padding nội dung 24 · khoảng giữa card 16 · padding trong card 16 · topbar cao 52 · sidebar rộng 272 · khung tối thiểu 1280×760.

## 3. Màn hình `TongQuanPanel` (chỉ ghép component, không tự vẽ)
Bố cục từ trên xuống, theo ảnh:
1. `ScreenHeader`: "Chào buổi sáng, Quản trị viên" (đổi theo giờ: sáng/chiều/tối) · dòng phụ "Thứ Hai, 12/10/2026 · số liệu cập nhật lúc 09:15" · nút `Hôm nay` (icon lịch) và `Tải lại` (icon làm mới).
2. 4 `KpiCard` (GridLayout 1×4): Lịch hẹn hôm nay (pill cam "x chờ xác nhận") · Bệnh nhân chờ khám ("x đang khám · y đã kết luận") · Đã thu hôm nay ("Tiền mặt … · CK … · POS …") · Công nợ (pill đỏ "x hóa đơn còn nợ"). Bấm thẻ → mở màn hình tương ứng.
3. Hàng dưới, GridBagLayout tỉ lệ ~56/44:
   - Trái: `Card` "Việc cần xử lý" + badge đỏ tổng số + link "Sắp theo mức ưu tiên". Ba nhóm `PriorityGroupHeader` (đỏ / cam / xanh ngọc), mỗi nhóm các `TaskRow` với nút hành động: Lập phiếu, Kiểm tra, Phân công, Duyệt, Xác nhận, Xem, Mở khóa. Nhóm rỗng thì ẩn.
   - Phải (dọc): `Card` "Lịch hẹn hôm nay" + link "Mở lịch hẹn ›" chứa `StackedBar` + `Legend` 7 trạng thái · `Card` "Ca làm hôm nay" + link "Phân công ›" với 3 `ShiftTile` · `Card` "Thuốc & vật tư" + link "Tồn kho ›" với 3 `StatTile` (Dưới định mức, Lô sắp hết hạn, Lô đã hết hạn) + `AlertBar` hộp cấp cứu.

## 4. Dữ liệu
- Tạo `record TongQuanData` (và các record con: `KpiData`, `TaskItem(level, title, subtitle, actionText, screenKey)`, `AppointmentStat`, `ShiftStat`, `StockStat`).
- `TongQuanPanel` chỉ nhận `TongQuanData` qua `render(TongQuanData)`; `Tải lại` gọi service lấy lại dữ liệu trên `SwingWorker`, không chặn EDT.
- Nếu `TongQuanService` chưa có: tạo interface + `MockTongQuanService` trả đúng số liệu trong ảnh (10 lịch hẹn: 2 chờ xác nhận, 2 đã xác nhận, 1 đã đến, 2 hoàn thành, 1 vắng, 1 hủy, 1 đổi lịch; 0 chờ khám, 1 đang khám, 2 đã kết luận; thu 700.000 đ tiền mặt; công nợ 3.600.000 đ / 5 hóa đơn; 9 việc cần xử lý như ảnh; ca chiều & ca tối thiếu 1 y tá; dưới định mức 2, sắp hết hạn 2, đã hết hạn 1; hộp cấp cứu kiểm tra 05/10/2026 07:45 cần bổ sung).
- Điều hướng: panel nhận `Consumer<String> openScreen`; mọi nút/thẻ/link gọi `openScreen.accept(screenKey)`, không tự tạo màn hình khác.
- Định dạng tiền `700.000 đ` (Locale vi-VN), ngày `dd/MM/yyyy`.

## 5. Yêu cầu kỹ thuật
- Mọi thao tác UI trên EDT; antialias chữ và hình; không `setBounds`/null layout (dùng BorderLayout, GridLayout, GridBagLayout, BoxLayout).
- Co giãn đúng từ 1280×760 đến toàn màn hình; chữ dài thì cắt "…" thay vì tràn.
- Không lặp code vẽ: màn hình chỉ dùng component trong `ui`.
- Thêm `main` demo (hoặc lớp `TongQuanPreview`) mở JFrame 1382×840 gồm Sidebar + TopBar + TongQuanPanel với `MockTongQuanService`.

## 6. Tiêu chí hoàn thành
- Chạy demo ra giao diện khớp ảnh `01-tong-quan.png` (bố cục, màu, chữ).
- Đổi một màu trong `Theme` → toàn màn hình đổi theo.
- Có thể dựng một card mới cho màn hình khác chỉ bằng `new Card(...)`, `Buttons.primary(...)`, `TaskRow`… không cần copy code.
- Biên dịch không cảnh báo; báo lại danh sách file đã tạo/sửa và cách chạy demo.
