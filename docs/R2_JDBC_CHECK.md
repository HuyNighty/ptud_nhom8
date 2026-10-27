# R2 - SQL Server JDBC connection + schema metadata check

This step verifies connectivity only. It **does not** create a database, execute
Flyway migrations, INSERT/UPDATE/DELETE rows, or change `sql/legacy`.

## Prerequisites

- SQL Server service running; TCP/IP enabled and an accessible fixed TCP port.
- A database that already exists (inspect its actual name in SSMS).
- Prefer a dedicated SQL login with CONNECT and metadata visibility/read access,
  but **no schema modification permission** for R2.
- JDK 21+, Maven Wrapper, and access to the Microsoft JDBC dependency.

## Commands (Windows PowerShell 5.1)

```powershell
.\mvnw.cmd clean compile
.\scripts\run.ps1 -Smoke
.\scripts\check-db.ps1 -Server localhost -Port 1433 -Database N08_NhaKhoa
.\scripts\check-db.ps1 -Server localhost -Port 1433 -Database N08_NhaKhoa -Inspect
```

The helper prompts for SQL login and a hidden password. The password is not
written to config files, the command line or this repository. Credentials and
URL are process environment variables for the duration of the check only.

Self-signed local SQL Server certificate in a **development environment only**:

```powershell
.\scripts\check-db.ps1 -Server localhost -Port 1433 -Database N08_NhaKhoa -TrustLocalCertificate -Inspect
```

`TrustLocalCertificate` does not validate the SQL Server identity, despite TLS
transport encryption; do not use it for production/remote DBs. Use trusted
server certificates for real deployments.

Optional Windows Authentication uses `-WindowsAuth`, but on Windows it may
require the native JDBC authentication DLL or correctly configured Kerberos.
If it fails, use a dedicated SQL login for the local development database.

For named instances (e.g., SQLEXPRESS), determine the **actual TCP port** in
SQL Server Configuration Manager or SQL Server logs; do not assume 1433.

The schema inspection uses SELECT metadata queries and displays only names
of visible dbo tables; it does not read patient, staff or account rows.
`Connection.setReadOnly(true)` is NOT an enforcement mechanism for this driver.
Enforce least privilege with the SQL login/database permissions instead.

## Expected output

- PASS: JDBC connection: Microsoft SQL Server
- DATABASE: <actual database>
- TABLE: dbo.<name> for visible tables
- PASS/WARN for dbo.NhanVien and dbo.TaiKhoan

A successful connection is **R2 connectivity PASS**, not permission to run
DDL, not confirmation the entire domain schema is complete, and not proof that
Login/CRUD functionality exists.

## Troubleshooting

- Connection refused/timeout: SQL Server service, TCP port, firewall, named
  instance dynamic port or wrong host.
- Login failed: SQL authentication, SQL login/password, disabled login, mixed
  authentication mode or default database permissions.
- Unknown database: check database spelling in SSMS, avoid creating a new
  database until schema review.
- TLS certificate error: trust a valid certificate; for a LOCAL demo only use
  `-TrustLocalCertificate` as an explicit temporary development exception.
- No suitable driver: run `mvnw.cmd dependency:tree`; Maven must download JDBC.

## Preservation

This change is scoped to App.java, two run scripts and NEW diagnostics files.
It does not modify the existing Database.java, MainFrame.java, sql/legacy,
Spring-era archives, or the original ptud_nhom8 worktree.
