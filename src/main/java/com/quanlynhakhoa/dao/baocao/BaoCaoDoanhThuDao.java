package com.quanlynhakhoa.dao.baocao;

import com.quanlynhakhoa.model.baocao.DongTongHopDoanhThu;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Chỉ đọc bằng connection/transaction do Service sở hữu. */
public final class BaoCaoDoanhThuDao {
    public List<DongTongHopDoanhThu> load(Connection c, LocalDate from, LocalDate to) throws SQLException {
                String sql = """
                        WITH InvoiceTotals AS (
                          SELECT maHoaDon,SUM(CAST(soLuong AS decimal(10,0))*donGiaApDung) AS total
                          FROM dbo.ChiTietHoaDon GROUP BY maHoaDon
                        ), ActiveInvoices AS (
                          SELECT h.maHoaDon,h.ngayLap,COALESCE(d.total,0) AS total FROM dbo.HoaDon h
                          LEFT JOIN InvoiceTotals d ON d.maHoaDon=h.maHoaDon
                          WHERE h.ngayLap<? AND (h.trangThai<>'DA_HUY' OR EXISTS (
                            SELECT 1 FROM dbo.YeuCauHuyHoaDon y WHERE y.maHoaDon=h.maHoaDon
                            AND y.trangThai='DA_DUYET' AND y.thoiGianDuyet>=?))
                        ), ValidPayments AS (
                          SELECT maHoaDon,thoiGian,soTien FROM dbo.DongTienHoaDon
                          WHERE thoiGian<? AND ? IS NOT NULL
                        )
                        SELECT 'REVENUE' AS kind,CAST(ngayLap AS date) AS day,COUNT(*) AS invoices,SUM(total) AS amount
                          FROM ActiveInvoices WHERE ngayLap>=? GROUP BY CAST(ngayLap AS date)
                        UNION ALL
                        SELECT 'COLLECTED',CAST(thoiGian AS date),0,SUM(soTien)
                          FROM ValidPayments WHERE thoiGian>=? GROUP BY CAST(thoiGian AS date)
                        UNION ALL
                        SELECT 'DEBT',CAST(NULL AS date),0,COALESCE(SUM(a.total-COALESCE(p.paid,0)),0)
                          FROM ActiveInvoices a LEFT JOIN
                          (SELECT maHoaDon,SUM(soTien) AS paid FROM ValidPayments GROUP BY maHoaDon) p ON p.maHoaDon=a.maHoaDon
                        """;
        List<DongTongHopDoanhThu> result = new ArrayList<>();
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setQueryTimeout(30);
            Timestamp cutoff = Timestamp.valueOf(to.plusDays(1).atStartOfDay());
            for (int i = 1; i <= 4; i++) s.setTimestamp(i, cutoff);
            s.setTimestamp(5, Timestamp.valueOf(from.atStartOfDay()));
            s.setTimestamp(6, Timestamp.valueOf(from.atStartOfDay()));
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    BigDecimal amount = r.getBigDecimal("amount");
                    String kind = r.getString("kind");
                    if (kind.equals("DEBT")) {
                        result.add(new DongTongHopDoanhThu(kind, null, 0, amount));
                    } else {
                        result.add(new DongTongHopDoanhThu(kind, r.getDate("day").toLocalDate(),
                                r.getInt("invoices"), amount));
                    }
                }
            }
        }
        return List.copyOf(result);
    }
}
