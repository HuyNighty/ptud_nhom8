package com.quanlynhakhoa.quantri.vaitro.catalog;

import com.quanlynhakhoa.entity.VaiTro;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Read-only V8 catalog. Caller supplies a verified V8 connection and owns its
 * transaction/lifecycle. No implementation is wired to application runtime in C2A.
 */
public interface RoleRepository {
    Optional<VaiTro> findByCode(Connection connection, String code) throws SQLException;
    /** Includes inactive and zero-grant roles; sorted by code, not a grant decision. */
    List<VaiTro> findAll(Connection connection) throws SQLException;
}

