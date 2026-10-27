# R3-A3 - Swing Login UI (sandbox-only integration)

- The default GUI entry in App.java is switched from MainFrame to LoginFrame. Existing
  --smoke, --check-db and --inspect-db commands keep their prior behavior.
- LoginFrame authenticates through JdbcAuthService outside the Swing EDT.
- Failed login returns a generic message; JDBC errors are not shown in the UI.
- MainFrame is opened only when authentication succeeds and password-change is not required.
- Forced password change is a blocker; password change is NOT implemented yet.
- SandboxLoginConnection checks `SELECT DB_NAME()` for exactly N08AuthIT_[8 hex digits]
  BEFORE any writable authentication transaction; N08Dental is refused.
- No source file except App.java is overwritten; original is archived in docs/legacy/r3-a3.
- No database schema operations, bootstrap seeds, or production test accounts are created.
- UI is not a complete role authorization layer and MainFrame remains a placeholder.
- Password API currently accepts immutable Java String; it cannot zero all memory copies.

## Verify

```powershell
.\mvnw.cmd clean compile
.\scripts\test-auth-policy.ps1
.\scripts\test-auth-transaction.ps1
.\scripts\test-login-gate.ps1
.\scripts\run.ps1 -Smoke
```

## Run isolated login UI (real writes to SYNTHETIC test account only)

```powershell
.\scripts\run-login-isolated.ps1 -Database N08AuthIT_485d35bd
```

For the previously created R3-A2C synthetic sandbox, demo login:
- username: `it.success`
- password: `R3A2C-SYNTHETIC-ONLY-CORRECT!`

These credentials have no relationship to the real ADMIN in N08Dental. SQL Server
login credentials are prompted separately and should never be committed to Git.

## Pending
- Manual Windows UI acceptance, focus and Vietnamese glyphs
- Concurrent login and lock contention tests
- Production least-privilege SQL account
- Password reset/change workflow and per-role authorization enforcement
- No production N08Dental login authorized by this stage
