package com.quanlynhakhoa.service.hoadon;

import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.nghiepvu.dichvu.dao.ServiceCatalogDao;
import java.sql.Connection;

/** Opt-in read-only check of empty-search behavior against the actual SQL Server. */
public final class ServiceCatalogReadSqlIT {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].equals("--read-only") || !TeamDevConnection.hasLocalConfig()) {
            throw new IllegalArgumentException("Requires --read-only and existing local dev config");
        }
        try (Connection c = TeamDevConnection.open()) {
            ServiceCatalogDao dao = new ServiceCatalogDao();
            for (Boolean active : new Boolean[]{null, true, false}) {
                String sql = "SELECT COUNT(*) FROM dbo.DichVu"
                        + (active == null ? "" : " WHERE dangApDung=" + (active ? "1" : "0"));
                try (var s = c.createStatement(); var r = s.executeQuery(sql)) {
                    r.next();
                    int expected = r.getInt(1);
                    int actual = dao.find(c, "", active).size();
                    if (actual != expected) throw new AssertionError("Empty search: expected " + expected + ", got " + actual);
                    System.out.println("PASS empty search, active=" + active + ": " + actual + " services");
                }
            }
        }
    }
}
