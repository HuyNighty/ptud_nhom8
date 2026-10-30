package com.quanlynhakhoa.account.ui;

import com.quanlynhakhoa.security.AccessDeniedException;
import java.sql.SQLTimeoutException;

/** Never exposes JDBC messages, SQL text or connection details. */
final class AccountStateFeedback {
    private AccountStateFeedback() { }

    static String forFailure(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof AccessDeniedException) {
                return "Phiên đăng nhập đã kết thúc hoặc tài khoản không có quyền quản lý tài khoản.";
            }
            if (current instanceof SQLTimeoutException) {
                return "SQL Server phản hồi quá lâu. Hãy làm mới danh sách trước khi thử lại.";
            }
            if (current instanceof IllegalArgumentException validation) {
                return validation.getMessage() == null || validation.getMessage().isBlank()
                        ? "Dữ liệu khóa hoặc mở khóa không hợp lệ." : validation.getMessage();
            }
        }
        return "Không thể thay đổi trạng thái tài khoản. Hãy làm mới và kiểm tra kết nối, quyền SQL.";
    }
}
