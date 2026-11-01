# Tạo hóa đơn — bổ sung phạm vi Service & Billing

**Cập nhật sau chỉ đạo “tự làm luôn”: đã triển khai adapter và bộ sinh mã, nối mặc định
vào HoaDonPanel. Quy tắc hiện hành và điều kiện chạy nằm trong
[INVOICE_CREATION_CONTRACT.md](INVOICE_CREATION_CONTRACT.md).** Những mô tả “chưa có adapter”
và “nút mặc định bị khóa” bên dưới là ghi nhận khảo sát trước chỉ đạo mới, không còn
mô tả cấu hình chạy hiện tại. Quyền SQL vẫn chưa được cấp, không có INSERT thật được thử.

Yêu cầu mới thay thế giới hạn chỉ đọc **đối với tạo hóa đơn**. Không thêm API thu tiền,
hủy lần thu, duyệt hủy, thuế, giảm giá hoặc trạng thái NHAP. PR #78 vẫn đóng;
không tự mở lại/push thay đổi của yêu cầu mới.

## Bằng chứng môi trường

Đã đọc code/schema HoaDon, ChiTietHoaDon, PhieuKham, DichVu, TaiKhoan và NhanVien.
Đã kiểm tra quyền hiệu dụng bằng **TeamDevConnection/application login**, không dùng sa:

| Bảng | SELECT | INSERT |
|---|---:|---:|
| HoaDon | 1 | 0 |
| ChiTietHoaDon | 1 | 0 |
| NhatKyThaoTac | 0 | 1 |
| PhieuKham | 1 | 1 |
| DichVu | 1 | 1 |
| BenhNhan | 1 | 1 |
| TaiKhoan | 1 | 0 |
| NhanVien | 1 | 0 |
| ChiTietDieuTri | 0 | 0 |
| ChiDinhCanLamSang | 0 | 0 |

Schema có ChiTietDieuTri liên kết KeHoachDieuTri, và ChiDinhCanLamSang liên kết
PhieuKham. **Không tìm thấy adapter/module khám hoặc hợp đồng xác định chính xác
dòng tính phí/số lượng/giá trong source hiện tại.** Không suy ra mỗi dòng điều trị
là một lần tính phí hoặc lấy tất cả dịch vụ trong danh mục. Không tự chọn trạng thái
DA_KET_LUAN là điều kiện đủ. Chưa có cơ chế sinh mã HoaDon đã được duyệt.

## Code đã hoàn thành độc lập

- InvoiceDraft chỉ ở bộ nhớ, bất biến; UI không sửa dòng, giá hoặc tổng.
- HoaDonService có patientsForInvoice, visitsForInvoice, previewInvoice, createInvoice.
  Mọi API đi qua BILLING_MANAGE và session trước connection; service đọc cũ vẫn BILLING_READ.
- InvoiceCreateDao: tìm bệnh nhân thật, lượt khám ứng viên, xác minh quan hệ, chống lập
  trùng, lấy nhân viên làm việc gắn tài khoản đang hoạt động, kiểm tra dịch vụ có thật
  và trạng thái danh mục, INSERT header/chi tiết và audit có timeout/parameter/row count.
- InvoiceCreationService: một transaction SERIALIZABLE khóa lượt khám và kiểm tra lại
  hóa đơn chưa hủy; đọc nguồn tính phí đáng tin lần nữa khi lưu, so sánh toàn bộ revision
  và dòng với bản đã xem. Giá thay đổi phải xem/xác nhận lại, không âm thầm ghi giá mới.
  Số lượng int > 0, giá BigDecimal nguyên 0..999999999999999999; không hóa đơn rỗng.
- Chỉ ghi giá từ source vào donGiaApDung; không ghi hoặc tính bằng tổng do form gửi.
  Nhân viên lấy từ accountId session; không dùng mã do form cung cấp.
- Sinh mã qua dependency được duyệt trong cùng transaction, kiểm tra mã 1–10 ASCII;
  không MAX+1/cấp quyền BoDemMa. PK/unique index hiện có bảo vệ trùng ở SQL.
- Header CHUA_THANH_TOAN; không INSERT ThanhToan. Có audit ACCOUNT/session.accountId,
  không tạo sessionId SQL giả. Lỗi chi tiết/audit rollback toàn bộ; lỗi rollback/close
  giữ nguyên nguyên nhân. Commit đã xác nhận rồi close lỗi mang invoiceId cho UI.
- TaoHoaDonDialog/Controller: tìm/chọn BN thật → chọn lượt khám ứng viên → tải các dòng
  được source duyệt → xem tổng → xác nhận → lưu ngoài EDT, khóa các control khi chạy.
  Thay bệnh nhân/lượt khám xóa bản xem trước. Ghi lỗi xóa quyền lưu bản cũ, không retry.
  Thành công lọc danh sách theo mã vừa tạo rồi mở chi tiết; reload lỗi báo rõ đã lưu.

## Điều kiện để bật tính năng

Constructor hai tham số cũ vẫn đọc Billing. Nút btnTaoHoaDon xuất hiện nhưng bị khóa,
có tooltip về dependency còn thiếu. **Chưa phải tính năng tạo hóa đơn end-to-end có
thể sử dụng trên DB hiện tại**. Không nối một adapter giả để làm nút hoạt động.

Sau khi Huy/module khám duyệt hợp đồng và quyền, composition code gọi overload:

```java
new HoaDonPanel(session, TeamDevConnection::open, approvedClinicalSource, approvedInvoiceIds);
```

Hai dependency phải là object do composition tin cậy cấp, không đến từ form:

- ClinicalBillingSource.load(connection, patientId, visitId, lock): xác minh trạng thái
  lượt khám đủ điều kiện, đọc đúng các dịch vụ đã thực hiện được phép tính phí, số lượng
  và giá theo hợp đồng. Không tự commit/đóng connection; lock=true phải giữ bằng chứng
  lâm sàng và giá đến hết transaction. revision thay đổi khi bằng chứng/quy tắc thay đổi.
- ClinicalBillingSource.validateServiceState: áp dụng chính sách danh mục đang/ngừng
  cho từng dòng lịch sử; nếu không đủ điều kiện phải ném lỗi, không bỏ dòng âm thầm.
- InvoiceIdGenerator.next(connection): cơ chế sinh mã đã được duyệt, cùng connection;
  chịu trách nhiệm an toàn đồng thời, không tự nâng quyền và không tự retry ghi.

Các quyết định cần module khám/Huy trả lời: nguồn dòng CLS/điều trị, liên kết lượt khám
thực hiện (không nhầm với lượt tạo kế hoạch), chống tính hai nguồn trùng dịch vụ,
số lượng, điều kiện kết thúc/đủ tính phí, thời điểm chốt giá, trạng thái ngừng dịch vụ,
revision/khóa bằng chứng, cơ chế mã. Không suy đoán các quy tắc này từ tên cột.

## Đề xuất quyền tối thiểu — chỉ để Huy review, CHƯA CHẠY

Đối tượng cấp: đúng application login n08dev_* / database N08Dev_* đang cấu hình.
Đề xuất bổ sung **INSERT trên dbo.HoaDon và dbo.ChiTietHoaDon**.
Giữ SELECT hiện có và INSERT audit hiện có. Không cần DELETE, UPDATE hóa đơn,
UPDATE/INSERT ThanhToan, db_owner, DDL hoặc quyền BoDemMa cho code hiện tại.
Sau khi chọn adapter khám, review riêng SELECT trên đúng bảng/cột nguồn cần dùng
(có thể gồm ChiTietDieuTri, KeHoachDieuTri, ChiDinhCanLamSang). Không cấp quyền nguồn
theo phỏng đoán. Quyền cần bởi bộ sinh mã phải được review riêng sau chọn cơ chế.
Không chỉnh grant script Core, migration V1–V5 hoặc tự thực thi GRANT.

Tài khoản dev.admin SYSTEM_ADMIN trong onboarding có thể không gắn NhanVien;
service cố ý từ chối trường hợp đó. Nghiệm thu ghi bằng RECEPTIONIST có NhanVien thật
do chủ Core chuẩn bị, không tự tạo nhân viên hay bypass điều kiện.

## Kiểm thử

Đã chạy PASS `mvnw.cmd -q clean verify test-compile` và script self-test bên dưới,
bao gồm 125 checks nghiệp vụ cũ, Swing headless và InvoiceCreationSelfTest.
`git diff --check` PASS cho thay đổi tracked. Không sửa Core/migration/grant script.

Chạy `scripts/test-service-billing-m1.ps1`: self-test cũ, Swing headless và
InvoiceCreationSelfTest. Kiểm thử tạo dùng JDBC double và adapter synthetic chỉ trong
test: ghi header + hai dòng (gồm giá 0) + audit, tổng đúng; giá nguồn thay đổi chặn;
lỗi dòng thứ hai và audit rollback header/dòng; trùng lượt khám, sai BN/lượt khám,
thiếu nhân viên, thiếu quyền SQL, rỗng/số lượng/giá sai, role chỉ đọc và logout bị chặn;
không cấu hình hợp đồng bị chặn trước JDBC; không có thao tác ghi ThanhToan.

**Chưa nghiệm thu**: INSERT/đọc lại SQL thật, restart ứng dụng thấy hóa đơn, giá danh mục
thay đổi sau lưu không ảnh hưởng dữ liệu DB, hai connection lập cùng lượt khám,
bấm Lưu liên tục trong dialog thật, flow UI thật với nguồn khám được duyệt.
Không có dữ liệu khám giả được thêm. Các kiểm thử này chờ contract, ID, SQL grants
và tài khoản nghiệp vụ hợp lệ; không gọi kết quả JDBC double là SQL integration PASS.
