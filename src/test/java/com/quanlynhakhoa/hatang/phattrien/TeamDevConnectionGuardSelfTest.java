package com.quanlynhakhoa.hatang.phattrien;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/** Real config parsing + synthetic JDBC driver. No SQL Server driver/network/database. */
public final class TeamDevConnectionGuardSelfTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static final String DB = "N08Dev_0123abcd";
    private static final String USER = "n08dev_0123abcd";
    private static Path config;

    private static void write(String database, String user, String port, String password) throws Exception {
        Properties properties = new Properties();
        if (database != null) properties.setProperty("dev.database", database);
        if (user != null) properties.setProperty("dev.user", user);
        if (port != null) properties.setProperty("dev.port", port);
        if (password != null) properties.setProperty("dev.password", password);
        try (var writer = Files.newBufferedWriter(config, StandardCharsets.UTF_8)) {
            properties.store(writer, "Synthetic test only");
        }
    }
    private static void rejectBeforeJdbc(FakeDriver driver, String db, String user, String port, String password)
            throws Exception {
        write(db, user, port, password);
        int before = driver.connections;
        try {
            TeamDevConnection.open();
            throw new AssertionError("Invalid config accepted");
        } catch (SQLException expected) {
            check(driver.connections == before, "Invalid config must not reach JDBC connect");
        }
    }

    private static final class FakeDriver implements Driver {
        int connections, closes, statementCloses, resultCloses, timeout;
        String url, user, returnedDatabase = DB;
        boolean emptyIdentity, failQuery;
        @Override public Connection connect(String url, Properties properties) {
            if (!acceptsURL(url)) return null;
            this.url = url; user = properties.getProperty("user"); connections++;
            check(properties.getProperty("password").equals("synthetic-only"), "Synthetic credential binding");
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Connection.class}, (p, method, args) -> {
                        return switch (method.getName()) {
                            case "close" -> { closes++; yield null; }
                            case "prepareStatement" -> {
                                check(args[0].equals("SELECT DB_NAME()"), "Identity check query retained");
                                yield statement();
                            }
                            default -> throw new AssertionError("Unexpected connection call: " + method.getName());
                        };
                    });
        }
        private PreparedStatement statement() {
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class}, (p, method, args) -> {
                        return switch (method.getName()) {
                            case "setQueryTimeout" -> { timeout = (int) args[0]; yield null; }
                            case "close" -> { statementCloses++; yield null; }
                            case "executeQuery" -> {
                                if (failQuery) throw new SQLException("Synthetic identity query failure");
                                yield result();
                            }
                            default -> throw new AssertionError(method.getName());
                        };
                    });
        }
        private ResultSet result() {
            return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{ResultSet.class}, (p, method, args) -> {
                        return switch (method.getName()) {
                            case "next" -> !emptyIdentity;
                            case "getString" -> returnedDatabase;
                            case "close" -> { resultCloses++; yield null; }
                            default -> throw new AssertionError(method.getName());
                        };
                    });
        }
        @Override public boolean acceptsURL(String url) { return url.startsWith("jdbc:sqlserver://127.0.0.1:"); }
        @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new DriverPropertyInfo[0]; }
        @Override public int getMajorVersion() { return 1; }
        @Override public int getMinorVersion() { return 0; }
        @Override public boolean jdbcCompliant() { return false; }
        @Override public Logger getParentLogger() { return Logger.getLogger("synthetic.jdbc"); }
    }

    public static void main(String[] args) throws Exception {
        String previous = System.getProperty("user.dir");
        Path directory = Files.createTempDirectory("n08-config-guard-");
        config = directory.resolve(".n08-dev.local.properties");
        FakeDriver driver = new FakeDriver();
        DriverManager.registerDriver(driver);
        try {
            // Fail closed rather than risk another installed driver opening a real connection.
            for (Driver installed : Collections.list(DriverManager.getDrivers()))
                check(installed == driver, "Run with test/classes only, no real JDBC drivers");
            System.setProperty("user.dir", directory.toString());
            check(TeamDevConnection.configPath().equals(config), "Isolated synthetic config path");
            for (String invalid : new String[]{null, "", "N08Dev", "N08Dev_", "N08Dev_0123abc",
                    "N08Dev_0123abcde", "N08Dev_0123ABCd", "N08Dev_0123abcz",
                    "Other_0123abcd", "N08Dev_0123abcd;serverName=remote"}) {
                rejectBeforeJdbc(driver, invalid, USER, "1433", "synthetic-only");
            }
            for (String invalid : new String[]{null, "", "n08dev", "n08dev_", "n08dev_0123abc",
                    "n08dev_0123abcde", "n08dev_0123ABCd", "n08dev_0123abcz", "other_0123abcd"}) {
                rejectBeforeJdbc(driver, DB, invalid, "1433", "synthetic-only");
            }
            rejectBeforeJdbc(driver, "N08Dev", "n08dev", "1433", "synthetic-only");
            rejectBeforeJdbc(driver, DB, "n08dev_0123abce", "1433", "synthetic-only");
            for (String port : new String[]{null, "", "abc", "0", "-1", "65536"})
                rejectBeforeJdbc(driver, DB, USER, port, "synthetic-only");
            for (String password : new String[]{null, "", " "})
                rejectBeforeJdbc(driver, DB, USER, "1433", password);

            for (String port : new String[]{"1", "1433", "65535"}) {
                write(DB, USER, port, "synthetic-only");
                int closed = driver.closes;
                try (Connection ignored = TeamDevConnection.open()) {
                    check(driver.closes == closed, "Success connection owned by caller");
                    check(driver.url.startsWith("jdbc:sqlserver://127.0.0.1:" + port + ";"), "Loopback restriction");
                    check(driver.url.contains(";databaseName=" + DB + ";"), "Expected database URL");
                    check(driver.user.equals(USER) && driver.timeout == 5, "User and query timeout");
                }
                check(driver.closes == closed + 1, "Caller cleanup");
            }
            write(DB, USER, "1433", "synthetic-only");
            for (int failure = 0; failure < 3; failure++) {
                driver.returnedDatabase = failure == 0 ? "N08Dev_deadbeef" : DB;
                driver.emptyIdentity = failure == 1;
                driver.failQuery = failure == 2;
                int closed = driver.closes;
                try {
                    TeamDevConnection.open();
                    throw new AssertionError("Invalid DB identity/query accepted");
                } catch (SQLException expected) {
                    check(driver.closes == closed + 1, "Identity failure closes connection");
                }
            }
            check(driver.statementCloses == driver.connections, "Statements closed on success/failure");
            check(driver.resultCloses == driver.connections - 1, "All created result sets closed");
            System.out.println("PASS: TeamDevConnection guards (" + checks
                    + " checks; synthetic driver/config, no SQL Server/network).");
        } finally {
            System.setProperty("user.dir", previous);
            DriverManager.deregisterDriver(driver);
            Files.deleteIfExists(config);
            Files.deleteIfExists(directory);
        }
    }
}

