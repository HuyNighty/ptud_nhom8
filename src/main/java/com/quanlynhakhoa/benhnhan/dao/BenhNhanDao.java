package com.quanlynhakhoa.benhnhan.dao;

import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.dev.TeamDevConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class BenhNhanDao {
    private static final int THOI_GIAN_CHO_TOI_DA = 15;

    private static final int NOI_DUNG_NHAT_KY_TOI_DA = 1000;

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection open() throws SQLException;
    }

    private final ConnectionProvider cachMoKetNoi;

    public BenhNhanDao() {
        this(TeamDevConnection::open);
    }

    public BenhNhanDao(ConnectionProvider cachMoKetNoi) {
        this.cachMoKetNoi = java.util.Objects.requireNonNull(cachMoKetNoi, "cachMoKetNoi");
    }

    private static final String CAC_COT =
            "maBenhNhan, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChi, nguoiLienHe, ngayTao ";

    public ArrayList<BenhNhan> getAllBenhNhan() throws SQLException {
        ArrayList<BenhNhan> danhSachBenhNhan = new ArrayList<>();
        String sql = "SELECT TOP 200 " + CAC_COT + "FROM dbo.BenhNhan ORDER BY maBenhNhan DESC";

        try (Connection ketNoi = cachMoKetNoi.open();
             PreparedStatement cauLenh = taoCauLenh(ketNoi, sql);
             ResultSet ketQua = cauLenh.executeQuery()) {
            while (ketQua.next()) {
                danhSachBenhNhan.add(docMotBenhNhan(ketQua));
            }
        }
        return danhSachBenhNhan;
    }

    public ArrayList<BenhNhan> timKiem(String tuKhoa, String gioiTinh) throws SQLException {
        ArrayList<BenhNhan> danhSachBenhNhan = new ArrayList<>();
        String sql = "SELECT TOP 200 " + CAC_COT
                + "FROM dbo.BenhNhan "
                + "WHERE (hoTen LIKE ? OR maBenhNhan LIKE ? OR soDienThoai LIKE ?) "
                + "AND (? = '' OR gioiTinh = ?) "
                + "ORDER BY maBenhNhan DESC";

        try (Connection ketNoi = cachMoKetNoi.open();
             PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            String mauTim = "%" + doiKyTuDacBietThanhKyTuThuong(tuKhoa) + "%";
            cauLenh.setNString(1, mauTim);
            cauLenh.setString(2, mauTim);
            cauLenh.setString(3, mauTim);
            cauLenh.setString(4, gioiTinh);
            cauLenh.setString(5, gioiTinh);

            try (ResultSet ketQua = cauLenh.executeQuery()) {
                while (ketQua.next()) {
                    danhSachBenhNhan.add(docMotBenhNhan(ketQua));
                }
            }
        }
        return danhSachBenhNhan;
    }

    public static String doiKyTuDacBietThanhKyTuThuong(String tuKhoa) {
        StringBuilder ketQua = new StringBuilder();
        for (int viTri = 0; viTri < tuKhoa.length(); viTri++) {
            char kyTu = tuKhoa.charAt(viTri);
            if (kyTu == '%' || kyTu == '_' || kyTu == '[') {
                ketQua.append('[').append(kyTu).append(']');
            } else {
                ketQua.append(kyTu);
            }
        }
        return ketQua.toString();
    }

    public BenhNhan timTheoMa(String maBenhNhan) throws SQLException {
        String sql = "SELECT b.maBenhNhan, b.hoTen, b.ngaySinh, b.gioiTinh, b.soDienThoai, "
                + "b.diaChi, b.nguoiLienHe, b.ngayTao, h.tienSuBenh, h.diUng "
                + "FROM dbo.BenhNhan AS b "
                + "LEFT JOIN dbo.HoSoBenhAn AS h ON h.maBenhNhan = b.maBenhNhan "
                + "WHERE b.maBenhNhan = ?";

        try (Connection ketNoi = cachMoKetNoi.open();
             PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maBenhNhan);
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                if (ketQua.next()) {
                    return docBenhNhanKemTienSu(ketQua);
                }
            }
        }
        return null;
    }

    public ArrayList<BenhNhan> timNghiTrung(String soDienThoai, String hoTen,
                                            LocalDate ngaySinh, String maBoQua) throws SQLException {
        ArrayList<BenhNhan> danhSachTrung = new ArrayList<>();
        String sql = "SELECT TOP 10 " + CAC_COT
                + "FROM dbo.BenhNhan "
                + "WHERE hoTen = ? AND ngaySinh = ? AND soDienThoai = ? "
                + "AND (? IS NULL OR maBenhNhan <> ?)";

        try (Connection ketNoi = cachMoKetNoi.open();
             PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setNString(1, hoTen);
            cauLenh.setDate(2, java.sql.Date.valueOf(ngaySinh));
            cauLenh.setString(3, soDienThoai);
            cauLenh.setString(4, maBoQua);
            cauLenh.setString(5, maBoQua);
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                while (ketQua.next()) {
                    danhSachTrung.add(docMotBenhNhan(ketQua));
                }
            }
        }
        return danhSachTrung;
    }

    public BenhNhan timTheoMaDeSua(Connection ketNoi, String maBenhNhan) throws SQLException {
        String sql = "SELECT b.maBenhNhan, b.hoTen, b.ngaySinh, b.gioiTinh, b.soDienThoai, "
                + "b.diaChi, b.nguoiLienHe, b.ngayTao, h.tienSuBenh, h.diUng "
                + "FROM dbo.BenhNhan AS b WITH (UPDLOCK, HOLDLOCK) "
                + "LEFT JOIN dbo.HoSoBenhAn AS h WITH (UPDLOCK, HOLDLOCK) ON h.maBenhNhan = b.maBenhNhan "
                + "WHERE b.maBenhNhan = ?";
        try (PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maBenhNhan);
            try (ResultSet ketQua = cauLenh.executeQuery()) {
                if (ketQua.next()) {
                    return docBenhNhanKemTienSu(ketQua);
                }
            }
        }
        return null;
    }

    public String taoMaBenhNhanMoi(Connection ketNoi) throws SQLException {
        String sql = "SELECT ISNULL(MAX(CAST(SUBSTRING(maBenhNhan, 3, 6) AS INT)), 0) + 1 "
                + "FROM dbo.BenhNhan WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE maBenhNhan LIKE 'BN[0-9][0-9][0-9][0-9][0-9][0-9]'";
        return taoMa(ketNoi, sql, "BN");
    }

    public String taoMaHoSoMoi(Connection ketNoi) throws SQLException {
        String sql = "SELECT ISNULL(MAX(CAST(SUBSTRING(maHoSo, 3, 6) AS INT)), 0) + 1 "
                + "FROM dbo.HoSoBenhAn WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE maHoSo LIKE 'HS[0-9][0-9][0-9][0-9][0-9][0-9]'";
        return taoMa(ketNoi, sql, "HS");
    }

    private String taoMa(Connection ketNoi, String sql, String tienTo) throws SQLException {
        try (PreparedStatement cauLenh = taoCauLenh(ketNoi, sql);
             ResultSet ketQua = cauLenh.executeQuery()) {
            ketQua.next();
            int soTiepTheo = ketQua.getInt(1);
            if (soTiepTheo > 999999) {
                throw new SQLException("Đã hết mã " + tienTo);
            }
            return tienTo + String.format("%06d", soTiepTheo);
        }
    }

    public void themBenhNhan(Connection ketNoi, BenhNhan benhNhan, String maTaiKhoanTao) throws SQLException {
        String sql = "INSERT INTO dbo.BenhNhan "
                + "(maBenhNhan, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChi, nguoiLienHe, maTaiKhoanTao) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, benhNhan.getMaBenhNhan());
            cauLenh.setNString(2, benhNhan.getHoTen());
            cauLenh.setDate(3, java.sql.Date.valueOf(benhNhan.getNgaySinh()));
            cauLenh.setString(4, benhNhan.getGioiTinh());
            cauLenh.setString(5, benhNhan.getSoDienThoai());
            cauLenh.setNString(6, benhNhan.getDiaChi());
            cauLenh.setNString(7, benhNhan.getNguoiLienHe());
            cauLenh.setString(8, maTaiKhoanTao);
            cauLenh.executeUpdate();
        }
    }

    public void themHoSoBenhAn(Connection ketNoi, String maHoSo, String maBenhNhan) throws SQLException {
        themHoSoBenhAn(ketNoi, maHoSo, maBenhNhan, null, null);
    }

    public void themHoSoBenhAn(Connection ketNoi, String maHoSo, String maBenhNhan,
                               String tienSuBenh, String diUng) throws SQLException {
        String sql = "INSERT INTO dbo.HoSoBenhAn (maHoSo, maBenhNhan, ngayLap, tienSuBenh, diUng) "
                + "VALUES (?, ?, CAST(GETDATE() AS DATE), ?, ?)";
        try (PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maHoSo);
            cauLenh.setString(2, maBenhNhan);
            cauLenh.setNString(3, tienSuBenh);
            cauLenh.setNString(4, diUng);
            cauLenh.executeUpdate();
        }
    }

    public boolean capNhatBenhNhan(Connection ketNoi, BenhNhan benhNhan) throws SQLException {
        String sql = "UPDATE dbo.BenhNhan SET hoTen = ?, ngaySinh = ?, gioiTinh = ?, "
                + "soDienThoai = ?, diaChi = ?, nguoiLienHe = ? "
                + "WHERE maBenhNhan = ?";
        try (PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setNString(1, benhNhan.getHoTen());
            cauLenh.setDate(2, java.sql.Date.valueOf(benhNhan.getNgaySinh()));
            cauLenh.setString(3, benhNhan.getGioiTinh());
            cauLenh.setString(4, benhNhan.getSoDienThoai());
            cauLenh.setNString(5, benhNhan.getDiaChi());
            cauLenh.setNString(6, benhNhan.getNguoiLienHe());
            cauLenh.setString(7, benhNhan.getMaBenhNhan());
            int soDongDaSua = cauLenh.executeUpdate();
            return soDongDaSua > 0;
        }
    }

    public void ghiNhatKy(Connection ketNoi, String maTaiKhoan, String hanhDong, String maBenhNhan)
            throws SQLException {
        ghiNhatKy(ketNoi, maTaiKhoan, hanhDong, maBenhNhan, null);
    }

    public void ghiNhatKy(Connection ketNoi, String maTaiKhoan, String hanhDong, String maBenhNhan,
                          String noiDung) throws SQLException {
        if (noiDung != null && noiDung.length() > NOI_DUNG_NHAT_KY_TOI_DA) {
            noiDung = noiDung.substring(0, NOI_DUNG_NHAT_KY_TOI_DA - 3) + "...";
        }
        String sql = "INSERT INTO dbo.NhatKyThaoTac "
                + "(loaiTacNhan, maTaiKhoan, chucNang, hanhDong, ketQua, doiTuong, maDoiTuong, noiDung) "
                + "VALUES ('ACCOUNT', ?, N'BenhNhan', ?, 'SUCCESS', 'BenhNhan', ?, ?)";
        try (PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            cauLenh.setString(1, maTaiKhoan);
            cauLenh.setNString(2, hanhDong);
            cauLenh.setString(3, maBenhNhan);
            cauLenh.setNString(4, noiDung);
            cauLenh.executeUpdate();
        }
    }

    public static PreparedStatement taoCauLenh(Connection ketNoi, String sql) throws SQLException {
        PreparedStatement cauLenh = ketNoi.prepareStatement(sql);
        try {
            cauLenh.setQueryTimeout(THOI_GIAN_CHO_TOI_DA);
            return cauLenh;
        } catch (SQLException | RuntimeException loi) {
            try {
                cauLenh.close();
            } catch (SQLException loiKhiDong) {
                loi.addSuppressed(loiKhiDong);
            }
            throw loi;
        }
    }

    private BenhNhan docMotBenhNhan(ResultSet ketQua) throws SQLException {
        String maBenhNhan = ketQua.getString(1);
        String hoTen = ketQua.getNString(2);
        LocalDate ngaySinh = ketQua.getDate(3).toLocalDate();
        String gioiTinh = ketQua.getString(4);
        String soDienThoai = ketQua.getString(5);
        String diaChi = ketQua.getNString(6);
        String nguoiLienHe = ketQua.getNString(7);

        LocalDateTime ngayTao = null;
        Timestamp thoiGianTao = ketQua.getTimestamp(8);
        if (thoiGianTao != null) {
            ngayTao = thoiGianTao.toLocalDateTime();
        }

        return new BenhNhan(maBenhNhan, hoTen, ngaySinh, gioiTinh,
                soDienThoai, diaChi, nguoiLienHe, ngayTao);
    }

    private BenhNhan docBenhNhanKemTienSu(ResultSet ketQua) throws SQLException {
        BenhNhan benhNhan = docMotBenhNhan(ketQua);
        benhNhan.setTienSuBenh(ketQua.getNString(9));
        benhNhan.setDiUng(ketQua.getNString(10));
        return benhNhan;
    }
}
