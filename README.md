# PTUD Nhom 8 â€” Quan ly nha khoa (Java SE + Swing + JDBC)

Desktop application foundation in a separate refactor worktree. No Spring Boot, Hibernate/JPA or Flyway.

## Requirements
JDK 21+, PowerShell, and network access on first Maven Wrapper/dependency download.

## Start UI without SQL Server
Open PowerShell in repository root and run:

```powershell
.\scripts\run.ps1
```

Compile/smoke test without GUI and without DB:

```powershell
.\scripts\run.ps1 -Smoke
```

Without Maven (JDK only):

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

This scaffold does not create tables, migrate schemas, login, read/write business data, or persist demo UI actions. Read `docs/JAVA_DESKTOP_TRANSITION.md` before implementation.
