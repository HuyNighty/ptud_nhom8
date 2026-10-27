# R4-A4 - Authorization and final desktop shell acceptance

## Scope
Add one explicit action gate and a headless test harness. This is NOT a
production security certification or implementation of any real dental
business service. All new test accounts are in memory.

## Fail-closed rules
- The old policy remains unchanged: RECEPTIONIST has PATIENT_READ/CREATE/UPDATE;
  DOCTOR has PATIENT_READ and MEDICAL_RECORD_READ/UPDATE; SYSTEM_ADMIN has all.
  NURSE, CLINIC_MANAGER and INVENTORY_MANAGER still have no approved permissions.
- `AuthorizedActionGate` calls `AuthorizationService.require(session, permission)`
  before invoking a service action or query callback.
- Missing permissions, null sessions, logged-out sessions and unapproved grants
  never execute the callback.
- Navigation visibility never grants permission. A future service must check
  permission independently before DAO or external side effects.
- The MainFrame public entry point must require a `DesktopSession` plus logout
  callback; no unauthenticated public no-arg constructor.

## Run
```powershell
.\mvnw.cmd clean compile
.\scripts\test-r4-final-shell.ps1
.\scripts\test-r4-session-lifecycle.ps1
.\scripts\test-r4-role-session.ps1
.\scripts\test-r4-main-navigation.ps1
.\scripts\test-login-gate.ps1
.\scripts\run.ps1 -Smoke
```

## Important limitations
- No dental business DAO/service exists yet for end-to-end service-level
  authorization. Calling the gate is a convention enforced by review and tests
  for each future business operation, not a JVM sandbox.
- Java desktop authentication/authorization by itself cannot enforce arbitrary
  malicious local code; SQL logins must be least privileged in a deployable app.
- Local logout is NOT SQL `PhienDangNhap` revocation, and a live desktop session
  does not dynamically reflect role/account changes in SQL Server.
- Missing network/disconnection scenarios, concurrent real SQL requests, audit,
  user/session expiration and minimal database rights remain out of scope.
- R4-A4 acceptance cannot authorize a production deployment or Git merge.
- Test with isolated `N08AuthIT_...` only; NEVER test destructive scenarios on
  `N08Dental` or the original Spring Boot checkout.
