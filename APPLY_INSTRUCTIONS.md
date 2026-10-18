# N08 clinical-examination — M1 service catalog patch

## Scope

This patch adds the independent `DichVu` catalog under `features/examination`, a new Flyway V5 migration, request/response DTOs, a secured feature Service, a Swing `ServicePanel`, unit tests, and an opt-in SQL Server integration test.

It intentionally does **not** modify `MainFrame`, `Permission`, `RolePermissionPolicy`, `SecurityConfig`, `GlobalExceptionHandler`, V1–V4, Maven dependencies, or SQL Server volumes.

## Important security boundary

`DichVuAccessPolicy` checks authorities from Spring Security's authenticated `SecurityContext`, never a role or account ID from a form or HTTP payload. It fails closed unless the trusted context contains `SERVICE_READ` or `SERVICE_MANAGE`.

The current `main` does not yet map declared roles through the centralized `AuthorizationService` / `RolePermissionPolicy` to these authorities and does not have the complete Swing login flow. Therefore this patch alone does not make the panel usable by end users. This feature-local guard is intentionally fail-closed; do not bypass it to demo CRUD. A separately approved change to the shared permission enum/policy is required before the panel is wired into the main navigation or a REST controller is exposed.

The DTOs define the input/output contract that can be used by a later REST controller. No controller is added in this slice because the permission mapping and login/token issuance are not complete; exposing an endpoint now would give a misleading impression of an end-to-end authenticated workflow.

## Apply on a fresh feature branch

From the repository root after updating `main`:

```powershell
git switch main
git pull origin main
git switch -c feature/clinical-examination
git apply .\n08-clinical-examination.patch
```

Place the downloaded patch file in the repository root before running `git apply`.

## Verification

1. Compile and run unit tests (JDK 25, Maven Wrapper, environment configured as the main project requires):

   ```powershell
   .\mvnw.cmd test
   ```

2. The SQL Server test is intentionally named `*IT` and is not selected by Surefire's default patterns. With SQL Server 2022 running and the required `DB_SA_PASSWORD` / `N08_JWT_SIGNING_KEY_BASE64` environment configured, run it explicitly:

   ```powershell
   .\mvnw.cmd '-Dtest=DichVuServiceSqlServerIT' test
   ```

3. Review schema migration with the actual database, then inspect `git diff --check`.

## Known incomplete integration

- No REST controller has been added yet.
- `ServicePanel` is a Spring component but remains unmounted from `MainFrame`, as agreed in option A.
- Service catalog permissions need a separate change to the shared permission enum/policy; access is denied until trusted authorities are issued.
- SQL Server integration and desktop end-to-end tests have **not** been run in this environment. Do not report them as PASS.
