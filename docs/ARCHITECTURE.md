# N08 architecture — RF-R2

The application uses Java 25 and Spring Boot 4.1.1. REST and Swing run in
one JVM, sharing the Spring application context. `QuanLyNhaKhoa`, the Swing
launcher, the desktop shell, shared responses/exceptions and infrastructure
configuration retain their existing responsibilities.

## Feature-based layered packages

Business code lives under `com.quanlynhakhoa.features`. Each feature owns its
entities, repositories and services. The implemented main-source packages are:

```text
com.quanlynhakhoa/
  features/
    account/
      entity/Account.java
      enums/AccountStatus.java, LockSource.java
      repository/AccountRepository.java
      service/AccountService.java
    employee/
      entity/Employee.java
      enums/EmployeePosition.java, EmployeeStatus.java
      repository/EmployeeRepository.java
    auth/
      enums/Role.java, Permission.java
      exception/PermissionDeniedException.java
      service/AuthorizationService.java, RolePermissionPolicy.java
  configuration/
    swing/
    web/
  desktop/frame/
  shared/
    exception/
    response/
```

When a feature needs them, use `controller/` for REST endpoints,
`dto/request/` for validated input, `dto/response/` for external results,
and `ui/` for its Swing views. Keep request and response DTOs separate.
Add `service/`, `repository/` and `entity/` only when there is implementation
to place there. Use concrete services; a Service/ServiceImpl pair is unnecessary
unless an actual requirement calls for interchangeable implementations.

The remaining canonical features are `patient`, `appointment`, `examination`,
`billing`, `inventory` and `audit`. Their main-source directories are not
materialized yet. The old empty `modules/*/package-info.java` placeholders
are replaced by this feature map. `workforce` becomes `employee`, `identity`
is split into `account` and `auth`, `scheduling` becomes `appointment`, and
`clinical` becomes `examination` when those features gain implementations.

## Dependencies and boundaries

Use Controller → Service → Repository → JPA Entity. Swing feature UI also
calls the same services through Spring; it does not access repositories or
open a separate application context. Services own business decisions,
authorization checks and transaction boundaries. Repositories own persistence
queries, and entities keep SQL Server field mappings.

`AccountService` preserves the existing read-only account lookups by username
and ID. It returns the mapped `Account` directly to internal callers, replacing
`AccountRepositoryPort`, `AccountJpaAdapter` and `AccountSnapshot`. Those three
classes have no remaining consumers. The repository interfaces retain exactly
their existing read methods and extend Spring Data `Repository`; this refactor
does not add write operations, employee workflows or endpoints. Never serialize
an `Account` directly into a REST response or log its password hash. A future
external account response must use a response DTO containing only safe fields.

`AuthorizationService.require(...)` remains the centralized permission guard.
`RolePermissionPolicy` retains its complete grants, multi-role union, immutable
permission sets, and null/empty-role handling. Preserve these checks when
connecting future REST and Swing entry points. The refactor does not alter
authentication or authorization policy. Login is the next milestone after
Architect approval and is not implemented here.

## Persistence and runtime invariants

`Account` still maps to `dbo.TaiKhoan`; `Employee` still maps to
`dbo.NhanVien`. All field names, column names, lengths, nullability, enum
storage and nationalized annotations are preserved. Explicit JPA entity names
`TaiKhoanJpaEntity` and `NhanVienJpaEntity` preserve the previous logical
mapping names while the Java types become `Account` and `Employee`. Each
table has one mapped entity. The root application package already scans the
new feature packages, so no scan configuration is necessary.

Flyway V1–V4 are unchanged. Docker Compose, application configuration,
Maven configuration and the REST/Swing bootstrap are unchanged. No database
operation is part of RF-R2. `.gitignore` uses `/out/` for root build output,
allowing nested Java packages named `out` to be tracked normally.

## Tests and verification

The existing authorization test moves to `features/auth/service`. SQL schema
tests move to `features/auth/repository` and `features/audit/repository` in
the test source tree. Only their package declarations and imports change;
their assertions and SQL remain unchanged.

RF-R2 verification is limited to:

```powershell
.\mvnw.cmd '-DskipTests' compile
.\mvnw.cmd '-DskipTests' test-compile
git diff --check
```

These commands compile the application and tests without executing Spring
integration tests or connecting to SQL Server. Static source inspection and
byte/hash comparisons verify mapping, policy and migration preservation.
