# Chuẩn hóa Entity nghiệp vụ — K43–K47

Phạm vi: tách Entity nghiệp vụ, giữ model lựa chọn/hiển thị/tổng hợp; bảo toàn SQL, nghiệp vụ và phân quyền. Hộp cấp cứu và màn hình Kế hoạch điều trị không thay đổi.

| Batch | Entity | Cách sử dụng |
| --- | --- | --- |
| K43 | HoaDon, ChiTietHoaDon, ThanhToan, YeuCauHuyHoaDon | Model hóa đơn/chi tiết/yêu cầu hủy giữ phần hiển thị và tổng hợp, chứa Entity phần dữ liệu gốc. ThanhToan thay thế model ThongTinThanhToan trong DAO, Service và caller. |
| K44 | TaiKhoan, NhanVien | TaiKhoan thay thế ThongTinTaiKhoan, giữ bất biến và che hash trong toString. NhanVien chứa hồ sơ đầy đủ phục vụ DAO tạo nhân viên từ request đã được Service kiểm tra. Snapshot nhân viên tối thiểu phục vụ xác thực vẫn giữ riêng. |
| K45 | LoHang, PhanCong | Model lô hàng giữ tên hàng/nhà cung cấp; model phân công giữ tên nhân viên và API cũ. Dữ liệu nghiệp vụ được chuyển vào Entity và getter/setter ủy quyền tương ứng. |
| K46 | PhieuKham, ChiDinhCanLamSang, DonThuoc, ChiTietDonThuoc | Projection phiếu khám/chỉ định/thuốc giữ thông tin join. Đơn thuốc và chi tiết được sử dụng trong DAO, giữ khóa đơn ở các dòng đã tải, có thể chưa có khóa ở dòng bản nháp. |

HoSoBenhAn được giữ là Entity dữ liệu hồ sơ; danh sách lượt khám chuyển sang ThongTinHoSoBenhAn và LuotKhamBenhNhan trong model.benhnhan. Điều này tránh Entity phụ thuộc model hiển thị và tránh hai Entity cùng đại diện phiếu khám.

Getter chuẩn Java được cung cấp cho các Entity mới. Setter được giữ cho dữ liệu lô hàng/hồ sơ vốn có API mutable, và thêm cho thông tin hồ sơ nhân viên, lời dặn đơn thuốc. Giá trị tài chính, khóa định danh và các snapshot bảo mật/giao dịch giữ tính bất biến khi cần; Service tiếp tục chịu trách nhiệm kiểm tra quyền và chuyển trạng thái. Setter không tự ghi database.

Các trường tổng hợp, bộ lọc, request, bản nháp và model lựa chọn tiếp tục nằm trong model. Không thêm tầng/framework/dependency, không đổi schema hay seed.

Kiểm thử local: build sạch; 56/56 SelfTest; 9/9 bài SQL chỉ đọc/ghi thử rồi rollback; 90.105 phép đối chiếu trên 14 API trước/sau; 112 kiểm tra ranh giới Entity và mapping JDBC đơn thuốc. 312 SQL literals bằng baseline K42. Smoke GUI thủ công chưa chạy lại trong đợt này.

Thư mục triển khai: `ptud_nhom8-trien-khai`. Thư mục kiểm thử: `ptud_nhom8-kiem-thu`, nhánh `refactor/kiem-thu-tong-hop`. Các nhánh hiện tại dùng tiền tố refactor; lịch sử commit và thảo luận PR cũ được giữ để truy nguyên.
