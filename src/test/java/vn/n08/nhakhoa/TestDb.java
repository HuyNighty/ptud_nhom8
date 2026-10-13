package vn.n08.nhakhoa;

import vn.n08.nhakhoa.config.Database;
import vn.n08.nhakhoa.config.DbConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CSDL THAY THẾ cho kiểm thử tự động: Apache Derby nhúng trong bộ nhớ.
 * KHÔNG phải SQL Server. Schema/seed Derby được dịch tự động từ sql/02 và sql/03
 * (bỏ chỉ mục lọc, đổi kiểu NVARCHAR/DATETIME2/BIT). Ngày {{D:n}} = ngày chạy + n.
 * Mục đích: chạy thật toàn bộ câu SQL của DAO và luồng Service qua JDBC khi máy không có SQL Server.
 */
public final class TestDb {
    private static final Pattern DAY = Pattern.compile("\\{\\{D:(-?\\d+)}}");

    private TestDb() {}

    public static void init(String name) throws Exception {
        String url = "jdbc:derby:memory:" + name + ";create=true";
        try (Connection c = DriverManager.getConnection(url)) {
            run(c, Path.of("src/test/resources/derby/schema.sql"));
            run(c, Path.of("src/test/resources/derby/seed.sql"));
        }
        Database.init(DbConfig.of("jdbc:derby:memory:" + name, "", ""));
    }

    private static void run(Connection c, Path file) throws Exception {
        String all = Files.readString(file, StandardCharsets.UTF_8);
        Matcher m = DAY.matcher(all);
        StringBuilder sb = new StringBuilder();
        LocalDate today = LocalDate.now();
        while (m.find()) m.appendReplacement(sb, today.plusDays(Integer.parseInt(m.group(1))).toString());
        m.appendTail(sb);
        int n = 0;
        for (String s : sb.toString().split(";\\s*\\n")) {
            s = s.trim();
            if (s.isEmpty()) continue;
            n++;
            try (Statement st = c.createStatement()) {
                st.execute(s);
            } catch (SQLException e) {
                throw new IllegalStateException(file + " câu " + n + ": " + e.getMessage() + "\n" + s, e);
            }
        }
    }
}
