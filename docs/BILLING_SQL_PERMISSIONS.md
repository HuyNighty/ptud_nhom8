# Cấp quyền SQL Service & Billing — 31/10/2026

Theo yêu cầu trực tiếp “Thiếu quyền gì tự cấp đi”, đã bổ sung quyền còn thiếu trên
database development hiện có trong `.n08-dev.local.properties` của worktree
`service-billing-m1`. Không thay đổi cấu hình đăng nhập hoặc phân quyền Java.

## Quyền đã cấp thực tế

| Đối tượng | Quyền bổ sung |
|---|---|
| `dbo.HoaDon` | INSERT; UPDATE riêng cột `trangThai` |
| `dbo.ChiTietHoaDon` | INSERT |
| `dbo.ThanhToan` | INSERT |
| `dbo.YeuCauHuyHoaDon` | SELECT, INSERT; UPDATE riêng `trangThai`, `maTaiKhoanDuyet`, `thoiGianDuyet`, `lyDoDuyet` |
| `dbo.ChiTietDieuTri` | SELECT |
| `dbo.ChiDinhCanLamSang` | SELECT |

Tổng cộng **12 quyền còn thiếu**, commit cùng một transaction. Các quyền đọc nguồn
khác và INSERT nhật ký đã có được kiểm tra, giữ nguyên. UPDATE toàn bảng HoaDon hoặc
YeuCauHuyHoaDon vẫn bằng 0 là đúng: chỉ các cột liệt kê ở trên có UPDATE bằng 1.

Dùng Windows integrated authentication qua shared memory đến đúng instance cục bộ
khớp cổng development. Kết nối TCP Windows ban đầu lỗi SSPI; ứng dụng vẫn giữ JDBC
TCP và application login hiện có. Không dùng sa hoặc lưu mật khẩu quản trị.

Không cấp db_owner, DELETE, ALTER hoặc UPDATE lịch sử thanh toán/chi tiết hóa đơn/
dữ liệu khám. Không thay đổi Core, migration V1–V5, schema hoặc bản ghi nghiệp vụ.
Kiểm tra quyền Service theo session/role tiếp tục áp dụng như trước.

## Kiểm tra thực tế

- `scripts/grant-billing-dev.ps1 -Check`: xác nhận trước khi cấp đúng 12 quyền thiếu;
  chạy lại sau cấp không còn quyền thiếu.
- `scripts/grant-billing-dev.ps1 -Apply`: PASS, 12 quyền đã commit. Script chỉ cấp
  quyền thiếu trong danh sách cố định; xác minh đúng database N08Dev, mapping login,
  39 bảng, các cột, tài khoản ứng dụng không phải db_owner và giới hạn quyền trước commit.
- Maven `test-compile`: PASS.
- `BillingPermissionsSqlIT --verify`: **PASS 60 kiểm tra trên SQL Server thật bằng
  application login**, gồm quyền cần có, quyền không được cấp, DAO kiểm tra INSERT,
  truy vấn danh sách yêu cầu hủy, SELECT có UPDLOCK/HOLDLOCK, INSERT và UPDATE với
  điều kiện luôn sai (0 dòng), transaction probe luôn rollback.
- `BillingUiSqlPreflight --read-only`: PASS tên hàng, số điện thoại/ngày khám,
  JOIN chi tiết và đọc YeuCauHuyHoaDon; không còn lỗi S0005/229. Danh sách yêu cầu
  hủy hiện có 0 dòng, truy vấn chạy thành công.
- `git diff --check`: PASS; cấu hình bí mật và migration không thay đổi.

Đây là kiểm thử quyền và thực thi truy vấn thật. Không tạo hóa đơn, lần thu, yêu cầu
hủy hoặc dữ liệu khám để thử. Chưa thay thế nghiệm thu đầy đủ một ca tạo hóa đơn →
thu tiền → xét duyệt bằng dữ liệu nghiệp vụ thực và hai tài khoản nhân viên khác nhau.

## File và cách chạy lại

- Mới: `scripts/grant-billing-dev.ps1` — tiện ích cấp quyền tập trung cho Billing,
  tách khỏi script M1 cũ dùng phạm vi chỉ đọc hóa đơn.
- Mới: `src/test/java/com/quanlynhakhoa/billing/BillingPermissionsSqlIT.java` —
  kiểm tra application login thật, không tạo session ứng dụng giả.
- Mới: `docs/BILLING_SQL_PERMISSIONS.md`.
- Cập nhật: `docs/BILLING_UI_REVIEW.md` — ghi nhận vấn đề quyền đã được giải quyết.
- Biên nhận cục bộ: `target/billing-permission-grant.json` — thời điểm và danh sách
  quyền đã cấp, không có mật khẩu; target không đưa vào Git.

```powershell
# Windows user cần có quyền quản trị đúng database development cho tiện ích này.
.\scripts\grant-billing-dev.ps1 -Check
# -Apply là thao tác cấp quyền, chỉ chạy khi được chủ dự án cho phép.
.\scripts\grant-billing-dev.ps1 -Apply
```

Ứng dụng đang mở có thể bấm **Làm mới** để đọc lại dữ liệu. Không cần đổi tài khoản
SQL trong cấu hình sang tài khoản quản trị.
