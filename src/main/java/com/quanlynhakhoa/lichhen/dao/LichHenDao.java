package com.quanlynhakhoa.lichhen.dao;

import com.quanlynhakhoa.benhnhan.dao.BenhNhanDao;
import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.lichhen.model.BacSi;
import com.quanlynhakhoa.lichhen.model.LichHen;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

public class LichHenDao {
    private final BenhNhanDao.ConnectionProvider cachMoKetNoi;

    public LichHenDao() {
        this(TeamDevConnection::open);
    }

    public LichHenDao(BenhNhanDao.ConnectionProvider cachMoKetNoi) {
        this.cachMoKetNoi = cachMoKetNoi;
    }

    public Connection moKetNoi() throws SQLException {
        return cachMoKetNoi.open();
    }

    public ArrayList<LichHen> layDanhSachTheoNgay(LocalDate ngay, String tuKhoa) throws SQLException {
        ArrayList<LichHen> danhSachLichHen = new ArrayList<>();
        String sql = "SELECT TOP 500 l.maLichHen, l.thoiGianHen, l.maBenhNhan, b.hoTen, b.soDienThoai, "
                + "l.maNhanVien, n.hoTen, l.lyDoHen, l.trangThai, l.maLichHenGoc, l.lyDoHuy "
                + "FROM dbo.LichHen AS l "
                + "JOIN dbo.BenhNhan AS b ON b.maBenhNhan = l.maBenhNhan "
                + "JOIN dbo.NhanVien AS n ON n.maNhanVien = l.maNhanVien "
                + "WHERE l.thoiGianHen >= ? AND l.thoiGianHen < ? "
                + "AND (? = '' OR l.maLichHen LIKE ? OR l.maBenhNhan LIKE ? "
                + "     OR b.hoTen LIKE ? OR b.soDienThoai LIKE ?) "
                + "ORDER BY l.thoiGianHen, l.maLichHen";

        Timestamp dauNgay = Timestamp.valueOf(ngay.atStartOfDay());
        Timestamp dauNgayHomSau = Timestamp.valueOf(ngay.plusDays(1).atStartOfDay());
        String mauTim = "%" + BenhNhanDao.doiKyTuDacBietThanhKyTuThuong(tuKhoa) + "%";

        try (Connection ketNoi = cachMoKetNoi.open();
             PreparedStatement cauLenh = BenhNhanDao.taoCauLenh(ketNoi, sql)) {
            cauLenh.setTimestamp(1, dauNgay);
            cauLenh.setTimestamp(2, dauNgayHomSau);
            cauLenh.setString(3, tuKhoa);
            cauLenh.setString(4, mauTim);
            cauLenh.setString(5, mauTim);
            cauLenh.setNString(6, mauTim);
            cauLenh.setString(7, mauTim);
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                while (ketQua.next()) {
                    LichHen lichHen = new LichHen(ketQua.getString(1));
                    lichHen.setThoiGianHen(ketQua.getTimestamp(2).toLocalDateTime());
                    lichHen.setMaBenhNhan(ketQua.getString(3));
                    lichHen.setTenBenhNhan(ketQua.getNString(4));
                    lichHen.setSoDienThoaiBenhNhan(ketQua.getString(5));
                    lichHen.setMaBacSi(ketQua.getString(6));
                    lichHen.setTenBacSi(ketQua.getNString(7));
                    lichHen.setLyDoHen(ketQua.getNString(8));
                    lichHen.setTrangThai(ketQua.getString(9));
                    lichHen.setMaLichHenGoc(ketQua.getString(10));
                    lichHen.setLyDoHuy(ketQua.getNString(11));
                    danhSachLichHen.add(lichHen);
                }
            }
        }
        return danhSachLichHen;
    }

    public ArrayList<BacSi> layDanhSachBacSi() throws SQLException {
        ArrayList<BacSi> danhSachBacSi = new ArrayList<>();
        String sql = "SELECT maNhanVien, hoTen FROM dbo.NhanVien "
                + "WHERE chucDanh = 'BS' AND trangThai = 'DANG_LAM_VIEC' "
                + "ORDER BY hoTen";
        try (Connection ketNoi = cachMoKetNoi.open();
             PreparedStatement cauLenh = BenhNhanDao.taoCauLenh(ketNoi, sql);
             ResultSet ketQua = cauLenh.executeQuery()) {
            while (ketQua.next()) {
                danhSachBacSi.add(new BacSi(ketQua.getString(1), ketQua.getNString(2)));
            }
        }
        return danhSachBacSi;
    }

    public ArrayList<LocalTime[]> layCaLamViec(Connection ketNoi, String maBacSi, LocalDate ngay)
            throws SQLException {
        ArrayList<LocalTime[]> danhSachCa = new ArrayList<>();
        String sql = "SELECT c.gioBatDau, c.gioKetThuc "
                + "FROM dbo.PhanCong AS p "
                + "JOIN dbo.CaLamViec AS c ON c.maCa = p.maCa "
                + "WHERE p.maNhanVien = ? AND p.ngayLam = ? AND p.trangThai <> 'DA_HUY' "
                + "ORDER BY c.gioBatDau";
        try (PreparedStatement cauLenh = BenhNhanDao.taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maBacSi);
            cauLenh.setDate(2, java.sql.Date.valueOf(ngay));
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                while (ketQua.next()) {
                    Time gioBatDau = ketQua.getTime(1);
                    Time gioKetThuc = ketQua.getTime(2);
                    LocalTime[] motCa = new LocalTime[2];
                    motCa[0] = gioBatDau.toLocalTime();
                    motCa[1] = gioKetThuc.toLocalTime();
                    danhSachCa.add(motCa);
                }
            }
        }
        return danhSachCa;
    }

    public ArrayList<LocalDateTime> layGioDaHen(Connection ketNoi, String maBacSi, LocalDate ngay,
                                                String maBoQua) throws SQLException {
        ArrayList<LocalDateTime> danhSachGioDaHen = new ArrayList<>();
        String sql = "SELECT thoiGianHen FROM dbo.LichHen "
                + "WHERE maNhanVien = ? AND thoiGianHen >= ? AND thoiGianHen < ? "
                + "AND trangThai NOT IN ('DA_HUY', 'DA_DOI_LICH') "
                + "AND (? IS NULL OR maLichHen <> ?)";
        try (PreparedStatement cauLenh = BenhNhanDao.taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maBacSi);
            cauLenh.setTimestamp(2, Timestamp.valueOf(ngay.atStartOfDay()));
            cauLenh.setTimestamp(3, Timestamp.valueOf(ngay.plusDays(1).atStartOfDay()));
            cauLenh.setString(4, maBoQua);
            cauLenh.setString(5, maBoQua);
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                while (ketQua.next()) {
                    danhSachGioDaHen.add(ketQua.getTimestamp(1).toLocalDateTime());
                }
            }
        }
        return danhSachGioDaHen;
    }

    public int demLichChongGio(Connection ketNoi, String maBacSi, LocalDateTime batDau, LocalDateTime ketThuc,
                               int thoiLuongPhut, String maBoQua) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.LichHen WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE maNhanVien = ? AND thoiGianHen > ? AND thoiGianHen < ? "
                + "AND trangThai NOT IN ('DA_HUY', 'DA_DOI_LICH') "
                + "AND (? IS NULL OR maLichHen <> ?)";
        try (PreparedStatement cauLenh = BenhNhanDao.taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maBacSi);
            cauLenh.setTimestamp(2, Timestamp.valueOf(batDau.minusMinutes(thoiLuongPhut)));
            cauLenh.setTimestamp(3, Timestamp.valueOf(ketThuc));
            cauLenh.setString(4, maBoQua);
            cauLenh.setString(5, maBoQua);
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                ketQua.next();
                return ketQua.getInt(1);
            }
        }
    }

    public LichHen timTheoMaDeSua(Connection ketNoi, String maLichHen) throws SQLException {
        String sql = "SELECT maLichHen, maBenhNhan, maNhanVien, thoiGianHen, lyDoHen, trangThai "
                + "FROM dbo.LichHen WITH (UPDLOCK, HOLDLOCK) WHERE maLichHen = ?";
        try (PreparedStatement cauLenh = BenhNhanDao.taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maLichHen);
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                if (!ketQua.next()) {
                    return null;
                }
                LichHen lichHen = new LichHen(ketQua.getString(1));
                lichHen.setMaBenhNhan(ketQua.getString(2));
                lichHen.setMaBacSi(ketQua.getString(3));
                lichHen.setThoiGianHen(ketQua.getTimestamp(4).toLocalDateTime());
                lichHen.setLyDoHen(ketQua.getNString(5));
                lichHen.setTrangThai(ketQua.getString(6));
                return lichHen;
            }
        }
    }

    public String taoMaLichHenMoi(Connection ketNoi) throws SQLException {
        String sql = "SELECT ISNULL(MAX(CAST(SUBSTRING(maLichHen, 3, 6) AS INT)), 0) + 1 "
                + "FROM dbo.LichHen WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE maLichHen LIKE 'LH[0-9][0-9][0-9][0-9][0-9][0-9]'";
        try (PreparedStatement cauLenh = BenhNhanDao.taoCauLenh(ketNoi, sql);
             ResultSet ketQua = cauLenh.executeQuery()) {
            ketQua.next();
            int soTiepTheo = ketQua.getInt(1);
            if (soTiepTheo > 999999) {
                throw new SQLException("Đã hết mã lịch hẹn");
            }
            return "LH" + String.format("%06d", soTiepTheo);
        }
    }

    public void themLichHen(Connection ketNoi, LichHen lichHen, String maTaiKhoanTao) throws SQLException {
        String sql = "INSERT INTO dbo.LichHen "
                + "(maLichHen, maBenhNhan, maNhanVien, thoiGianHen, lyDoHen, trangThai, maLichHenGoc, maTaiKhoanTao) "
                + "VALUES (?, ?, ?, ?, ?, 'CHO_XAC_NHAN', ?, ?)";
        try (PreparedStatement cauLenh = BenhNhanDao.taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, lichHen.getMaLichHen());
            cauLenh.setString(2, lichHen.getMaBenhNhan());
            cauLenh.setString(3, lichHen.getMaBacSi());
            cauLenh.setTimestamp(4, Timestamp.valueOf(lichHen.getThoiGianHen()));
            cauLenh.setNString(5, lichHen.getLyDoHen());
            cauLenh.setString(6, lichHen.getMaLichHenGoc());
            cauLenh.setString(7, maTaiKhoanTao);
            cauLenh.executeUpdate();
        }
    }

    public boolean doiTrangThai(Connection ketNoi, String maLichHen, String trangThaiMoi, String lyDoHuy)
            throws SQLException {
        String sql = "UPDATE dbo.LichHen SET trangThai = ?, lyDoHuy = ? "
                + "WHERE maLichHen = ? AND trangThai IN ('CHO_XAC_NHAN', 'DA_XAC_NHAN')";
        try (PreparedStatement cauLenh = BenhNhanDao.taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, trangThaiMoi);
            cauLenh.setNString(2, lyDoHuy);
            cauLenh.setString(3, maLichHen);
            int soDongDaSua = cauLenh.executeUpdate();
            return soDongDaSua > 0;
        }
    }

    public int demLuotKhamDangMo(Connection ketNoi, String maBenhNhan) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.PhieuKham WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE maBenhNhan = ? AND trangThai IN ('CHO_KHAM', 'DANG_KHAM')";
        try (PreparedStatement cauLenh = BenhNhanDao.taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maBenhNhan);
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                ketQua.next();
                return ketQua.getInt(1);
            }
        }
    }

    public String taoMaPhieuKhamMoi(Connection ketNoi) throws SQLException {
        String sql = "SELECT ISNULL(MAX(CAST(SUBSTRING(maPhieuKham, 3, 6) AS INT)), 0) + 1 "
                + "FROM dbo.PhieuKham WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE maPhieuKham LIKE 'PK[0-9][0-9][0-9][0-9][0-9][0-9]'";
        try (PreparedStatement cauLenh = BenhNhanDao.taoCauLenh(ketNoi, sql);
             ResultSet ketQua = cauLenh.executeQuery()) {
            ketQua.next();
            int soTiepTheo = ketQua.getInt(1);
            if (soTiepTheo > 999999) {
                throw new SQLException("Đã hết mã phiếu khám");
            }
            return "PK" + String.format("%06d", soTiepTheo);
        }
    }

    public void themLuotKhamChoKham(Connection ketNoi, String maPhieuKham, String maBenhNhan, String maHoSo,
                                    String maBacSi, String maLichHen, LocalDateTime thoiGianTiepNhan,
                                    String lyDoDen) throws SQLException {
        String sql = "INSERT INTO dbo.PhieuKham "
                + "(maPhieuKham, maBenhNhan, maHoSo, maNhanVien, maLichHen, thoiGianTiepNhan, trieuChung, trangThai) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'CHO_KHAM')";
        try (PreparedStatement cauLenh = BenhNhanDao.taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maPhieuKham);
            cauLenh.setString(2, maBenhNhan);
            cauLenh.setString(3, maHoSo);
            cauLenh.setString(4, maBacSi);
            cauLenh.setString(5, maLichHen);
            cauLenh.setTimestamp(6, Timestamp.valueOf(thoiGianTiepNhan));
            cauLenh.setNString(7, lyDoDen);
            cauLenh.executeUpdate();
        }
    }

    public int demNguoiDangCho(Connection ketNoi, String maBacSi, LocalDate ngay) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dbo.PhieuKham "
                + "WHERE maNhanVien = ? AND trangThai = 'CHO_KHAM' "
                + "AND thoiGianTiepNhan >= ? AND thoiGianTiepNhan < ?";
        try (PreparedStatement cauLenh = BenhNhanDao.taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maBacSi);
            cauLenh.setTimestamp(2, Timestamp.valueOf(ngay.atStartOfDay()));
            cauLenh.setTimestamp(3, Timestamp.valueOf(ngay.plusDays(1).atStartOfDay()));
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                ketQua.next();
                return ketQua.getInt(1);
            }
        }
    }

    public String timLichHenChuaDenTrongNgay(Connection ketNoi, String maBenhNhan, LocalDate ngay)
            throws SQLException {
        String sql = "SELECT TOP 1 maLichHen FROM dbo.LichHen "
                + "WHERE maBenhNhan = ? AND thoiGianHen >= ? AND thoiGianHen < ? "
                + "AND trangThai IN ('CHO_XAC_NHAN', 'DA_XAC_NHAN') "
                + "ORDER BY thoiGianHen";
        try (PreparedStatement cauLenh = BenhNhanDao.taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maBenhNhan);
            cauLenh.setTimestamp(2, Timestamp.valueOf(ngay.atStartOfDay()));
            cauLenh.setTimestamp(3, Timestamp.valueOf(ngay.plusDays(1).atStartOfDay()));
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                if (!ketQua.next()) {
                    return null;
                }
                return ketQua.getString(1);
            }
        }
    }

    public void ghiNhatKyTiepNhan(Connection ketNoi, String maTaiKhoan, String maPhieuKham, String noiDung)
            throws SQLException {
        String sql = "INSERT INTO dbo.NhatKyThaoTac "
                + "(loaiTacNhan, maTaiKhoan, chucNang, hanhDong, ketQua, doiTuong, maDoiTuong, noiDung) "
                + "VALUES ('ACCOUNT', ?, N'TiepNhan', N'Tiếp nhận không hẹn', 'SUCCESS', 'PhieuKham', ?, ?)";
        try (PreparedStatement cauLenh = BenhNhanDao.taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maTaiKhoan);
            cauLenh.setString(2, maPhieuKham);
            cauLenh.setNString(3, noiDung);
            cauLenh.executeUpdate();
        }
    }

    public void ghiNhatKy(Connection ketNoi, String maTaiKhoan, String hanhDong, String maLichHen,
                          String noiDung) throws SQLException {
        String sql = "INSERT INTO dbo.NhatKyThaoTac "
                + "(loaiTacNhan, maTaiKhoan, chucNang, hanhDong, ketQua, doiTuong, maDoiTuong, noiDung) "
                + "VALUES ('ACCOUNT', ?, N'LichHen', ?, 'SUCCESS', 'LichHen', ?, ?)";
        try (PreparedStatement cauLenh = BenhNhanDao.taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maTaiKhoan);
            cauLenh.setNString(2, hanhDong);
            cauLenh.setString(3, maLichHen);
            cauLenh.setNString(4, noiDung);
            cauLenh.executeUpdate();
        }
    }
}
