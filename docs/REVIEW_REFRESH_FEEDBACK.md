# Rà soát làm mới và thông báo kết quả

## Tổng quan — đã triển khai ngày 08/11/2026

Hiện có bốn thẻ: tổng bệnh nhân, lịch hẹn hôm nay, đang khám, đã thu hôm nay.

Ưu tiên bổ sung:
1. Việc cần xử lý: yêu cầu hủy hóa đơn chờ duyệt, yêu cầu cấp phát/kiểm kê chờ duyệt; bấm để mở danh sách. Chỉ hiện theo quyền.
2. Lịch hẹn sắp tới trong ngày: giờ, bệnh nhân, bác sĩ, trạng thái; mở chi tiết từ dòng được chọn.
3. Cảnh báo kho: thuốc/vật tư tồn thấp, lô sắp hết hạn; cần thống nhất ngưỡng tồn và khoảng ngày cảnh báo.
4. Biểu đồ bảy ngày: lượt khám và tiền đã thu; chỉ hiện doanh thu với người có quyền.
5. Lối tắt theo vai trò: đặt lịch, tiếp nhận, lập hóa đơn, gửi yêu cầu cấp phát.

Bố cục đã bổ sung: bốn thẻ, thao tác nhanh, hai cột lịch hẹn/việc chờ xử lý, biểu đồ và cảnh báo kho. Lịch hẹn hiển thị tối đa 8 lịch trong ngày còn chờ xác nhận/đã xác nhận, sắp theo giờ; chọn dòng rồi mở lịch hẹn để xem chi tiết. Cảnh báo thiếu hàng dùng định mức hiện có, chỉ tính tồn còn hạn; cảnh báo hạn dùng mặc định 30 ngày, mỗi nhóm tối đa 8 dòng. Biểu đồ tiền thu chỉ hiện theo quyền báo cáo; bác sĩ chỉ thấy lượt khám của mình. Quay về Tổng quan hoặc bấm Làm mới sẽ tải lại.

## Đã sửa làm mới

Bỏ từ khóa và bộ lọc để tải lại danh sách: hóa đơn, công nợ, duyệt hủy, danh mục dịch vụ, bệnh nhân, thuốc, vật tư, nhà cung cấp, lô hàng, nhân viên, tài khoản, nhật ký. Thanh toán bỏ từ khóa và ngữ cảnh hóa đơn cũ khi không có lần thu đang xử lý/chưa rõ kết quả. Hàng chờ khám bỏ từ khóa; lịch làm việc bỏ bộ lọc nhân viên.

Các màn hình theo ngày/tuần vẫn giữ khoảng thời gian làm việc đang xem. Phiếu khám đặt lại bộ lọc mặc định, đồng thời tải lại bác sĩ. Làm mới không mở rộng quyền truy cập, không tự gửi lại thao tác ghi dữ liệu.

## Đã bổ sung thông báo kết quả

- Hóa đơn: lập hóa đơn, gửi yêu cầu hủy, duyệt hủy, từ chối hủy, ghi nhận thanh toán.
- Kho: lưu thuốc/vật tư/nhà cung cấp, nhập kho, xuất kho, gửi kiểm kê, duyệt/từ chối kiểm kê.
- Điều trị: hoàn thành bước, hủy bước, hủy kế hoạch.
- Lịch làm việc: lưu và hủy phân công, lỗi ghi dữ liệu.

Thông báo thành công xuất hiện sau khi thao tác ghi hoàn tất, trước khi tải lại danh sách. Hóa đơn/thanh toán có trường hợp kết quả chưa xác định: yêu cầu kiểm tra lại, không khẳng định thất bại hoặc cho phép tự tạo lần thu mới. Các ngoại lệ báo đã commit được xử lý như thành công theo cơ chế sẵn có.

## Các chỗ đã có phản hồi khi rà soát

Đặt/xác nhận/hủy lịch hẹn, tiếp nhận, lưu bệnh nhân, lưu nháp/kết luận phiếu khám, tạo kế hoạch/thêm bước/hoàn tất điều trị, yêu cầu cấp phát (gửi/duyệt/từ chối/cấp phát), kiểm tra hộp cấp cứu đã có thông báo. Dịch vụ, tài khoản và nhân viên có phản hồi kết quả riêng trên giao diện. Giữ phản hồi hiện có tại các luồng này.

## Kiểm tra

Biên dịch mã chính và mã kiểm tra bằng Maven wrapper. Kiểm tra làm mới mới sử dụng Swing không hiển thị cửa sổ và JDBC mô phỏng; kiểm tra bộ lọc ngày sai vẫn được xóa. Các kiểm tra nghiệp vụ dùng dữ liệu mô phỏng không thay thế nghiệm thu trên SQL Server thật hoặc kiểm tra trực quan hộp thoại.
