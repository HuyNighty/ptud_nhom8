# Tinh chỉnh Service & Billing theo sáu ảnh mới — 31/10/2026

Đã sửa trong worktree `service-billing-m1`. Giữ menu, route, phân quyền, transaction
và kiến trúc hiện có. Đợt này không thay đổi quyền SQL, schema hoặc dữ liệu development.

## Thay đổi

- Bộ lọc có chiều rộng giới hạn: tìm kiếm 320 px, trạng thái 210 px, ngày 175 px;
  cả nhóm trường xuống hàng khi cửa sổ hẹp. Chiều cao nhóm được tính theo nội dung/font.
  Bảng nhận không gian tăng thêm. Nhãn thường dùng chữ thường nét, thẻ tổng hợp có nhãn
  nhỏ/giá trị lớn, còn nợ có màu và nhãn rõ ràng. Trạng thái trống đủ tương phản và
  được tách khỏi hướng dẫn cuối trang.
- **Dịch vụ & Bảng giá:** form phải 350 px, mô tả cao tối đa khoảng 120 px trong bố
  cục thường; co lại khi chiều cao thiếu. Nút lưu nằm ngay dưới form, ghi rõ Lưu mới/
  Cập nhật và tiêu đề Thêm mới/Chỉnh sửa. Không hiển thị ngừng áp dụng khi đang tạo mới.
- **Hóa đơn:** cột kiểm tra số tiền là giá trị `HoaDon.canhBao()` dùng đối soát nội bộ.
  Giữ giá trị trong model và toàn bộ kiểm tra Service; thay cột bằng cảnh báo có giải
  thích khi danh sách/chi tiết phát hiện bất nhất. Ưu tiên cột tên bệnh nhân/dịch vụ;
  các thẻ cuối trang ghi rõ thuộc hóa đơn đang chọn, chưa chọn hiển thị “—”.
- **Tạo hóa đơn:** không tự chọn bệnh nhân/lượt khám đầu tiên. Hướng dẫn theo từng bước;
  phân biệt không tìm thấy bệnh nhân, không có lượt khám và không có dịch vụ tính phí.
  Đổi lựa chọn hoặc tìm mới hủy bản xem trước cũ; Service vẫn kiểm tra dữ liệu và giá.
- **Thanh toán:** tìm/chọn chứng từ thành nhóm liền nhau; form thu gọn, nút xác nhận
  ngay sau phần phân bổ, tiền thừa và nợ dự kiến. Chọn Tiền mặt/Chuyển khoản/Kết hợp;
  một phương thức tự phân bổ, kết hợp nhập hai phần. Ẩn trường không liên quan;
  tiền thừa tính từ tiền khách đưa trừ phần thu tiền mặt. Không tự điền tiền thực nhận.
  Đổi số tiền chuyển khoản bỏ xác nhận cũ; Service tiếp tục kiểm tra nhận tiền thực tế.
  Tìm kiếm mới không để chứng từ cũ sẵn sàng thu. Phản hồi tải chi tiết đến muộn không
  vượt qua khóa ngữ cảnh kết quả tìm kiếm. Khi cửa sổ rất hẹp, chỉ phần nhập được cuộn;
  các số dự kiến và thao tác xác nhận luôn ở ngoài vùng cuộn.
- **Duyệt hủy:** chưa chọn có hướng dẫn, ý kiến/nút bị khóa. Thông tin có bệnh nhân,
  chứng từ, người gửi, thời điểm, trạng thái và lý do; vùng này chỉ đọc. Ô ý kiến chỉ
  sửa khi Controller xác nhận được xử lý; nội dung dài có cuộn, kèm bộ đếm 500 ký tự.
  Khung nội dung được giới hạn chiều cao; danh sách nhận không gian tăng thêm.
- **Công nợ:** tiêu đề bảng phải ở trên, có mã/tên bệnh nhân đang chọn. Hai vùng thẳng
  hàng tiêu đề/thao tác/đầu bảng; ưu tiên nhìn thấy số còn phải thu trong cửa sổ nhỏ.
  Các thẻ tổng hợp ghi phạm vi bộ lọc ngày lập hóa đơn; giữ chia vùng điều chỉnh được.

## Kiểm tra thực tế

| Kiểm tra | Kết quả |
|---|---|
| Maven `verify` | PASS |
| `scripts/test-service-billing-m1.ps1` | PASS 10 bộ kiểm thử; bộ mới BillingRefinementSelfTest có 22 assertions |
| `CreationStepsSelfTest` | PASS 10 kiểm tra chuyển bước bất đồng bộ, thiếu dữ liệu, hủy draft khi đổi lựa chọn; không lưu SQL hoặc chụp dữ liệu giả |
| `BillingLayoutSelfTest` | PASS 2.834 kiểm tra mỗi lần chạy, DPI 100% và 125%; kích thước logical 1050×720, 1280×720, 1366×768, 1920×1080. Kiểm tra cả vùng cha che nút, đang xử lý, dữ liệu mẫu, trống và lỗi |
| Nghiệp vụ hồi quy | Kiểm thử tự động bằng JDBC mô phỏng: thu một phần/hết/phối hợp, tiền thừa, thiếu xác nhận ngân hàng, phân bổ sai, vượt nợ, busy/kết quả chưa xác định, quyền/session, tạo hóa đơn và hủy, rollback và đồng bộ trang cache |
| SQL development | Đọc thành công snapshot thật: 0 dịch vụ, 0 bệnh nhân, 0 hóa đơn, 0 yêu cầu hủy |
| Ảnh kiểm tra | 36 PNG từ snapshot SQL thật: sáu màn hình × ba kích thước được yêu cầu × hai mức DPI. Transform thực tế được kiểm tra là 1.0 và 1.25 |
| `git diff --check` | PASS |

Ảnh tại `target/ui-refinement-real/dpi100/` và `dpi125/`. Chúng là bản render component
Swing trong MainFrame, không phải thao tác đăng nhập thủ công. Header XEM-CHI-DOC
là ngữ cảnh hiển thị kiểm tra, không phải tài khoản được tạo trong database. Chỉ đọc
dữ liệu thật trước khi render; các connection của UI render bị vô hiệu hóa. Chiều cao
ảnh nội dung trừ phần trang trí cửa sổ; ảnh DPI 125% được render theo device transform.

Không tạo dữ liệu nghiệp vụ hoặc dùng dữ liệu giả để làm ảnh. Fixtures của các bài
kiểm thử logic chỉ nằm trong bộ nhớ; mặc định BillingLayoutSelfTest không xuất ảnh
fixtures. Đã xem lại ảnh cả sáu màn hình, cửa sổ hẹp và DPI 125%.

**Chưa xác minh:** thao tác đầy đủ tạo/lưu hóa đơn, thu tiền, xét duyệt trên SQL thật
với bản ghi hợp lệ; kiểm tra lại sau đóng/mở ứng dụng và thao tác đồng thời trên SQL.
Database hiện trống nên các luồng có dữ liệu chỉ được kiểm thử bằng fixtures riêng,
không được coi là nghiệm thu giao dịch thật. Chưa kiểm thử thủ công cửa sổ đăng nhập
và chuyển DPI giữa nhiều màn hình Windows.

## File chính của đợt tinh chỉnh

Đường dẫn Java UI/controller tính từ `src/main/java/com/quanlynhakhoa/`:

- `billing/ui/FilterBar.java` (mới), `BillingUi.java`.
- `billing/ui/HoaDonPanel.java`, `PaymentPanel.java`, `PaymentForm.java`,
  `TaoHoaDonDialog.java`, `CancellationPanel.java`, `DebtPanel.java`.
- `dichvu/ui/DichVuPanel.java`.
- `billing/controller/PaymentController.java`, `PaymentScreenController.java`,
  `TaoHoaDonController.java`, `CancellationController.java`, `DebtController.java`.
- `src/test/java/com/quanlynhakhoa/billing/BillingRefinementSelfTest.java`,
  `CreationStepsSelfTest.java`, `BillingActualUiPreview.java` (mới).
- `src/test/java/com/quanlynhakhoa/billing/BillingLayoutSelfTest.java` (cập nhật).
- `scripts/test-service-billing-m1.ps1`, `docs/BILLING_UI_REFINEMENT.md`.

Ứng dụng đang chạy cần được mở lại để nạp các class giao diện mới.
