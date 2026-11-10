> **PO APPROVED — 01/11/2026 (R1-D):** Chỉ CHO_HIEU_LUC → DA_HUY, bắt buộc lý do. DANG_THUC_HIEN, HOAN_THANH và DA_HUY không được hủy. Patch R1-C đã áp dụng; không xóa lịch sử. Xem N08_FINAL_SCREENS_R1D_APPROVED.md cho trạng thái hiện tại.

> Nội dung bên dưới được lưu như bằng chứng lịch sử của giai đoạn trước; các ghi chú PROPOSED/OPEN/chưa áp dụng đã được quyết định PO ở trên thay thế.

# N08-FINAL-SCREENS-R1 — Phân công ca và Lịch làm việc của tôi

## Trạng thái bàn giao

Đã triển khai hai màn hình JDBC/Swing và tích hợp menu. Compile, smoke và 32 SelfTest PASS. **Chưa nghiệm thu end-to-end SQL Server: BLOCKED vì worktree không có cấu hình N08Dev cô lập/phiên tài khoản thật đã xác thực.** Không có thao tác DB thật trong đợt này. Không được đọc kết quả synthetic thành bằng chứng persistence thật.

Worktree: C:\Dev\N08\ptud_nhom8-final-screens-r1. Branch: refactor/n08-final-screens-r1-shifts. Base: 1f5ada23035f15d2b6f59cfa2e1d441a52e496eb, origin/main lúc fetch (PR #101). Không commit, push, deploy hoặc merge. Không sửa nội dung worktree khác hoặc OneDrive.

## Trước / sau

| Màn hình | Trước | Sau |
|---|---|---|
| Phân công ca | Menu disabled | Menu có phân quyền; danh sách khoảng ngày/tuần, tìm và chọn nhân viên, chọn ca, tạo, xem chi tiết, hủy có lý do, tải lại |
| Lịch làm việc của tôi | MY_SCHEDULE chưa có factory | Panel chỉ đọc, điều hướng tuần/khoảng ngày, giờ ca, nhiệm vụ, trạng thái, chi tiết, trạng thái trống/lỗi |

Tái sử dụng shell/menu hiện tại. Bảng theo ngày thay vì sao chép toàn bộ prototype lịch ma trận; dùng chung panel với hai chế độ nhưng service/API lịch cá nhân không nhận employee ID. Logic sự kiện và SwingWorker nằm trong panel theo convention EmployeePanel, không thêm controller/interface hình thức.

## Hợp đồng dữ liệu và nghiệp vụ

- Đã đọc V1–V5, docs/THIET_KE_DU_LIEU.md, ảnh tham chiếu 12-phan-cong-lich-lam-viec.png; MainFrame, Module, FeatureBootstrap/Registry, DesktopSession, RolePermissionPolicy, EmployeeDirectoryDao, TeamDevConnection. Không có AGENTS.md trong repo.
- V1: accountId từ DesktopSession → TaiKhoan.maTaiKhoan → maNhanVien có FK/unique → NhanVien. Mỗi lần đọc lịch cá nhân đều phân giải lại qua PreparedStatement; không có ô nhập mã người khác. Tài khoản không có liên kết trả thông báo rõ và không truy vấn lịch tổng.
- V5: CaLamViec(maCa,tenCa,gioBatDau,gioKetThuc); PhanCong(maPhanCong,maNhanVien,maCa,ngayLam,nhiemVu,trangThai,lyDoHuy). Giữ CHO_HIEU_LUC, DANG_THUC_HIEN, HOAN_THANH, DA_HUY. Bản mới là CHO_HIEU_LUC. Hủy chỉ đổi trạng thái/lý do, không DELETE hoặc thay thông tin ca/nhiệm vụ cũ; không thêm workflow R2–R5.
- Ca kết thúc phải sau bắt đầu, phù hợp CK_CaLamViec_Gio. Không định nghĩa ca qua đêm/định mức staffing mới.
- Kiểm tra nhân viên tồn tại/đang làm việc và ngày phân công không trước ngày vào làm. Khóa dòng nhân viên bằng UPDLOCK,HOLDLOCK trong transaction; đọc ca bằng HOLDLOCK. Các thao tác create/cancel của module cùng nhân viên được tuần tự hóa qua khóa này, kể cả ngày chưa có phân công.
- Kiểm tra giao nhau khoảng thời gian [bắt đầu,kết thúc), cho phép ca liền kề. DA_HUY được loại khỏi trùng giờ và đếm ca, phù hợp UX_PhanCong_HieuLuc và cách LichHenDao đọc ca.
- SO_CA_TOI_DA_NGAY đọc từ ThamSoHeThong; nếu chưa có hàng cấu hình dùng mặc định 2 trong tài liệu mục 9.2. Giá trị cấu hình sai chặn ghi; không sửa/seed bảng tham số.
- Mã ngẫu nhiên 10 ký tự base32 SecureRandom (50 bit), theo convention mã ngẫu nhiên của tính năng JDBC hiện tại; không yêu cầu quyền ghi BoDemMa. PK trùng trả lỗi và yêu cầu tải lại, không overwrite hay tự động retry transaction.
- Tạo/hủy + ghi audit theo V4 cùng transaction. Kiểm tra quyền trước JDBC và trước commit. Chỉ trả thành công sau commit; lỗi audit/commit/logout rollback, commit không xác định không hiển thị success và yêu cầu tải lại trước retry. Lỗi quyền/schema được phản hồi BLOCKED, không sửa grant/schema.
- Guard SELECT DB_NAME() chặn ghi nếu tên không khớp N08Dev_<8 lowercase hex>; sử dụng TeamDevConnection hiện có, không đổi credentials.

## Quyền

| Vai trò | Phân công: đọc | Tạo/hủy | Lịch cá nhân |
|---|---|---|---|
| SYSTEM_ADMIN | Có | Có | Có; liên kết nhân viên có thể trống |
| CLINIC_MANAGER | Có | Có | Có |
| RECEPTIONIST | Không | Không | Có |
| DOCTOR | Không | Không | Có |
| NURSE | Không | Không | Có |
| INVENTORY_MANAGER | Không | Không | Có |

SHIFT_ASSIGNMENTS dùng WORKFORCE_READ; service ghi dùng WORKFORCE_MANAGE. MY_SCHEDULE dùng SCHEDULE_SELF_READ. Giữ nguyên RolePermissionPolicy và vòng đời session. Logout/anonymous bị từ chối, quyền áp dụng tại service và registry; UI chỉ hỗ trợ trạng thái nút.

## File mới và file dùng chung

Source mới tại quantri/lichlamviec:

- model/CaLamViec.java, NhanVienChon.java, PhanCong.java.
- dao/PhanCongDao.java.
- service/PhanCongService.java.
- view/LichLamViecPanel.java.

File dùng chung đổi: Module (thêm SHIFT_ASSIGNMENTS), MainFrame (menu/nhãn), FeatureBootstrap (hai factory). FeatureRegistry không cần đổi vì đã có guard permission và lazy creation.

Test mới: PhanCongSelfTest (105 checks), LichLamViecUiSelfTest (28 checks), helper ShiftJdbcDouble. Cập nhật bốn test hiện hữu: R4FinalShellSelfTest, R4RoleSessionSelfTest, R4MainNavigationSelfTest, FeatureRegistrySelfTest để kiểm tra module mới, count 21, danh sách manager và registration. Không sửa các feature thành viên khác.

## Kiểm thử và ảnh

| Lệnh / nhóm | Trước | Sau |
|---|---|---|
| .\mvnw.cmd clean test-compile | PASS: 181 main / 36 test | PASS: 187 main / 39 test |
| node C:\Dev\N08\final-screens-r1-evidence\verify.cjs baseline hoặc after | 30/30 PASS | 32/32 PASS |
| .\scripts\run.ps1 -Smoke | — | PASS |
| git diff --check | Sạch | PASS; kiểm tra cả file mới bằng index review riêng |

Các bài hiện hữu gồm AuthPolicy, JdbcAuthTransaction, DesktopLoginGate, core role/session/navigation, feature registry, Billing, Examination, account/workforce. Hai bài mới kiểm tra phân quyền sáu vai trò, chặn JDBC trước khi bị từ chối, guard DB, identity không thể chọn người khác, thiếu liên kết, overlap/ca liền kề/giới hạn/nghỉ việc/mất ca, tạo–đọc–lịch cá nhân–hủy bằng JDBC double, rollback khi audit/commit/logout, giữ lịch sử; Swing menu/factory, tải dữ liệu ngoài EDT, lọc cá nhân, trạng thái nút/trống/ngày sai/logout.

JDK 25, compiler release 21. Runner không nạp SQL Server driver, loại N08_DB_* khỏi environment test; SwingWorker dùng doubles. LichLamViecUiSelfTest và ba bài Swing có JFrame/Dialog dùng headless=false, còn lại headless=true. Không test locking/concurrency bằng SQL engine thật.

Ảnh render từ MainFrame và panel Swing thật với dữ liệu kiểm thử, **không phải ảnh database thực**: target/shifts-ui/assignment-synthetic.png, target/shifts-ui/my-schedule-synthetic.png. Có bản sao ngoài repo tại C:\Dev\N08\final-screens-r1-evidence\screenshots để không mất khi clean. Kiểm tra bố cục ở 1280×760; không khẳng định đã kiểm tra mọi DPI/kích thước màn hình.

Log và kết quả JSON: C:\Dev\N08\final-screens-r1-evidence (baseline-compile.log, after-compile.log, baseline-tests.json, after-tests.json, smoke.log, review.patch, changed-files.txt, diff-stat.txt).

## SQL verification / blockers

1. **BLOCKED — Actual JDBC persistence flow chưa chạy.** Không có .n08-dev.local.properties trong worktree mới, không tự sao chép credential, seed dữ liệu hoặc tạo principal giả trên DB thật. Chưa kiểm chứng live schema/permission; contract triển khai dựa vào V1–V5 checked-in. Chưa có bằng chứng DB read/write thực để tuyên bố end-to-end PASS.
2. Script grant-dev-all.ps1 hiện có SELECT PhanCong/CaLamViec nhưng không cấp INSERT PhanCong, UPDATE PhanCong(trangThai,lyDoHuy) hoặc SELECT ThamSoHeThong. Với chỉ các grant này, create/cancel cần DBA/PO cấp quyền riêng trước nghiệm thu; không thay script/grant hoặc né quyền. Audit cần INSERT NhatKyThaoTac. Ca/nhân viên/tham số cần quyền SELECT tương ứng. Lock hint trên truy vấn có thể cần quyền hiệu lực phù hợp; phải xác minh trên SQL Server thực.
3. Luồng nghiệm thu còn lại trên N08Dev đã được PO chấp thuận: đăng nhập thật với manager/admin → chọn nhân viên và ca đã có → tạo phân công → đọc lại quản lý → đăng nhập nhân viên tương ứng xem lịch cá nhân → manager hủy có lý do → hai màn hình đọc lại DA_HUY + reason → kiểm tra audit. Không tạo schema hoặc data mẫu tự động. Nếu thiếu bảng/cột/grant, giữ BLOCKED và báo chính xác lỗi SQL, không dùng db_owner/GRANT workaround.
4. Kiểm tra khóa và race bằng hai JDBC connection thật, rollback/audit, permissions thực, UI save/cancel bằng tài khoản thật vẫn là phần nghiệm thu DB chưa hoàn thành.

## Commit đề xuất

feat(nhan-su): triển khai phân công ca và lịch làm việc cá nhân bằng Swing JDBC
