package com.quanlynhakhoa.billing.dao;

import java.sql.Connection;
import java.sql.SQLException;

/** Supplies a fresh, owned connection (normally TeamDevConnection.open). */
@FunctionalInterface
public interface ConnectionProvider {
    Connection open() throws SQLException;
}
