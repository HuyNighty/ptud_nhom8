# Gửi yêu cầu và duyệt hủy hóa đơn

## Luồng đã triển khai

Menu Thanh toán giữ Hóa đơn, Công nợ, Thanh toán và thêm **Duyệt hủy hóa đơn**.
Route INVOICE_CANCELLATIONS dùng INVOICE_CANCEL_APPROVE. Không đổi RolePermissionPolicy:
RECEPTIONIST có INVOICE_CANCEL_REQUEST; CLINIC_MANAGER có INVOICE_CANCEL_APPROVE;
SYSTEM_ADMIN theo policy Core hiện có. Người gửi không được tự duyệt **hoặc từ chối**
do cả kiểm tra Service và CK_YCHuy_KhongTuDuyet của schema.

Tại Hóa đơn, nút **Yêu cầu hủy** xuất hiện với quyền gửi yêu cầu. Dialog đọc thông tin
hóa đơn, tổng/đã thu/còn nợ, yêu cầu lý do trim 1–500 ký tự và xác nhận. INSERT trạng thái
CHO_DUYET cùng audit, lưu accountId session/thời điểm SQL. Không đổi trạng thái hóa đơn.
Khóa header và unique index UX_YCHuy_ChoDuyet bảo vệ gửi trùng, không tự retry ghi.
Sau lỗi chưa biết commit, khóa gửi lại và yêu cầu kiểm tra/tải lại trước khi xác nhận mới.

Trang xét duyệt có tìm theo mã yêu cầu/HĐ/BN/tên BN/người gửi, lọc thời gian tạo và
CHO_DUYET/DA_DUYET/TU_CHOI. Ngày kết thúc bao gồm cả ngày. Danh sách có lý do yêu cầu;
khung chi tiết hiển thị đầy đủ lý do, người gửi/thời điểm, người xử lý/thời điểm/ý kiến,
thông tin chứng từ và hai tab dòng hóa đơn/lịch sử thanh toán. Chọn dòng sau sort dùng
convertRowIndexToModel. Dữ liệu bảng không sửa trực tiếp.

Chỉ yêu cầu CHO_DUYET được thao tác. Duyệt và từ chối đều cần xác nhận; từ chối cần
lý do 1–500 ký tự. Khi duyệt, ý kiến có thể trống và Service ghi mặc định “Đồng ý hủy
hóa đơn” để đáp ứng CK_YCHuy_DaXuLy. thoiGianDuyet/maTaiKhoanDuyet/lyDoDuyet cũng lưu
quyết định TU_CHOI theo schema, không thêm trạng thái hay cột riêng.

## Điều kiện và transaction

Mọi API Service đọc/ghi yêu cầu quyền tương ứng và session trước mở connection, kiểm tra
lại trước trả kết quả/commit. SQL PreparedStatement, timeout 15s, try-with-resources.
DAO không commit hoặc đóng connection của Service. JDBC chạy qua SessionWorker ngoài EDT;
nút/fields bị khóa khi xử lý, session logout không nhận dữ liệu nhạy cảm vào UI.

- **Gửi:** transaction SERIALIZABLE → khóa header UPDLOCK/HOLDLOCK → xác minh hóa đơn
  tồn tại/chưa hủy → kiểm tra chưa có yêu cầu CHO_DUYET → INSERT yêu cầu → audit → commit.
  Có thể gửi yêu cầu khi đã thu tiền, nhưng UI thông báo chưa thể duyệt cho đến khi
  có quy trình xử lý khoản thu phù hợp; không giả lập giao dịch hoàn tiền.
- **Xét duyệt:** tìm invoiceId của request trong auto-commit, kết thúc câu đọc đó rồi
  bắt đầu SERIALIZABLE → khóa header trước, khóa request sau → đọc lại request/kiểm tra
  cùng hóa đơn, trạng thái CHO_DUYET, người xét khác người gửi → đọc lại khoản thu hợp lệ.
  Thứ tự khóa ghi trùng với gửi/thu tiền, tránh giữ khóa request trước khi chờ header.
- **Duyệt:** daThu tính SUM(ThanhToan.soTien) chỉ THANH_CONG. Nếu khác 0, Service chặn với
  lý do chưa có hoàn tiền. UI khóa nút duyệt và giải thích, vẫn có thể từ chối hợp lệ.
  Khi được duyệt, UPDATE HoaDon.trangThai=DA_HUY và yêu cầu=DA_DUYET, ghi audit cùng
  transaction. Không sửa số đã thu, dòng hóa đơn hoặc lịch sử thanh toán.
- **Từ chối:** chỉ UPDATE yêu cầu=TU_CHOI và audit. Không tự viết lại trạng thái hóa đơn,
  giá hoặc khoản thu. Hóa đơn tiếp tục trạng thái nghiệp vụ trước khi gửi yêu cầu.
- UPDATE request có điều kiện CHO_DUYET và maTaiKhoanTao<>người xét; kiểm tra đúng một
  dòng. Yêu cầu đã xử lý không xét lại. Lỗi cập nhật/audit rollback toàn bộ, giữ lỗi gốc
  cùng suppressed rollback/close. Commit đã xác nhận nhưng close lỗi có exception riêng.

Khi chờ duyệt, hóa đơn vẫn còn nợ và vẫn xuất hiện trong Công nợ. PaymentService kiểm
tra yêu cầu chờ duyệt dưới khóa header trong transaction trước INSERT; PaymentController
cũng đọc trạng thái chờ để khóa form thu mới và hiện lý do. Sau từ chối có thể thu tiếp;
sau duyệt DA_HUY không thể thu và bị loại khỏi projection công nợ. Replay một mã lần thu
đã ghi chỉ trả kết quả cũ, không ghi thêm tiền. Hiện dự án không có API sửa nội dung
hóa đơn đã lưu (tạo và đọc là các thao tác hiện hữu), nên không có đường chỉnh nội dung
lách qua yêu cầu chờ. Thay giá danh mục không thay giá chốt của chứng từ.

Trang xét duyệt tải lại sau mỗi thao tác. Hóa đơn/Công nợ/Thanh toán và trang xét duyệt
đều implement RefreshableFeature, tự đọc dữ liệu committed khi quay lại menu. Dialog
gửi yêu cầu làm mới danh sách hóa đơn; các trang đang cache không giữ dữ liệu cũ khi
được điều hướng lại. Không mở lại PR #78 hoặc tự push thay đổi.

## Schema và SQL development

Không đổi schema hoặc migration V1–V5. Tái sử dụng YeuCauHuyHoaDon và ba trạng thái,
FK, CK_KhongTuDuyet, CK_DaXuLy, unique index yêu cầu chờ hiện có. Không DELETE/DDL.
Mã yêu cầu dùng RandomInvoiceIdGenerator đã có (10 ký tự), PK bảo vệ trùng, không MAX+1.

Đã kiểm tra bằng TeamDevConnection/application login trên SQL thật:

| Quyền hiệu dụng | Kết quả |
|---|---:|
| SELECT YeuCauHuyHoaDon | 0 |
| INSERT YeuCauHuyHoaDon | 0 |
| UPDATE YeuCauHuyHoaDon | 0 |
| UPDATE HoaDon.trangThai | 0 |

Do vậy **chưa nghiệm thu gửi/xét duyệt SQL thật**, không khẳng định đã ghi/đọc lại sau
restart. Không dùng sa, không tự GRANT hoặc tạo dữ liệu giả để vượt giới hạn.
Đề xuất cho Huy review riêng: SELECT và INSERT YeuCauHuyHoaDon; UPDATE các cột
trangThai,maTaiKhoanDuyet,thoiGianDuyet,lyDoDuyet; UPDATE HoaDon.trangThai; giữ INSERT
audit và SELECT chứng từ/chi tiết/thanh toán/bệnh nhân hiện có. Không cần quyền ghi
ThanhToan cho việc hủy. Quyền ứng dụng vẫn được kiểm tra riêng tại Service dù dùng
chung SQL login. Có thể cần chủ SQL kiểm tra quyền đối với lock hints khi cấp quyền cột.

## File chính

- billing/model/YeuCauHuyHoaDon.java
- billing/dao/CancellationDao.java
- billing/service/CancellationService.java
- billing/ui/CancellationRequestDialog.java, CancellationPanel.java
- billing/controller/CancellationRequestController.java, CancellationController.java
- Cập nhật HoaDonPanel/HoaDonController (gửi), PaymentDao/PaymentService/PaymentController
  (khóa thu khi chờ), navigation/Module.java, view/MainFrame.java, features/FeatureBootstrap.java.
- Kiểm thử CancellationSelfTest, CancellationConcurrencySelfTest; cập nhật registry,
  role/navigation/shell tests, PaymentSelfTest, ServiceBillingPreview và script self-test.

Đường dẫn Java tính từ src/main/java/com/quanlynhakhoa hoặc src/test/java/com/quanlynhakhoa.

## Kiểm thử

`scripts/test-service-billing-m1.ps1` chạy rõ ràng các main self-test, gồm:

- Gửi/preview, lý do rỗng/quá dài, gửi trùng, hóa đơn đã hủy, duyệt và từ chối.
- Không tự hủy khi gửi, không có API hoàn tiền giả, chặn duyệt khi còn lần thu hợp lệ;
  từ chối giữ nguyên hóa đơn/khoản thu. Không duyệt lại/tự xét, thiếu quyền/logout trước JDBC.
- Lỗi audit rollback cả cập nhật request và header, không DELETE/UPDATE lịch sử thu.
- Hai người gửi cùng lúc chỉ một yêu cầu thành công; hai quản lý duyệt/từ chối cùng
  lúc chỉ một quyết định được commit. Bài kiểm thử này dùng transaction/lock mô phỏng
  trong bộ nhớ, **không phải** thử hai connection SQL Server thật.
- Hồi quy thu tiền có chặn yêu cầu chờ; kiểm tra quyền cho API trạng thái chờ mới.

Regression hiện có sau thêm route: role 686 checks, navigation 75, shell 1697;
registry tăng thêm kiểm tra route xét duyệt. Đã render/xem bố cục trang bằng dữ liệu
tổng hợp ở target/, không commit ảnh. Maven verify/test-compile và các self-test được
chạy trong quá trình bàn giao; không suy ra kiểm thử JDBC main từ riêng Maven verify.

Chưa chạy UI xét duyệt với DB thật hoặc cạnh tranh thu tiền/xét duyệt trên SQL thật.
Sau khi quyền được duyệt, cần hai tài khoản thực khác nhau (người gửi và quản lý),
hóa đơn development hợp lệ: gửi → chặn thu nhưng còn công nợ → từ chối có lý do → thu
hoạt động lại; hóa đơn chưa thu khác → gửi → duyệt → DA_HUY/hết danh sách công nợ;
hóa đơn có thu → gửi → chặn duyệt, giữ nguyên lịch sử. Thử hai cửa sổ quyết định cùng
lúc, đóng/mở App đọc lại kết quả và lỗi giữa các UPDATE/audit phải rollback toàn bộ.
