# N08 M1-CORE-01 — Quyền JDBC cho nghiệp vụ (dev-only)

## Phạm vi

Java 21, Swing, JDBC, SQL Server. Chỉ tác động lên database phát triển **N08Dev_XXXXXXXX** trên `127.0.0.1`, với SQL login `n08dev_XXXXXXXX` đã được `setup-local.ps1` tạo. Không thao tác `N08Dental`, không chạy migration V1–V5 lần hai và không dùng `sa` để chạy ứng dụng.

| Bảng | Quyền cấp trong M1 | Phần sử dụng |
|---|---|---|
| BenhNhan, HoSoBenhAn | SELECT, INSERT, UPDATE | Trí: bệnh nhân và hồ sơ |
| LichHen | SELECT | Trí: đọc lịch; ghi lịch thuộc M2 |
| PhieuKham | SELECT, INSERT, UPDATE | Thịnh: phiếu khám (cần dữ liệu FK hợp lệ) |
| KeHoachDieuTri | SELECT | Thịnh: xem kế hoạch; ghi kế hoạch thuộc M2 |
| DichVu | SELECT, INSERT, UPDATE | Khang: danh mục dịch vụ |
| Thuoc, VatTu, NhaCungCap | SELECT, INSERT, UPDATE | Đăng: danh mục kho |
| LoHang | SELECT | Đăng: đọc lô; nhập/xuất thuộc M2 |
| HoaDon, ChiTietHoaDon, ThanhToan | SELECT | Khang: danh sách hóa đơn, công nợ đọc |
| NhatKyThaoTac | INSERT | Nhật ký append-only; không được UPDATE/DELETE |

**Không có DELETE, ALTER, CREATE, DROP, db_owner.** Những bảng không nêu trong bảng quyền không được cấp bởi gói này. M1 không cấp quyền thay đổi ca làm, nhân sự, lịch hẹn, tồn kho hoặc thanh toán. Những việc đó phải đưa vào đợt M2 riêng và có kiểm soát.

**Giới hạn:** các màn hình nghiệp vụ dùng chung **một SQL application login**, nên quyền SQL Server chỉ là giới hạn tối đa của ứng dụng, **không tách riêng được theo vai trò Swing**. Controller và đặc biệt Service vẫn phải kiểm tra Role/Authorization trước khi ghi dữ liệu. Gói này không chứng minh CRUD end-to-end hoặc triển khai Service. Mọi thao tác multi-table phải dùng transaction, xử lý JDBC ngoài Swing EDT. Không sử dụng dữ liệu bệnh nhân thực trong DB dev.

## Áp dụng trên branch riêng

1. Lấy `main` mới nhất và tạo branch `feat/n08-core-permissions-m1`/worktree riêng, không thay đổi worktree đã bảo vệ.
2. Giải nén ZIP. Copy `src/main/java/com/quanlynhakhoa/dev/TeamM1Permissions.java` vào đúng thư mục source; copy `scripts/grant-team-m1.ps1` vào `scripts/`; copy tài liệu này vào `docs/`.
3. `git apply --check` patch `patches/DevSetup-M1.patch`, chỉ `git apply` nếu check PASS. Patch thêm **một dòng** vào `DevSetup.java`: `TeamM1Permissions.grant(dev, sqlUser);` trong transaction setup, trước `dev.commit()`. Do đó, các máy **setup mới** sẽ nhận quyền M1 tự động cùng V1–V5.
4. Chạy `mvnw.cmd clean compile`, `scripts/run.ps1 -Smoke`, `git diff --check`, `git status --short`.
5. **Database đã setup trước** không có quyền M1: chạy `scripts/grant-team-m1.ps1 -Check` (không ghi SQL) và chỉ sau khi xác nhận đúng dev DB chạy `-Apply`. Script yêu cầu nhập `GRANT` và mật khẩu SQL admin ẩn. Tài khoản admin chỉ được dùng trong quá trình cấp quyền; App tiếp tục dùng `n08dev_...`.
6. Chạy `scripts/grant-team-m1.ps1 -Check` lại sau apply và thực hiện một thao tác CRUD thực tế bằng role hợp lệ sau khi các module triển khai. Nếu lệnh lỗi, không retry tự động và không chạy SQL trên `N08Dental`.

Cả Java và PowerShell đều không in password, SQL login password vẫn ở file `.n08-dev.local.properties` **bị Git ignore**. Không đưa file cấu hình này vào PR.

## Lệnh dùng thử trong PowerShell

```powershell
# Trong worktree Core mới; cấu hình DB local phải tồn tại trong root của worktree
.\mvnw.cmd clean compile
.\scripts\run.ps1 -Smoke
.\scripts\grant-team-m1.ps1 -Check -SqlAdminUser sa

# Chỉ nếu đúng database local N08Dev_* của chính mình:
.\scripts\grant-team-m1.ps1 -Apply -SqlAdminUser sa
.\scripts\grant-team-m1.ps1 -Check -SqlAdminUser sa

git diff --check
git status --short
```

Nếu worktree riêng chưa có `.n08-dev.local.properties`, có thể sao chép file **của chính mình** từ worktree onboarding, sau khi kiểm tra `git check-ignore` của file đích. Tuyệt đối không gửi file đó cho người khác. Việc sao chép local secret vào worktree tăng số bản sao chứa credential; xóa bản sao khi không còn cần.

## Tiêu chí chấp nhận

- Maven compile và smoke PASS.
- Fresh setup: `DevSetup` chạy V1–V5 và cấp quyền M1 trong transaction tạo DB mới. **Chưa kiểm thử thực tế trong gói này.**
- Existing setup: `-Check` không thực hiện ghi SQL, `-Apply` yêu cầu người dùng phê duyệt, chỉ ghi các GRANT đã định nghĩa. **Chưa kiểm thử thực tế trong gói này.**
- Không có bảng bị xóa, không sửa bất kỳ schema V1–V5 hay dữ liệu nghiệp vụ.
- Có kiểm tra quyền hiệu dụng SELECT/INSERT/UPDATE và từ chối nếu SQL login có DELETE trên bảng M1.
- Tài khoản Swing vẫn đăng nhập sau khi áp dụng; bảng và dữ liệu không thay đổi.
