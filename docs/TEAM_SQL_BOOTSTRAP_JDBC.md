# N08 - SQL Server schema bootstrap for Java 21 + JDBC

The four files under `sql/bootstrap/` are **unmodified legacy Flyway migration SQL**, copied from Git baseline `2766f26`.
They are SQL Server schema reference files, **not an automated migration or account provisioning tool**.
Spring Boot/Flyway is absent from the new desktop runtime.

## Isolated development database only

1. Create a dedicated **empty, disposable** development database on SQL Server 2022. Do not use `N08Dental` or any real data.
2. In SSMS, explicitly select the intended empty database and verify it has no `NhanVien`, `TaiKhoan`, `PhienDangNhap`, `RefreshToken` or `NhatKyThaoTac` tables.
3. Execute V1, V2, V3, V4 sequentially, in the same database:
   - `sql/bootstrap/V1__create_workforce_identity.sql`
   - `sql/bootstrap/V2__create_auth_sessions.sql`
   - `sql/bootstrap/V3__harden_account_lock_constraints.sql`
   - `sql/bootstrap/V4__create_audit_log.sql`
4. These statements are **not idempotent**. Never rerun them on an initialized database. They do not create or populate Flyway schema history.

## Still not ready for a fresh clone login demo

- No database creation command, test account provisioner, or credential setup is included. Provisioning requires a separately reviewed dev-only BCrypt workflow.
- `scripts/run-login-isolated.ps1` still restricts access to a local `N08AuthIT_[8 hex digits]` sandbox database. It is not a universal team login launcher.
- Never commit `.env`, SQL passwords, synthetic account secrets, or patient data. Do not run SQL against `N08Dental` without a separate migration plan.
- Feature schema/foreign keys need owner proposals and review before application.
- UI and synthetic security tests are not proof of real feature-level database authorization.

## Portable smoke tests

From a checkout of this Java 21 Maven project on Windows:

```powershell
.\mvnw.cmd clean compile
.\scripts\test-r4-final-shell.ps1
.\scripts\test-r4-session-lifecycle.ps1
.\scripts\test-r4-role-session.ps1
.\scripts\test-r4-main-navigation.ps1
.\scripts\test-login-gate.ps1
.\scripts\run.ps1 -Smoke
```

The above test launchers no longer require a particular directory or branch name.
The installation patch and SQL database protections remain guarded separately.
