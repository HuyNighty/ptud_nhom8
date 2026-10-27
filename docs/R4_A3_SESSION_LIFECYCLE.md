# R4-A3 — Desktop Session Lifecycle Acceptance

## Scope

This stage adds a pure-Java, headless session lifecycle test. It does NOT edit existing main Java sources, create SQL sessions, revoke database sessions, mutate accounts, change Docker, commit, or merge Git history.

## Tests

- Only an eligible authenticated result creates a DesktopSession.
- No anonymous/no-session access.
- Roles are not broadened; existing deny-by-default permissions are preserved.
- On logout: `isActive()` becomes false, all navigation is denied, and service authorization is denied.
- Repeated/logout-concurrent calls are safe and never reactivate a session.
- Logging in again creates a distinct DesktopSession; the old one stays inactive.
- The public MainFrame constructor requires `(DesktopSession, Runnable)`; no public no-arg constructor.
- No Swing window, JDBC request, hash, credential, or live account is used by the synthetic test.

Run in the worktree root:

```powershell
.\mvnw.cmd clean compile
.\scripts\test-r4-session-lifecycle.ps1
.\scripts\test-r4-role-session.ps1
.\scripts\test-r4-main-navigation.ps1
.\scripts\test-login-gate.ps1
.\scripts\run.ps1 -Smoke
```

## Manual UI verification (isolated DB only)

After synthetic tests PASS, run the existing `run-login-isolated.ps1` with the **isolated** `N08AuthIT_485d35bd` database. Check (a) logout returns to LoginFrame and requires a new login, (b) closing MainFrame with window X closes the app without leaving a usable authenticated window. Use the existing synthetic account; never brute-force login or use the production N08Dental database for tests.

## Limitations / out of scope

Java Swing screenshot/manual tests are separate from the headless suite. Current application logout is **local in-memory invalidation** and does not revoke `dbo.PhienDangNhap` in SQL Server. Real inactivity expiration, automatic account disable/role changes during an already-active session, crash recovery, database privilege minimization, and multi-process invalidation are not established by this stage.

No business modules are connected by this stage; service-layer authorization must still be enforced when implementing any real patient, appointment, billing, or inventory data operations.