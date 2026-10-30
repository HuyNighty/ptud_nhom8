package com.quanlynhakhoa.billing.service;

import java.sql.Connection;
import java.sql.SQLException;

/** Only wire an approved generator. No MAX+1, unapproved counter writes or retries. */
public interface InvoiceIdGenerator {
    String next(Connection connection) throws SQLException;
}
