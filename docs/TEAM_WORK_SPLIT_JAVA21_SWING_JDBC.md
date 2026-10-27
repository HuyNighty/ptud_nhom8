# N08 - TEAM HANDOFF: Java 21 Swing + JDBC + SQL Server

- **Date:** 27/10/2026
- **Repository:** https://github.com/HuyNighty/ptud_nhom8
- **Skeleton branch (local, before publication):** `refactor/java-swing-jdbc`
- **Recorded base:** `2766f26d35b6380a9c7ce627060871e8732be662`

## Decision and current status

This is a **team skeleton**, not a completed dental management application.
R1-R4 have delivered Java 21/Maven bootstrapping, JDBC connectivity,
sandbox login, BCrypt authentication policy, transaction integration checks,
Swing MainFrame navigation, local sessions and role/permission guard foundations.

Proof reported from the Windows worktree:
- Maven `clean compile`: PASS
- R4-A4 authorization and shell: 1,114 synthetic checks PASS
- R4-A3 local session lifecycle: 567 synthetic checks PASS
- R4-A1 role/session/navigation: 457 synthetic checks PASS
- R4-A2 navigation: 67 synthetic checks PASS
- R3-A3 login gate: 9 synthetic checks PASS
- Prior isolated SQL integration on `N08AuthIT_485d35bd`: 87 checks PASS
- Manual: sandbox login, switch panels, logout, close/restart: PASS

**Limitations:** Feature screens are placeholders; no patient,
appointments, clinical, billing or inventory DB workflows yet. The SQL
integration tests were on isolated test database(s). The live `N08Dental`
database has not been certified for the desktop app. MySQL, REST, JWT,
JPA, Hibernate, Flyway runtime and Spring Boot are not part of the new
application. Historical SQL migration files are reference-only.

## Ownership: recovered from the internal draft dated 17/10/2026

This is **proposed ownership**, to be confirmed by the five members.

| Member | Owns | First vertical slice |
|---|---|---|
| Nguyễn Quang Huy | Core, auth, desktop shell, role permission rules, repository integration | Stable skeleton + integration contracts + reviews |
| Nguyễn Hải Đăng | Patient, appointment, reception | Patient search/create/view/update; `PatientPanel` + Service + DAO |
| Đỗ Quốc Thịnh | Service catalogue, examination, treatment | Service catalogue + `ServicePanel`, then examination contract |
| Nguyễn Văn Trí | Medicines, supplies, inventory | Inventory catalogue + `InventoryPanel`, then lot/stock |
| Trần Phúc Khang | Billing, payment, receivables | Billing calculation/validation unit tests + clearly labeled mock panel; later SQL when examination contract is fixed |

Do NOT equate one member to one Role. Roles cut across features.
Admin reuses the same feature services/panels; there is no separate copy of each feature for Admin.

## Java desktop architecture contract

```
Swing JPanel/Dialog (UI)
    -> Feature Service (validation, authorization, transaction boundary)
    -> Feature DAO (PreparedStatement; JDBC; no business logic)
    -> SQL Server
```

One JVM, no local HTTP call, no JPA/Spring/JWT/Flyway runtime.
Shared `DesktopSession` is created after authentication and must not be
constructed from UI-supplied role claims. All privileged service operations
must enforce permission server-side/in-process at the Service boundary.
Slow I/O must run off the Swing EDT via SwingWorker or equivalent.

Suggested organization for *new* features (adapt to actual source in branch):

```
com.quanlynhakhoa/
  patient/{model,dao,service,view}
  appointment/{model,dao,service,view}
  clinical/{model,dao,service,view}
  inventory/{model,dao,service,view}
  billing/{model,dao,service,view}
  auth/          # Huy-owned
  security/      # Huy-owned
  navigation/    # Huy-owned
  config/        # Huy-owned
  view/          # shared shell, Huy-owned
```

Exact API names, DTOs and SQL columns must be proposed and reviewed before implementation;
this layout is a convention, not a claim that folders already exist.

## Git collaboration protocol

1. Publish the local refactor to `refactor/java-swing-jdbc` with an explicit
   staged-file review. No `git reset --hard`, `git clean -fd`, `git push --force`,
   or automatic merge.
2. Open a **draft** PR to `main` describing this as a **breaking architectural
   replacement** and listing missing live-DB/feature/CI coverage.
3. After review and intentional merge, members create **fresh branches from
   the new main**; old branches targeting Spring Boot are not automatically
   compatible with the new Java 21 skeleton; do not reuse those branches.
4. Suggested new branches:
   - `feat/jdbc-patient-appointment` — Đăng
   - `feat/jdbc-clinical-examination` — Thịnh
   - `feat/jdbc-inventory` — Trí
   - `feat/jdbc-billing-payment` — Khang
   - `chore/jdbc-core-integration` — Huy (only as needed)
5. Each PR must be confined to the owner feature. Shared security,
   navigation, Maven, schema contracts and migrations require Huy's review.
6. Never commit `.env`, passwords, credential dumps, `target/`,
   database backups, or personal logs.

## Schema and dependency rules

- Centralize approval of shared SQL schema and foreign keys.
- Test CREATE/INSERT/UPDATE on a fresh isolated SQL Server test DB,
  **never** on `N08Dental` by default.
- Patient (Đăng) -> reception/appointment (Đăng) -> examination (Thịnh)
  -> billing (Khang) is the main end-to-end path.
- Inventory (Trí) progresses in parallel and integrates with prescribing
  once the data contracts are approved.
- Billing does not manufacture examination facts or service prices.
- No silent hardcoded Admin, password reset or bypassing clinical responsibility.
- Nurse, clinic-manager and inventory-manager grants must be reviewed;
  default deny remains until defined.

## First delivery (M0/M1)

| Owner | M0 agreement | M1 evidence |
|---|---|---|
| Huy | Navigation panel registration contract, database config, role matrix, CI plan | Build/run/login shell + reviewed integration PR |
| Đăng | Patient fields, validation, duplicate alert, data migrations reviewed | Patient create/search/read/update Swing -> Service -> JDBC -> test DB |
| Thịnh | Service model, proposed price/visit contract with Đăng & Khang | CRUD service catalogue + Swing + test DB |
| Trí | Stock item model, future lot/FK contract | Item catalogue + Swing + test DB |
| Khang | Invoice input, money formulas, examination dependencies | Independent calculation tests + clearly marked mock Swing UI |

## Definition of Done for each feature PR

- Verified branch base and diff scope; no shared file changes without agreement.
- Maven compile and applicable tests PASS, with command output.
- Validation and `AuthorizationService` checks in Service, not only hidden menu.
- JDBC prepared statements; multi-write workflow uses transaction.
- No real patient data committed; SQL integration uses isolated test DB.
- Screenshot of Swing flow + evidence of saved/read-back rows where applicable.
- Explicit report of still-untested scenarios, schema conflicts and dependencies.
- Reviewed and merged intentionally; no owner merges own PR without review.

## Historical sources

- Internal draft: `N08_Phan_cong_BE_Swing_Prompt_AI_2026-10-17.md`,
  17/10/2026, **draft / assignments subject to confirmation**.
- GitHub documents on base commit:
  `docs/THIET_KE_DU_LIEU.md`, `docs/KIEM_THU.md`.
  Use for business rules; the old Spring Boot architectural references are obsolete.
