# Menu nghiệp vụ và thu tiền — trạng thái bàn giao

Tài liệu này cập nhật phạm vi mới, thay thế các giới hạn “chỉ đọc/không thu tiền” trong
tài liệu M1 cũ. Không tự mở lại PR #78. Mã được sửa tại worktree service-billing-m1.

## Điều hướng và giao diện

- Thanh toán → Hóa đơn: mọi hóa đơn, tìm/lọc ngày/trạng thái, tạo và xem chứng từ,
  chi tiết/đã thu/còn nợ. Không đưa hóa đơn còn nợ ra khỏi danh sách chứng từ.
- Thanh toán → Công nợ: cùng dữ liệu hóa đơn, chỉ chưa hủy và còn nợ > 0; tổng công nợ
  tính trên kết quả lọc. Không tự thêm hạn thanh toán/quá hạn. “Thu nợ” dùng PaymentService.
- Thanh toán → Thanh toán: tìm hóa đơn còn nợ, xem chi tiết/lịch sử và thu tiền.
  Tìm theo mã HĐ, mã/tên BN hoặc số điện thoại (schema BenhNhan có soDienThoai).
- Nút Thanh toán/Thu nợ chuyển đến trang Thanh toán và mở đúng hóa đơn đã chọn.
  Khi thu xong tải lại trang Thanh toán; các trang cache khác tự tải lại khi quay lại
  menu. Hóa đơn thu đủ biến mất khỏi Công nợ, vẫn tồn tại trong Hóa đơn và lịch sử.
- Danh mục → Dịch vụ: thêm/sửa metadata, CLS, trạng thái. Dịch vụ mới có giá 0,
  đặt giá ở trang Bảng giá. UPDATE metadata không ghi đè donGia khi người khác sửa giá.
- Danh mục → Bảng giá dịch vụ: chọn dịch vụ có sẵn, sửa donGia/trạng thái áp dụng.
  UPDATE giá không ghi đè tên/mô tả/CLS. Hai trang dùng cùng model/service/DAO/bảng DichVu.
  Không có ngày hiệu lực/phiên bản giá trong schema hiện tại, không tạo dữ liệu giả cho chúng.
- Module BILLING/SERVICE_CATALOG được giữ làm route chứng từ/bảng giá. Thêm SERVICES,
  DEBTS, PAYMENTS. Không còn route trùng trong menu Hóa đơn cũ. Màu menu con và menu cha
  cập nhật theo route. Không có màn hình Duyệt hủy đã triển khai trong baseline này;
  không tự bổ sung chức năng hủy. Giữ nguyên RolePermissionPolicy và quyền duyệt hiện có.

Phân quyền không đổi: đọc Hóa đơn/Công nợ cần BILLING_READ; thu tiền/mở trang Thanh toán
cần BILLING_MANAGE; cả hai mục Danh mục cần SERVICE_CATALOG_MANAGE. CLINIC_MANAGER chỉ
đọc Billing, quản lý danh mục; RECEPTIONIST thu tiền; SYSTEM_ADMIN vẫn theo policy Core.
Service luôn kiểm tra session/quyền trước JDBC. Thu tiền cần nhân viên thật, đang làm
việc, gắn tài khoản đang hoạt động; không tự gán NV cho dev.admin nếu thiếu liên kết.

## Nghiệp vụ và transaction thu tiền

PaymentRequest chứa invoiceId, số tiền thu và một hoặc hai phần phân bổ. Mỗi phần có
maThanhToan riêng ổn định trong lần xác nhận, số tiền, phương thức, tham chiếu và tiền
khách đưa (chỉ tiền mặt). Tiền VND nguyên dùng BigDecimal, mỗi phần trong DECIMAL(18,0).
Tổng phần phân bổ phải bằng số tiền thu, từng phần > 0. Tiền khách đưa >= phần tiền mặt.
Chuyển khoản yêu cầu mã giao dịch 1–50 ASCII và xác nhận nhận tiền thủ công. Không có
cấu hình ngân hàng/QR trong source hiện tại; không bịa tài khoản hoặc tự xác nhận QR.

PaymentService.collect thực hiện cùng một transaction SERIALIZABLE:

1. Kiểm tra quyền Java, quyền SQL ghi cần thiết, khóa header hóa đơn UPDLOCK/HOLDLOCK.
2. Lấy nhân viên từ accountId session; kiểm tra các ID lần thu đã tồn tại hay chưa.
   Gửi lại đúng cùng ID, invoice, cashier, amount, method, reference, tender và trạng
   thái THANH_CONG trả kết quả đã ghi nhận, không INSERT/audit lần hai. ID khác dữ liệu
   hoặc bộ phân bổ chỉ tồn tại một phần bị chặn để đối soát.
3. Đọc lại tổng từ giá chốt ChiTietHoaDon và đã thu từ ThanhToan.THANH_CONG bằng các
   subquery riêng, chỉ cho hóa đơn đang khóa. Không nhân bản tổng do JOIN nhiều dòng.
   Không lấy tienKhachDua thay soTien; không lấy giá hiện tại danh mục cho hóa đơn cũ.
4. Chặn hóa đơn hủy, hết nợ, dữ liệu trạng thái/tiền bất nhất, chờ duyệt hủy, thu vượt.
   Kiểm tra tham chiếu chuyển khoản chưa có trong lần thu thành công khác dưới khóa.
5. Tiền mặt + CK ghi thành hai hàng ThanhToan trong cùng transaction vì schema hiện
   có mỗi hàng một phương thức. Một phần lỗi rollback cả hai hàng, trạng thái và audit.
6. UPDATE chỉ trangThai hóa đơn sang THANH_TOAN_MOT_PHAN hoặc DA_THANH_TOAN; ghi audit
   ACCOUNT theo session thật rồi commit. Không xóa/hủy dữ liệu, không ghi tổng tiền riêng.

UI khóa nút/fields khi xử lý. Nếu ghi có lỗi chưa biết commit, giữ các ID cũ trong dialog,
khóa thu tiếp và cho “Kiểm tra kết quả lần thu” bằng truy vấn read-only. Khi thấy đủ ID,
kiểm tra lại dữ liệu và trạng thái trước khi báo đã thu. Nếu chưa có ID thì cho người
dùng kiểm tra/xác nhận mới, không tự retry ghi. Sau đóng dialog/app, trạng thái request
trong bộ nhớ không còn; phải xem lịch sử thu để đối soát trước khi lập yêu cầu mới.
Chỉ bảo đảm chống nhấn lặp và gửi lại cùng ID, không tuyên bố chống được mọi lần nhập
lại tiền mặt bằng ID mới. Commit đã xác nhận nhưng close lỗi có exception riêng.

## Database thực tế và quyền đang thiếu

Đã đọc INFORMATION_SCHEMA.COLUMNS và HAS_PERMS_BY_NAME bằng application login thật.
Schema thực tế khớp V5. DichVu có INSERT/UPDATE; HoaDon, ChiTietHoaDon và ThanhToan hiện
**INSERT=0, UPDATE=0**. Không chạy sa, GRANT, DDL, bootstrap hoặc tạo dữ liệu khám/thu giả.

**Không thay đổi schema/database**, không cần migration, không tạo bảng giá/công nợ/
receipt trùng. Các ID lần thu dùng khóa chính ThanhToan hiện có; phối hợp phương thức
được bảo vệ bằng transaction. Đề xuất cấp quyền tối thiểu cho chủ Core review riêng:

| Đối tượng | Quyền cần bổ sung |
|---|---|
| HoaDon | INSERT cho tạo hóa đơn; UPDATE chỉ cột trangThai cho thu tiền |
| ChiTietHoaDon | INSERT cho tạo hóa đơn |
| ThanhToan | INSERT; giữ SELECT hiện có |
| YeuCauHuyHoaDon | SELECT để chặn thu khi chờ duyệt hủy |
| ChiTietDieuTri, ChiDinhCanLamSang | SELECT cho nguồn tính phí tạo hóa đơn |

Giữ các SELECT source khác và INSERT NhatKyThaoTac hiện có. Không cần UPDATE/DELETE
ThanhToan, DELETE danh mục, BoDemMa, quyền cấu trúc hoặc db_owner. Bộ kiểm tra UPDATE
dùng quyền cột trangThai; không đòi UPDATE toàn bảng. Không tự thực thi đề xuất này.

Vì thiếu quyền ghi, **chưa có bằng chứng ghi/đọc lại thu tiền SQL thật sau restart**.
Không coi compile/JDBC double là kiểm thử thật. Sau cấp quyền và có bệnh nhân, khám,
hóa đơn, nhân viên hợp lệ, chạy App để nghiệm thu bằng dữ liệu development được duyệt.

## File thêm/sửa trong đợt tái tổ chức này

| Nhóm | File |
|---|---|
| Menu/Core | navigation/Module.java; view/MainFrame.java; features/FeatureBootstrap.java; features/RefreshableFeature.java (mới) |
| Chứng từ/công nợ | billing/ui/HoaDonPanel.java; billing/controller/HoaDonController.java; billing/dao/HoaDonDao.java; billing/service/DebtService.java (mới) |
| Thu tiền mới | billing/model/PaymentRequest.java; billing/dao/PaymentDao.java; billing/service/PaymentService.java; billing/ui/PaymentDialog.java; billing/controller/PaymentController.java |
| Danh mục | dichvu/ui/DichVuPanel.java; dichvu/controller/DichVuController.java; dichvu/service/DichVuService.java; dichvu/dao/DichVuDao.java |
| Kiểm thử mới | billing/PaymentSelfTest.java; billing/CatalogSeparationSelfTest.java |
| Kiểm thử cập nhật | billing/ServiceBillingPreview.java; features/FeatureRegistrySelfTest.java; security/R4RoleSessionSelfTest.java; security/R4FinalShellSelfTest.java; view/R4MainNavigationSelfTest.java; scripts/test-service-billing-m1.ps1 |

Đường dẫn Java trong bảng tính từ src/main/java/com/quanlynhakhoa hoặc src/test/java/
com/quanlynhakhoa. Những file tạo hóa đơn còn uncommitted từ yêu cầu trước được giữ nguyên.

## Chạy và kiểm thử

```powershell
cd C:\Java\ptud_nhom8\.worktrees\service-billing-m1
.\mvnw.cmd clean verify test-compile
.\scripts\test-service-billing-m1.ps1
.\scripts\test-r4-role-session.ps1
.\scripts\test-r4-main-navigation.ps1
.\scripts\test-r4-final-shell.ps1
.\scripts\run.ps1
```

Đã PASS Maven clean verify/test-compile, self-test nghiệp vụ cũ 125 checks, Swing headless,
tạo hóa đơn/nguồn tính phí, PaymentSelfTest (một phần/đủ/phối hợp, replay, chặn vượt/hủy,
tham chiếu trùng, rollback phần thứ hai/audit, sai role/logout), và test tách UPDATE
giá/metadata. Regression quyền 670 checks, navigation 72, shell 1675, registry 27.
Đã dựng/xem ảnh bố cục các trang bằng dữ liệu tổng hợp dưới target/, không commit ảnh.

Chưa nghiệm thu SQL write/restart, khóa đồng thời trên hai connection SQL thật, nhận
chuyển khoản thực tế hoặc tương tác đầy đủ dialog thu tiền với DB. Cần thử sau khi quyền
được review: tạo HĐ thật → thu một phần → kiểm tra Công nợ giảm → phối hợp tiền mặt/CK
thu hết → Công nợ hết dòng, Hóa đơn còn chứng từ, lịch sử có đủ các phương thức → đóng
App/mở lại đọc đúng. Thử hai máy cùng thu và lỗi giữa các INSERT/audit phải không thu vượt.
