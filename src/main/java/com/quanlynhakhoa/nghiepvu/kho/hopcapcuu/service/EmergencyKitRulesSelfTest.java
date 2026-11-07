package com.quanlynhakhoa.nghiepvu.kho.hopcapcuu.service;

import java.time.LocalDate;

public final class EmergencyKitRulesSelfTest {
    private static int checks;

    private static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError("EmergencyKitRules check " + checks + ": " + label);
    }

    private static void reject(Runnable operation) {
        boolean rejected = false;
        try {
            operation.run();
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "invalid input rejected");
    }

    public static void main(String[] args) {
        LocalDate today = LocalDate.of(2026, 11, 6);
        check(!EmergencyKitRules.itemPasses(2, 1, null, null, today, 30), "below minimum quantity fails");
        check(EmergencyKitRules.itemPasses(2, 2, null, null, today, 30), "non-expiring item at minimum passes");
        check(!EmergencyKitRules.itemPasses(1, 1, today.plusDays(60), null, today, 30), "missing actual expiry fails when expiry is configured");
        check(!EmergencyKitRules.itemPasses(1, 1, today.plusDays(60), today.plusDays(30), today, 30), "expiry exactly at threshold fails");
        check(EmergencyKitRules.itemPasses(1, 1, today.plusDays(10), today.plusDays(31), today, 30), "actual expiry beyond threshold passes");
        check(EmergencyKitRules.isOverdue(null, today, 7), "no previous check is due immediately");
        check(!EmergencyKitRules.isOverdue(today.minusDays(7), today, 7), "exact interval boundary is not overdue");
        check(EmergencyKitRules.isOverdue(today.minusDays(8), today, 7), "more than interval is overdue");
        check(!EmergencyKitRules.isOverdue(today.plusDays(1), today, 7), "future check date is not overdue");
        reject(() -> EmergencyKitRules.itemPasses(-1, 0, null, null, today, 30));
        reject(() -> EmergencyKitRules.isOverdue(today, today, 0));
        System.out.println("PASS: EMERGENCY KIT RULES (" + checks + " checks; no DB, no Swing)");
    }
}
