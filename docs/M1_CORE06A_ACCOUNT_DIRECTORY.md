# M1-CORE-06A — Danh sách tài khoản nhân viên (read-only)

Baseline `main`: `6a20e3a1c74529a2df900075cc085078c4fd0eef` (CORE-05 merged via PR #69).

## Scope

This is a real JDBC account-directory screen, not an account CRUD writer.
`System -> Accounts` opens `AccountPanel` through `FeatureBootstrap` / `FeatureRegistry`.
It searches and filters rows in `dbo.TaiKhoan`, LEFT JOIN `dbo.NhanVien`, with `TOP (201)`
and a maximum of 200 visible results. It never selects `matKhauHash`, nor changes SQL data.
The query runs in `SwingWorker`, outside the Swing EDT.

Required role permission: `ACCOUNT_MANAGE` at the Service boundary; the current policy
allows `CLINIC_MANAGER` and `SYSTEM_ADMIN`. Login/Navigation also apply independent guards.
An inactive session cannot start a new search or receive previously queued results.

## Existing SQL permissions

`DevSetup.java` already grants SELECT on `dbo.TaiKhoan` and `dbo.NhanVien` to the isolated
`n08dev_XXXXXXXX` SQL login. **No additional SQL grant is needed for this read-only screen.**
The login has only column-scoped UPDATE rights for authentication status fields; it does
NOT have `INSERT` into `TaiKhoan`, UPDATE `vaiTro`, or UPDATE `matKhauHash`.
Do not attempt CREATE/LOCK/RESET/ROLE updates in CORE-06A or run as `sa`.

## Usage and boundaries

- Launch with `.n08-dev.local.properties` in the worktree and `scripts/run.ps1`.
- Login with an authorized account; open **Hệ thống → Tài khoản**.
- Search account ID, username, display name or staff name. Filter by role and status.
- Select a row to view linked employee ID / forced password-change flag.
- Empty results mean the database has no matching rows, not fabricated records.
- Failed data retrieval shows a generic error (no JDBC statement, hash, or password leaks).
- SQL app-account privileges are shared by Swing users: Java RBAC alone is not an SQL
  security boundary. Avoid connecting untrusted clients to the database.

## Intentionally deferred to CORE-06B

Account creation, employee association, role changes, lock/unlock, password reset,
audit trail and explicit SQL-grant migration. Before those actions: scope approved SQL
grants, transaction and constraint rules, no self-escalation/last-admin lockout,
BCrypt password handling, audit and active-session invalidation.

## Local execution

Run in a CLEAN dedicated worktree on the baseline main commit:

```powershell
.\mvnw.cmd clean compile
.\scripts\run.ps1 -Smoke
.\scripts\run.ps1
```

Use `git diff --check` and inspect changed files before committing. This patch changes
only `FeatureBootstrap.java` plus four new account Java classes and this document.
Standalone JDK 21 compile with dependency stubs is not a full Maven/SQL integration test.
