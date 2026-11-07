package com.quanlynhakhoa.nghiepvu.kho.workflow;

import com.quanlynhakhoa.nghiepvu.kho.workflow.service.InventoryApprovalRules;

/** Run with: java -cp "target/classes;target/test-classes" ...InventoryApprovalRulesSelfTest */
public final class InventoryApprovalRulesSelfTest {
    private static int assertions;
    public static void main(String[] args) {
        InventoryApprovalRules.requireApprover("CHO_DUYET", "A", "B"); assertions++;
        fails(() -> InventoryApprovalRules.requireApprover("CHO_DUYET", "A", "A"));
        fails(() -> InventoryApprovalRules.requireApprover("DA_DUYET", "A", "B"));
        fails(() -> InventoryApprovalRules.requireApprover("TU_CHOI", "A", "B"));
        InventoryApprovalRules.requireQuantity(5, 0); assertions++;
        InventoryApprovalRules.requireQuantity(5, 8); assertions++;
        fails(() -> InventoryApprovalRules.requireQuantity(0, 0));
        fails(() -> InventoryApprovalRules.requireQuantity(-1, 5));
        fails(() -> InventoryApprovalRules.requireQuantity(5, -1));
        System.out.println("PASS InventoryApprovalRulesSelfTest (" + assertions + " assertions; no database)");
    }
    private static void fails(Runnable action) {
        try { action.run(); throw new AssertionError("Expected rejection"); }
        catch (IllegalArgumentException expected) { assertions++; }
    }
}