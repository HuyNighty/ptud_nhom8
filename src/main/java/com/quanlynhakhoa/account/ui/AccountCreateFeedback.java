package com.quanlynhakhoa.account.ui;

import java.sql.SQLException;

/** Friendly, non-sensitive messages for asynchronous account creation failures. */
final class AccountCreateFeedback {
    private AccountCreateFeedback() { }

    static String forFailure(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof IllegalArgumentException validation) {
                String detail = validation.getMessage();
                return detail == null || detail.isBlank()
                        ? "Dữ liệu tạo tài khoản không hợp lệ."
                        : detail;
            }
            if (current instanceof SQLException sql) {
                for (SQLException next = sql; next != null; next = next.getNextException()) {
                    if (next.getErrorCode() == 2601 || next.getErrorCode() == 2627) {
                        return "Tên đăng nhập đã tồn tại hoặc nhân viên đã có tài khoản.";
                    }
                }
            }
        }
        return "Không thể tạo tài khoản. Kiểm tra dữ liệu và quyền SQL.";
    }
}
