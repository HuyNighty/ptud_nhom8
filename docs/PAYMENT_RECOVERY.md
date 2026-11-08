# Khôi phục lần thu chờ đối soát

- Mỗi lần thu chỉ dùng tiền mặt hoặc chuyển khoản. Mã lần thu và nội dung xác nhận được ghi xuống máy trước khi gửi lệnh ghi SQL.
- Nhật ký nằm tại `~/.n08-dental/pending-payments/<database-scope>/`, tách theo database và cổng kết nối. Không lưu tên bệnh nhân hoặc mật khẩu. Đây là cơ chế khôi phục trên cùng máy, không thay thế nhật ký giao dịch dùng chung giữa nhiều máy.
- Mở lại hóa đơn sẽ khôi phục số tiền, phương thức, mã giao dịch và tiền khách đưa ở trạng thái chỉ đọc. Chưa đối soát xong thì không tạo lần thu mới.
- Nếu SQL đã có đúng khoản thu, chỉ xác nhận kết quả và xóa nhật ký đã hoàn tất; không ghi thêm khoản thu.
- Nếu chưa thấy khoản thu, giữ nguyên mã và cho người dùng xác nhận lại chính lần thu đó. Không tự gửi lại khi bấm Kiểm tra. Kiểm tra mã đã tồn tại trong transaction của PaymentService ngăn ghi trùng khi phản hồi cũ về muộn.
- Nhật ký hỏng, khoản thu khác dữ liệu hoặc lỗi đọc SQL đều không cho tạo lần thu mới. Không tự xóa nhật ký lỗi.
- Hộp thoại không cho đóng khi đang gửi hoặc đang chờ đối soát. Tắt ứng dụng hoặc đăng xuất vẫn giữ nhật ký để lần sau khôi phục.
- Sửa mã giao dịch hoặc số tiền chuyển khoản sẽ bỏ dấu xác nhận đã nhận tiền.
- Hóa đơn có khoản thu hợp lệ bị chặn gửi yêu cầu hủy ở cả giao diện và service, trong transaction đã khóa hóa đơn. Chưa bổ sung quy trình hoàn tiền.

Kiểm tra: `PaymentRecoverySelfTest`, `PaymentSelfTest`, `BillingRefinementSelfTest`, `CancellationSelfTest`, `CancellationConcurrencySelfTest` và `BillingLayoutSelfTest`. Các bài này dùng dữ liệu mô phỏng, không phải kiểm thử nhiều máy trên SQL Server thật.
