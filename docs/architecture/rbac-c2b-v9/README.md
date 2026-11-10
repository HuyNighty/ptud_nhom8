# C2B — V9 Dynamic RBAC schema foundation

Source: 0940bc2da37d9b93b6963e3c67cfc7b5c51673c0 (R3 remote branch + completed model→entity package refactor), not origin/main ef171cef. Local branch refactor/n08-rbac-c2b-v9-schema. No SQL Server connection or migration execution. No runtime activation, account schema changes, SQL grants, bootstrap wiring or PR changes.

## Exact delta / C2A contract

Four new tables, informational total47 after fullV8; never a total-table-count gate:

| Table | Contract |
|---|---|
| VaiTro | roleCode varchar(32) BIN2 PK; roleName nvarchar(100) NOT NULL/nonblank; description nvarchar(500) NOT NULL; enabled/isSystem/isProtected bit NOT NULL; revision rowversion NOT NULL (8 bytes) |
| Quyen | permissionCode varchar(64) BIN2 PK, constrained to current26 codes; permissionName nvarchar(120) NOT NULL/nonempty; enabled bit NOT NULL |
| VaiTroQuyen | composite PK(roleCode,permissionCode); two trusted-on-create FKs to catalogs, explicit NO ACTION for delete/update; no duplicates/dangling references |
| RbacEpoch | singletonId tinyint PK CHECK=1; epoch bigint CHECK>=1, seeded1; revision rowversion; at most one row, not an immutable singleton |

C2A findByCode/findAll SELECT precisely roleCode,roleName,description,enabled,isSystem,isProtected,revision FROM dbo.VaiTro; no aliases or package/runtime edits needed. roleCode is strict [A-Z][A-Z0-9_]{0,31}, no padding/case folding. isSystem iff one of six legacy codes, isProtected iff SYSTEM_ADMIN; protected admin enabled=1. Name CHECK models Java String.isBlank whitespace. RoleEntity/Repository still have historical V8 comments; actual catalog is thisV9, not billingV8. Those four C2A files are unchanged in C2B.

TaiKhoan.vaiTro remains VARCHAR(32) BIN2 NOT NULL with existing trusted CK_TaiKhoan_VaiTro. No ALTER TABLE TaiKhoan, no new account FK and no account-data mutation. One role/account and six-role runtime contract remain. Custom catalog rows may be represented later, but cannot be assigned to legacy accounts without separately approved C3/C4 migration/runtime changes.

## Seed snapshot

Seed blocks in V9 are source-controlled fixtures, not dynamically generated at deployment. Static test compares every role×permission against the actual baseline RolePermissionPolicy, using refactored quantri.vaitro.entity.Role. It checks missing/extra grants in both directions, duplicate pairs and privilege-expansion sensitivity. SQL seed table PKs reject duplicates and postconditions use six EXCEPT queries for two-way role/permission/grant equality.

| Role | Grants | enabled | isSystem | isProtected |
|---|---:|---:|---:|---:|
| RECEPTIONIST | 11 | 1 | 1 | 0 |
| DOCTOR | 7 | 1 | 1 | 0 |
| NURSE | 4 | 1 | 1 | 0 |
| CLINIC_MANAGER | 11 | 1 | 1 | 0 |
| INVENTORY_MANAGER | 4 | 1 | 1 | 0 |
| SYSTEM_ADMIN | 26 | 1 | 1 | 1 |

Total6/26/63, including REVENUE_REPORT_READ for CLINIC_MANAGER and SYSTEM_ADMIN. ROLE_MANAGE remains existing read-only non-admin UI behavior; seed does not grant new mutation authority. Exact mappings are explicit63 rows in BEGIN/END SEED GRANTS and independently checked by Java policy at test time.

## Required V8 manifest and upgrade prerequisites

Authoritative named table manifest: BEGIN/END REQUIRED TABLES in SQL. Test derives baseline CREATE TABLE names from V1–V8 files and compares the full name set;43 is descriptive, not an acceptance condition. Extra unrelated tables do not make the gate fail.

- V1–V5 core tables39; V6 YeuCauCapPhat and ChiTietYeuCauCapPhat; V7 Thuoc.donGiaBan decimal(18,0) nullable; V8 LanThuHoaDon and DieuChinhThanhToan.
- V8 DongTienHoaDon view; HoaDon snapshot name/phone and ChiTietHoaDon.tenHangChot column shape.
- Trusted/enabled CK_TaiKhoan_VaiTro, CK_Thuoc_DonGiaBan, CK_ThanhToan_TrangThai; visible existing account CHECK metadata, current account roles within the six-code set, and account column collation. V9 does not parse engine-rewritten IN/OR constraint text or claim semantic equivalence from string normalization.
- V8 unique/enabled/filtered UX_LanThu_DangXuLy plus enabled triggers TR_HoaDon_ChotTen, TR_ChiTietHoaDon_ChotTen, TR_KeHoach_KhoaSauHoaDon on correct tables.

This is a minimum prerequisite manifest, not a full cryptographic fingerprint of every V1–V8 object/trigger body/filter expression. SQL integration must still verify definitions/constraints/data and restricted-principal behavior; object names alone do not establish security.

Fresh installation: complete V1→V8 and commit first, then run V9 on separate standalone migration-admin connection only after PO execution approval. Existing dev withV7 must separately verify/apply missingV6/V8 through approved upgrade procedure. V9 does not repair older or partial schema. DevSetup is deliberately unchanged: it currently opens an ambient transaction, so appendingV9 directly would fail @@TRANCOUNT gate. No out-of-order replay, reset or rename of appliedV8.

V9 exact DB guard uses DATALENGTH=30 bytes for15 Unicode characters, binary case-sensitive N08Dev_ prefix and8 lowercase hex characters. Rejects genericN08Dev, uppercase suffix, trailing spaces, wrong length/case/characters. This name check limits accidental targeting; it does not authenticate actor or prove the database is disposable.

## Transaction, retry and rollback

Standalone owner only (@@TRANCOUNT=0), SET XACT_ABORT ON, BEGIN TRY/TRANSACTION; transaction-owned sp_getapplock serializes cooperating V9 migrations. Required-object validation and existing/partial-catalog rejection happen before CREATE TABLE. All four DDLs, seeds and seed postconditions occur before single COMMIT. CATCH rolls back when XACT_STATE()!=0 then THROW; no persistent work after COMMIT.

Not replay-idempotent by design: second or partial application stops rather than merging/overwriting policy. If execution fails, do not mask it by dropping/reseeding existing objects. Cooperative applock does not prevent arbitrary DBA DDL; use approved maintenance window.

**Rollback execution NOT VERIFIED.** Static tests check source ordering, XACT_ABORT, CATCH rollback/rethrow and no writes outside owned transaction. Synthetic/domain tests do not emulate the SQL Server engine or prove transactional DDL, compilation-error handling, connection-failure recovery or concurrency. No such PASS is claimed.

Before C3/C4, request PO approval for isolated N08Dev_<8hex>, then test (not executed here):
1. Missing V8 prerequisites, invalid names/collation, untrusted constraints, ambient transaction and partial catalog: rejection with no changes.
2. Fresh/fullV8 and existingV8 upgrade: exact catalog rows/types/FKs/rowversion, account data/schema/CK unchanged.
3. In isolated test copies inject THROW after each CREATE and seed statement, plus duplicate/FK/postcondition violations. Compare object names/definitions, existing table hashes/data, grants, transaction count before/after. Confirm no four catalog tables survive rollback and original schema/data unchanged.
4. Kill connection / statement recompilation failure and deadlock cases: rollback/recovery and pool cleanup; do not assume all error classes reach same-level CATCH.
5. Replay and two concurrent migration connections: one commit, other safe rejection; no partial state.
6. Actual RoleCatalog mapping (worker only), rowversion bytes, SQL negative code/blank-name/FK/duplicate-grant cases and least-privilege principal. No db_owner for app and no automatic RBAC DML.

Before execution retain backup/restore rehearsal and schema/data/grants baseline. After successful commit, recovery requires explicit approval and data review, not automatic down migration. Do not drop catalogs that gained custom data/dependencies; prefer approved restore/forward repair. No destructive rollback script is shipped.

## Optimistic concurrency / epoch contract for C3 (not implemented)

One caller-owned service transaction, bound to authenticated SYSTEM_ADMIN identity:
1. Lock/verify current account and authorization within transaction; role object/actorId supplied by Swing is not proof.
2. CAS UPDATE VaiTro SET description=description WHERE roleCode=? AND revision=? AND isProtected=0. Require exactly1 affected row before replacing grants; zero means conflict/notfound/protected. Same-value UPDATE bumps rowversion. Never rely on grant-table writes alone to update parent revision.
3. Validate exact allowed permission set; replace grants and keep protected admin26 grants immutable unless separately approved.
4. Lock singleton RbacEpoch row (UPDLOCK,HOLDLOCK), require exactly1 row, increment epoch once; overflow or missing row is error. Commit audit+grant+revision+epoch in same transaction; rollback on any failure. Do not allow partial commit or auto-retry stale CAS as success.
5. Future sessions carry/check epoch at service boundary; define account-specific revocation separately. Current DesktopSession/JWT/auth logic is not changed here.

No trigger currently bumps revision/epoch on raw SQL changes. App currently has no newly granted catalog write authority. DAO read access is unwired; deployment of catalog SELECT permissions also needs separate approval.

## Protected-role gaps reported before any design expansion

CHECK prevents invalid SYSTEM_ADMIN metadata/disable and fake system/protected flags. FK prevents deleting referenced catalog rows while grants remain. It does **not** stop deleting admin grants first, deleting/recreating admin row, deleting epoch row, disabling permissions or bypass by SQL owner. PK/CHECK guarantee at-most-one epoch, not that a row must always exist. No last-active-admin account invariant is added because account/runtime mutations are outside this checkpoint.

PO must decide before C3/C4:
- Protected role/permission/grants deletion/rename semantics and enforcement mechanism; whether triggers/procedures or restricted trusted API boundary is approved.
- Last-admin concurrency/recovery, bootstrap admin proof, auditable break-glass procedure.
- CAS ownership and epoch bump enforcement against direct DML; whether shared desktop JDBC can be trusted (currently it cannot establish production-grade identity enforcement).
- Account FK/CHECK transition and custom role assignment, session revocation semantics, read grants and isolated SQL execution approval.

No implementation task or new authorization was inferred from these gaps. C2B is a local schema/contract artifact only, not production security certification.

## Fast validation

Run `.\mvnw.cmd test-compile`, then (classpath excludes SQL Server driver):

    java -Djava.awt.headless=true -cp "target/classes;target/test-classes" com.quanlynhakhoa.quantri.vaitro.catalog.RoleCatalogSelfTest
    java -Djava.awt.headless=true -cp "target/classes;target/test-classes" com.quanlynhakhoa.quantri.vaitro.catalog.RbacV9SqlContractSelfTest

RbacV9SqlContractSelfTest is a static/domain test, not a T-SQL parser or SQL engine integration test. See external c2b-v9-evidence logs for executed results. Full40-test suite is intentionally not rerun: no related runtime code changed.

## Kết quả local 2026-11-08

- Maven test-compile: PASS exit0, Java25.0.4.1 / release21.
- RoleCatalogSelfTest: PASS exit0,96 checks, synthetic JDBC only.
- RbacV9SqlContractSelfTest: PASS exit0,360 static/domain checks;6/26/63 parity,43 named prerequisite tables.
- Không có tracked-file changes; ba file mới là V9 SQL, static test và tài liệu này. Không stage/commit/push.
- SQL migration, transactional rollback/FK/CHECK enforcement và real JDBC integration: NOT VERIFIED.
