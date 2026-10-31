# Chỉnh giao diện Service & Billing — 31/10/2026

**Cập nhật sau nghiệm thu UI:** theo yêu cầu cấp quyền tiếp theo của người dùng, đã
cấp đủ 12 quyền SQL còn thiếu và qua 60 kiểm tra quyền/truy vấn thật bằng application
login. Xem [BILLING_SQL_PERMISSIONS.md](BILLING_SQL_PERMISSIONS.md). Bảng quyền và
giới hạn do quyền dưới đây là ảnh chụp trạng thái **trước đợt cấp quyền này**.

Thay đổi trong worktree `.worktrees/service-billing-m1`, nhánh `codex/latest-shell-integration`.
Giữ nguyên sáu menu ngang và các route đã tích hợp. Đợt chỉnh UI không thay đổi schema,
migration, quyền SQL, tài khoản hoặc dữ liệu development; đợt cấp quyền tiếp theo được
ghi riêng trong tài liệu liên kết ở trên.

## Thay đổi giao diện

- **Nguyên nhân cuộn toàn trang:** `MainFrame.loadFeature` bọc trang đã có bảng cuộn
  trong một `JScrollPane`. Các trang `RefreshableFeature` giờ được gắn trực tiếp vào
  vùng `CardLayout`; các màn hình khác giữ cách hiển thị hiện có.
- **Chung:** SansSerif 14, tiêu đề 22, hàng bảng 32, ô nhập/nút 34 px; nền sáng,
  nút chính navy, thao tác hủy/từ chối có màu riêng. Nhãn bộ lọc đặt trên trường,
  nút Làm mới, ngày `dd/MM/yyyy` với lịch `ChonNgayDialog` có sẵn. Tiền dùng
  BigDecimal, căn phải và định dạng Việt Nam. Nhận cả `1500000` và `1.500.000`,
  từ chối cách nhóm sai hoặc số vượt giới hạn. Bảng rộng có cuộn ngang và tooltip
  giá trị đầy đủ, không thu hẹp cột đến mức không đọc được.
- **Dịch vụ & Bảng giá:** bảng trái co giãn, form phải 350 px, nhãn ngắn, hướng dẫn
  trong tooltip. Tách rõ thêm mới/sửa, khóa mã khi sửa; nút ngừng/áp dụng lại dùng
  chung một vị trí theo trạng thái. Không dựng lịch sử giá giả.
- **Hóa đơn:** bộ lọc/thanh thao tác cố định; danh sách và chi tiết/lịch sử chia bằng
  `JSplitPane`. Ba khối tổng/đã thu/còn nợ nằm dưới, luôn nhìn thấy. Thanh toán và
  Yêu cầu hủy chỉ bật sau khi chọn và tải được chứng từ hợp lệ theo thông tin hiện có;
  Service tiếp tục kiểm tra quyền, trạng thái và yêu cầu đang chờ. Có tên dịch vụ,
  thuốc, vật tư từ các bảng thật; giá và thành tiền vẫn lấy từ chi tiết đã chốt.
- **Tạo hóa đơn:** tìm bệnh nhân có nhãn, chọn bệnh nhân/lượt khám rõ ràng, lượt khám
  có ngày tiếp nhận và trạng thái tiếng Việt. Tìm được bằng số điện thoại. Tổng tiền,
  Lưu và Hủy ở cuối; không bật tải dịch vụ/lưu khi thiếu bước trước.
- **Thanh toán:** luôn hiện form, thông tin chứng từ và lịch sử, kể cả khi chưa chọn.
  Khóa nhập khi chưa có chứng từ hợp lệ. Tách tiền mặt/chuyển khoản, hiển thị tổng
  phân bổ và nợ sau thu; xác nhận nhận tiền chuyển khoản vẫn bắt buộc theo Service.
  Khi chưa xác định kết quả lần thu, vị trí nút chính chuyển sang kiểm tra kết quả,
  không ghi lại tự động. Tìm không ra kết quả mới có ghi rõ nếu đang giữ chứng từ cũ.
- **Duyệt hủy:** danh sách/chi tiết chia co giãn; thông tin xét duyệt, chi tiết và lịch
  sử nằm trong tab; thanh duyệt/từ chối cố định. Trạng thái có chữ và màu. Lỗi quyền
  hiển thị thông báo dễ hiểu, có nút Thử lại; không đưa SQLState/mã lỗi lên màn hình.
- **Công nợ:** ba khối tổng theo bộ lọc/số bệnh nhân/số hóa đơn; hai bảng chính co giãn,
  thao tác xem chi tiết/thu nợ cạnh bảng hóa đơn. Chi tiết và lịch sử mở cửa sổ riêng.
  Khi tải thất bại, tổng hiển thị “—” và khóa thao tác thay vì coi là số dư 0 thành công.

Giữ UI → Controller → Service → DAO → Model, SessionWorker ngoài EDT, kiểm tra session
và quyền ở Service. Không sửa transaction thu tiền/tạo hóa đơn/xét duyệt, không thêm
giao dịch hoặc trạng thái. BillingChangeBus vẫn cập nhật các trang đã mở sau khi ghi.

## File đã sửa/thêm

Đường dẫn dưới đây tính từ thư mục Maven trong worktree.

| Nhóm | File |
|---|---|
| Shell | `src/main/java/com/quanlynhakhoa/view/MainFrame.java` |
| UI dùng chung | `src/main/java/com/quanlynhakhoa/billing/ui/BillingUi.java`, `InvoiceHistoryPanel.java` trong cùng thư mục |
| Các màn hình | `billing/ui/HoaDonPanel.java`, `TaoHoaDonDialog.java`, `PaymentPanel.java`, `PaymentForm.java`, `CancellationPanel.java`, `DebtPanel.java` (dưới `src/main/java/com/quanlynhakhoa/`) |
| Yêu cầu hủy | `src/main/java/com/quanlynhakhoa/billing/ui/CancellationRequestDialog.java`, `billing/controller/CancellationRequestController.java` |
| Điều khiển | `src/main/java/com/quanlynhakhoa/billing/controller/HoaDonController.java`, `TaoHoaDonController.java`, `PaymentController.java`, `PaymentScreenController.java`, `CancellationController.java`, `DebtController.java` trong cùng thư mục |
| Dịch vụ | `src/main/java/com/quanlynhakhoa/dichvu/ui/DichVuPanel.java`, `dichvu/controller/DichVuController.java` |
| Đọc dữ liệu | `src/main/java/com/quanlynhakhoa/billing/dao/HoaDonDao.java`, `InvoiceCreateDao.java` trong cùng thư mục |
| Model | `src/main/java/com/quanlynhakhoa/billing/model/ChiTietHoaDon.java` — bổ sung tên hàng để hiển thị; constructor cũ vẫn dùng được |
| Kiểm thử đã cập nhật | `src/test/java/com/quanlynhakhoa/billing/FeatureJdbcDouble.java`, `ServiceBillingSelfTest.java`, `ServiceBillingUiSelfTest.java` |
| Kiểm thử mới | `src/test/java/com/quanlynhakhoa/billing/BillingLayoutSelfTest.java`, `BillingUiSqlPreflight.java` |
| Bàn giao | `docs/BILLING_UI_REVIEW.md` |

## Kiểm tra đã chạy

| Kiểm tra | Kết quả thực tế |
|---|---|
| Maven `verify` | PASS, JDK 25.0.4, target Java 21 |
| `scripts/test-service-billing-m1.ps1` | PASS cả 9 chương trình kiểm thử; ServiceBillingSelfTest 126 checks |
| ServiceBillingUiSelfTest bổ sung | PASS nhập giá `1.500.000` ghi đúng `1500000` vào JDBC mô phỏng; lưu kép một lần; lỗi quyền 229 có retry rõ ràng, không lộ SQLState; retry đọc ngoài EDT; ngày sai bị chặn |
| Nghiệp vụ hồi quy | PASS trên JDBC mô phỏng: tạo hóa đơn, chốt giá; thu một phần/hết/phối hợp; ghi trùng; rollback; yêu cầu/duyệt/từ chối hủy, hóa đơn đã thu, phân quyền và session; đồng bộ bốn trang qua BillingChangeBus |
| `LatestMainShellSelfTest` | PASS sáu menu, route/factory thật và điều hướng lười; synthetic, không SQL/cửa sổ hiển thị |
| `BillingLayoutSelfTest` | PASS 366 checks trong MainFrame thật, cửa sổ quy ước 1280×720 và màn hình 1920×1080; có dữ liệu, trống và lỗi quyền. Kiểm tra vùng cha không che khuất dòng nợ sau thu, các nút không ra ngoài khung; dữ liệu tổng hợp, không SQL/cửa sổ hiển thị |
| `scripts/run.ps1 -Smoke` | PASS bootstrap |
| `git diff --check` | PASS |
| Log | Có SQLState/mã lỗi và vị trí gọi trong `logs/billing-ui-*.log`; không có thông điệp driver nhạy cảm giả lập dùng trong kiểm thử. Không log SQL parameters, mật khẩu hoặc hồ sơ bệnh nhân |

Đã render 26 PNG vào `target/ui-review/`, bao gồm sáu màn hình ở hai kích thước
và trạng thái lỗi quyền. Đã xem lại ảnh của cả sáu màn hình; kiểm tra thêm Thanh toán
ở 1920×1080 và lỗi Duyệt hủy ở 1280×720. Ảnh dùng dữ liệu tổng hợp có nhãn KIỂM THỬ,
không phải bằng chứng thao tác SQL thật. PNG là vùng nội dung, trừ khoảng trang trí
cửa sổ khỏi chiều cao quy ước. Thư mục target/logs không đưa vào Git.

Lệnh tái tạo ảnh sau `test-compile`:

```powershell
java -Djava.awt.headless=false -cp 'target/classes;target/test-classes' com.quanlynhakhoa.billing.BillingLayoutSelfTest
```

## SQL development thực tế và giới hạn

Đã chạy `BillingUiSqlPreflight --read-only` bằng **application login trong cấu hình
development hiện có**, không tạo session nhân viên giả để đọc dữ liệu nghiệp vụ.
Đã kiểm tra quyền hiệu dụng và thực thi truy vấn lấy tên hàng trên schema thật,
không in dữ liệu chứng từ/bệnh nhân hoặc thông tin đăng nhập.

| Đối tượng | SELECT | INSERT | UPDATE |
|---|---:|---:|---:|
| HoaDon | 1 | 0 | 0 |
| ChiTietHoaDon | 1 | 0 | 0 |
| ThanhToan | 1 | 0 | 0 |
| YeuCauHuyHoaDon | 0 | 0 | 0 |
| DichVu | 1 | 1 | 1 |
| ChiTietDieuTri | 0 | 0 | 0 |
| ChiDinhCanLamSang | 0 | 0 | 0 |

Các cột tên của DichVu/Thuoc/VatTu, BenhNhan.soDienThoai và PhieuKham.thoiGianTiepNhan
đã được truy vấn kiểm tra trên SQL thật. Truy vấn chi tiết có JOIN tên hàng chạy được.
Truy vấn đọc YeuCauHuyHoaDon tái hiện đúng **SQLState S0005, code 229**: thiếu SELECT,
không phải lỗi layout hoặc tự động sửa bằng cách tăng quyền Java.

Không chạy GRANT, không dùng tài khoản quản trị/sa, không migration hay thay đổi V1–V5.
Huy cần review quyền tối thiểu cho các nghiệp vụ ghi và nguồn dịch vụ khám trước khi
kiểm thử thật các luồng bị chặn. Nhật ký và tài liệu nghiệp vụ cũ vẫn giữ nguyên.

**Chưa xác minh:** đăng nhập và thao tác thủ công trên cửa sổ hiển thị; tạo/thu tiền/
duyệt hủy thật và đọc lại sau mở lại ứng dụng; khóa/tranh chấp transaction trên SQL
Server thật. Các kiểm thử hồi quy cho những luồng đó hiện dùng JDBC mô phỏng, không
được coi là kiểm thử SQL thật thành công. Phiên này chỉ đọc SQL, không tạo dữ liệu mới.
