# N08 M1-CORE-02 — Panel integration contract

Target baseline: `main` commit `54fdad555ac4d605c64cb08c6a87d1f42d2641a0`.
Java 21 / Swing / JDBC / SQL Server. No Spring, REST, schema changes, or new grants.

## Responsibility

Core owns `MainFrame`, `FeatureBootstrap`, `FeatureRegistry`, navigation and session handling.
Feature owners provide their own `JPanel` and business-service implementations.
Feature PRs must not modify `MainFrame`, `FeatureBootstrap`, `Module`, `NavigationPolicy`, or role grants.

- Nguyễn Văn Trí: patient / appointments. Existing navigation slot: `Module.PATIENTS`.
- Đỗ Quốc Thịnh: clinical / examination. Dedicated `Module.EXAMINATION` does **not** exist yet; do not register clinical work under `MEDICAL_RECORDS` without an approved destination/permission contract.
- Trần Phúc Khang: service catalogue / billing. Existing navigation slot: `Module.BILLING`. Dedicated service-catalogue slot does not exist yet.
- Nguyễn Hải Đăng: inventory / management. Existing navigation slot: `Module.INVENTORY`. Workforce uses `Module.WORKFORCE`, but responsibilities and role grants need review.

## How Core later integrates a delivered JPanel

Feature owner submits a panel with a constructor accepting the authenticated `DesktopSession` and a JDBC `Connection` provider, without making SQL calls inside the Swing constructor. Data loads through `SwingWorker` and service methods check permission and active session.

After the feature PR has been reviewed, only the Core owner changes `FeatureBootstrap.createRegistry()`. Example when `PatientPanel` actually exists:

```java
return FeatureRegistry.builder()
        .register(Module.PATIENTS,
                session -> new PatientPanel(session, TeamDevConnection::open))
        .build();
```

The example is NOT included as live code: `PatientPanel` has not been delivered. Avoid registering placeholders as if they were implemented.

MainFrame constructs the page only on the **first authorized navigation**. Navigating to an unimplemented module retains the existing placeholder. The original `MainFrame(DesktopSession, Runnable)` constructor and login flow remain intact. A three-argument overload supports isolated integration/testing with a supplied registry.

`FeatureRegistry.create` re-checks navigation authorization before invoking the registered factory. This is a UI guard, not business authorization. Every feature Service must independently enforce its required permission; never trust a role claimed by the view. Do not perform JDBC on the EDT.

## Local verification

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test-compile
.\scripts\run.ps1 -Smoke
```

For Windows PowerShell the last command can be run as:

```powershell
java -cp 'target/classes;target/test-classes' com.quanlynhakhoa.features.FeatureRegistrySelfTest
```

The self-test creates only synthetic in-memory sessions and lightweight Swing panels; no real account, JFrame, or database write.

Manual acceptance: log in as `dev.admin`; HOME and existing placeholders render; logout invalidates navigation; registered pages are only shown for authorized modules. No module is registered by this change yet.

Scope: one Core-only `MainFrame.java` edit, three new Core classes, one new self-test. No edits to `LoginFrame`, security policy or SQL.
