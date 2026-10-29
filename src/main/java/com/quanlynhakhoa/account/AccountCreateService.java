package com.quanlynhakhoa.account;

import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;

public final class AccountCreateService {
    private static final Pattern EMPLOYEE_ID = Pattern.compile("[A-Z0-9]{1,10}");
    private static final Pattern USERNAME = Pattern.compile("[a-zA-Z0-9._-]{3,50}");
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DesktopSession session;
    private final AccountDirectoryService.ConnectionProvider connections;
    private final AuthorizationService authorization =
            new AuthorizationService(new RolePermissionPolicy());
    private final AccountCreateDao dao = new AccountCreateDao();

    public AccountCreateService(DesktopSession session,
                                AccountDirectoryService.ConnectionProvider connections) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    public String create(String employeeId, String username, String displayName,
                         Role role, char[] chosenPassword) throws SQLException {
        authorization.require(session, Permission.ACCOUNT_MANAGE);
        Objects.requireNonNull(chosenPassword, "chosenPassword");
        Objects.requireNonNull(role, "role");
        String staff = Objects.requireNonNull(employeeId, "employeeId").trim().toUpperCase(java.util.Locale.ROOT);
        String login = Objects.requireNonNull(username, "username").trim();
        String display = Objects.requireNonNull(displayName, "displayName").trim();
        if (!EMPLOYEE_ID.matcher(staff).matches()) {
            throw new IllegalArgumentException("Mã nhân viên gồm 1–10 chữ in hoa hoặc chữ số.");
        }
        if (!USERNAME.matcher(login).matches()) {
            throw new IllegalArgumentException("Tên đăng nhập gồm 3–50 ký tự chữ/số/._-.");
        }
        if (display.isEmpty() || display.length() > 100) {
            throw new IllegalArgumentException("Tên hiển thị phải có từ 1 đến 100 ký tự.");
        }
        if (role == Role.SYSTEM_ADMIN) {
            throw new IllegalArgumentException("Không được tạo SYSTEM_ADMIN qua giao diện.");
        }
        int passwordLength = new String(chosenPassword).getBytes(StandardCharsets.UTF_8).length;
        if (passwordLength < 12 || passwordLength > 72) {
            throw new IllegalArgumentException("Mật khẩu phải dài 12–72 byte UTF-8.");
        }

        // BCrypt accepts String; the caller must clear its mutable char[] after invocation.
        String passwordHash = BCrypt.hashpw(new String(chosenPassword), BCrypt.gensalt(12));
        String newId = newId();
        authorization.require(session, Permission.ACCOUNT_MANAGE);
        try (Connection connection = connections.open()) {
            if (!connection.getAutoCommit()) throw new SQLException("Expected a fresh JDBC connection");
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                authorization.require(session, Permission.ACCOUNT_MANAGE);
                dao.requireEligibleEmployee(connection, staff, role);
                authorization.require(session, Permission.ACCOUNT_MANAGE);
                dao.insertAccount(connection, newId, staff, login, display, passwordHash, role);
                authorization.require(session, Permission.ACCOUNT_MANAGE);
                dao.auditCreated(connection, session.accountId(), newId);
                authorization.require(session, Permission.ACCOUNT_MANAGE);
                connection.commit();
                return newId;
            } catch (SQLException | RuntimeException error) {
                try { connection.rollback(); }
                catch (SQLException rollbackError) { error.addSuppressed(rollbackError); }
                throw error;
            }
        }
    }

    private static String newId() {
        char[] chars = new char[10];
        chars[0] = 'T';
        chars[1] = 'K';
        for (int i = 2; i < chars.length; i++) chars[i] = HEX[RANDOM.nextInt(HEX.length)];
        return new String(chars);
    }
}
