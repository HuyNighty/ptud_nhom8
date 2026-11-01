> **PO APPROVED — 01/11/2026 (R1-D):** Chỉ CHO_HIEU_LUC → DA_HUY, bắt buộc lý do. DANG_THUC_HIEN, HOAN_THANH và DA_HUY không được hủy. Patch R1-C đã áp dụng; không xóa lịch sử. Xem N08_FINAL_SCREENS_R1D_APPROVED.md cho trạng thái hiện tại.

> Nội dung bên dưới được lưu như bằng chứng lịch sử của giai đoạn trước; các ghi chú PROPOSED/OPEN/chưa áp dụng đã được quyết định PO ở trên thay thế.

# N08-FINAL-SCREENS-R1-B — SQL readiness & final review

Workspace: C:\Dev\N08\ptud_nhom8-final-screens-r1. Branch: codex/n08-final-screens-r1-shifts. HEAD/base: 1f5ada23035f15d2b6f59cfa2e1d441a52e496eb. Giữ toàn bộ thay đổi R1 chưa commit; bổ sung readiness trên cùng branch. Không commit/push/merge. Không chạy grant hoặc ghi dữ liệu SQL. Không sao chép credentials, không đổi RolePermissionPolicy/schema.

## Kết luận review

Giữ **32/32 SelfTest PASS**, compile sạch và Swing smoke PASS. Script đề xuất có đúng 9 mục quyền; kiểm thử hoàn toàn offline đã PASS. **Live SQL readiness/integration vẫn BLOCKED** vì không có .n08-dev.local.properties hợp lệ trong worktree. Không chạy cả -Check SQL live lẫn -Apply; trạng thái quyền hiệu lực thực tế trước/sau chưa được đo và chưa thay đổi.

**OPEN BUSINESS DECISION:** có cho phép hủy phân công HOAN_THANH hay không? docs/THIET_KE_DU_LIEU.md mục 7 chỉ ghi CHO_HIEU_LUC → DANG_THUC_HIEN → HOAN_THANH; DA_HUY có lý do. V5 chỉ ràng buộc trạng thái và lý do, không quy định nguồn chuyển sang DA_HUY. Các tài liệu Markdown hiện có không có quyết định rõ cho HOAN_THANH → DA_HUY (hoặc riêng DANG_THUC_HIEN → DA_HUY). Không coi schema cho phép là nghiệp vụ đã được duyệt.

Predicate R1 hiện là trangThai <> 'DA_HUY': về kỹ thuật vẫn hủy được HOAN_THANH. **Không thay đổi predicate, service hoặc tự chặn/mở state transition trong R1-B.** DAO chỉ thêm comment đánh dấu quyết định còn mở. PO cần xác nhận các trạng thái nguồn được phép hủy trước khi nghiệm thu/release nhánh hủy. Không thử chuyển HOAN_THANH trên DB thật khi chưa có quyết định này.

## Ma trận quyền trước/sau (tĩnh, không phải quyền DB đã đo)

“Trước” dưới đây đối chiếu scripts/grant-dev-all.ps1 hiện hữu. “Sau đề xuất” là quyền module sẽ yêu cầu; script chỉ GRANT phần còn thiếu theo effective permission, không cấp lại phần đã có. Quyền phục vụ feature khác giữ nguyên, không tự revoke.

| Đối tượng | Quyền cần theo DAO | Trước: grant-dev-all.ps1 | Sau đề xuất / chưa Apply |
|---|---|---|---|
| dbo.NhanVien | SELECT | Có | Giữ; chỉ cấp nếu thực tế thiếu |
| dbo.TaiKhoan | SELECT | Có | Giữ; chỉ cấp nếu thực tế thiếu |
| dbo.CaLamViec | SELECT | Có | Giữ; chỉ cấp nếu thực tế thiếu |
| dbo.PhanCong | SELECT | Có | Giữ; chỉ cấp nếu thực tế thiếu |
| dbo.ThamSoHeThong | SELECT | Thiếu | Bổ sung nếu thiếu |
| dbo.PhanCong | INSERT | Thiếu | Bổ sung nếu thiếu |
| dbo.PhanCong.trangThai | UPDATE cột | Thiếu | Bổ sung đúng cột nếu thiếu |
| dbo.PhanCong.lyDoHuy | UPDATE cột | Thiếu | Bổ sung đúng cột nếu thiếu |
| dbo.NhatKyThaoTac | INSERT | Có | Giữ; chỉ cấp nếu thực tế thiếu |

Không thêm DELETE, CONTROL, db_owner, GRANT OPTION hoặc UPDATE toàn bảng. DAO dùng PreparedStatement; lockEmployee dùng SELECT WITH(UPDLOCK,HOLDLOCK), không UPDATE NhanVien. Không thêm quyền UPDATE NhanVien để né lỗi; cần kiểm chứng lock hint với app login trên SQL Server thực, nếu lỗi quyền thì giữ BLOCKED và báo DBA/PO.

SQL dự kiến cho hai cột sửa, chỉ khi kiểm tra còn thiếu:

```sql
GRANT UPDATE ON OBJECT::[dbo].[PhanCong] ([trangThai]) TO [n08dev_<8_hex>];
GRANT UPDATE ON OBJECT::[dbo].[PhanCong] ([lyDoHuy]) TO [n08dev_<8_hex>];
```

Đây là mẫu review, không phải batch để chạy trực tiếp; dùng script có guard/xác nhận bên dưới.

## Script grant đề xuất

[scripts/grant-lichlamviec-dev.ps1](../scripts/grant-lichlamviec-dev.ps1):

- -Plan: chỉ xuất 9 mục quyền offline, không đọc config/không kết nối DB.
- -Check: đọc config endpoint, xác minh target và quyền bằng SELECT metadata, EXECUTE AS/REVERT trong session. Không mở transaction ghi, không GRANT, không DDL hoặc probe DML. In PRESENT/MISSING; NOT READY nếu thiếu, UNKNOWN/null hoặc lỗi thì fail closed.
- -Apply: **chưa chạy trong nhiệm vụ này**. Sau kiểm tra, yêu cầu nhập chính xác GRANT SCHEDULE <database> <appUser>. Chỉ sau xác nhận mới mở transaction, cấp phần thiếu, kiểm tra lại và commit; lỗi rollback. Có write gate kiểm tra đồng thời Apply + confirmed + transaction; không có bypass confirmation parameter.
- Chỉ database có tên khớp chính xác N08Dev_[0-9a-f]{8}; user phải n08dev_ cùng suffix; port hợp lệ, endpoint cố định tcp:127.0.0.1. Kiểm tra lại DB_NAME khi kết nối, trước từng grant và trước commit.
- Dùng Windows Integrated Security đã được DBA cấp quyền quản trị phù hợp; không nhận/đọc/in mật khẩu app hoặc admin, không ghi connection string. Không tạo login/user/schema, không thay file credentials. Nếu Windows authentication/SPN/metadata/impersonation bị chặn: dừng, không tự fallback vào endpoint/credential khác.
- Kiểm tra mapping database user ↔ login qua SID. Từ chối app sysadmin/db_owner, database/schema/object CONTROL, DELETE trên bảng module, UPDATE toàn bảng PhanCong hoặc trên cột ngoài trangThai/lyDoHuy. Quyền rộng có sẵn gây dừng, không tự revoke.
- Từ chối explicit DENY trên phạm vi module (kể cả token vai trò/public) thay vì tự override DENY bằng column GRANT. Gate này chủ ý bảo thủ; DBA phải review DENY nếu gặp.
- HAS_PERMS_BY_NAME kiểm tra quyền hiệu lực gồm quyền kế thừa; null không bị hiểu là thiếu quyền để tự cấp. Quyền identifiers cố định trong allowlist; các tham số kiểm tra được bind, user name bị ràng buộc suffix hex.

Cơ sở cú pháp: [Microsoft HAS_PERMS_BY_NAME](https://learn.microsoft.com/en-us/sql/t-sql/functions/has-perms-by-name-transact-sql?view=sql-server-ver17) mô tả kiểm tra quyền hiệu lực; [Microsoft GRANT object permissions](https://learn.microsoft.com/en-us/sql/t-sql/statements/grant-object-permissions-transact-sql?view=sql-server-ver17) mô tả GRANT theo cột và lưu ý DENY/column GRANT. Lock semantics: [Microsoft table hints](https://learn.microsoft.com/en-us/sql/t-sql/queries/hints-transact-sql-table?view=sql-server-ver17). Tài liệu không thay thế kiểm chứng SQL engine thực tế.

## Review nghiệp vụ còn lại

- Ca/ngày: đọc SO_CA_TOI_DA_NGAY; nếu không có hàng dùng 2 theo mục 9.2 tài liệu hiện hữu. Giá trị <1/không phải số chặn ghi; không sửa tham số. Đếm các phân công cùng người/ngày không DA_HUY, gồm cả HOAN_THANH.
- Trùng giờ: [start,end), cho phép ca liền kề; DA_HUY không chặn. Nhân viên được UPDLOCK/HOLDLOCK trước đọc lịch và ghi, giúp tuần tự hóa các transaction tạo/hủy của module. Không thay bằng quyền rộng hay bỏ lock; race trên SQL engine vẫn chưa kiểm chứng.
- Nghỉ việc: tạo mới từ chối nhân viên thiếu/NGHI_VIEC hoặc ngày trước ngayVaoLam. Hủy bản cũ không yêu cầu DANG_LAM_VIEC (R1 giữ lịch sử và cho phép xử lý lịch cũ); không đổi quy tắc này. Chưa có quyết định riêng cho việc hủy lịch đã hoàn thành của nhân viên nghỉ việc — thuộc OPEN BUSINESS DECISION trên.
- Không bổ sung rule ca qua đêm, staffing, đổi người hoặc bắt đầu/hoàn tất ca.

## UI refinement và source/test

- LichLamViecPanel: đổi nhãn thành “Tìm lựa chọn NV” và “Lọc bảng”; thêm tooltip/dòng hướng dẫn. Text search chỉ thu hẹp danh sách lựa chọn, không tìm nội dung bảng. Combo + Tải lại lọc bảng phân công.
- Giữ nhân viên đang lọc trong combo nếu tìm kiếm không khớp, tránh trường hợp bảng vẫn lọc người A nhưng combo âm thầm chuyển sang Tất cả. Không đổi truy vấn DAO hay service.
- ShiftJdbcDouble mô phỏng lọc danh sách nhân viên bằng keyword để test được ngữ nghĩa; LichLamViecUiSelfTest thêm 4 checks cho scope search, áp dụng combo, giữ active filter và kết quả bảng.
- PhanCongDao chỉ thêm comment OPEN BUSINESS DECISION; PhanCongService giống từng byte với bản R1 trước review.
- Script mới grant-lichlamviec-dev.ps1 và test-lichlamviec-permissions.ps1. Không sửa grant-dev-all.ps1 hoặc quyền/module khác.

## Xác minh

| Kiểm tra | Kết quả |
|---|---|
| .\mvnw.cmd clean test-compile | PASS: 187 main / 39 test |
| node C:\Dev\N08\final-screens-r1b-evidence\verify.cjs after | 32/32 PASS |
| Swing smoke LichLamViecUiSelfTest (nằm trong 32 test) | PASS: 32 checks (trước 28); ảnh là dữ liệu giả lập |
| PhanCongSelfTest | PASS: 105 checks, không đổi nghiệp vụ |
| .\scripts\run.ps1 -Smoke | PASS |
| .\scripts\test-lichlamviec-permissions.ps1 | PASS: parser, exact 9-entry matrix, invalid modes, closed write gates; không SQL |
| git diff --check + review index gồm file mới | PASS |
| SQL -Check/live permission matrix | BLOCKED / chưa chạy |
| SQL -Apply / business writes / live integration | KHÔNG CHẠY |

Tổng số SelfTest vẫn 32, tất cả PASS. JDK 25 biên dịch release 21; không có SQL Server driver trong classpath runner test; N08_DB_* bị loại khỏi tiến trình con. Không tự thêm config từ workspace khác. File target/shifts-ui/*.png là ảnh Swing với JDBC double, không chứng minh SQL readiness thật.

## Hướng dẫn PO/DBA kiểm thử thực tế — cần phê duyệt riêng

1. Review 9-entry matrix, script và OPEN BUSINESS DECISION; ghi quyết định trạng thái nguồn được phép hủy. Chưa release nhánh hủy completed trước quyết định này.
2. Owner provision cấu hình N08Dev cô lập hợp lệ cho đúng worktree bằng quy trình được duyệt. Không sao chép bí mật từ checkout khác; không commit/log cấu hình. Chuẩn bị Windows principal đã được DBA cho phép đọc metadata, impersonate app user và cấp đúng các quyền còn thiếu; script không tự cấp quyền cho principal quản trị.
3. Chạy offline trước: .\scripts\grant-lichlamviec-dev.ps1 -Plan. Sau khi được phép kiểm tra DB: .\scripts\grant-lichlamviec-dev.ps1 -Check. Lưu matrix PRESENT/MISSING, target identifier và lỗi đã lọc; không lưu credential. Check không thay đổi DB. Dừng nếu có broad rights, DENY, schema/mapping sai hoặc evaluation UNKNOWN.
4. **Chỉ sau phê duyệt cấp quyền riêng:** .\scripts\grant-lichlamviec-dev.ps1 -Apply, nhập chuỗi xác nhận chứa đúng target/user được script hiển thị. Không dùng batch GRANT trực tiếp từ tài liệu. Chạy -Check lại để lưu matrix sau thực tế; nếu lỗi/rollback không rõ, DBA kiểm tra trước retry. Không cấp db_owner/CONTROL/DELETE để xử lý lỗi.
5. Với tài khoản ứng dụng manager/admin đã xác thực thật trên N08Dev: chọn nhân viên đang làm và ca có sẵn, ngày không xung đột → tạo CHO_HIEU_LUC → đọc lại management → đăng nhập tài khoản liên kết nhân viên đó xem My Schedule → manager hủy với lý do → đọc lại DA_HUY/reason ở cả hai màn hình → đối chiếu audit tạo/hủy. Không chạy nhánh HOAN_THANH hoặc DANG_THUC_HIEN khi quyết định nguồn hủy chưa được PO làm rõ.
6. Kiểm tra âm: người khác không đọc được lịch riêng qua UI; role không quản lý bị chặn service; ngày sai/nhiệm vụ trống/lý do trống/nhân viên nghỉ việc/đủ ca/trùng giờ bị chặn; ca liền kề hợp lệ; hủy không DELETE dữ liệu. Hai kết nối JDBC đồng thời cho cùng nhân viên/ngày để kiểm chứng khóa và overlap; quan sát lỗi quyền lock hint nếu có, không tự thêm UPDATE NhanVien.
7. Lưu bằng chứng target N08Dev, app role, maPhanCong kiểm thử, trạng thái trước/sau, audit, số dòng bị ảnh hưởng và matrix trước/sau. Không dùng principal giả với DB thật; không thay schema/seed dữ liệu/user data ngoài kế hoạch thử đã duyệt. Nếu thiếu ca, tài khoản, grant hoặc schema thì giữ BLOCKED.

## Artifacts

Bằng chứng ngoài repo: C:\Dev\N08\final-screens-r1b-evidence gồm compile.log, smoke.log, after-tests.json, static-permissions.log, permission-plan.json, review.patch (toàn branch từ HEAD), r1b-delta.patch (chỉ thay đổi kể từ bàn giao R1), changed-files.txt, r1b-changed-files.txt, ảnh screenshots. before-files.json là snapshot đối chiếu R1, không chứa credentials (worktree không có config).

Commit message đề xuất, chưa thực hiện: chore(lich-lam-viec): chuẩn bị quyền SQL tối thiểu và làm rõ bộ lọc nhân viên
