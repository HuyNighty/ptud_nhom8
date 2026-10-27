# R3-A2 - Pure Login Policy + Synthetic Test

Status: NOT ACCEPTED until PowerShell test results reviewed.

- Preserves existing Java Swing/JDBC layers; does not modify LoginFrame or connect to SQL.
- Adds a `jbcrypt:0.4` Maven dependency to verify existing BCrypt `$2a` hashes.
- Defines `PasswordVerifier`, `BCryptPasswordVerifier`, `LoginPolicy`, `LoginPlan`.
- No transactions, row locks, login sessions, DAO update methods, or write operations in this phase.
- No `AuthService` facade yet: this is a deterministic policy core for a later transactional service.
- Lock after 5 failed attempts during a 15-minute window; expired *automatic* lock may reset.
- An admin-action lock and disabled account never auto-unlock.
- SYSTEM_ADMIN can have no employee; other roles require a matching active employee.
- Successful login produces an instruction to clear failure fields and update last login.
- `forcePasswordChange` is surfaced as a post-login restriction for the later UI layer.
- All failure results remain generic to UI callers; do not expose whether username exists.
- Desktop app must never act on `LoginPlan` before persistence succeeds.

### Commands (worktree root)

```powershell
.\mvnw.cmd clean compile
.\scripts\run.ps1 -Smoke
.\scripts\test-auth-policy.ps1
```

### Future R3-A2B gate

No live account mutation until tests exist for SQL `UPDLOCK` + transaction semantics,
commit-on-rejected-login, rollback-on-error, and database check constraints.
Prior to integration, use a disposable test database and an account without real credentials;
never intentionally fail the sole production/demo ADMIN account five times.
Check that changing `pom.xml` does not break `scripts/run-without-maven.ps1`;
that runner may require a separate jBCrypt JAR when integrating authentication.
