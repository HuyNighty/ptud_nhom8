# M1-CORE-04 — Role & Permission integration

Baseline: GitHub `main` SHA `3df23d2c9c703824cee951f4f621b6a06d78b16b` (PR #67 merged).

## Evidence and scope

This is a proposed application-level authorization mapping, derived from the functional-role matrix (FR01–FR08) in `docs/tuan_cuoi/9_REPORT_ApplicationDevelopment.docx`. That report defines five operational roles; `SYSTEM_ADMIN` is an existing technical role in the SQL/application and retains its existing all-permissions behavior in this change.

No SQL schema/migration/grant changes; no new real accounts or patient data; no implementation of business actions. The file changes are limited to `Permission.java`, `RolePermissionPolicy.java`, four legacy self-tests, and this document. There are no REST/Spring/JPA dependencies.

| Role | App permissions | Intended operation |
| --- | --- | --- |
| RECEPTIONIST | PATIENT_READ, PATIENT_CREATE, PATIENT_UPDATE, PATIENT_CHECK_IN, APPOINTMENT_READ, APPOINTMENT_MANAGE, BILLING_READ, BILLING_MANAGE, INVOICE_CANCEL_REQUEST, SCHEDULE_SELF_READ | Patient registration, reception, appointments, invoice/payment; request cancellation, not approve |
| DOCTOR | PATIENT_READ, MEDICAL_RECORD_READ, MEDICAL_RECORD_UPDATE, REEXAM_SCHEDULE, SUPPLY_REQUEST, SCHEDULE_SELF_READ | Examination/treatment, own re-exam scheduling, supply request |
| NURSE | SUPPLY_REQUEST, EMERGENCY_KIT_CHECK, SCHEDULE_SELF_READ | Supply request, emergency-kit check, own shifts |
| CLINIC_MANAGER | BILLING_READ, WORKFORCE_READ, WORKFORCE_MANAGE, ACCOUNT_MANAGE, ROLE_MANAGE, AUDIT_READ, SERVICE_CATALOG_MANAGE, INVOICE_CANCEL_APPROVE, SCHEDULE_SELF_READ | Billing read and approval, staff, roles/accounts, catalog prices, audit |
| INVENTORY_MANAGER | INVENTORY_READ, INVENTORY_MANAGE, SCHEDULE_SELF_READ | Inventory catalog, in/out, lot, supplier, inspection |
| SYSTEM_ADMIN | All (existing behavior preserved) | Technical bootstrap and administration; must not be used as a substitute for testing operational roles |

## Important implementation boundaries

1. `BILLING_MANAGE` allows receptionist invoice creation/payment **only**. Cancellation **approval** MUST additionally require `INVOICE_CANCEL_APPROVE` and verify actor separation, invoice state and amount in a transaction. `INVOICE_CANCEL_REQUEST` does not approve.
2. `INVENTORY_MANAGE` is only for stock/warehouse actions. Supply-requesting clinicians and emergency kit checking nurses MUST use `SUPPLY_REQUEST` and `EMERGENCY_KIT_CHECK`; do not broaden access to full inventory.
3. `WORKFORCE_READ` means managing/viewing staff schedules. All staff have `SCHEDULE_SELF_READ`, but DAO/service must filter by authenticated employee identity. Do not expose full workforce records to them.
4. `REEXAM_SCHEDULE` is distinct from broad appointment management. Doctor-created re-exam bookings must be restricted to appropriate clinical cases.
5. `SERVICE_CATALOG_MANAGE` is separate from `BILLING_MANAGE`: manager sets prices; receptionist issues invoices at approved prices.
6. Only the business Service decides whether an operation is permitted and validates row-level ownership, workflow status, and transaction constraints. Showing a Swing menu does **not** authorize data access.
7. Newly added fine-grained permissions have no dedicated menu pages yet. In particular, `NURSE` still only sees HOME until approved module destinations and UI panels are built. This is expected, not an authorization bug.
8. The shared JDBC login has its own limited SQL GRANTs. These Java application permissions **do not grant database privileges**; new M2 SQL actions require separately reviewed local-dev grants.
9. The original R4 policy self-tests asserted three roles had no permissions. Their expected matrices must be updated in the same commit; they are not removed.

## Deliverable and validation

Only on a fresh feature worktree branched from the exact baseline:

```powershell
.\mvnw.cmd clean test-compile
.\scripts\test-r4-role-session.ps1
.\scripts\test-r4-main-navigation.ps1
.\scripts\test-r4-final-shell.ps1
.\scripts\run.ps1 -Smoke
```

The minimum required for an initial code check is `clean test-compile` plus the role/session test, followed by launching the app. Do not merge if baseline or permission regression checks fail. Tests are synthetic, no SQL or patient data.

**Open decisions (future):** separate menu destinations for Service Catalog, Examination, own shift calendar, supply requests, and emergency-kit checks; exact row-level policy; test users with these roles. Do not imply these screens are currently implemented.
