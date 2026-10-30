package com.quanlynhakhoa.dichvu.service;

import com.quanlynhakhoa.billing.dao.ConnectionProvider;
import com.quanlynhakhoa.dichvu.dao.DichVuDao;
import com.quanlynhakhoa.dichvu.model.DichVu;
import com.quanlynhakhoa.security.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public final class DichVuService {
    private final DesktopSession session;
    private final ConnectionProvider connections;
    private final DichVuDao dao;
    private final AuthorizationService authorization = new AuthorizationService(new RolePermissionPolicy());

    public DichVuService(DesktopSession session, ConnectionProvider connections) {
        this(session, connections, new DichVuDao());
    }

    public DichVuService(DesktopSession session, ConnectionProvider connections, DichVuDao dao) {
        this.session = Objects.requireNonNull(session);
        this.connections = Objects.requireNonNull(connections);
        this.dao = Objects.requireNonNull(dao);
    }

    public List<DichVu> find(String keyword, Boolean active) throws SQLException {
        require();
        String text = trimmed(keyword);
        if (text.length() > 100) throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");
        try (Connection connection = connections.open()) {
            require();
            List<DichVu> result = dao.find(connection, text, active);
            require();
            return result;
        }
    }

    /** Only ASCII digits, no thousands separator, decimal separator, sign or exponent. Zero is valid. */
    public static BigDecimal parsePrice(String text) {
        String value = trimmed(text);
        if (value.isEmpty() || value.length() > 18) throw invalidPrice();
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) < '0' || value.charAt(i) > '9') throw invalidPrice();
        }
        return new BigDecimal(value);
    }

    private static IllegalArgumentException invalidPrice() {
        return new IllegalArgumentException("Đơn giá: 1–18 chữ số (VND nguyên, giá 0 hợp lệ), không dấu phân cách.");
    }

    public static DichVu validate(DichVu value) {
        Objects.requireNonNull(value, "Dịch vụ");
        String id = validateId(value.maDichVu());
        String name = trimmed(value.tenDichVu());
        String description = trimmed(value.moTa());
        if (name.isEmpty() || name.length() > 100) throw new IllegalArgumentException("Tên dịch vụ dài 1–100 ký tự.");
        if (description.length() > 300) throw new IllegalArgumentException("Mô tả tối đa 300 ký tự.");
        BigDecimal price = value.donGia();
        if (price == null || price.signum() < 0 || price.stripTrailingZeros().scale() > 0
                || price.compareTo(new BigDecimal("999999999999999999")) > 0) throw invalidPrice();
        return new DichVu(id, name, price.setScale(0), description.isEmpty() ? null : description,
                value.laCanLamSang(), value.dangApDung());
    }

    public void save(DichVu value, boolean creating) throws SQLException {
        require();
        DichVu normalized = validate(value);
        mutate(normalized, creating, null);
    }

    public void changeActive(String id, boolean active) throws SQLException {
        require();
        mutate(new DichVu(validateId(id), null, null, null, false, active), false, active);
    }
    public void saveInformation(DichVu value,boolean creating)throws SQLException {
        require();
        DichVu normalized=validate(new DichVu(value.maDichVu(),value.tenDichVu(),BigDecimal.ZERO,value.moTa(),value.laCanLamSang(),value.dangApDung()));
        mutate(normalized,creating,null,"INFORMATION");
    }
    public void savePrice(DichVu value)throws SQLException {
        require();DichVu normalized=validate(value);mutate(normalized,false,null,"PRICE");
    }

    private void mutate(DichVu value, boolean creating, Boolean activeOnly) throws SQLException {
        mutate(value,creating,activeOnly,"ALL");
    }
    private void mutate(DichVu value, boolean creating, Boolean activeOnly,String fields) throws SQLException {
        boolean committed = false;
        try (Connection connection = connections.open()) {
            if (!connection.getAutoCommit()) throw new SQLException("Expected fresh auto-commit connection");
            connection.setAutoCommit(false);
            try {
                require();
                if (activeOnly != null) dao.changeActive(connection, value.maDichVu(), activeOnly);
                else if (creating) {
                    if (dao.exists(connection, value.maDichVu())) throw new IllegalArgumentException("Mã dịch vụ đã tồn tại.");
                    dao.insert(connection, value);
                } else if(fields.equals("INFORMATION"))dao.updateInformation(connection,value);
                else if(fields.equals("PRICE"))dao.updatePrice(connection,value);
                else dao.update(connection, value);
                dao.audit(connection, session.accountId(), value.maDichVu(), activeOnly == null
                        ? (creating ? "Thêm dịch vụ" : "Sửa dịch vụ")
                        : (activeOnly ? "Áp dụng lại" : "Ngừng áp dụng"));
                require();
                connection.commit();
                committed = true;
            } catch (SQLException | RuntimeException failure) {
                try { connection.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
                if (failure instanceof SQLException sql && isDuplicate(sql)) {
                    throw new IllegalArgumentException("Mã dịch vụ đã tồn tại (có thể do thao tác đồng thời).", failure);
                }
                throw failure;
            }
        } catch (SQLException failure) {
            if (committed) throw new CommittedWriteException(failure);
            throw failure;
        }
    }

    /** Acknowledged commit followed by close failure: callers must never ask the user to save again. */
    public static final class CommittedWriteException extends SQLException {
        public CommittedWriteException(SQLException cause) { super("Đã lưu; lỗi đóng kết nối.", cause); }
    }

    private static boolean isDuplicate(SQLException error) {
        for (SQLException next = error; next != null; next = next.getNextException()) {
            if (next.getErrorCode() == 2601 || next.getErrorCode() == 2627) return true;
        }
        return false;
    }

    private void require() { authorization.require(session, Permission.SERVICE_CATALOG_MANAGE); }
    private static String trimmed(String value) { return value == null ? "" : value.trim(); }
    private static String validateId(String text) {
        String id = trimmed(text);
        if (id.isEmpty() || id.length() > 10) throw new IllegalArgumentException("Mã dịch vụ dài 1–10 ký tự ASCII hiển thị, không khoảng trắng.");
        for (int i = 0; i < id.length(); i++) {
            if (id.charAt(i) < 33 || id.charAt(i) > 126) throw new IllegalArgumentException("Mã dịch vụ chỉ gồm ký tự ASCII hiển thị, không khoảng trắng.");
        }
        return id;
    }
}
