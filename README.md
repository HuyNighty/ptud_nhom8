# PTUD Nhom 8 â€” Quan ly nha khoa (Java SE + Swing + JDBC)

Desktop application foundation merged into main via PR #57. No Spring Boot, Hibernate/JPA or Flyway.

## Requirements
JDK 21+, PowerShell, and network access on first Maven Wrapper/dependency download.

## Start UI AFTER one-time SQL setup
Open PowerShell in repository root and run:

```powershell
.\scripts\run.ps1
```

Compile/smoke test without GUI and without DB:

```powershell
.\scripts\run.ps1 -Smoke
```

Without Maven (JDK-only smoke test; interactive login requires Maven JDBC + BCrypt libraries):

```powershell
.\scripts\run-without-maven.ps1 -Smoke
.\scripts\run-without-maven.ps1
```

## Optional JDBC connection test
Set environment variables ONLY on your computer (never commit real secrets):

```powershell
$env:N08_DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=N08_NhaKhoa;encrypt=true;trustServerCertificate=true'
$env:N08_DB_USER = 'n08_app'
$secure = Read-Host 'SQL Server password' -AsSecureString
$env:N08_DB_PASSWORD = [System.Net.NetworkCredential]::new('n08_app', $secure).Password
.\scripts\run.ps1 -CheckDb
```

`trustServerCertificate=true` is ONLY an expedient local development setting with a self-signed certificate. Use certificate validation for real deployments. Supply credentials via environment or local secret storage; avoid putting passwords into the URL.

Running without Maven requires a compatible `mssql-jdbc*.jar` placed manually under the untracked `lib/` folder for DB access. Offline UI needs no external JAR. The JDK alone cannot supply the SQL Server JDBC driver.

For one-time per-developer SQL bootstrap and login, see `docs/TEAM_LOCAL_ONBOARDING.md`. The feature pages are still placeholders; no real patient/business data support exists yet.


## Team first-run setup (SQL Server native OR Docker)

Run `scripts/setup-local.ps1 -Port 1433 -SqlAdminUser sa` once after starting local SQL Server. For Docker with host port 14333, use `-Port 14333`. Then IntelliJ Run `com.quanlynhakhoa.App` and sign in with username `dev.admin` and your own setup password. SQL administrator credentials are never stored in Git. See `docs/TEAM_LOCAL_ONBOARDING.md`.
