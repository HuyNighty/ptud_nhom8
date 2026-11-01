# Treatment Plan R1 — Stage 3, Core navigation integration

Baseline: main b9f526d07e3f68332c88f806a32c6c52699137b0.
Branch: feat/n08-treatment-core-r1. No commit, push or merge by this script.

Integrated TREATMENT_PLANS into Module, FeatureBootstrap and MainFrame.
Navigation permission: MEDICAL_RECORD_READ (doctor and SYSTEM_ADMIN).
Mutating treatment methods remain restricted to DOCTOR by Stage 2 service guards.
Updated existing navigation, registry, shell authorization and GUI menu tests.

NOT READY FOR MERGE:
- No JDBC E2E test executed by this script.
- Need confirm SQL grants (SELECT/INSERT/UPDATE) for doctor account on
  dbo.KeHoachDieuTri, dbo.ChiTietDieuTri, referenced tables and audit log.
- Need verify Stage 2 doctor ownership SQL on real development DB.
- Need verify clinical workflow and concurrency on dev-only data.
- Need review against newer main and the two agents' other PRs before integrating.
- Login illustration/UI is a separate task; no auth changes here.