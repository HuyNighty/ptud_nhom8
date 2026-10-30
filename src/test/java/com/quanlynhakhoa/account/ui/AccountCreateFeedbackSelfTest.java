package com.quanlynhakhoa.account.ui;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

/** Zero-DB, zero-GUI regression checks for account creation error feedback. */
public final class AccountCreateFeedbackSelfTest {
    private static int checks;

    private static void check(String expected, Throwable failure) {
        checks++;
        String actual = AccountCreateFeedback.forFailure(failure);
        if (!expected.equals(actual)) {
            throw new AssertionError("Check " + checks + ": expected " + expected + ", got " + actual);
        }
    }

    public static void main(String[] args) {
        check("Nhân viên đã có tài khoản.",
                new ExecutionException(new IllegalArgumentException("Nhân viên đã có tài khoản.")));
        check("Tên đăng nhập đã tồn tại hoặc nhân viên đã có tài khoản.",
                new ExecutionException(new SQLException("Do not show SQL details", "23000", 2627)));
        check("Tên đăng nhập đã tồn tại hoặc nhân viên đã có tài khoản.",
                new SQLException("Do not show SQL details", "23000", 2601));

        SQLException wrapper = new SQLException("Do not show SQL details", "HY000", 0);
        wrapper.setNextException(new SQLException("Do not show SQL details", "23000", 2627));
        check("Tên đăng nhập đã tồn tại hoặc nhân viên đã có tài khoản.", wrapper);
        check("Dữ liệu tạo tài khoản không hợp lệ.", new IllegalArgumentException());
        check("Không thể tạo tài khoản. Kiểm tra dữ liệu và quyền SQL.",
                new ExecutionException(new SQLException("Sensitive JDBC details", "08001", 0)));
        check("Không thể tạo tài khoản. Kiểm tra dữ liệu và quyền SQL.",
                new RuntimeException("Unexpected details"));
        System.out.println("PASS: ACCOUNT CREATE FEEDBACK (" + checks + " checks; no DB, no GUI)");
    }
}
