> **PO APPROVED — 01/11/2026 (R1-D):** Chỉ CHO_HIEU_LUC → DA_HUY, bắt buộc lý do. DANG_THUC_HIEN, HOAN_THANH và DA_HUY không được hủy. Patch R1-C đã áp dụng; không xóa lịch sử. Xem N08_FINAL_SCREENS_R1D_APPROVED.md cho trạng thái hiện tại.

> Nội dung bên dưới được lưu như bằng chứng lịch sử của giai đoạn trước; các ghi chú PROPOSED/OPEN/chưa áp dụng đã được quyết định PO ở trên thay thế.

# N08-FINAL-SCREENS-R1-C — Final Integration Hardening

Workspace: C:\Dev\N08\ptud_nhom8-final-screens-r1. Branch: codex/n08-final-screens-r1-shifts. HEAD: 1f5ada23035f15d2b6f59cfa2e1d441a52e496eb. Giữ toàn bộ R1/R1-B. Chưa commit/push/merge hoặc tạo PR. Không mở kết nối SQL, không chạy Check live, không chạy Apply/grant hay ghi dữ liệu SQL Server. Không sao chép credentials.

## SQL Authentication compatibility

| Script đối chiếu | Convention hiện hữu | R1-C |
|---|---|---|
| setup-local.ps1 | SQL Authentication, SqlAdminUser mặc định sa, localhost TCP, secure prompt | Cùng default SQL/sa, endpoint lấy từ config N08Dev đã có |
| grant-dev-all.ps1 | SQL Authentication mặc định; -WindowsAuth tùy chọn | Cùng cách chọn mode, không sửa script cũ |
| grant-team-m1.ps1 | SqlAdminUser/secure prompt, chuyển credential tạm qua process environment tới Java | Tương thích login/endpoint nhưng không dùng environment: SqlCredential nhận SecureString trực tiếp |

grant-lichlamviec-dev.ps1 mới:

- Default SQL Authentication, -SqlAdminUser 'sa' hoặc tên admin hợp lệ; mật khẩu qua Read-Host -AsSecureString. -WindowsAuth dùng Integrated Security, không hỏi mật khẩu SQL và không mang SqlCredential.
- SqlCredential giữ SecureString read-only; connection string không chứa User ID/Password. Không trích xuất plaintext, không dùng NetworkCredential/BSTR/environment/password file để truyền mật khẩu. Không dùng application-user password; từ chối admin login có dạng n08dev_<8 hex>.
- Pooling=false, Persist Security Info=false; đóng/dispose connection và dispose SecureString trong finally, kể cả khi cleanup transaction lỗi. Nếu tạo credential/connection thất bại cũng dispose secret. Không ghi raw SQL exception payload hoặc connection string ra log.
- Plan return trước config/auth factory, không prompt/connection. Check chỉ metadata/effective permissions trong session, không transaction ghi/GRANT/DML/DDL. Apply vẫn cần chuỗi xác nhận GRANT SCHEDULE <database> <appUser>, transaction, rollback, kiểm tra lại quyền và target. Không có auto-fallback từ Windows sang SQL hoặc ngược lại.
- Giữ nguyên đúng 9 mục quyền, strict target N08Dev_[0-9a-f]{8}, app user cùng suffix, fixed loopback TCP và port hợp lệ. Giữ guard chống db_owner/CONTROL/DELETE/UPDATE rộng, DENY và unknown permission evaluation.
- Không đổi setup-local, grant-dev-all, grant-team-m1, schema, RBAC, source Java hoặc feature khác. Đây là xác minh tương thích cấu hình/API offline; chưa xác nhận đăng nhập SQL Server thành công trên máy thực.

Ma trận giữ nguyên: SELECT NhanVien, TaiKhoan, CaLamViec, PhanCong, ThamSoHeThong; INSERT PhanCong; UPDATE PhanCong(trangThai), UPDATE PhanCong(lyDoHuy); INSERT NhatKyThaoTac. Không thêm quyền khác.

## Cách dùng cho PO sau khi có cấu hình hợp lệ

```powershell
# Offline, không prompt/kết nối:
.\scripts\grant-lichlamviec-dev.ps1 -Plan

# Kiểm tra chỉ đọc bằng SQL Authentication (secure prompt):
.\scripts\grant-lichlamviec-dev.ps1 -Check -SqlAdminUser sa

# Hoặc Windows Authentication, khi môi trường hỗ trợ:
.\scripts\grant-lichlamviec-dev.ps1 -Check -WindowsAuth
```

Nếu admin login khác sa, dùng tên đã được DBA provision; không truyền mật khẩu trên command line. Không dùng lại application SQL login. -Apply chỉ dành cho lần được PO phê duyệt cấp quyền riêng; không chạy trong task này. Target/config sai, TLS/authentication/impersonation/metadata thiếu quyền hoặc DENY/broad rights đều phải giữ BLOCKED, không đổi endpoint hoặc cấp quyền rộng để vượt gate. TrustServerCertificate chỉ cho endpoint loopback cô lập như setup hiện tại.

## Business decision: PO APPROVED 01/11/2026 — patch đã áp dụng tại R1-D

Patch: [proposals/N08_FINAL_SCREENS_R1C_STATE_TRANSITIONS.patch](proposals/N08_FINAL_SCREENS_R1C_STATE_TRANSITIONS.patch).

| Trạng thái nguồn | Đề xuất chuyển sang DA_HUY | Trạng thái phê duyệt |
|---|---|---|
| CHO_HIEU_LUC | Cho phép, bắt buộc lý do | APPROVED 01/11/2026 |
| DANG_THUC_HIEN | Không cho phép | APPROVED 01/11/2026 |
| HOAN_THANH | Không cho phép | APPROVED 01/11/2026 |
| DA_HUY | Không cho phép | APPROVED 01/11/2026 |

**PO APPROVED 01/11/2026 (R1-D).** Patch đã áp dụng. Predicate DAO hiện là trangThai='CHO_HIEU_LUC'; chỉ trạng thái này được hủy, bắt buộc lý do. Quyết định nghiệp vụ đã đóng; nghiệm thu SQL engine thật vẫn BLOCKED.

Patch đề xuất chạm năm file:

1. Service: canCancelStatus helper duy nhất cho hint UI, giữ service là entrypoint quyền/reason/transaction/audit và luôn đi qua DAO có conditional UPDATE; không dùng SELECT rồi tin rằng trạng thái còn nguyên.
2. DAO: UPDATE ... WHERE maPhanCong=? AND trangThai='CHO_HIEU_LUC'; số dòng khác 1 trả lỗi stale/forbidden, service rollback; không audit success/commit khi không ghi được. State predicate được đánh giá ngay tại bước ghi nên UI hoặc SELECT cũ không thể ép hủy trạng thái đã tiến triển.
3. UI: nút/action hủy dùng canCancelStatus, báo trạng thái đã thay đổi; đây chỉ là hỗ trợ UX, không thay kiểm tra trong UPDATE.
4. ShiftJdbcDouble: fixture conditional UPDATE và hook mô phỏng trạng thái đổi ngay trước ghi.
5. PhanCongSelfTest: matrix bốn trạng thái, lý do sẵn có vẫn bắt buộc; denied không commit/không success audit/giữ lịch sử; mô phỏng trạng thái từ CHO_HIEU_LUC đổi sang DANG_THUC_HIEN/HOAN_THANH/DA_HUY trước UPDATE, phải trả 0 dòng và rollback.

R1-C đã kiểm tra patch; tại R1-D, sau PO approval ngày 01/11/2026, git apply --check và git apply đã thành công. Clean compile và 32 SelfTest đã PASS với policy mới (xem báo cáo R1-D). Patch lưu trong proposals là bằng chứng lịch sử đã áp dụng, không áp dụng lại. SQL engine concurrency vẫn chưa được kiểm chứng.

## Verification

| Kiểm tra | Kết quả |
|---|---|
| .\mvnw.cmd clean test-compile | PASS: 187 main / 39 test |
| node C:\Dev\N08\final-screens-r1c-evidence\verify.cjs after | 32/32 PASS, bảo toàn R1/R1-B |
| LichLamViecUiSelfTest / Swing smoke | PASS: 32 checks, synthetic JDBC |
| .\scripts\run.ps1 -Smoke | PASS |
| .\scripts\test-lichlamviec-permissions.ps1 | PASS: exact matrix 9; mode/write gate; SQL secure prompt/Windows selection; invalid target/login; Plan cả hai auth |
| PowerShell parser trên grant và offline test script | PASS |
| git diff --check, bao gồm file mới qua index review riêng | PASS |
| git apply --check proposal | PASS tại R1-C; đã apply sau PO approval tại R1-D |
| Byte comparison mọi source Java/SQL/R1/R1-B docs và script khác | Không thay đổi so với đầu R1-C |

Offline auth tests mock secure prompt với credential giả, tạo SqlConnection rồi assert State=Closed, đúng endpoint/encryption/mode, SQL credential read-only, connection string không có credential, Windows không prompt, invalid target/app-user admin không được prompt. Không gọi Open; không chạy Apply entrypoint; gate tests chỉ kiểm tra điều kiện đóng. SecureString test được dispose. Live SQL authentication, permission matrix và persistence vẫn chưa kiểm chứng.

## Blockers / PO review

- Không có .n08-dev.local.properties trong worktree; không tự copy credential từ workspace khác. SQL Check/live integration vẫn BLOCKED.
- Chưa được phê duyệt cấp quyền: Apply không chạy; quyền hiệu lực thực tế chưa đo/chưa thay đổi.
- Transition policy đã PO APPROVED 01/11/2026 và đã áp dụng tại R1-D. Chưa tuyên bố SQL end-to-end PASS.
- Luồng SQL thật tạo → đọc → My Schedule → hủy → đọc lại/audit, kiểm tra quyền tối thiểu với lock hints và hai transaction đồng thời vẫn cần môi trường N08Dev được duyệt, tài khoản ứng dụng đăng nhập thật và dữ liệu thử được PO cho phép.

## Artifacts bàn giao

- Script thay đổi: scripts/grant-lichlamviec-dev.ps1 và scripts/test-lichlamviec-permissions.ps1.
- Báo cáo này và proposal patch; không có Java production/test change thực thi trong R1-C.
- C:\Dev\N08\final-screens-r1c-evidence: compile.log, after-tests.json, smoke.log, offline-permissions.log, proposal-apply-check.log, permission-script.diff, review.patch (đầy đủ branch từ HEAD), r1c-delta.patch (riêng R1-C), r1c-changed-files.txt, diff-stat.txt, REPORT.md.

Commit đề xuất, chưa thực hiện: chore(lich-lam-viec): hỗ trợ SQL Authentication an toàn và chuẩn bị policy hủy ca
