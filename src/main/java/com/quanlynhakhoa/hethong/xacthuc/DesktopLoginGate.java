package com.quanlynhakhoa.hethong.xacthuc;

import com.quanlynhakhoa.security.KetQuaDangNhap;

/** Only a successful, fully eligible result may unlock the placeholder main window. */
public final class DesktopLoginGate {
    private DesktopLoginGate() { }

    public static boolean mayOpenMain(KetQuaDangNhap result) {
        return result != null && result.authenticated()
                && result.role() != null
                && result.accountId() != null && !result.accountId().isBlank()
                && !result.forcePasswordChange();
    }
}
