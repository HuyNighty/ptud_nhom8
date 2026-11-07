# Giá bán thuốc và tiền thuốc trên hóa đơn

`dbo.Thuoc.donGiaBan` lưu giá bán VND cho một đơn vị tính. `NULL` nghĩa là chưa định giá; `0` là giá miễn phí được nhập chủ động. Giá nhập kho vẫn nằm ở `ChiTietPhieuNhap.donGiaNhap`, không được tự dùng làm giá bán.

## Sử dụng

1. Quản lý kho hoặc admin mở **Hoạt động → Danh mục thuốc & vật tư → Thuốc**.
2. Thêm/sửa thuốc và nhập **Giá bán (VND)**. Có thể để trống để tiếp tục quản lý thuốc chưa định giá.
3. Lễ tân lập hóa đơn từ lượt khám đã kết luận và bấm **Tải dịch vụ và thuốc**.
4. Bản xem trước gồm dịch vụ đã thực hiện và thuốc trong đơn của đúng lượt khám/bệnh nhân. Thuốc chưa có giá sẽ chặn lập hóa đơn và nêu rõ mã cần cập nhật.
5. Khi xác nhận, hệ thống đọc lại đơn thuốc và giá dưới transaction SERIALIZABLE. Nếu số lượng/giá thay đổi từ lúc xem trước, phải tải lại trước khi lưu.

Thuốc được ghi vào `ChiTietHoaDon.maThuoc`; số lượng lấy từ đơn thuốc, giá bán được chốt vào `donGiaApDung`. Tổng tiền và báo cáo doanh thu tự bao gồm các dòng thuốc đã chốt. Không tự thêm thuốc vào hóa đơn đã lập hoặc sửa giá hóa đơn cũ khi đổi danh mục.

Phần chi tiết hóa đơn chỉ hiển thị mã, tên, đơn vị, số lượng, đơn giá và thành tiền. Thuốc kê nhưng chưa có dòng tính tiền vẫn hiện **Chưa chốt**, không cộng vào tổng tiền đã lưu.

## Database

- Database local của phiên làm việc đã áp dụng `sql/bootstrap/V7__add_medicine_sale_price.sql`.
- Database local khác cần chạy migration này bằng tài khoản quản trị trước khi dùng bản code mới. Script chỉ chấp nhận database `N08Dev_<8 hex>`, có thể chạy lại và không tự gán giá cho thuốc cũ.
- Setup database mới đã bao gồm V7. Quyền SELECT đơn thuốc/chi tiết đơn thuốc và UPDATE giá bán được bổ sung vào script cấp quyền phù hợp; không tự mở rộng quyền người dùng ứng dụng.

## Kiểm tra

- `InvoiceMedicineSelfTest`: giá trống, miễn phí, âm/lẻ/vượt giới hạn, hiển thị và giữ tổng tiền.
- `MedicineSalePriceSqlIT --rollback-check`: dùng bộ TST local, tạo nguồn thuốc thử, ghi giá chốt, thay đổi giá danh mục và xác nhận hóa đơn giữ giá cũ; chặn giá chưa nhập. Tất cả dữ liệu ghi thử được rollback.
- Bộ SelfTest dịch vụ/hóa đơn vẫn đạt. Chưa tự gán giá bán thật cho dữ liệu thuốc hiện có.
