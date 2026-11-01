# M1-CORE-06B1 — Tạo tài khoản nhân viên (Java 21/Swing/JDBC)

## Prerequisite

- CORE-06A must first be **committed** on its existing branch (`feat/n08-core06a-account-directory`) and preferably pushed/merged via PR.
- Branch CORE-06B1 from that commit as `feat/n08-core06b1-account-create`. Apply via `apply-core06b1.ps1` on a **clean** branch. If AccountPanel was intentionally edited since CORE-06A package, review changes and integrate manually; never override unreviewed code.
- Active local development database must be `N08Dev_[8 lowercase hex]`, restricted SQL login `n08dev_[same 8 hex]` on loopback. No production DB or real people data.

## Delivered feature

- `Hệ thống → Tài khoản → Tạo tài khoản`: employee ID, username, display name, role, employee-chosen password and confirmation. `SYSTEM_ADMIN` cannot be created from this UI.
- `AccountCreateService`: enforces `ACCOUNT_MANAGE` at Service and again throughout transaction; requires a linked active employee, not already assigned a login, and a role consistent with `chucDanh` (`LT`, `BS`, `YT`, `QL`, `KHO`).
- `AccountCreateDao`: locks target employee row, inserts account with BCrypt and `HOAT_DONG`, inserts an `ACCOUNT` audit record into `NhatKyThaoTac`. Commit is atomic; any failure rolls both rows back.
- Passwords remain only in the UI field and transient hashing path. No hash, password, or raw JDBC error is shown in the UI. Username unique and one-account-per-employee uniqueness are enforced by SQL constraints.
- Newly created account is usable **without a forced password change**, since the employee chooses their own password at creation. This is a **development/school-workflow compromise**, not an administrator-generated temporary password. Do not distribute passwords by text/chat. For a production implementation, design credential enrollment and mandatory password-change flow first.

## Scoped SQL grant

Baseline M1 SQL dev login can `SELECT` dbo.TaiKhoan/NhanVien and `INSERT` dbo.NhatKyThaoTac, but cannot create accounts. Explicit local admin approval is needed to `GRANT INSERT ON OBJECT::dbo.TaiKhoan TO [n08dev_...]`.

Only on the user's own `N08Dev_*` local database, run:

```powershell
.\scripts\grant-core06b1-account-create.ps1 -Check -SqlAdminUser sa
.\scripts\grant-core06b1-account-create.ps1 -Apply -SqlAdminUser sa
```

`-Apply` requires typing `GRANT` and hidden SQL admin password; both modes preflight 39-table schema, SQL principal identity and non-db_owner membership. The grant utility does not insert, update, or delete any account. `-Check` can report the missing grant before applying. Subsequent new local dev databases also require this explicit grant; `DevSetup` is deliberately unchanged.

**Security boundary:** Desktop roles are in-process application guards. The shared SQL application login is not limited by Swing role, so even scoped `INSERT` is a new database privilege. Only use the grant in isolated school development databases. A trusted backend or more restrictive SQL execution model is necessary for stronger production enforcement.

## Acceptance and constraints

```powershell
.\mvnw.cmd clean compile
.\scripts\run.ps1 -Smoke
.\scripts\grant-core06b1-account-create.ps1 -Check -SqlAdminUser sa
.\scripts\grant-core06b1-account-create.ps1 -Apply -SqlAdminUser sa # explicit manual approval
.\scripts\run.ps1
```

Login as `dev.admin`, open `Hệ thống → Tài khoản` and click `Tạo tài khoản`. Use **only a synthetic employee** already present in `dbo.NhanVien`. Confirm creation is displayed in directory and new account can authenticate using employee-chosen password. Validate duplicate username and duplicate employee fail, without partial audit entries.

No role updates, existing account lock/unlock, password reset, bulk import, SQL schema migrations, automatic rights elevation or SQL DELETE in this phase. No end-to-end SQL test executed by the author; verification occurs locally in the user's N08Dev DB.
