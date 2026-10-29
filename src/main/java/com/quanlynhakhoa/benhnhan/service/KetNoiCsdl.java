package com.quanlynhakhoa.benhnhan.service;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Nguồn cấp kết nối JDBC cho tính năng Bệnh nhân.
 * Khi chạy thật dùng TeamDevConnection::open (tài khoản n08dev_*, không phải sa).
 */
@FunctionalInterface
public interface KetNoiCsdl {
    Connection moKetNoi() throws SQLException;
}
