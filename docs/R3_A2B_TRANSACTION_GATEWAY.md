# N08 R3-A2B - Transactional Authentication Gateway Candidate

Status: IMPLEMENTATION CANDIDATE (NOT LIVE AUTH ACCEPTANCE).

Scope:
- Add `JdbcAuthService` and `AuthDecision` only; no UI wiring or application startup changes.
- Existing `LoginPolicy` and R3-A1 read-only DAOs are reused, unmodified.
- `JdbcAuthService` is a WRITE-CAPABLE class if explicitly invoked with a real writable JDBC connection. **The R3-A2B scripts do NOT instantiate it with real SQL Server or perform DB operations.**
- Preserve existing tables, ADMIN account, audit history, legacy sessions, Flyway history, Docker containers and volumes.

Transaction contract:
1. Reject null/blank username or password before opening a connection.
2. Fresh JDBC connection; `READ_COMMITTED`, `autoCommit=false`.
3. Acquire an update/serializable-hold lock on `TaiKhoan` by indexed username.
4. Load sensitive account snapshot with existing `AccountDao` in the same transaction.
5. If employeeId exists, lock employee row, then load with `EmployeeDao`.
6. Evaluate pure `LoginPolicy` with UTC from injected `Clock`.
7. If stateChanged: update only 9 account lifecycle fields using typed `PreparedStatement`, assert exactly one row updated; commit failure and success states alike.
8. If no stateChanged: commit read-only transaction without writing.
9. On SQLException/RuntimeException after starting transaction: rollback, then propagate error.
10. Return `AuthDecision` without password/hash. Never log raw credentials or IDs.

Isolation limitations:
- 34 synthetic checks use JDK dynamic JDBC proxies; tested on fixed synthetic account only.
- No real SQL Server write, contention/deadlock, database constraint or transaction rollback test yet.
- Employee-lock path not covered by proxy tests; no real employees exist in current DB.
- Never test multiple incorrect credentials against the current ADMIN account.
- No admin bypass or automatic unlocking of `ADMIN_ACTION` lock.
- Error messages for SQL exceptions must be sanitized in later UI integration.
- Clock is UTC; SQL DATETIME2(3) values are represented as LocalDateTime via Timestamp; verify this contract with real SQL Server in a dedicated test database.

Next gate: dedicated disposable integration database under an isolated Docker project/volume, with explicit backup verification before any write tests. Do not use `N08Dental`, sa credentials, or the existing n08-dental volume for live mutation tests. Explicit human authorization required before UI wiring/live writes.

Commands:
```
.\mvnw.cmd clean compile
.\scripts\test-auth-policy.ps1
.\scripts\test-auth-transaction.ps1
.\scripts\run.ps1 -Smoke
```

Acceptance for this stage: compile success + policy 19 PASS + synthetic transaction 34 PASS + smoke PASS. Do not claim real DB integration PASS.
