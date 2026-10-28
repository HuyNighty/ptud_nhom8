# M0-CORE-02 - One-time local SQL onboarding for the 5 N08 team members

**Baseline:** GitHub PR #57 merged into `main`; Java 21 + Maven + Swing + JDBC. This change must be reviewed and merged separately.

**Goal:** after a one-time private setup, anyone with an eligible SQL Server instance can click **IntelliJ Run `com.quanlynhakhoa.App`**, type their own Swing account credentials, and open `MainFrame` using their own database. Docker is **not** required.

## Preconditions (one-time on each developer's own Windows computer)

1. Install JDK 21 and open the project as Maven in IntelliJ. Ensure Maven dependencies download successfully.
2. Have SQL Server running locally (SQL Server Developer/Express native on Windows OR SQL Server in Docker). It must accept **TCP connections on 127.0.0.1**. The **SQL Authentication** login used for first-time setup must have permissions to create a database and server login (e.g. local SQL administrator); Windows-only integrated authentication is not supported by this first version of setup. Do not share this credential with the group.
3. SQL Server named instance `SQLEXPRESS` may use a dynamic TCP port. In SQL Server Configuration Manager enable TCP/IP and configure a stable port (often `1433`), then restart that instance. Use its *actual* port; port 1433 is an example only.
4. Existing `N08Dental` is protected. NEVER run setup on or copy patient data from it. Do NOT commit any `.env`, SQL credential, data dump or `.n08-dev.local.properties`.

## Clone and provision a NEW isolated database

```powershell
# On each member's PC:
git clone https://github.com/HuyNighty/ptud_nhom8.git
cd ptud_nhom8
.\mvnw.cmd clean compile

# Native SQL Server default-instance example on port 1433:
.\scripts\setup-local.ps1 -Port 1433 -SqlAdminUser sa

# Alternatively, SQL Server Docker published to 14333:
.\scripts\setup-local.ps1 -Port 14333 -SqlAdminUser sa
```

The script asks for explicit `CREATE` consent, then prompts invisibly for the **SQL administrator password** (for initial setup only) and a **new Swing `dev.admin` password** (entered twice). It creates a new random-name `N08Dev_XXXXXXXX` database, applies **unmodified** `sql/bootstrap/V1..V4` SQL sequentially, inserts a BCrypt-hashed **synthetic** `dev.admin` account, generates a restricted SQL application login, verifies a real JDBC authentication transaction and saves its *SQL login* details to the untracked `.n08-dev.local.properties` file. SQL admin credentials are **not** written to disk by the setup script.

The script **refuses to overwrite any existing local configuration or database**. On partial SQL failure it does not auto-drop databases or SQL logins; an administrator should inspect and remove only the specifically generated disposable resources before a new attempt.

The local configuration stores a machine-specific SQL login password as **plaintext**. Treat this as a development-only local secret: do not share it or commit it, and protect the developer's Windows user profile/working folder. The SQL login initially has only the authentication-specific SELECT and column-level UPDATE grants; **future feature DB privileges need explicit review**. Never use the local setup for real patient data or production.

## Run IntelliJ

1. Open project `ptud_nhom8` as Maven, set JDK 21.
2. `Run -> Edit Configurations`: main class `com.quanlynhakhoa.App`; **Working directory must be the project root** (folder containing `pom.xml` and `.n08-dev.local.properties`).
3. Click **Run**. App checks its local database first, then shows LoginFrame.
4. Username: `dev.admin`; password: **the Swing password you personally set during setup** (not the SQL admin password). Successful login opens MainFrame.
5. Repeat Run later: no additional setup or PowerShell prompt needed as long as SQL Server is running on the same port and the local configuration exists.

Optional verification before IntelliJ Run:

```powershell
.\scripts\check-team-dev.ps1
.\scripts\run.ps1 -Smoke
```

## Different errors and responses

| Symptom | Check |
|---|---|
| Maven compile fails | JDK 21, Maven sync, first-download network/dependencies |
| SQL connection refused | SQL Server service/container is running, TCP/IP enabled, port matches setup |
| SQL login failure on bootstrap | SQL Authentication/Mixed Mode enabled, local admin username/password correct, server CREATE DATABASE/CREATE LOGIN privileges |
| Setup fails after DB was created | Do **not** retry against the same DB; inspect disposable resource names printed by setup; do not touch `N08Dental` |
| App says config missing | Check IntelliJ working directory, run setup once |
| App says DB connection failed | Check local port, SQL login and password file; no need to reset Git |
| Login rejected | Use `dev.admin` + chosen Swing password, not SQL login password; 5 wrong attempts may trigger temporary account lock |
| Missing feature SQL permission | Expected for M0; feature owner + Core owner must approve a new grant/schema migration |

## Explicit limitations

- **One-time setup is necessary**. Cloning a Git repo cannot automatically install or start SQL Server or know private database credentials.
- **Windows + local TCP + SQL Authentication** is the covered bootstrap route. Windows integrated-only setup, nonlocal SQL hosts, and existing team DB migrations are not yet supported.
- `N08Dev_XXXXXXXX` is **isolated dev-only**. Existing `N08AuthIT_XXXXXXXX` test launcher and its guard are retained.
- This onboarding kit is a **candidate** until actually exercised against clean **Docker SQL Server** and **native SQL Server** installations by two independent team machines.
- `MainFrame` business pages remain placeholders; M0-CORE-02 proves startup/login, **not** patient/billing/inventory workflows.
