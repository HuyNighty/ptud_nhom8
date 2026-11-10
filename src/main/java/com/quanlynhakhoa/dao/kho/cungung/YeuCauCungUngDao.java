package com.quanlynhakhoa.dao.kho.cungung;

import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import com.quanlynhakhoa.dao.kho.quytrinh.QuyTrinhKhoDao;
import com.quanlynhakhoa.model.kho.PhanBoLo;
import com.quanlynhakhoa.model.kho.DongYeuCauCungUng;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persistence for the additive V6 request schema and existing V5 issue tables. */
public final class YeuCauCungUngDao {
    public List<Object[]> danhSachCuaToi(String accountId) throws SQLException {
        return queryList("WHERE y.maTaiKhoanTao=?", accountId);
    }

    public List<Object[]> danhSachChoKho() throws SQLException {
        return queryList("", null);
    }

    private List<Object[]> queryList(String filter, String accountId) throws SQLException {
        String sql = "SELECT TOP (200) y.maYeuCau,y.thoiGianTao,nv.hoTen,y.trangThai,y.lyDo,COUNT(d.soThuTu) "
                + "FROM dbo.YeuCauCapPhat y INNER JOIN dbo.NhanVien nv ON nv.maNhanVien=y.maNhanVienYeuCau "
                + "LEFT JOIN dbo.ChiTietYeuCauCapPhat d ON d.maYeuCau=y.maYeuCau "
                + (filter.isEmpty() ? "" : filter + " ")
                + "GROUP BY y.maYeuCau,y.thoiGianTao,nv.hoTen,y.trangThai,y.lyDo ORDER BY y.thoiGianTao DESC";
        List<Object[]> rows = new ArrayList<>();
        try (Connection con = TeamDevConnection.open();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            if (accountId != null) stmt.setString(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{rs.getString(1), rs.getTimestamp(2), rs.getNString(3), rs.getString(4), rs.getNString(5), rs.getInt(6)});
                }
            }
        }
        return rows;
    }

    public List<Object[]> chiTiet(String requestId, String accountId, boolean manager) throws SQLException {
        String sql = "SELECT d.soThuTu,CASE WHEN d.maThuoc IS NULL THEN 'VATTU' ELSE 'THUOC' END AS loaiHang,"
                + "COALESCE(d.maThuoc,d.maVatTu) AS maHang,COALESCE(t.tenThuoc,v.tenVatTu) AS tenHang,"
                + "d.soLuongYeuCau,d.maLoCapPhat,d.soLuongCapPhat FROM dbo.YeuCauCapPhat y "
                + "INNER JOIN dbo.ChiTietYeuCauCapPhat d ON d.maYeuCau=y.maYeuCau "
                + "LEFT JOIN dbo.Thuoc t ON t.maThuoc=d.maThuoc LEFT JOIN dbo.VatTu v ON v.maVatTu=d.maVatTu "
                + "WHERE y.maYeuCau=?" + (manager ? "" : " AND y.maTaiKhoanTao=?") + " ORDER BY d.soThuTu";
        List<Object[]> rows = new ArrayList<>();
        try (Connection con = TeamDevConnection.open();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, requestId);
            if (!manager) stmt.setString(2, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getNString(4), rs.getInt(5), rs.getString(6), rs.getInt(7)});
                }
            }
        }
        return rows;
    }

    public void taoYeuCau(Connection con, String accountId, String requestId, String reason, List<DongYeuCauCungUng> lines) throws SQLException {
        String employeeId = QuyTrinhKhoDao.layMaNhanVien(con, accountId);
        try (PreparedStatement stmt = con.prepareStatement("INSERT INTO dbo.YeuCauCapPhat "
                + "(maYeuCau,thoiGianTao,maNhanVienYeuCau,maTaiKhoanTao,lyDo,trangThai) VALUES (?,SYSUTCDATETIME(),?,?,?,'CHO_DUYET')")) {
            stmt.setString(1, requestId);
            stmt.setString(2, employeeId);
            stmt.setString(3, accountId);
            stmt.setNString(4, reason);
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = con.prepareStatement("INSERT INTO dbo.ChiTietYeuCauCapPhat "
                + "(maYeuCau,soThuTu,maThuoc,maVatTu,soLuongYeuCau) VALUES (?,?,?,?,?)")) {
            short index = 1;
            for (DongYeuCauCungUng line : lines) {
                stmt.setString(1, requestId);
                stmt.setShort(2, index++);
                stmt.setString(3, "THUOC".equals(line.itemType()) ? line.itemCode() : null);
                stmt.setString(4, "VATTU".equals(line.itemType()) ? line.itemCode() : null);
                stmt.setInt(5, line.quantity());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
        QuyTrinhKhoDao.ghiNhatKy(con, accountId, "Tạo yêu cầu cấp phát", "YeuCauCapPhat", requestId);
    }

    public void duyet(Connection con, String accountId, String requestId) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement("UPDATE dbo.YeuCauCapPhat SET trangThai='DA_DUYET',maTaiKhoanXuLy=?,thoiGianXuLy=SYSUTCDATETIME() "
                + "WHERE maYeuCau=? AND trangThai='CHO_DUYET' AND maTaiKhoanTao<>?")) {
            stmt.setString(1, accountId);
            stmt.setString(2, requestId);
            stmt.setString(3, accountId);
            if (stmt.executeUpdate() != 1) throw new IllegalArgumentException("Yêu cầu không còn chờ duyệt hoặc bạn không thể tự duyệt yêu cầu của mình.");
        }
        QuyTrinhKhoDao.ghiNhatKy(con, accountId, "Duyệt yêu cầu cấp phát", "YeuCauCapPhat", requestId);
    }

    public void tuChoi(Connection con, String accountId, String requestId, String reason) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement("UPDATE dbo.YeuCauCapPhat SET trangThai='TU_CHOI',maTaiKhoanXuLy=?,thoiGianXuLy=SYSUTCDATETIME(),ghiChuXuLy=? "
                + "WHERE maYeuCau=? AND trangThai='CHO_DUYET' AND maTaiKhoanTao<>?")) {
            stmt.setString(1, accountId);
            stmt.setNString(2, reason);
            stmt.setString(3, requestId);
            stmt.setString(4, accountId);
            if (stmt.executeUpdate() != 1) throw new IllegalArgumentException("Yêu cầu không còn chờ duyệt hoặc bạn không thể tự từ chối yêu cầu của mình.");
        }
        QuyTrinhKhoDao.ghiNhatKy(con, accountId, "Từ chối yêu cầu cấp phát", "YeuCauCapPhat", requestId);
    }

    public void capPhat(Connection con, String accountId, String requestId, String voucherId,
                        String department, String receiver, String reason, List<PhanBoLo> allocations) throws SQLException {
        String employeeId = QuyTrinhKhoDao.layMaNhanVien(con, accountId);
        try (PreparedStatement lock = con.prepareStatement("SELECT trangThai,maTaiKhoanTao FROM dbo.YeuCauCapPhat WITH (UPDLOCK,HOLDLOCK) WHERE maYeuCau=?")) {
            lock.setString(1, requestId);
            try (ResultSet rs = lock.executeQuery()) {
                if (!rs.next() || !"DA_DUYET".equals(rs.getString(1))) throw new IllegalArgumentException("Yêu cầu phải được duyệt trước khi cấp phát.");
                if (accountId.equals(rs.getString(2))) throw new IllegalArgumentException("Người tạo không được tự cấp phát yêu cầu của mình.");
            }
        }
        Map<Integer, Object[]> lines = new LinkedHashMap<>();
        try (PreparedStatement stmt = con.prepareStatement("SELECT soThuTu,maThuoc,maVatTu,soLuongYeuCau FROM dbo.ChiTietYeuCauCapPhat WITH (UPDLOCK,HOLDLOCK) WHERE maYeuCau=? ORDER BY soThuTu")) {
            stmt.setString(1, requestId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lines.put(rs.getInt(1), new Object[]{rs.getString(2), rs.getString(3), rs.getInt(4)});
            }
        }
        if (lines.isEmpty() || allocations.size() != lines.size()) throw new IllegalArgumentException("Cần phân bổ đầy đủ tất cả các dòng yêu cầu.");
        Map<Integer, PhanBoLo> byLine = new LinkedHashMap<>();
        for (PhanBoLo a : allocations) {
            if (byLine.put(a.lineNo(), a) != null) throw new IllegalArgumentException("Dòng phân bổ bị lặp.");
        }
        if (!byLine.keySet().equals(lines.keySet())) throw new IllegalArgumentException("Mỗi dòng yêu cầu phải có đúng một lô cấp phát.");

        Map<String, Integer> qtyByLot = new LinkedHashMap<>();
        for (Map.Entry<Integer, Object[]> entry : lines.entrySet()) {
            int lineNo = entry.getKey();
            Object[] line = entry.getValue();
            PhanBoLo allocation = byLine.get(lineNo);
            String lotId = allocation.lotCode();
            String expectedThuoc = (String) line[0];
            String expectedVatTu = (String) line[1];
            int qty = (Integer) line[2];

            try (PreparedStatement stmt = con.prepareStatement("SELECT maThuoc,maVatTu,soLuongTon FROM dbo.LoHang WITH (UPDLOCK,ROWLOCK) WHERE maLo=?")) {
                stmt.setString(1, lotId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy lô " + lotId + ".");
                    if (!java.util.Objects.equals(expectedThuoc, rs.getString(1)) || !java.util.Objects.equals(expectedVatTu, rs.getString(2))) {
                        throw new IllegalArgumentException("Lô " + lotId + " không khớp với mặt hàng ở dòng " + lineNo + ".");
                    }
                    if (rs.getInt(3) < qty) throw new IllegalArgumentException("Lô " + lotId + " không đủ tồn cho dòng " + lineNo + ".");
                }
            }
            try (PreparedStatement stmt = con.prepareStatement("UPDATE dbo.LoHang SET soLuongTon=soLuongTon-? WHERE maLo=? AND soLuongTon>=? AND (hanSuDung IS NULL OR hanSuDung>=CONVERT(date, SYSDATETIME()))")) {
                stmt.setInt(1, qty);
                stmt.setString(2, lotId);
                stmt.setInt(3, qty);
                if (stmt.executeUpdate() != 1) throw new IllegalArgumentException("Tồn kho vừa thay đổi; tải lại và thử lại.");
            }
            try (PreparedStatement stmt = con.prepareStatement("UPDATE dbo.ChiTietYeuCauCapPhat SET maLoCapPhat=?,soLuongCapPhat=? WHERE maYeuCau=? AND soThuTu=? AND soLuongCapPhat=0")) {
                stmt.setString(1, lotId);
                stmt.setInt(2, qty);
                stmt.setString(3, requestId);
                stmt.setInt(4, lineNo);
                if (stmt.executeUpdate() != 1) throw new IllegalArgumentException("Dòng yêu cầu đã được cấp phát trước đó.");
            }
            qtyByLot.merge(lotId, qty, Integer::sum);
        }
        try (PreparedStatement stmt = con.prepareStatement("INSERT INTO dbo.PhieuXuatKho (maPhieuXuat,ngayXuat,boPhanNhan,nguoiNhan,lyDo,trangThai,maNhanVien) VALUES (?,?,?,?,?,'HOAN_TAT',?)")) {
            stmt.setString(1, voucherId);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setNString(3, department);
            stmt.setNString(4, receiver);
            stmt.setNString(5, reason);
            stmt.setString(6, employeeId);
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = con.prepareStatement("INSERT INTO dbo.ChiTietPhieuXuat (maPhieuXuat,maLo,soLuong) VALUES (?,?,?)")) {
            for (Map.Entry<String, Integer> entry : qtyByLot.entrySet()) {
                stmt.setString(1, voucherId);
                stmt.setString(2, entry.getKey());
                stmt.setInt(3, entry.getValue());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
        try (PreparedStatement stmt = con.prepareStatement("UPDATE dbo.YeuCauCapPhat SET trangThai='DA_CAP_PHAT',maTaiKhoanXuLy=?,thoiGianXuLy=SYSUTCDATETIME(),maPhieuXuat=? WHERE maYeuCau=? AND trangThai='DA_DUYET'")) {
            stmt.setString(1, accountId);
            stmt.setString(2, voucherId);
            stmt.setString(3, requestId);
            if (stmt.executeUpdate() != 1) throw new IllegalArgumentException("Trạng thái yêu cầu đã thay đổi; không thể ghi phiếu xuất.");
        }
        QuyTrinhKhoDao.ghiNhatKy(con, accountId, "Cấp phát theo yêu cầu", "YeuCauCapPhat", requestId);
    }
}
