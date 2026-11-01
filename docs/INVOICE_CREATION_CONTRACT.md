# Hợp đồng tạo hóa đơn đang áp dụng

Cập nhật theo yêu cầu đưa vào giao diện chính: FeatureBootstrap đã đăng ký
SERVICE_CATALOG → DichVuPanel và BILLING → HoaDonPanel. Đây là thay đổi Core tối thiểu
được người dùng yêu cầu sau giới hạn cũ. Chạy `scripts/run.ps1` hoặc main `App`,
đăng nhập rồi mở menu Hóa đơn → Bảng giá dịch vụ / Danh sách hóa đơn.
Nút Tạo hóa đơn nằm trong trang Danh sách hóa đơn. Launcher riêng vẫn dùng để thử feature.

Người dùng yêu cầu tự quyết định triển khai nguồn tính phí/sinh mã sau bản bàn giao
interface. Đã nối JdbcClinicalBillingSource và RandomInvoiceIdGenerator vào constructor
HoaDonPanel(session, connections), bao gồm launcher feature hiện tại. Không cần sửa Core.
HoaDonService overload với source/id và InvoiceCreationService vẫn kiểm tra quyền/session.
Service đọc được tạo riêng bằng constructor cũ vẫn chỉ đọc; không tự có quyền ghi.

## Quy tắc nguồn khám

1. PhieuKham thuộc đúng BenhNhan, hồ sơ của phiếu thuộc cùng bệnh nhân, trạng thái DA_KET_LUAN.
2. Các kế hoạch có maPhieuKham trỏ phiếu này là phạm vi thanh toán của phiếu, kể cả hạng
   mục thực hiện nhiều ngày. Không suy luận một lượt tái khám khác chỉ từ ngày thực hiện.
   Chặn nếu kế hoạch còn DANG_THUC_HIEN hoặc hạng mục CHUA_THUC_HIEN. Kế hoạch đã hủy
   mà vẫn có hạng mục thực hiện phải đối soát, chưa cho lập tự động.
3. ChiDinhCanLamSang gắn trực tiếp phiếu: phải DA_CO_KET_QUA, có ketQua và thoiGianKetQua.
   Chỉ định chưa đầy đủ sẽ chặn cả hóa đơn, tránh bỏ sót phí.
4. ChiTietDieuTri thuộc kế hoạch HOAN_THANH gắn phiếu: chỉ lấy DA_THUC_HIEN có ngày,
   kết quả và nhân viên thực hiện. Không tính hạng mục DA_HUY. Không lấy kế hoạch của
   bệnh nhân khác. Kế hoạch liên kết sai hồ sơ sẽ bị chặn.
5. Một source row đã thực hiện là một đơn vị. Nhiều source row cùng dịch vụ trong cùng
   loại nguồn là nhiều lần thực hiện và được giữ thành các dòng riêng, quantity=1.
   Cùng mã dịch vụ xuất hiện ở cả CLS và điều trị sẽ chặn để đối soát, không cộng hai
   nguồn theo phỏng đoán. Không cho người dùng tự thêm/bớt/sửa dòng hoặc sửa đơn giá.
6. CLS phải gắn dịch vụ có laCanLamSang=true. Dịch vụ đã ngừng vẫn tính được nếu có
   bằng chứng thực hiện hợp lệ; không tính phí chỉ vì dịch vụ có trong danh mục.
7. Giá áp dụng là DichVu.donGia tại thời điểm lập, không tuyên bố đây là giá tại ngày
   điều trị vì schema nguồn không lưu giá lịch sử. Bản xem trước được đọc lại dưới khóa
   khi lưu; giá/bằng chứng/dòng thay đổi phải xem và xác nhận lại. Giá lưu cố định vào
   ChiTietHoaDon.donGiaApDung. Không thuế/phụ phí/giảm giá, không ghi ThanhToan.

DAO dùng HOLDLOCK, timeout 15s và parameter. Source chỉ chạy trên connection do service
truyền, autoCommit=false, SERIALIZABLE. Khóa source/danh mục giữ đến commit. Hash revision
dựa trên nguồn/dòng/giá/trạng thái áp dụng; không ghi ghi chú lâm sàng vào log hoặc revision.

## Mã và transaction

Mã ngẫu nhiên 10 ký tự base32 từ SecureRandom (50 bit), không đọc/ghi BoDemMa,
không MAX+1, không thêm quyền SQL. PK hiện có bảo vệ trùng mã, unique index bảo vệ
một hóa đơn chưa hủy/lượt khám. Nếu va chạm mã hoặc lập đồng thời, báo lỗi và tải lại;
không tự retry ghi. Lấy nhân viên từ tài khoản đăng nhập có liên kết thật, đang làm việc;
dev.admin không có maNhanVien thì cố ý bị từ chối, không tự gán nhân viên giả.

## Trạng thái thực tế và quyền cần Huy duyệt

Nút Tạo hóa đơn hiện bật cho BILLING_MANAGE (RECEPTIONIST và SYSTEM_ADMIN theo policy),
không bật cho CLINIC_MANAGER chỉ có BILLING_READ. Nguồn dữ liệu và bộ sinh mã đã được nối.
**Nút có thể mở luồng nhưng chưa có nghĩa DB đã cho ghi.** SQL sẽ từ chối nếu thiếu quyền.
Quyền application login đã kiểm tra ở lần khảo sát: INSERT HoaDon/ChiTietHoaDon=0;
SELECT ChiTietDieuTri/ChiDinhCanLamSang=0. Không dùng sa hoặc chạy GRANT để vượt giới hạn.

Đề xuất tối thiểu cho Huy review:

- Bổ sung INSERT dbo.HoaDon, dbo.ChiTietHoaDon.
- Bổ sung SELECT dbo.ChiTietDieuTri, dbo.ChiDinhCanLamSang.
- Giữ SELECT PhieuKham, BenhNhan, HoSoBenhAn, KeHoachDieuTri, DichVu, TaiKhoan,
  NhanVien, HoaDon/ChiTietHoaDon và INSERT NhatKyThaoTac hiện có.
- Không cấp quyền ghi dữ liệu khám/ThanhToan, DELETE, DDL hoặc BoDemMa cho tính năng này.

Không có dữ liệu khám/nhân viên giả được tạo. Chưa thực hiện ghi SQL thật, restart
để đọc lại hóa đơn hoặc thử đồng thời hai connection. Các bước đó cần quyền trên và
tài khoản nghiệp vụ/nguồn khám thật của development đã có. PR #78 vẫn đóng.

## Chạy và kiểm thử

Từ root worktree feature:

```powershell
.\scripts\test-service-billing-m1.ps1
.\scripts\run-service-billing-m1.ps1
```

InvoiceCreationSelfTest xác minh transaction/rollback bằng JDBC double.
ClinicalBillingSourceSelfTest xác minh quy tắc nguồn, trường hợp dịch vụ lặp/cả hai nguồn,
giá đổi, giá không hợp lệ, dịch vụ ngừng và lấy mẫu 10.000 mã không trùng. Lấy mẫu không
chứng minh mã ngẫu nhiên không thể va chạm; PK vẫn là bảo vệ cuối cùng.
Chưa chạy adapter SQL với dữ liệu khám thực tế vì thiếu SELECT nguồn, không coi self-test
là bằng chứng tính phí/INSERT SQL Server đã PASS.
