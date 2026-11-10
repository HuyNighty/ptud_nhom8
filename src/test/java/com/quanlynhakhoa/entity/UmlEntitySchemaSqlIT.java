package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Read-only schema verification for the eight entities added from the class diagram. */
public final class UmlEntitySchemaSqlIT {
    private static int checks;
    private static void check(boolean ok, String why) { checks++; if (!ok) throw new AssertionError(why); }
    private static Class<?> javaType(String sqlType) {
        return switch (sqlType.toLowerCase(Locale.ROOT)) {
            case "varchar", "nvarchar", "char", "nchar" -> String.class;
            case "date" -> LocalDate.class;
            case "datetime2", "datetime", "smalldatetime" -> LocalDateTime.class;
            case "decimal", "numeric" -> BigDecimal.class;
            case "int" -> int.class;
            case "bit" -> boolean.class;
            default -> throw new AssertionError("Unsupported schema type " + sqlType);
        };
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].equals("--read-only"))
            throw new IllegalArgumentException("Pass --read-only to verify the approved development schema");
        DanhGiaNhanSu review = new DanhGiaNhanSu("QA_DG", "QA_NV", "QA_QL", "2026-11",
                LocalDate.of(2026,11,10), null, null, "NHAP");
        check(review.getDiem() == null, "Draft score is optional");
        for (String valid : List.of("1", "4.25", "5")) {
            review.setDiem(new BigDecimal(valid));
            check(review.getDiem().equals(new BigDecimal(valid)), "Score precision must be preserved");
        }
        for (String invalid : List.of("0.99", "5.01")) {
            try { review.setDiem(new BigDecimal(invalid)); throw new AssertionError("Invalid score accepted"); }
            catch (IllegalArgumentException expected) { checks++; }
            check(review.getDiem().equals(new BigDecimal("5")), "Rejected score must not mutate the entity");
        }
        review.setNhanXet("Nhận xét thử"); review.setDiem(null);
        check(review.getTrangThai().equals("NHAP") && review.getMaNguoiDanhGia().equals("QA_QL"),
                "Editing review content must not change workflow state or evaluator");
        Class<?>[] entities = {PhieuNhapKho.class, ChiTietPhieuNhap.class, PhieuXuatKho.class,
                ChiTietPhieuXuat.class, KeHoachDieuTri.class, ChiTietDieuTri.class,
                BanGiaoCa.class, DanhGiaNhanSu.class};
        int columns = 0;
        try (Connection connection = TeamDevConnection.open()) {
            for (Class<?> entity : entities) {
                Set<String> fields = new HashSet<>();
                for (Field field : entity.getDeclaredFields())
                    if (!Modifier.isStatic(field.getModifiers())) fields.add(field.getName());
                Set<String> seen = new HashSet<>();
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT c.name,t.name,c.is_nullable,c.precision,c.scale
                        FROM sys.columns c JOIN sys.types t ON t.user_type_id=c.user_type_id
                        WHERE c.object_id=OBJECT_ID(?) ORDER BY c.column_id
                        """)) {
                    statement.setQueryTimeout(10);
                    statement.setString(1, "dbo." + entity.getSimpleName());
                    try (ResultSet rows = statement.executeQuery()) {
                        while (rows.next()) {
                            String name = rows.getString(1);
                            Field field = entity.getDeclaredField(name);
                            check(Modifier.isPrivate(field.getModifiers()), "Entity field must be private: " + name);
                            check(field.getType().equals(javaType(rows.getString(2))), "SQL/Java type mismatch: " + entity.getSimpleName() + "." + name);
                            check(!rows.getBoolean(3) || !field.getType().isPrimitive(), "Nullable SQL column needs a reference type: " + name);
                            String getter = (field.getType() == boolean.class ? "is" : "get")
                                    + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                            check(entity.getMethod(getter).getReturnType().equals(field.getType()), "Getter type mismatch: " + name);
                            if (name.equals("donGiaNhap")) check(rows.getInt(4) == 18 && rows.getInt(5) == 0, "Receipt price precision changed");
                            if (entity == DanhGiaNhanSu.class && name.equals("diem"))
                                check(rows.getInt(4) == 3 && rows.getInt(5) == 2, "Review score precision changed");
                            seen.add(name); columns++;
                        }
                    }
                }
                check(!seen.isEmpty() && seen.equals(fields), "Entity columns differ from actual table: " + entity.getSimpleName());
                System.out.println("PASS schema " + entity.getSimpleName() + ": " + seen.size() + " columns");
            }
        }
        System.out.println("PASS UmlEntitySchemaSqlIT: " + entities.length + " entities, " + columns + " columns, "
                + checks + " checks; read-only metadata and synthetic score validation, no business data printed.");
    }
}
