# Quy ước giao diện Admin — N08 Nha Khoa (Java Swing)

Áp dụng cho mọi màn hình. Ảnh mẫu: `01-tong-quan.png`, `02-tiep-nhan-benh-nhan.png`, `03-lich-hen.png`.

## Khung chung
- Tông màu **xanh ngọc**: menu trái nền xanh ngọc đậm, chữ trắng; nền nội dung xanh xám rất nhạt; nút chính xanh ngọc.
- Menu trái 6 nhóm nghiệp vụ, xổ xuống / thu gọn, icon bên trái, badge đỏ số việc chờ:
  Tiếp nhận & lịch hẹn · Khám & điều trị · Thanh toán & công nợ · Quản lý nhân viên (gồm phân ca, bàn giao ca) · Quản lý thuốc & vật tư · Kiểm soát truy cập.
- Thanh trên: `Quản trị / <tên màn hình>` bên trái; tên người dùng, nhãn vai trò, Đổi mật khẩu, Đăng xuất bên phải.

## Đầu màn hình
- Tiêu đề 22 đậm; dòng phụ: luồng thao tác (A → B → C) · `Thứ Hai, 12/10/2026, 09:15`.
- Góc phải: `Tải lại` (nút phụ) + nút hành động chính nếu có (`+ Tạo …`).

## Nút
| Loại | Dùng cho | Kiểu |
|---|---|---|
| Chính | hành động chính của khung (Lọc, Xác nhận, Tiếp nhận…) | nền xanh ngọc, chữ trắng |
| Phụ | Tải lại, Đổi lịch, Đổi bác sĩ, Xóa lọc, Bỏ chọn | nền trắng, viền xám |
| Nguy hiểm | Hủy lịch, Hủy phiếu, Xóa… | **nền đỏ, chữ trắng** |
| Liên kết | Mở lịch hẹn ›, Xem lịch sử › | chữ xanh ngọc, không viền |

## Tên nút & vị trí thao tác
- Nút mở hộp thoại kết thúc bằng "…" (Sửa thông tin…, Đổi lịch…, Hủy lịch…, Kết luận…, Đặt lịch hẹn…).
- Nút tạo mới thống nhất: `+ Tạo hồ sơ`, `+ Tạo lịch hẹn`.
- Thao tác trên **một bản ghi đang chọn** đặt ở **chân khung chi tiết**: nút nguy hiểm (đỏ) bên trái, nút phụ rồi nút chính bên phải.
- Thao tác trên **danh sách** đặt ở đầu khung danh sách (Đánh dấu vắng, Đổi bác sĩ…, Hủy phiếu…).
- Thao tác thuộc một tab (Sửa thông tin…, Cập nhật tiền sử…) đặt trong tab đó.
- Nút chưa hợp lệ với lựa chọn hiện tại thì **làm mờ**, không ẩn khỏi khung.

## Quy tắc nghiệp vụ đã chốt
- Lịch hẹn "Chờ xác nhận" **vẫn được tiếp nhận** khi bệnh nhân đến: tiếp nhận chuyển lịch thẳng sang "Đã đến" và tạo phiếu khám.
- Công nợ chỉ phát sinh từ hóa đơn của lượt khám → bệnh nhân còn nợ luôn có ngày khám gần nhất.
- Dị ứng nằm trong tab **Tiền sử** (Hồ sơ bệnh nhân, Phiếu khám).

## Khung (card)
- Nền trắng, viền xám 1px; đầu khung nền xanh ngọc nhạt: tiêu đề 15 đậm + dòng gợi ý 12 xám bên dưới; nút đặt bên phải đầu khung.
- Luồng nhiều bước đánh số ô vuông xanh ngọc (1, 2, 3).

## Bộ lọc
- Một hàng; nhãn nhỏ xám phía trên từng ô; lựa chọn dùng **combo xổ xuống** (kèm số lượng nếu có); ô tìm kiếm giãn hết phần còn lại; cuối hàng `Lọc` (chính) + `Xóa lọc` (phụ).

## Bảng (JTable)
- Chỉ kẻ ngang, dòng cao 29–32, tiêu đề nền nhạt chữ xám đậm; cột số căn phải; không để thanh cuộn ngang.
- Danh sách + thao tác: chọn dòng → khung chi tiết bên phải chỉ hiện **nút hợp lệ với trạng thái** đang chọn.

## Màu trạng thái (chấm ● + chữ đậm)
| Trạng thái | Màu |
|---|---|
| Chờ xác nhận | cam |
| Đã xác nhận · Chờ khám | xanh |
| Đã đến · Đang khám | tím |
| Hoàn thành · Đã kết luận | xanh lá |
| Vắng mặt | xám đậm |
| Đã hủy | đỏ |
| Đã đổi lịch | xám |
| Mức việc: Khẩn đỏ · Chờ duyệt cam · Theo dõi xanh | |

## Dữ liệu mẫu thống nhất
Mốc thời gian chung: **Thứ Hai 12/10/2026, 09:15**. Thanh trên giống nhau mọi màn hình: breadcrumb · ô nhắc xanh ngọc · chuông · ô người dùng (AD, Quản trị viên, admin · Quản trị, menu xổ Đổi mật khẩu / Đăng xuất). Lịch hẹn hôm nay 10 lịch (LH000001–LH000010); phiếu khám PK000021 Lan, PK000022 Hùng (đã kết luận), PK000023 Mai (đang khám); bác sĩ: Trần Minh, Phạm Anh, Nguyễn Hà, Lê Thu.
