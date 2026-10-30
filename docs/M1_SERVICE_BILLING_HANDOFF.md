# Khang — Service & Billing M1

Baseline: `origin/main` commit `09b2db46cf22d8e1d0423b8536fa61efa3378030`.
Ngày kiểm tra: 30/10/2026. Runtime Java SE 21+, Swing, JDBC, SQL Server.

## Phạm vi đã triển khai

- `com.quanlynhakhoa.dichvu.{ui,controller,service,dao,model}`: tìm mã/tên, lọc trạng thái,
  thêm, sửa, ngừng áp dụng/áp dụng lại; ghi dịch vụ và audit trong cùng transaction.
- `com.quanlynhakhoa.billing.{ui,controller,service,dao,model}`: danh sách hóa đơn, lọc
  ngày/trạng thái, chi tiết dịch vụ/thuốc/vật tư, lịch sử thu và metadata hủy,
  tổng tiền/đã thu/còn nợ, công nợ bệnh nhân. Billing chỉ có API đọc.
- Hai panel không gọi JDBC trong constructor. Khi panel được gắn vào cửa sổ, lần đọc
  đầu tiên chạy qua controller/SwingWorker. Các nút đọc/lưu bị khóa trong lúc xử lý.
- Dùng `DesktopSession` và `AuthorizationService.require` của Core trước khi mở kết nối,
  kiểm tra lại session khi kết nối xong và khi trả kết quả/commit. Kết quả bất đồng bộ
  của session đã logout bị bỏ, dữ liệu trên panel bị xóa và control bị khóa.

Main hiện có module/menu/quyền và panel tài khoản; chưa có Dịch vụ/Billing. Không tìm
thấy PR Billing có sẵn lúc khảo sát. Đã đọc hợp đồng panel, policy, schema V1–V5,
onboarding, code tài khoản/bệnh nhân và quy tắc control từ PR #73.

Không sửa App, MainFrame, LoginFrame, FeatureBootstrap/Registry, security, navigation,
DevSetup, cấu hình kết nối, grant scripts, pom.xml hoặc migration V1–V5.
Menu chính **chưa tích hợp hai panel**; đây là phần Core cần Huy review và nối riêng.

## Quy tắc dữ liệu và tiền

- Mã nhập thủ công: trim, 1–10 ký tự ASCII hiển thị, không khoảng trắng; không dùng
  BoDemMa hoặc MAX+1. Mã của bản ghi đã lưu bị khóa trên form và không có trong SET UPDATE.
- Tên bắt buộc, tối đa 100 ký tự; mô tả tối đa 300, trống thành NULL.
- Giá nhập 1–18 chữ số ASCII, VND nguyên, không dấu `.`/`,`/`+`/`-` hoặc số mũ.
  Giá `0` hợp lệ. Service cũng kiểm tra BigDecimal được gọi trực tiếp:
  0 ≤ giá ≤ 999999999999999999, không có phần lẻ; không dùng double/float.
- Kiểm tra mã trùng trước INSERT và xử lý SQL Server 2601/2627 khi đua đồng thời.
- Ngừng áp dụng là UPDATE `dangApDung`, không DELETE. Không sửa giá chốt hay liên kết
  trong ChiTietHoaDon khi sửa bảng giá. Không cung cấp API ghi/hủy Billing M1.
- Tổng hóa đơn là SUM(soLuong × donGiaApDung) của **mọi loại hàng**.
  Đã thu chỉ cộng `soTien` của `THANH_CONG`, không cộng `DA_HUY` hay `tienKhachDua`.
  Còn nợ = tổng − đã thu, giữ số âm và cảnh báo thu vượt. Trạng thái khác số tiền
  cũng được cảnh báo để đối soát.
- Công nợ BN tính trên mọi hóa đơn chưa hủy của bệnh nhân, trước bộ lọc màn hình;
  hóa đơn hủy vẫn hiển thị và có chú thích, không cộng công nợ theo dõi.
- SQL dùng hai tập tổng hợp chi tiết/thanh toán riêng rồi LEFT JOIN; COALESCE về 0,
  không nhân bản tiền khi nhiều dòng × nhiều lần thu. Không lấy giá danh mục hiện tại.
- Cả danh sách và bộ chi tiết dùng transaction `TRANSACTION_SERIALIZABLE` để bảo vệ
  các lần đọc/tập tổng hợp trên SQL Server, không phụ thuộc database đã bật SNAPSHOT.
  Khóa đọc và khóa khoảng được giữ đến commit; có thể chặn writer. Query timeout 15s,
  không retry tự động khi deadlock/timeout. Chi tiết đối soát lại tổng Java với tổng SQL.
- Mỗi lần xem chi tiết mở transaction mới: đây là ảnh nhất quán tại lần chọn đó,
  có thể mới hơn danh sách đã tải trước. Bấm Tải lại để cập nhật danh sách.
- Không tồn tại hóa đơn được báo riêng; hóa đơn có thật nhưng chưa có dòng được hiện
  0 tiền và thông báo chưa có chi tiết. DB trống không được chèn dữ liệu giả.

Audit dùng `maTaiKhoan` từ session, `loaiTacNhan='ACCOUNT'`, `ketQua='SUCCESS'`.
Không ghi sessionId vì DesktopSession không tương ứng một hàng PhienDangNhap.
Service giữ quyền sở hữu transaction/connection; DAO không commit/đóng connection.
Lỗi audit rollback thay đổi dịch vụ. Lỗi rollback/close được giữ dưới suppressed exceptions.
Commit đã được xác nhận nhưng close lỗi có exception riêng để UI không báo lưu thất bại.
Lưu thành công nhưng reload lỗi được báo thành hai trạng thái riêng, giữ mã ở chế độ sửa.
Lỗi kết nối trong lúc commit có thể chưa xác định kết quả: UI yêu cầu tải lại kiểm tra,
không tự retry thao tác ghi. UI chỉ hiện SQLState/mã lỗi, không in SQL thô/credential.

## API panel cho Core

```java
new DichVuPanel(DesktopSession session, ConnectionProvider connections);
new HoaDonPanel(DesktopSession session, ConnectionProvider connections);
```

Các constructor trên là chữ ký mô tả; cách gọi thực tế:

```java
new DichVuPanel(session, TeamDevConnection::open);
new HoaDonPanel(session, TeamDevConnection::open);
```

`ConnectionProvider` thuộc `com.quanlynhakhoa.billing.dao`,
`Connection open() throws SQLException`; phải trả connection mới ở auto-commit.
Cả hai panel cũng có overload chỉ nhận session, dùng TeamDevConnection mặc định.
Core có thể tham khảo đoạn đăng ký dưới đây **sau khi review** (chưa áp dụng trong PR):

```java
import com.quanlynhakhoa.dichvu.ui.DichVuPanel;
import com.quanlynhakhoa.billing.ui.HoaDonPanel;
import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.features.FeaturePanelFactory;
import com.quanlynhakhoa.security.DesktopSession;
import javax.swing.JPanel;

// Nối vào builder hiện tại, giữ nguyên đăng ký Module.ACCOUNTS.
.register(Module.SERVICE_CATALOG, new FeaturePanelFactory() {
    @Override public JPanel create(DesktopSession session) {
        return new DichVuPanel(session, TeamDevConnection::open);
    }
})
.register(Module.BILLING, new FeaturePanelFactory() {
    @Override public JPanel create(DesktopSession session) {
        return new HoaDonPanel(session, TeamDevConnection::open);
    }
})
```

## Chạy độc lập

Chạy từ **root của worktree feature** (thư mục chứa pom.xml).
Máy cần JDK 21+, Maven Wrapper/dependencies và cấu hình development riêng đã onboarding.
Không dùng file `.env` của kiến trúc Spring cũ hoặc credential SQL admin để chạy module.

```powershell
.\mvnw.cmd clean verify test-compile
.\scripts\test-service-billing-m1.ps1
.\scripts\run-service-billing-m1.ps1
```

Launcher `com.quanlynhakhoa.billing.ui.ServiceBillingLauncher` dùng JdbcAuthService,
LoginPolicy, BCryptPasswordVerifier và TeamDevConnection hiện có; không tự tạo principal.
Nhập username/password Swing thật. CLINIC_MANAGER có hai tab; RECEPTIONIST chỉ đọc
Billing; các role khác không được tự cấp thêm quyền. Logout/đóng cửa sổ vô hiệu session.
Chưa có cấu hình `.n08-dev.local.properties` thì launcher dừng, không fallback DB khác.

Nếu worktree riêng chưa có cấu hình, làm theo `docs/TEAM_LOCAL_ONBOARDING.md` và quy định
file local trong `docs/M1_CORE01_PERMISSIONS.md`. Feature không chạy setup/GRANT/DDL.
Không commit hoặc gửi credential. Quyền SQL thiếu phải được Huy xử lý riêng.

## Bằng chứng kiểm thử và phần còn mở

Máy thực thi có JDK 25.0.4; Maven biên dịch với `--release 21` từ pom hiện có.
Maven cần quyền ghi cache `.m2` ngoài workspace; đã chạy lại sau sandbox từ chối cache.

| Kiểm tra | Kết quả |
|---|---|
| `mvnw.cmd -q clean verify test-compile` | PASS |
| `scripts/test-service-billing-m1.ps1` | PASS: self-test nghiệp vụ 125 checks + self-test Swing headless |
| `scripts/test-r4-role-session.ps1` | PASS: 623 checks |
| `scripts/test-r4-main-navigation.ps1` | PASS: 63 checks |
| `scripts/test-r4-final-shell.ps1` | PASS: 1610 checks |
| `scripts/run.ps1 -Smoke` | PASS: chỉ bootstrap |
| `ServiceBillingPreview` | Đã render và xem bố cục hai panel bằng dữ liệu tổng hợp, không SQL |
| `ServiceBillingSqlIT --read TSTM1001` | BLOCKED: không có cấu hình development được duyệt; không mở SQL |
| Lưu/đọc lại SQL thật, mở lại app | **Chưa thực hiện** |
| Login thật/UI tương tác trực tiếp với SQL | **Chưa thực hiện** |

Self-test main được chạy rõ ràng bởi script; `mvn verify` một mình không chạy chúng.
Các test độc lập bao gồm nhiều loại hàng, nhiều lần thu, thu 0/một phần/đủ/hủy,
BigDecimal lớn, công nợ âm/cảnh báo, phân quyền mọi role và logout trước JDBC,
validate giá/độ dài, mã trùng ở precheck và lỗi PK đồng thời, audit rollback,
giữ lỗi gốc khi rollback/close lỗi, commit thành công rồi close lỗi, timeout/parameter
JDBC, ánh xạ chi tiết/thanh toán/hủy, ngày kết thúc bao gồm cả ngày, và hóa đơn không
tồn tại so với không có dòng. Kiểm thử lịch sử/transaction dùng dữ liệu immutable và
JDBC double; **không chứng minh đã chạy công thức SQL trên SQL Server**.

Swing headless đã kiểm tra: constructor không mở JDBC, chọn dòng sau sort đúng model,
khóa mã, trạng thái áp dụng lại, clear form, bảng không sửa ô, ghi ngoài EDT,
bấm Lưu lặp chỉ ghi một lần, lưu thành công/reload thất bại báo đúng, lỗi kết nối,
ngày sai, dữ liệu trống, không đưa kết quả vào UI sau logout. Ảnh bố cục ở `target/`
chỉ dùng QA local, không commit và không thay cho nghiệm thu UI tương tác thực tế.

## Nghiệm thu SQL và UI cần chạy trên máy đã cấu hình

Đăng nhập bằng tài khoản CLINIC_MANAGER thật đã được Core cấp, dùng n08dev_* / N08Dev_*.
Runner sau sẽ tạo **một dịch vụ tổng hợp được đánh dấu kiểm thử** và giữ lại:

```powershell
# Chọn mã TST mới, dài tối đa 10 ký tự; nhập password Swing qua console ẩn.
.\scripts\run-service-billing-m1.ps1 -SqlWrite -TestId TSTM1001
# Đóng process rồi mở process mới:
.\scripts\run-service-billing-m1.ps1 -SqlRead -TestId TSTM1001
```

Write runner kiểm tra mã chưa có, thêm giá 0, đọc lại, sửa giá 123456, ngừng áp dụng,
đọc lại, áp dụng lại, đọc lại. Mỗi thao tác mở connection mới, có audit cùng transaction.
Read runner không ghi dịch vụ, xác minh giá/trạng thái đã lưu từ process trước và đọc
danh sách Billing. Cơ chế đăng nhập hiện có vẫn có thể cập nhật thông tin đăng nhập.
Không tự xóa dịch vụ kiểm thử, không chèn bệnh nhân/lượt khám/hóa đơn/lần thu giả.

Sau đó chạy UI bằng launcher; tìm mã TST, lọc đang/ngừng/tất cả, chọn dòng, sort,
sửa tên/giá/mô tả/CLS, thử mã trùng/giá sai, bấm Lưu lặp, kiểm tra reopen/logout/lỗi DB.
Dùng dữ liệu hóa đơn tổng hợp đã được thành viên sở hữu phê duyệt để kiểm chứng SQL:
nhiều dòng × nhiều lần thu, đủ ba loại hàng, lần thu hủy, hóa đơn hủy, tổng lớn,
thu vượt tổng và đối chiếu công nợ BN ngoài bộ lọc. M1 không tự cấp quyền ghi để tạo fixture.

## Hợp đồng/phụ thuộc M2 (chưa triển khai)

- Có lượt khám hợp lệ, giá chốt tại thời điểm lập, nhân viên liên kết tài khoản thật.
- Hợp đồng tích hợp các module khám/kho và quyền SQL ghi được review riêng.
- Transaction ghi hóa đơn/chi tiết/trạng thái/thu/audit; khóa và kiểm tra lại số còn nợ
  trong transaction để tránh thu đồng thời vượt tổng, chống ghi/thu trùng bằng khóa nghiệp vụ.
- Mỗi lần thu một phương thức. Thu một phần giữ công nợ; tiền mặt có tiền khách đưa
  ≥ số thu; CK/POS có mã giao dịch và quy tắc chống trùng lần thu thành công.
- Hủy giữ lịch sử, phân tách yêu cầu và người duyệt, kiểm tra điều kiện hóa đơn/lần thu.
  Không QR/bank tự xác nhận, không duyệt hủy hoặc API ghi Billing trong PR M1.
