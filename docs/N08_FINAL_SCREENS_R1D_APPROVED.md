# N08-FINAL-SCREENS-R1-D — PO APPROVED state transitions

**PO APPROVED — 01/11/2026. Đã triển khai quyết định.**

Workspace: C:\Dev\N08\ptud_nhom8-final-screens-r1. Branch: codex/n08-final-screens-r1-shifts. HEAD: 1f5ada23035f15d2b6f59cfa2e1d441a52e496eb, giữ nguyên. Working tree đã có R1/R1-B/R1-C chưa commit trước khi bắt đầu; snapshot và status được lưu ngoài repo, không reset/clean/ghi đè công việc khác. Chưa commit/push/merge hoặc tạo PR.

## Quyết định chính thức

| Nguồn | Đích DA_HUY | Kết quả |
|---|---|---|
| CHO_HIEU_LUC | Có lý do hợp lệ | Cho phép |
| DANG_THUC_HIEN | Mọi lý do | Không cho phép |
| HOAN_THANH | Mọi lý do | Không cho phép |
| DA_HUY | Mọi lý do | Không cho phép |

Không DELETE lịch sử. Lý do hủy bắt buộc, trim và dài 1–300 ký tự. Anonymous, session logout và role thiếu WORKFORCE_MANAGE bị từ chối như trước.

## Patch và thực thi

1. Kiểm tra branch/HEAD/status và lưu snapshot trước thay đổi.
2. git apply --check docs/proposals/N08_FINAL_SCREENS_R1C_STATE_TRANSITIONS.patch: PASS.
3. git apply cùng patch: PASS. Patch gốc năm file được giữ nguyên như artifact lịch sử; **đã áp dụng, không apply lại**.
4. Chuyển comment nguồn sang PO APPROVED 01/11/2026 và bổ sung regression trực tiếp theo nhiệm vụ D.

DAO dùng đúng câu ghi có điều kiện:

```sql
UPDATE dbo.PhanCong
SET trangThai='DA_HUY', lyDoHuy=?
WHERE maPhanCong=? AND trangThai='CHO_HIEU_LUC'
```

Không tách kiểm tra trạng thái thành SELECT rồi ghi không điều kiện. Nếu trạng thái thay đổi trước UPDATE, không có dòng phù hợp. DAO yêu cầu đúng 1 dòng; 0 hoặc số khác 1 ném lỗi trước audit. Service giữ quyền/lý do/transaction/khóa nhân viên; gọi DAO guarded UPDATE, chỉ audit sau DAO thành công, chỉ trả thành công sau commit. Lỗi gây rollback, không success audit/commit. Helper canCancelStatus phục vụ UI, không thay thế SQL predicate.

UI bật nút và chấp nhận action hủy chỉ với CHO_HIEU_LUC. Worker thất bại không đi vào nhánh success. Test mô phỏng stale UI gọi service với HOAN_THANH xác minh hiển thị lỗi thay vì “Đã hủy”. Không đổi SQL Authentication hoặc bất kỳ quyền tối thiểu nào.

## File thay đổi trong R1-D

Sáu Java file:

- service/PhanCongService.java: helper policy được phê duyệt; transaction/validation hiện hữu được giữ.
- dao/PhanCongDao.java: state predicate ngay trong UPDATE và lỗi khi không ghi đúng một dòng.
- view/LichLamViecPanel.java: enable/action guard theo policy.
- PhanCongSelfTest.java: ma trận bốn trạng thái, unknown/null, ba mô phỏng race, unexpected affected-row count.
- ShiftJdbcDouble.java: conditional UPDATE/hook tiến trạng thái trước ghi và fixture count khác 1.
- LichLamViecUiSelfTest.java: bốn trạng thái nút và error-worker không báo success.

Ba tài liệu R1/R1-B/R1-C có banner PO APPROVED ngày 01/11/2026; phần decision của R1-C đã chuyển từ Chờ PO sang APPROVED và ghi patch đã áp dụng. Báo cáo D này là trạng thái mới nhất. Nội dung lịch sử còn lưu có nhãn superseded, không phải blocker nghiệp vụ hiện tại.

## Kiểm thử đã chạy

| Kiểm tra | Kết quả |
|---|---|
| .\mvnw.cmd clean test-compile | PASS: 187 main / 39 test |
| node C:\Dev\N08\final-screens-r1d-evidence\verify.cjs after | 32/32 SelfTest PASS |
| PhanCongSelfTest | PASS: 132 checks, gồm matrix bốn trạng thái, race và affected-row khác 1 |
| LichLamViecUiSelfTest | PASS: 38 Swing checks, gồm disabled states và failure không báo thành công |
| .\scripts\run.ps1 -Smoke | PASS |
| .\scripts\test-lichlamviec-permissions.ps1 | PASS: 9-entry matrix, parser, auth selection và closed write gates; không mở SQL |
| git diff --check, kể cả file mới qua review index riêng | PASS |
| So byte các file ngoài phạm vi D với snapshot đầu D | Giữ nguyên: SQL auth/grant scripts, schema, RBAC, modules khác và proposal gốc |

30 SelfTest ngoài hai bài được mở rộng giữ nguyên exit/stdout/stderr so với R1-C. Hai bài mở rộng cũng PASS. Race fixture giả lập trạng thái tiến ngay trước UPDATE sang DANG_THUC_HIEN/HOAN_THANH/DA_HUY; tất cả phải không commit, rollback, không success audit và giữ trạng thái người ghi khác. Test count=2 xác minh guard đúng một dòng; schema PK thực tế vẫn giữ nguyên.

## Blockers còn lại

- Không có .n08-dev.local.properties trong worktree, không tạo/copy credentials. Live SQL integration vẫn BLOCKED.
- Không chạy grant -Apply, không mở SQL/ghi DB thật. Quyền hiệu lực và SQL Authentication mới chỉ xác minh offline; SQL end-to-end chưa PASS.
- Chưa thực thi luồng đăng nhập thật → tạo → management/My Schedule đọc lại → hủy → đọc lại/audit trên N08Dev được PO cho phép.
- Race/locking thực tế cần kiểm tra bằng hai JDBC connection trên SQL Server sau khi môi trường được phê duyệt; synthetic double không chứng minh engine locking.
- Quyết định trạng thái **đã đóng** theo PO, không còn chờ business approval cho bốn trạng thái trên. Nhánh sẵn sàng review PR về code/synthetic regression; không đồng nghĩa đã nghiệm thu SQL production.

## Git diff và đề xuất commit / PR

Artifacts ngoài repo: C:\Dev\N08\final-screens-r1d-evidence gồm compile.log, smoke.log, offline-permissions.log, after-tests.json, patch-application.log, before-status.txt, review.patch (toàn branch từ HEAD), r1d-delta.patch (chỉ bước D), r1d-changed-files.txt, diff-stat.txt, REPORT.md, PR_PROPOSAL.md.

Commit đề xuất cho toàn nhánh: feat(nhan-su): tích hợp lịch làm việc và áp dụng quy tắc hủy ca đã được PO duyệt

PR title đề xuất: feat(nhan-su): hoàn thiện phân công ca và lịch cá nhân với policy hủy đã duyệt

PR description đề xuất nằm trong PR_PROPOSAL.md, nêu rõ 32/32 PASS và SQL integration BLOCKED. Chưa tạo PR hoặc commit/push.
