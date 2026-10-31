package com.quanlynhakhoa.workforce;

import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Create an employee, not an account. Employee and audit writes commit together. */
public final class EmployeeCreateService {
    private static final Pattern ID = Pattern.compile("[A-Z0-9]{1,10}");
    private static final Pattern PHONE = Pattern.compile("0[0-9]{9}");
    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    private static final Set<String> JOB_TITLES = Set.of("LT", "BS", "YT", "QL", "KHO");
    private static final Set<String> GENDERS = Set.of("NAM", "NU", "KHAC");

    private final DesktopSession session;
    private final EmployeeDirectoryService.ConnectionProvider connections;
    private final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());
    private final EmployeeCreateDao dao = new EmployeeCreateDao();

    public EmployeeCreateService(DesktopSession session,
                                 EmployeeDirectoryService.ConnectionProvider connections) {
        this.session = Objects.requireNonNull(session, "session");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    public String create(EmployeeCreateRequest draft) throws SQLException {
        authorization.require(session, Permission.WORKFORCE_MANAGE);
        EmployeeCreateRequest request = validate(draft);
        authorization.require(session, Permission.WORKFORCE_MANAGE);
        try (Connection connection = connections.open()) {
            if (!connection.getAutoCommit()) {
                throw new SQLException("Expected a fresh JDBC connection");
            }
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try {
                authorization.require(session, Permission.WORKFORCE_MANAGE);
                dao.requireUnique(connection, request);
                authorization.require(session, Permission.WORKFORCE_MANAGE);
                dao.insert(connection, request);
                authorization.require(session, Permission.WORKFORCE_MANAGE);
                dao.auditCreated(connection, session.accountId(), request.employeeId());
                authorization.require(session, Permission.WORKFORCE_MANAGE);
                connection.commit();
                return request.employeeId();
            } catch (SQLException | RuntimeException failure) {
                try { connection.rollback(); }
                catch (SQLException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
                if (failure instanceof SQLException sql &&
                        (sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627)) {
                    throw new IllegalArgumentException("Mã nhân viên hoặc số điện thoại đã tồn tại.");
                }
                throw failure;
            }
        }
    }

    public static EmployeeCreateRequest validate(EmployeeCreateRequest input) {
        Objects.requireNonNull(input, "input");
        String id = trimmed(input.employeeId()).toUpperCase(Locale.ROOT);
        String name = trimmed(input.fullName());
        String phone = trimmed(input.phone());
        String job = trimmed(input.jobTitle()).toUpperCase(Locale.ROOT);
        String gender = optional(input.gender());
        String email = optional(input.email());
        String specialty = optional(input.specialty());
        LocalDate joined = input.joinedAt() == null ? LocalDate.now() : input.joinedAt();
        LocalDate birth = input.birthDate();

        if (!ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Mã nhân viên gồm 1–10 chữ in hoa hoặc số.");
        }
        if (name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("Họ tên nhân viên gồm 1–100 ký tự.");
        }
        if (!PHONE.matcher(phone).matches()) {
            throw new IllegalArgumentException("Số điện thoại phải có 10 chữ số và bắt đầu bằng 0.");
        }
        if (!JOB_TITLES.contains(job)) {
            throw new IllegalArgumentException("Chức danh không hợp lệ. Không tạo nhân viên AD tại đây.");
        }
        if (gender != null) {
            gender = gender.toUpperCase(Locale.ROOT);
            if (!GENDERS.contains(gender)) throw new IllegalArgumentException("Giới tính không hợp lệ.");
        }
        if (email != null && (email.length() > 100 || !EMAIL.matcher(email).matches())) {
            throw new IllegalArgumentException("Email không hợp lệ hoặc quá 100 ký tự.");
        }
        if (specialty != null && specialty.length() > 200) {
            throw new IllegalArgumentException("Chuyên môn tối đa 200 ký tự.");
        }
        if (joined.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày vào làm không được ở tương lai khi đang làm việc.");
        }
        if (birth != null && (birth.isAfter(LocalDate.now()) || birth.isAfter(joined))) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ.");
        }
        return new EmployeeCreateRequest(id, name, phone, job, birth, gender, email, joined, specialty);
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private static String optional(String value) {
        String normalized = trimmed(value);
        return normalized.isEmpty() ? null : normalized;
    }
}
