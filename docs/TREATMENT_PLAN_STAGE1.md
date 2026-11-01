# Treatment Plan R1: stage 1, SOURCE PORT ONLY

Baseline: main b9f526d07e3f68332c88f806a32c6c52699137b0.
Origin of four source files: origin/nguyen_hai_dang (PR #96).

Scope: four Java files plus independent TreatmentPlanSqlSupport.
No changes to MainFrame, FeatureBootstrap, Module, permissions, or schema.

NOT READY FOR MERGE/PRODUCTION:
1. Implement doctor ownership restrictions for list/detail and every mutation.
2. Confirm intended SYSTEM_ADMIN behavior: source currently attempts doctor identity for writes.
3. Confirm date/status validation, clinic workflow and SQL authorization.
4. Integrate MainFrame/FeatureBootstrap/Module + update role/navigation selftests.
5. Verify dev-only JDBC CRUD and transaction rollback; record evidence.

Do not merge stage 1 by itself.