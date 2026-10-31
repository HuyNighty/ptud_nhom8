package com.quanlynhakhoa.lichhen.service;

import com.quanlynhakhoa.benhnhan.service.DuLieuDaThayDoiException;
import com.quanlynhakhoa.lichhen.model.LichHen;
import java.time.LocalDate;

public final class LichHenTransitionGuardSelfTest {

    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(
                "INT3 check " + checks + ": " + message
            );
        }
    }

    public static void main(String[] args) {

        // Successful update must be accepted.
        LichHenService.yeuCauDaCapNhatTrangThai(
            true, "LH000001"
        );
        check(true, "Successful update accepted");

        // Zero-row update must be rejected.
        try {
            LichHenService.yeuCauDaCapNhatTrangThai(
                false, "LH000001"
            );
            throw new AssertionError(
                "Zero-row update should fail"
            );
        } catch (DuLieuDaThayDoiException expected) {
            check(
                expected.getMessage().contains("LH000001"),
                "Appointment ID present"
            );
            check(
                expected.getMessage().contains("tải lại"),
                "Refresh instruction present"
            );
        }

        LichHen lichHen = new LichHen("LH000001");
        lichHen.setThoiGianHen(
            LocalDate.now().atTime(12, 0)
        );

        String[] booked = {
            LichHen.CHO_XAC_NHAN,
            LichHen.DA_XAC_NHAN
        };

        for (String state : booked) {
            lichHen.setTrangThai(state);

            check(
                lichHen.laDaDat(),
                state + " is booked"
            );
            check(
                lichHen.duocTiepDon(),
                state + " allows check-in today"
            );
        }

        String[] closed = {
            LichHen.DA_DEN,
            LichHen.DA_HUY,
            LichHen.DA_DOI_LICH,
            "HOAN_THANH",
            "VANG_MAT"
        };

        for (String state : closed) {
            lichHen.setTrangThai(state);

            check(
                !lichHen.laDaDat(),
                state + " is not booked"
            );
            check(
                !lichHen.duocTiepDon(),
                state + " rejects check-in"
            );
        }

        lichHen.setTrangThai(LichHen.CHO_XAC_NHAN);
        lichHen.setThoiGianHen(
            LocalDate.now().plusDays(1).atTime(12, 0)
        );

        check(
            !lichHen.duocTiepDon(),
            "Future appointment rejects check-in"
        );

        System.out.println(
            "PASS: INT3 appointment transition guard ("
            + checks + " checks; no DB)"
        );
    }
}