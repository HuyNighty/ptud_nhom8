package com.quanlynhakhoa.controller.dieutri;

import com.quanlynhakhoa.controller.dieutri.KeHoachDieuTriController;
import com.quanlynhakhoa.hethong.phanquyen.AccessDeniedException;
import com.quanlynhakhoa.service.dieutri.KeHoachDieuTriKhongKhaDungException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

/** Static message contract, no Swing window or SQL access. */
public final class TreatmentPlanErrorMessageSelfTest {
    public static void main(String[] args) {
        String sensitive = "jdbc:sqlserver://synthetic;password=SYNTHETIC_ONLY";
        Throwable[] errors = {null, new SQLException(sensitive), new RuntimeException(sensitive),
                new IllegalArgumentException(sensitive), new AccessDeniedException(),
                new KeHoachDieuTriKhongKhaDungException(),
                new ExecutionException(new SQLException(sensitive)),
                new ExecutionException(new KeHoachDieuTriKhongKhaDungException())};
        int checks = 0;
        for (Throwable error : errors) {
            String message = KeHoachDieuTriController.messageFor(error);
            if (message == null || message.isBlank() || message.contains(sensitive)
                    || message.contains("jdbc:") || message.contains("password"))
                throw new AssertionError("Unsafe GUI error message");
            checks++;
        }
        String unavailable = new KeHoachDieuTriKhongKhaDungException().getMessage();
        if (!KeHoachDieuTriController.messageFor(new KeHoachDieuTriKhongKhaDungException()).equals(unavailable)
                || !KeHoachDieuTriController.messageFor(new ExecutionException(
                    new KeHoachDieuTriKhongKhaDungException())).equals(unavailable))
            throw new AssertionError("Unavailable state lost");
        checks++;
        if (!KeHoachDieuTriController.messageFor(new AccessDeniedException()).contains("không có quyền"))
            throw new AssertionError("Missing localized denial");
        checks++;
        System.out.println("PASS: H2 GUI error messages (" + checks + " checks; headless, no database).");
    }
}

