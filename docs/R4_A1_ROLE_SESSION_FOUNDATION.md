# R4-A1 - Desktop Session + Role and Navigation Policy (additive only)

This step creates a pure Java policy foundation. It does NOT modify LoginFrame,
MainFrame, App, pom.xml, database, Git history, Docker or runtime launch scripts.

## Basis and restrictions
- The six SQL-constrained roles come from `com.quanlynhakhoa.model.Role`.
- The 16 permissions and grants are a conservative framework-free port of the old
  `RolePermissionPolicy`: RECEPTIONIST = patient read/create/update; DOCTOR =
  patient read, medical record read/update; SYSTEM_ADMIN = all; NURSE,
  CLINIC_MANAGER and INVENTORY_MANAGER have no grants pending team approval.
- Even receptionists currently lack APPOINTMENT_READ in the *old* policy;
  do not invent a permission during migration.
- HOME is a non-sensitive placeholder accessible only with a live local session.
- Navigation filtering is NOT a security boundary. Future service operations
  MUST call AuthorizationService.require(session, permission).
- DesktopSession stores only accountId, role, start time and active state;
  it does NOT create/revoke SQL PhienDangNhap or refresh tokens.
- Closing a local session prevents further access through services that check it.
  This alone does not prevent a malicious in-process plugin or stale DB work.
- UI wiring, logout transitions and role matrix approval belong to R4-A2/A3.

## Acceptance commands (run from ptud_nhom8-java)

```powershell
.\mvnw.cmd clean compile
.\scripts\test-r4-role-session.ps1
.\scripts\test-auth-policy.ps1
.\scripts\test-auth-transaction.ps1
.\scripts\test-login-gate.ps1
.\scripts\run.ps1 -Smoke
```

All R4-A1 synthetic checks are local only. Do not use production N08Dental.

## Proposed next step
R4-A2: after confirming this policy and deciding which blank-role permissions
should be approved, connect `DesktopSession.fromSuccessfulLogin(decision)` in
LoginFrame, pass the session into a new MainFrame, and use NavigationPolicy to
render role-filtered module placeholders. No DAO/data schema changes.
