package com.quanlynhakhoa.service.kho.quytrinh;

import com.quanlynhakhoa.service.kho.quytrinh.QuyTacDuyetKho;

/** Run with: java -cp "target/classes;target/test-classes" ...InventoryApprovalRulesSelfTest */
public final class InventoryApprovalRulesSelfTest {
    private static int assertions;
    public static void main(String[] args) {
        QuyTacDuyetKho.requireApprover("CHO_DUYET", "A", "B"); assertions++;
        fails(() -> QuyTacDuyetKho.requireApprover("CHO_DUYET", "A", "A"));
        fails(() -> QuyTacDuyetKho.requireApprover("DA_DUYET", "A", "B"));
        fails(() -> QuyTacDuyetKho.requireApprover("TU_CHOI", "A", "B"));
        QuyTacDuyetKho.requireQuantity(5, 0); assertions++;
        QuyTacDuyetKho.requireQuantity(5, 8); assertions++;
        fails(() -> QuyTacDuyetKho.requireQuantity(0, 0));
        fails(() -> QuyTacDuyetKho.requireQuantity(-1, 5));
        fails(() -> QuyTacDuyetKho.requireQuantity(5, -1));
        System.out.println("PASS InventoryApprovalRulesSelfTest (" + assertions + " assertions; no database)");
    }
    private static void fails(Runnable action) {
        try { action.run(); throw new AssertionError("Expected rejection"); }
        catch (IllegalArgumentException expected) { assertions++; }
    }
}
