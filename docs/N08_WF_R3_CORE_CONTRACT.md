# N08 WF-R3 — Core authorization contract proposal

- Base main: d5ebd56f5824695b899b1fd13679cdd7e601453b (PR #105 merged)
- Added permission: `SHIFT_HANDOVER_MANAGE`.
- PO-approved in local script only for this isolated, reviewable branch: all six authenticated roles receive this action permission. It does NOT grant cross-employee ownership.
- Workforce business service/DAO must independently enforce that an assigned giver creates/cancels only their pending handovers; an assigned, distinct recipient confirms; no self-confirm; no cross-employee read by non-management; safe transaction and audit.
- Broader history visibility remains tied to existing `WORKFORCE_READ` and must not be conflated with handover ownership.
- Core now exposes the enum and role-matrix only. No `Module`, `FeatureBootstrap` or `MainFrame` registration until the Workforce code is committed/integrated and constructors are verified.
- Corrected an older synthetic role-session test that still expected 21 modules rather than 22 following Treatment PR #105.
- Test compilation does not prove JDBC E2E. No SQL grants or writes are performed by this script.
- Do NOT merge the Core branch to main unless policy/ownership tests, downstream Workforce checks, and PO review pass.