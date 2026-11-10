# Đối chiếu Entity với sơ đồ lớp — K49–K51

Đã bổ sung tám Entity trong com.quanlynhakhoa.entity, nâng tổng số lên 29. Có lớp tương ứng cho đủ 26 lớp nghiệp vụ trong hình; DichVu trong hình tương ứng DichVuNhaKhoa hiện có. Ba Entity ngoài hình là VaiTro, ChiDinhCanLamSang và YeuCauHuyHoaDon.

| Batch | Entity | Tích hợp |
| --- | --- | --- |
| K49 | PhieuNhapKho, ChiTietPhieuNhap, PhieuXuatKho, ChiTietPhieuXuat | Dùng trong các đường ghi nhập/xuất kho và cấp phát hiện có; giữ nguyên SQL, tham số, tồn kho, transaction và audit. |
| K50 | KeHoachDieuTri, ChiTietDieuTri | Dùng khi DAO lập kế hoạch/thêm chi tiết; giữ khóa, kiểm tra ownership, hóa đơn, trạng thái và audit. Service/màn hình điều trị vẫn giữ giới hạn sẵn có. |
| K51 | BanGiaoCa, DanhGiaNhanSu | Bổ sung Entity dữ liệu đầy đủ theo schema. Chưa có luồng Service/DAO/UI cho hai chức năng này trong baseline, nên không tự tạo hoặc bật thêm nghiệp vụ. |

Các lớp có private fields, constructor và getter. Chỉ có setter cho nội dung dự thảo phù hợp: mục tiêu kế hoạch, vị trí răng/ngày dự kiến/ghi chú bước điều trị, điểm/nhận xét nhân sự. Setter điểm chấp nhận null khi còn nháp và kiểm tra khoảng 1–5 theo CHECK hiện có. Khóa định danh, chứng từ đã ghi, số lượng/giá chốt và trạng thái kiểm soát vẫn bất biến. Các thao tác lưu, duyệt, hủy thuộc Service/DAO, không đặt CRUD JDBC trong Entity.

Dùng String cho mã khóa ngoại như cấu trúc dự án hiện tại; dùng LocalDate/LocalDateTime theo kiểu cột thực tế, BigDecimal cho tiền/điểm. ChiTietPhieuNhap không nhân đôi hạn sử dụng: dữ liệu đó thuộc LoHang và liên kết qua maLo. Mã chi tiết điều trị do SQL IDENTITY cấp; giá trị 0 ở đối tượng chưa lưu không được bind vào cột IDENTITY.

Kiểm thử local: build sạch, 58/58 SelfTest, 10/10 bài SQL thực tế. Bài schema mới kiểm tra 8 Entity với 65 cột/279 assertions trên DB thử nghiệm, chỉ đọc metadata và dùng dữ liệu giả cho kiểm tra điểm; không in dữ liệu nghiệp vụ. Bài DAO mới có 21 kiểm tra binding, locking, ownership, audit và transaction. Các bài SQL có ghi dùng dữ liệu thử rồi rollback. Toàn bộ 312 SQL literals production giữ nguyên so với K48; không thay SQL/schema/seed/dependency, Service điều trị, hai màn hình Hộp cấp cứu và Kế hoạch điều trị. Chưa chạy lại smoke GUI thủ công.

Đủ lớp tương ứng không đồng nghĩa toàn bộ thuộc tính, quan hệ đối tượng và hành vi trong hình đã được triển khai như nhau; schema hiện hành và quy tắc nghiệp vụ đang có là căn cứ. Các PR được giữ Draft để review, chưa merge main.
