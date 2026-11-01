# M1-CORE-05 — Operational navigation destinations

**Baseline:** `main` = `da9f809bfe55143d2b3651de364d199c24a246f2` (CORE-04 merged via PR #68).

**Runtime:** Java 21 Swing/JDBC SQL Server. No Spring, no schema migration, no new role grants.

## Dedicated module routes

| Route | Opening permission | Visible to | Business owner | Menu |
|---|---|---|---|---|
| `EXAMINATIONS` | `MEDICAL_RECORD_READ` | Doctor, SYSTEM_ADMIN | Thịnh | Khám & điều trị → Phiếu khám |
| `SERVICE_CATALOG` | `SERVICE_CATALOG_MANAGE` | Clinic manager, SYSTEM_ADMIN | Khang / manager business responsibility | Hóa đơn → Bảng giá dịch vụ |
| `SUPPLY_REQUESTS` | `SUPPLY_REQUEST` | Doctor, nurse, SYSTEM_ADMIN | Thịnh + Đăng integration | Kho vật tư → Yêu cầu cấp phát |
| `MY_SCHEDULE` | `SCHEDULE_SELF_READ` | Every authenticated role | Đăng (employee/schedule module) | Nhân viên → Lịch làm việc của tôi |
| `EMERGENCY_KIT` | `EMERGENCY_KIT_CHECK` | Nurse, SYSTEM_ADMIN | Đăng / Y tá business user | Kho vật tư → Kiểm tra hộp cấp cứu |

The view gets a new `Module` destination, **not** a new permission. `RolePermissionPolicy` from CORE-04 is unchanged. `EXAMINATIONS` uses existing `MEDICAL_RECORD_READ` only to open the screen; modifying examination data requires the appropriate service authorization. `SERVICE_CATALOG` is intentionally a management route; general service price lookup for other roles is a separate design decision.

No feature implementations are registered by this change. `FeatureBootstrap` remains empty. Each route shows the existing honest placeholder until its real panel is delivered and reviewed. There is no demo patient record, inventory entry, payment, or automatic SQL query caused by the new routes. `MainFrame` keeps the existing constructor and lazy `FeatureRegistry` behavior.

## Later integration examples

Core owner adds only the actual delivered module to `FeatureBootstrap.createRegistry()`:

```java
return FeatureRegistry.builder()
        .register(Module.EXAMINATIONS, session -> new ExaminationPanel(session, TeamDevConnection::open))
        .register(Module.SERVICE_CATALOG, session -> new ServiceCataloguePanel(session, TeamDevConnection::open))
        .build();
```

The names above are **illustrations, not existing source classes**; do not copy them into live Java until their feature PRs provide them. The UI navigation permission is not a substitute for a Service-level `AuthorizationService.require` check.

For **MY_SCHEDULE**, restrict SQL reads to the current employee linked to the authenticated account, not all staff records; the local session alone does not yet provide that mapping. Do not derive employee identity from a user-entered ID. For SUPPLY_REQUESTS and EMERGENCY_KIT, provide separate Service permission checks and audit events in the owning feature PR.

## Minimal acceptance

```powershell
.\mvnw.cmd clean test-compile
.\scripts\test-r4-main-navigation.ps1
.\scripts\test-r4-role-session.ps1
.\scripts\run.ps1 -Smoke
.\scripts\run.ps1
```

Sign in as the existing development administrator. Five new menu routes should open accurately labeled placeholders; existing Dashboard and logout should remain functional. No SQL writes should occur. Tests for each role will still be synthetic until the team has legitimate test accounts.

**Scope:** `Module.java`, `MainFrame.java`, three existing navigation regression self-tests, and this documentation. No production feature panel is claimed to exist.
