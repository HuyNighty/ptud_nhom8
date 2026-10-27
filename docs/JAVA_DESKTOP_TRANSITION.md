# Java SE desktop transition (isolated branch)

## Preservation
- Git history and remote `main` unchanged until an explicitly reviewed PR is merged.
- Four Flyway SQL files are archived in `sql/legacy` and NEVER run automatically.
- Old Spring-era POM / README have copies under `docs/legacy`.
- Original desktop checkout's uncommitted docs and auth changes are not touched.

## Architecture target
Swing View -> Controller -> Service (validation, authorization, transactions) -> JDBC DAO -> SQL Server.
Entities are plain Java classes/records; no JPA annotations. No Spring application context, REST, JWT, or Flyway.

## Important domain invariants to carry forward
- Employee job title and application account role are separate concepts.
- Verify account status AND employee status during login.
- BCrypt password hashes; login failures/lockout and unlock rules must be preserved.
- Service methods check roles; merely hiding GUI controls is NOT authorization.
- JDBC uses parameterized queries and explicit transaction boundaries for multi-step actions.
- Never log passwords, credentials, or patient private data.
- SQL schema creation and seed scripts are reviewed before they run; no implicit DROP or data loss.

## Current scope (scaffold only)
Only a minimal offline Swing UI, an optional JDBC connection check, and bootstrap scripts are created.
There is NO real login, NO DAO, NO schema, NO actual patient/appointment/invoice workflows yet.

## Acceptance gates before PR
1. `scripts/run.ps1 -Smoke` works with Maven Wrapper.
2. `scripts/run.ps1` opens an offline UI without requiring SQL Server.
3. `scripts/run.ps1 -CheckDb` succeeds only after explicit local SQL config.
4. Audit DB schema, DAO, auth and CRUD separately before shipping functional claims.
5. Keep staged deletions and newly generated files visible for human review.
