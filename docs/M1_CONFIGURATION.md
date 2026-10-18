# N08 — M1 Configuration Foundation

**Baseline:** `main` at `d643f2c0a8ffb30d88c81fdd44790df7903ea1d6`.
**Scope:** Java 25, Spring Boot 4.1.1, one JVM with embedded local REST API and Swing UI shell.
**Out of scope:** patient/business logic, JPA mapping, SQL Server connection, security, legacy Swing migration.

## Architecture

- One Maven build (`pom.xml`), one process, one Spring ApplicationContext.
- `configuration/swing/DesktopLauncher` listens for context readiness and opens `desktop/frame/MainFrame` on the Swing EDT.
- Closing the JFrame closes the Spring context (and therefore the embedded HTTP server).
- `configuration/web/SystemController` offers **only** a smoke endpoint for testing the HTTP stack.
- `modules/*/package-info.java` holds the agreed ownership boundaries until real code arrives.
- Later: Swing calls application Services directly; optional HTTP controllers call the **same** Services, never duplicate business rules.

## Requirements

- **JDK 25** configured for `JAVA_HOME` and IntelliJ Project SDK.
- Maven Wrapper (provided with the repo) or Maven compatible with Spring Boot 4.1.1.
- An available local TCP port 8080 (or override using environment variable below).
- A graphical desktop for the default JFrame mode.

## Commands — Windows PowerShell, repository root

```powershell
java -version
.\mvnw.cmd -version
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Expected in normal desktop mode: a **minimal** JFrame appears, and the embedded Tomcat server listens locally.
Check the HTTP transport in a second PowerShell window:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/v1/system/ping
```

Expected JSON: `{ "message": "pong" }`. This checks the REST transport, **not** database availability.

### Headless / server-only testing

The new shell can be disabled when CI or test environments have no desktop display.

```powershell
$env:N08_DESKTOP_ENABLED = "false"
.\mvnw.cmd spring-boot:run
```

Close the process with Ctrl+C. Clear the variable later with:

```powershell
Remove-Item Env:N08_DESKTOP_ENABLED
```

### Development profile

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.profiles.active=dev'
```

Alternatively, set the `SPRING_PROFILES_ACTIVE` environment variable to `dev`.

## Configuration keys

| Key | Default | Purpose |
|---|---|---|
| `N08_DESKTOP_ENABLED` | `true` | Open Swing after Spring context is ready |
| `N08_SERVER_ADDRESS` | `127.0.0.1` | Bind embedded HTTP server to loopback only |
| `N08_SERVER_PORT` | `8080` | Embedded HTTP server port |
| `SPRING_PROFILES_ACTIVE` | *(none)* | Optional `dev` logging profile |

The repository `.env` file is **not** automatically read by Spring Boot. Supply environment
variables through the IDE run configuration or the terminal. `.env` and local IDE settings
must stay untracked. The old `.env` is currently a tracked empty file; the application
patch alone does not remove it from Git's index.

## Review checklist

1. Confirm `git status --short` is clean before starting.
2. Create branch `chore/application-configuration` from expected `main` HEAD.
3. Apply M1 patch and untrack legacy `.idea/` and `.env` while keeping local files.
4. Confirm `java -version` shows JDK 25.
5. Run `./mvnw.cmd clean test` and inspect JUnit report for success.
6. Launch the app; verify the JFrame and `/api/v1/system/ping` both work.
7. Close the JFrame and verify the application process / embedded server shuts down.
8. Commit, push the feature branch, open a PR to `main`; do not force-push.

## Known limitations

- No JPA or datasource configured yet, intentionally. Having the SQL Server JDBC driver on the classpath does not by itself establish a datasource.
- No business API yet. The first full vertical slice will be `patient` (contract, validation, service, persistence, tests).
- This kit was statically checked outside the target machine. Maven/Java 25 integration tests must run on the developer's computer before declaring M1 complete.
- If the port is occupied, use `N08_SERVER_PORT` to change it.
