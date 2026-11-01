# R4-A2 — Authenticated Swing MainFrame shell

## Scope

- The MainFrame constructor now REQUIRES a DesktopSession created by the R3 login decision gate.
- The LoginFrame passes a fresh DesktopSession after a successful auth decision.
- NavigationPolicy filters the sidebar by role. The selected module is checked again on each navigation.
- Logout invalidates the in-memory DesktopSession first, closes MainFrame, then opens LoginFrame.
- Closing MainFrame also invalidates its local DesktopSession.
- R4-A1 permissions remain unchanged (unapproved roles see only Home).
- All business modules are placeholders. No business SQL, DDL, new database or migrations.
- The R3-A3 sandbox-only database guard remains unchanged; N08Dental remains blocked.
- No SQL PhienDangNhap record is created/revoked by logout; local logout is NOT server-side revocation.

## Verification

```powershell
.\mvnw.cmd clean compile
.\scripts\test-r4-role-session.ps1
.\scripts\test-r4-main-navigation.ps1
.\scripts\test-login-gate.ps1
.\scripts\test-auth-policy.ps1
.\scripts\test-auth-transaction.ps1
.\scripts\run.ps1 -Smoke
.\scripts\run-login-isolated.ps1 -Database N08AuthIT_485d35bd
```

## Manual acceptance

1. Login with isolated synthetic account and observe sidebar/header/Home.
2. SYSTEM_ADMIN should see all ten module placeholders; unimplemented actions must not write data.
3. Visit two modules using the sidebar and return Home.
4. Click Logout: main window closes and a new LoginFrame opens.
5. Without logging in again, no authenticated MainFrame remains open.
6. Login again with the same isolated account; session should be new.
7. Do not test on N08Dental; do not use real patient information.

## Known limitations

- Navigation visibility is not server-side/business-service authorization.
- No role matrix expanded beyond the R4-A1 policy.
- No concurrent login, SQL least-privilege, disconnect or per-module business workflow acceptance.
- A local user with code execution in the application process is not in the threat model of this UI-layer check.
