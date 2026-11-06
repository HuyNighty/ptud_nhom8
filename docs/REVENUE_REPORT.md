# Thống kê doanh thu

Màn hình nằm tại **Tổng kết → Thống kê doanh thu**, dựa trên UC029 và bảng công thức báo cáo trong `docs/tuan_cuoi/9_REPORT_ApplicationDevelopment.docx`.

Quản lý phòng khám và quản trị hệ thống có quyền `REVENUE_REPORT_READ`. Các vai trò khác không có menu và bị chặn tại service trước khi truy cập database.

## Giao diện

- Chọn nhanh tháng này, hôm nay, 7 ngày gần nhất hoặc tháng trước; chọn khoảng ngày bằng lịch.
- Chọn nhóm theo ngày/tháng, bấm **Xem báo cáo** để áp dụng. Khi mở lần đầu tự tải tháng hiện tại đến hôm nay.
- Bốn ô tổng hợp: doanh thu, tiền đã thu, công nợ cuối kỳ và số hóa đơn trong kỳ.
- Biểu đồ hai dãy cột; bảng chi tiết cùng nguồn dữ liệu, có thể sắp xếp và in.
- Khoảng ngày đang hiển thị nằm cạnh biểu đồ. Bản in dùng kỳ của dữ liệu đã tải, không dùng bộ chọn chưa áp dụng.

## Cách tính

- **Doanh thu:** tổng số lượng × đơn giá đã chốt của hóa đơn lập trong kỳ và chưa bị hủy tại thời điểm cuối kỳ. Không dùng giá danh mục hiện tại.
- **Tiền đã thu:** các khoản thu hợp lệ phát sinh trong kỳ theo thời gian thu, kể cả khoản thu cho hóa đơn lập trước kỳ.
- **Công nợ cuối kỳ:** toàn bộ hóa đơn còn hiệu lực được lập trước hết ngày kết thúc, trừ khoản thu hợp lệ đến thời điểm đó. Bao gồm công nợ từ trước kỳ.
- **Hóa đơn trong kỳ:** số hóa đơn thuộc tổng doanh thu, không đếm số dòng chi tiết hoặc số khoản thu.

Ngày kết thúc bao gồm cả ngày, triển khai bằng mốc đầu ngày kế tiếp. Hóa đơn hủy sau cuối kỳ vẫn được tính cho kỳ trước khi hủy theo thời gian duyệt yêu cầu; khoản thu hủy sau cuối kỳ vẫn được tính cho kỳ trước khi hủy theo thời gian hủy khoản thu. Hóa đơn đã hủy nhưng thiếu lịch sử duyệt có thời gian không được tự suy đoán là còn hiệu lực. Số liệu quá khứ phụ thuộc việc lưu đầy đủ các mốc lịch sử này.

Các dòng hóa đơn và khoản thu được tổng hợp riêng, tránh nhân đôi tiền khi hóa đơn có nhiều dòng và nhiều khoản thu. Các truy vấn chỉ đọc và chạy trong một transaction SERIALIZABLE; không thay đổi schema hoặc dữ liệu.

Khoảng dài hơn 730 ngày tự nhóm theo tháng để bảng và biểu đồ dễ đọc. Các ngày/tháng không phát sinh vẫn có dòng bằng 0. Không tự tạo dữ liệu minh họa trong màn hình thật.

## Kiểm tra

- `RevenueReportSelfTest`: khoảng ngày, phân quyền, đăng ký module và dựng giao diện không kết nối database.
- `RevenueReportSqlIT --read-only`: đối chiếu hóa đơn/phiếu thu với database local, tổng nhóm ngày/tháng và công nợ cuối kỳ.
- `RevenueReportPreview`: ảnh thiết kế sử dụng dữ liệu giả lập, không mở kết nối SQL.
- Kiểm tra điều hướng và màn hình vai trò được cập nhật để bao gồm quyền báo cáo mới.

Nút **In báo cáo** mở hộp thoại in hệ thống và in bảng cùng tổng số liệu. Chưa kiểm tra xuất giấy trên máy in vật lý.
