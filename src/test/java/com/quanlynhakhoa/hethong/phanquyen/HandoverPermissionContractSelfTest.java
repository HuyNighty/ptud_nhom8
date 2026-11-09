package com.quanlynhakhoa.hethong.phanquyen;

import com.quanlynhakhoa.hethong.xacthuc.AuthDecision;
import com.quanlynhakhoa.enums.VaiTroHeThong;

/** Synthetic authorization contract; employee ownership is enforced in Workforce service/DAO. */
public final class HandoverPermissionContractSelfTest {
    private static int checks;
    private static void check(boolean value, String description) {
        checks++;
        if (!value) throw new AssertionError("Handover contract " + checks + ": " + description);
    }
    public static void main(String[] args) {
        RolePermissionPolicy policy = new RolePermissionPolicy();
        AuthorizationService auth = new AuthorizationService(policy);
        for (VaiTroHeThong role : VaiTroHeThong.values()) {
            DesktopSession current = DesktopSession.fromSuccessfulLogin(
                    new AuthDecision(true, "SYNTHETIC", role, false));
            check(policy.isAllowed(role, Permission.SHIFT_HANDOVER_MANAGE), "proposed six-role grant: " + role);
            check(auth.can(current, Permission.SHIFT_HANDOVER_MANAGE), "active session grant: " + role);
            current.logout();
            check(!auth.can(current, Permission.SHIFT_HANDOVER_MANAGE), "logout revoked: " + role);
        }
        check(!auth.can(null, Permission.SHIFT_HANDOVER_MANAGE), "anonymous denied");
        check(!policy.isAllowed(null, Permission.SHIFT_HANDOVER_MANAGE), "null role denied");
        System.out.println("PASS: HANDOVER PERMISSION CONTRACT (" + checks + " synthetic checks; no DB)");
    }
}
