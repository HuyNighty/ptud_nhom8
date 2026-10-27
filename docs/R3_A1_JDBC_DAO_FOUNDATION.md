# R3-A1 - Read-only Account/Employee DAO foundation

Status: PROPOSED FOR LOCAL REVIEW. R3-A0 aggregates were provided by the PO.

- SQL Server database: N08Dental; existing dbo.TaiKhoan and dbo.NhanVien.
- One SYSTEM_ADMIN/HOAT_DONG account, BCrypt $2a; zero employees.
- Preserve all six existing SQL tables and all legacy session rows.
- No Spring Boot, JPA, Hibernate, Flyway, JWT, DDL or DML in this step.

Contents:
- model: Role, AccountStatus, EmployeeStatus, LockSource, Account, Employee.
- dao: AccountDao.findByUsername, EmployeeDao.findById.
- Uses PreparedStatement placeholders. Caller owns JDBC Connection.
- Account record contains a sensitive hash; its toString deliberately REDACTS it.
- No database connection attempted by the bootstrap script.
- Do not invoke the DAO from the Swing login until R3-A2 is reviewed and tested.

Acceptance:
1. Run .\mvnw.cmd clean compile; expect BUILD SUCCESS.
2. Run .\scripts\run.ps1 -Smoke; expect PASS.
3. Open the GUI with .\scripts\run.ps1; previous UI must remain unchanged.
4. git status --short; only the isolated worktree contains changes.
5. Inspect the added files before commit; no merge/push in this phase.

R3-A2 design gates:
- Must verify BCrypt $2a with a compatible BCrypt verifier (not plaintext).
- Require transactional atomic lock/write on account row: SELECT ... WITH
  (UPDLOCK, ROWLOCK) in an explicit JDBC transaction + save state.
- Failed-attempt updates must COMMIT even on rejected password.
- Handle expired FAILED_LOGIN locks without unlocking ADMIN_ACTION locks.
- Existing SYSTEM_ADMIN with null employeeId is allowed, other roles are not.
- Desktop session in memory; no JWT or automatic session/token deletion.
- Never log passwords, hashes or full Account records.
- Use limited-permission database login for the real application, not sa.
- Keep all database changes pending until backup + explicit reviewed scope.
