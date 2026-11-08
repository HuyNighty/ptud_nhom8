# Củng cố luồng hóa đơn — 08/11/2026

## Các mục đã xử lý

1. **Điều trị sau khi chốt hóa đơn**: tạo kế hoạch khóa phiếu khám trước khi kiểm tra hóa đơn; từ chối gắn thêm kế hoạch vào lượt khám đã có hóa đơn chưa hủy. Lượt khám mới được dùng cho điều trị phát sinh. Khóa cùng phiếu khám với luồng lập hóa đơn để hai thao tác không vượt nhau.
2. **Phiên bị khóa/đổi quyền**: phiên tạo từ đăng nhập thật được gắn bộ kiểm tra tài khoản trong cơ sở dữ liệu. Mỗi lần service yêu cầu quyền sẽ kiểm tra trạng thái tài khoản, vai trò, yêu cầu đổi mật khẩu và nhân viên. Các thao tác ghi hóa đơn còn kiểm tra tài khoản dưới transaction trước commit. Nếu tài khoản bị khóa hoặc vai trò thay đổi, phiên cũ bị vô hiệu hóa. Khi mất kết nối kiểm tra phiên, không cho tiếp tục thao tác. Dữ liệu giao diện đã tải có thể còn trên màn hình đến thao tác tiếp theo; không có truy vấn đồng bộ định kỳ trên giao diện.
3. **Đối soát nhiều máy**: `LanThuHoaDon` lưu số tiền, phương thức, mã giao dịch, người tạo và trạng thái dùng chung. Mỗi hóa đơn chỉ có một lần thu chờ xử lý. Khoản đã commit nhưng mất phản hồi vẫn giữ trạng thái chờ cho tới khi client xác nhận được kết quả; máy khác bị chặn tạo khoản mới. Chủ lần thu có thể xác nhận lại đúng mã; quản lý có thể đối soát khoản đã lưu, hoặc giải phóng khoản chưa lưu sau khi xác minh không nhận tiền. Không tự hết hạn reservation. Nhật ký cục bộ vẫn giữ làm lớp khôi phục bổ sung và được kiểm tra với máy chủ.
4. **Nội dung lịch sử**: hóa đơn lưu tên/số điện thoại bệnh nhân, dòng hóa đơn lưu tên hàng tại thời điểm lập. Đổi danh mục không làm đổi tên trên hóa đơn đã chốt. Hóa đơn có trước migration được điền từ thông tin còn có tại thời điểm nâng cấp; không thể khôi phục tên cũ đã bị ghi đè trước đó.
5. **Hóa đơn miễn phí**: tổng bằng 0 được chốt `DA_THANH_TOAN`; không cần tạo khoản thu giả. Migration chuẩn hóa hóa đơn 0 đồng có dòng chi tiết và chưa có khoản thu.
7. **Sửa sai và hoàn tiền**: quản lý phòng khám/quản trị hệ thống dùng nút `Điều chỉnh khoản thu` trong Hóa đơn, sau khi chọn dòng ở Lịch sử thanh toán. Hủy ghi nhận sai dùng khi không có tiền thực nhận; hoàn tiền dùng khi thực tế đã trả toàn bộ khoản thu đó. Bắt buộc lý do, tham chiếu/bằng chứng và xác nhận đối chiếu. Không xóa dòng gốc; lưu điều chỉnh riêng, người thao tác, thời gian và nhật ký. Chỉ điều chỉnh một lần cho mỗi khoản thu. Công nợ/trạng thái hóa đơn tính lại trong cùng transaction. Sau khi hoàn hết các khoản, hóa đơn có thể đi qua quy trình yêu cầu/duyệt hủy hiện có. Chưa có hoàn một phần của một khoản thu: cần phân biệt với hóa đơn được thanh toán thành nhiều khoản, từng khoản có thể được hoàn riêng.
8. **Truy vấn danh sách**: tổng tiền/đã thu dùng tổng hợp theo khóa hóa đơn với chỉ mục hỗ trợ; chỉ tính công nợ toàn bệnh nhân khi mở chi tiết. Danh sách hóa đơn phân trang 100 dòng và dùng một câu READ COMMITTED thay vì giữ range lock SERIALIZABLE. Bộ chọn hóa đơn thanh toán giới hạn 100 kết quả, lọc còn nợ ở SQL và nhắc tìm cụ thể khi vượt giới hạn. Công nợ toàn bộ vẫn đọc đủ dữ liệu để tổng hợp chính xác, không lấy tổng từ một trang hóa đơn. Chưa thực hiện kiểm thử tải quy mô lớn.

## Báo cáo

`DongTienHoaDon` biểu diễn khoản thu dương và điều chỉnh âm theo thời điểm điều chỉnh. Tổng quan, biểu đồ và báo cáo tiền thu dùng dòng tiền ròng; biểu đồ hỗ trợ số âm. Báo cáo công nợ tại cuối kỳ dùng các dòng tiền phát sinh trước thời điểm cuối kỳ. Hủy ghi nhận/hoàn tiền hôm nay không âm thầm sửa số thu của ngày thu gốc.

## Nâng cấp

- Cơ sở dữ liệu local đang cấu hình: `./scripts/update-billing-workflows.ps1 -Apply`. Script chỉ nhận cơ sở dữ liệu N08Dev đúng cấu hình và máy SQL local khớp cổng. Không xóa bảng hoặc dữ liệu.
- Kiểm tra cấu trúc: cùng script với `-Check`.
- Máy khác/checkout mới phải dùng mã và schema V8 đồng bộ trước khi chạy ứng dụng. `DevSetup` đã bổ sung thứ tự V6–V8.
- Kết nối thật từ `LoginFrame` được gắn `JdbcSessionVerifier`; các phiên dựng thủ công trong kiểm thử không có bộ kiểm tra trừ khi bài kiểm tra chủ động gắn nó.

## Kiểm tra

- `scripts/test-billing-hardening-sql.ps1`: tạo cơ sở dữ liệu tổng hợp riêng, chạy V1–V8, không ghi vào cơ sở dữ liệu ứng dụng. Kiểm tra chốt điều trị, giữ tên sau đổi danh mục, hóa đơn miễn phí, phân trang, hoàn tiền/hủy ghi nhận, không hoàn hai lần, hủy hóa đơn sau hoàn đủ, hai máy tranh chấp reservation, mất phản hồi sau commit thật và vô hiệu phiên khi khóa/đổi vai trò. Cơ sở dữ liệu test được giữ để kiểm tra, tên nằm trong `target/billing-test-database.txt`.
- Bộ kiểm tra hóa đơn cũ và khôi phục thanh toán vẫn được chạy; các JDBC double được điều chỉnh theo việc phân trang, tách reservation và dùng READ COMMITTED cho danh sách. Mô phỏng luồng thu cũ không thay thế kiểm tra SQL nhiều kết nối mới.
