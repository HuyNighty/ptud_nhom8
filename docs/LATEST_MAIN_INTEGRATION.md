# Tích hợp giao diện main mới

## Nguồn và bảo toàn thay đổi

- Đã fetch origin/main: `d9b11c5` — refactor(ui): đổi giao diện (#83).
  Trong lúc tích hợp, upstream đổi lịch sử từ d3db3da sang d9b11c5; đã xác minh hai
  commit có cùng tree 0cc2d90c46cc0fc41eb4f59637bc7718cb05ffce và merge cả head mới.
- Workspace gốc C:\Java\ptud_nhom8 vẫn giữ nhánh cũ và các thay đổi prototype chưa commit.
  Không reset, stash, ghi đè hoặc copy source Spring cũ sang tính năng JDBC.
- Worktree triển khai: C:\Java\ptud_nhom8\.worktrees\service-billing-m1.
- Nhánh tích hợp cục bộ: `codex/latest-shell-integration`.
- Checkpoint bảo toàn toàn bộ code Billing/dịch vụ trước merge: `d7e3d3a`.
- Merge main phát sinh ba xung đột ở MainFrame, FeatureBootstrap và FeatureRegistrySelfTest.
  MainFrame dùng bản sáu menu từ main mới; thêm lại factory/refresh/chuyển sang thu tiền.
  FeatureBootstrap giữ đăng ký PATIENTS mới và ACCOUNTS; bổ sung các factory nghiệp vụ.
- Source account/benhnhan/lichhen sau merge giữ nguyên so với origin/main; không thay
  các thay đổi nhân sự/tài khoản/bệnh nhân mới để giải quyết xung đột.
- Không push, tạo PR mới hay mở lại PR #78. Không đổi cấu hình DB, secret, schema hoặc grants.

## Đường dẫn màn hình thực tế

| Menu main hiện có | Mục con | Màn hình |
|---|---|---|
| Tổ chức | Dịch vụ & Bảng giá | DichVuPanel COMBINED, cùng bảng DichVu hiện có |
| Hoạt động | Hóa đơn | HoaDonPanel: chứng từ, tạo, xem chi tiết, yêu cầu hủy, chuyển thu tiền |
| Hoạt động | Thanh toán | PaymentPanel: tìm/chọn nhanh HĐ, PaymentForm và lịch sử giao dịch |
| Hoạt động | Duyệt hủy hóa đơn | CancellationPanel: yêu cầu, chi tiết, duyệt/từ chối/lịch sử |
| Tổng kết | Công nợ | DebtPanel: tổng hợp BN, các hóa đơn còn nợ của BN, Thu nợ |

Giữ Tổng quan – Tổ chức – Kế hoạch – Tiếp nhận – Hoạt động – Tổng kết. Không tạo
menu Danh mục hoặc Thanh toán cấp 1. Không đưa Hóa đơn/Thanh toán vào Tổng kết trùng.
Module SERVICES cũ là alias tương thích, MainFrame chuyển về SERVICE_CATALOG; không có
hai mục menu hay hai nguồn dữ liệu. Registry giữ factory tương thích cho lời gọi cũ.

Nguồn đã có đủ các màn hình yêu cầu, không đánh dấu placeholder là hoàn thành.
Các chức năng khác được giữ như main mới (Bệnh nhân/Tài khoản có đăng ký thật; các
placeholder còn lại không nằm trong phạm vi này và không bị tự nhận là đã triển khai).

## Thay đổi bố cục và xử lý

- Hóa đơn giữ danh sách chứng từ/filter/tạo/yêu cầu hủy. Các hóa đơn còn nợ vẫn tồn tại
  ở đây. Nút Thanh toán dùng callback MainFrame để chuyển CardLayout tới PaymentPanel
  và nạp đúng invoiceId, không sinh thêm chứng từ hoặc thu bằng cơ chế khác.
- DebtService.byPatient tổng hợp chỉ các hóa đơn chưa hủy, còn nợ > 0 theo kết quả lọc.
  DebtPanel hiển thị một dòng/BN; chọn BN mới xem bảng hóa đơn phía bên, chọn HĐ xem
  giá chốt/lịch sử. Thu nợ chuyển tới cùng PaymentPanel. Không thêm hạn thanh toán.
- PaymentPanel không kế thừa/sao chép nguyên HoaDonPanel. Nó có ô tìm kiếm và combo
  HĐ gọn, phần chính là form thu và lịch sử. PaymentForm được trích từ PaymentDialog
  có sẵn, cùng PaymentController/PaymentService/PaymentDao. PaymentDialog giữ wrapper
  để không làm hỏng caller cũ; không có hai implementation ghi nhận tiền.
- InvoiceHistoryPanel dùng chung cho form thu và xem công nợ. Tiền mặt/CK/phối hợp,
  mã giao dịch, khóa thao tác lặp, pending/kiểm tra commit không rõ kết quả, rollback,
  quyền/session, nhân viên từ tài khoản vẫn do Service cũ kiểm tra.
- Nếu khoản thu chưa rõ kết quả, không cho đổi HĐ/form làm mất các ID dùng đối soát.
  Trang thu giữ lịch sử HĐ vừa thu đủ dù HĐ không còn trong combo còn nợ.
- DichVuPanel gộp metadata và giá hiện hành, dùng model/service/DAO cũ. Không có bảng
  giá độc lập hoặc lịch sử/ngày hiệu lực trong schema nên không tạo tab dữ liệu giả.
  Giá chốt ChiTietHoaDon không bị cập nhật khi sửa danh mục.
- Palette trắng/xanh dùng màu của main mới; giữ nguyên header/thanh menu hiện có.

## Cập nhật các màn hình đã mở

BillingChangeBus giới hạn theo DesktopSession, listener weak để không giữ cửa sổ cũ
vĩnh viễn. Sau tạo HĐ, thu tiền, gửi/xét yêu cầu hủy hoặc sửa dịch vụ, controller phát
thông báo trên EDT. HoaDonController, DebtController, PaymentScreenController và
CancellationController đều nhận thông báo **kể cả card đang ẩn** và truy vấn lại bằng
SwingWorker. Khi đang tải, có cơ chế chờ một lần refresh tiếp theo; không gọi lại thao tác ghi.

PaymentForm giữ các số tiền đang nhập trong lúc tải lại và giữ request chưa rõ kết
quả; khi thu thành công mới xóa nhập liệu. Trang danh mục không tự xóa form chưa lưu
chỉ vì màn hình khác vừa thu tiền. Session logout không mở connection mới hoặc đưa
payload nhạy cảm lên UI. RefreshableFeature khi điều hướng vẫn được giữ làm lớp kiểm
tra bổ sung. Không tuyên bố live push giữa hai app/session trên hai máy khác nhau;
các session khác dùng Tải lại và Service luôn kiểm tra lại điều kiện trong transaction.

## File thay đổi/thêm trong tích hợp

Đường dẫn Java bên dưới tính từ src/main/java/com/quanlynhakhoa:

| Nhóm | File |
|---|---|
| Shell main | view/MainFrame.java; features/FeatureBootstrap.java |
| Đồng bộ | billing/controller/BillingChangeBus.java (mới); HoaDonController, PaymentController, CancellationController, CancellationRequestController, TaoHoaDonController |
| Công nợ | billing/model/PatientDebt.java (mới); billing/service/DebtService.java; billing/ui/DebtPanel.java và billing/controller/DebtController.java (mới) |
| Form thu dùng chung | billing/ui/PaymentForm.java và InvoiceHistoryPanel.java (mới); PaymentDialog.java; billing/ui/PaymentPanel.java và billing/controller/PaymentScreenController.java (mới) |
| Hình thức | billing/ui/BillingUi.java, HoaDonPanel.java, CancellationPanel.java; dichvu/ui/DichVuPanel.java; dichvu/controller/DichVuController.java |
| Kiểm thử | LatestShellUiSelfTest.java, view/LatestMainShellSelfTest.java (mới); FeatureRegistrySelfTest, ServiceBillingUiSelfTest, ServiceBillingPreview; scripts/test-service-billing-m1.ps1 |

Ngoài bảng trên, merge đưa vào các source account/benhnhan/lichhen mới từ main,
giữ đúng bản upstream. Logic giá lịch sử/ghi hóa đơn/thu tiền/hủy ở các Service/DAO
hiện hữu được tái sử dụng, không viết lại hoặc thay quyền nghiệp vụ.

## Bằng chứng kiểm thử

JDK 25.0.4, target release 21 từ pom không đổi:

- `mvnw.cmd -q clean verify test-compile`: PASS.
- `scripts/test-service-billing-m1.ps1`: PASS 9 self-test main (gồm nghiệp vụ cũ 125
  checks, Swing, tạo hóa đơn, nguồn giá, thu tiền, tách catalog, hủy và cạnh tranh mô phỏng,
  cộng kiểm tra bố cục/cached-card invalidation mới).
- Role/session 686 checks, navigation 75, shell 1697, registry 28: PASS.
- AccountStateSelfTest của main mới: PASS 685 checks (JDBC synthetic/headless).
- LatestMainShellSelfTest: PASS — dựng MainFrame thật, xác minh đúng sáu menu,
  đúng mục con, click điều hướng các factory thật. Không mở cửa sổ người dùng, không
  truy cập SQL; user.dir được đặt vào thư mục test không có config trước khi dựng shell.
- LatestShellUiSelfTest: PASS — công nợ một dòng/BN với hóa đơn con, layout thu riêng,
  thông báo thay đổi tới cả bốn card đã tạo, không ảnh hưởng session khác, logout chặn
  refresh trước JDBC. Dữ liệu/connection trong kiểm thử này là synthetic.
- Đã render và xem ảnh shell, Công nợ, Thanh toán dưới target/, không commit ảnh.
- `App --team-db` với application login: PASS kết nối SQL development thực tế.
- Smoke bootstrap và diff so với origin/main: PASS. Có một dòng whitespace sẵn có
  trong AccountPanel của upstream; giữ nguyên source thành viên khác, không đưa sửa
  định dạng ngoài phạm vi vào tích hợp này.

**Chưa nghiệm thu ghi SQL thật**, đăng nhập tương tác bằng tài khoản nghiệp vụ thật,
restart đọc lại một lần thu mới hoặc khóa cạnh tranh bằng hai connection SQL Server.
Application login trước đó đã xác minh thiếu quyền INSERT hóa đơn/thu, UPDATE trạng
thái và quyền bảng hủy/nguồn khám. Tích hợp này không tự cấp thêm quyền hoặc chạy bằng
sa. Không coi SQL connection PASS là nghiệp vụ thu/hủy SQL PASS. Các bảng/secret và dữ
liệu development hiện có được giữ nguyên.

## Chạy

```powershell
cd C:\Java\ptud_nhom8\.worktrees\service-billing-m1
.\scripts\run.ps1
```

Đóng bản app cũ trước khi chạy lại để nạp lớp/menu mới. Chạy main com.quanlynhakhoa.App
với working directory của worktree này cũng tương đương. Launcher feature cũ vẫn có
thể dùng kiểm thử độc lập, nhưng shell chính sáu menu là luồng tích hợp chuẩn.
