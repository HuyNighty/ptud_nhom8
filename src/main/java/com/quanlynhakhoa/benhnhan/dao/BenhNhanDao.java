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

// DAO: chỉ làm việc với SQL Server, không kiểm tra quyền, không kiểm tra nghiệp vụ
// Kết nối bằng TeamDevConnection (tài khoản n08dev_*, KHÔNG dùng sa)
public class BenhNhanDao {
    // Thời gian chờ tối đa (giây) cho mỗi câu SQL, kể cả lúc chờ khóa trong transaction
    private static final int THOI_GIAN_CHO_TOI_DA = 15;

    // Cột "noiDung" của bảng NhatKyThaoTac dài tối đa 1000 ký tự
    private static final int NOI_DUNG_NHAT_KY_TOI_DA = 1000;

    // Cách mở kết nối tới database. Bình thường là TeamDevConnection.open().
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

    // Liệt kê rõ tên cột (không dùng SELECT *) để thứ tự cột luôn đúng khi đọc cột 1..8
    private static final String CAC_COT =
            "maBenhNhan, hoTen, ngaySinh, gioiTinh, soDienThoai, diaChi, nguoiLienHe, ngayTao ";

    // ===================== XEM / TÌM KIẾM =====================

    // Lấy tất cả bệnh nhân (tối đa 200 dòng, mới nhất ở trên)
    public ArrayList<BenhNhan> getAllBenhNhan() throws SQLException {
        ArrayList<BenhNhan> danhSachBenhNhan = new ArrayList<>();
        String sql = "SELECT TOP 200 " + CAC_COT + "FROM dbo.BenhNhan ORDER BY maBenhNhan DESC";

        // try (...) tự đóng kết nối sau khi dùng xong
        try (Connection ketNoi = cachMoKetNoi.open();
             PreparedStatement cauLenh = taoCauLenh(ketNoi, sql);
             ResultSet ketQua = cauLenh.executeQuery()) {
            while (ketQua.next()) {
                danhSachBenhNhan.add(docMotBenhNhan(ketQua));
            }
        }
        return danhSachBenhNhan;
    }

    // Tìm bệnh nhân theo từ khóa (họ tên / mã BN / SĐT) và giới tính
    // gioiTinh = "" nghĩa là lấy tất cả giới tính
    public ArrayList<BenhNhan> timKiem(String tuKhoa, String gioiTinh) throws SQLException {
        ArrayList<BenhNhan> danhSachBenhNhan = new ArrayList<>();
        String sql = "SELECT TOP 200 " + CAC_COT
                + "FROM dbo.BenhNhan "
                + "WHERE (hoTen LIKE ? OR maBenhNhan LIKE ? OR soDienThoai LIKE ?) "
                + "AND (? = '' OR gioiTinh = ?) "
                + "ORDER BY maBenhNhan DESC";

        try (Connection ketNoi = cachMoKetNoi.open();
             PreparedStatement cauLenh = taoCauLenh(ketNoi, sql)) {
            // %...% = chứa từ khóa ở bất kỳ vị trí nào
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

    // Trong LIKE của SQL Server: % = nhiều ký tự bất kỳ, _ = 1 ký tự bất kỳ, [ ] = nhóm ký tự.
    // Người dùng gõ "_" hay "%" thì phải hiểu là chữ bình thường → bọc trong [ ]   (ví dụ "_" → "[_]")
    // public static: phần Lịch hẹn (LichHenDao) dùng lại hàm này.
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

    // Lấy 1 bệnh nhân theo mã, không có thì trả về null
    // Đọc kèm tiền sử bệnh + dị ứng từ bảng HoSoBenhAn (LEFT JOIN: chưa có hồ sơ thì 2 cột này NULL)
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

    // Tìm hồ sơ NGHI TRÙNG theo BR01: cùng họ tên VÀ cùng ngày sinh VÀ cùng số điện thoại.
    // maBoQua: mã của chính bệnh nhân đang sửa (để không tự báo trùng với mình), null khi thêm mới
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

    // ===================== Phần THÊM / SỬA =====================
    // Các hàm dưới đây nhận Connection từ Service, vì Service mở 1 transaction
    // cho nhiều câu lệnh (thêm bệnh nhân + thêm hồ sơ + ghi nhật ký).

    // Đọc lại 1 bệnh nhân NGAY TRƯỚC KHI SỬA và KHÓA dòng đó tới khi transaction kết thúc
    // (UPDLOCK, HOLDLOCK: máy khác muốn sửa cùng bệnh nhân phải chờ).
    // Service so dữ liệu này với dữ liệu lúc mở form để biết có ai vừa sửa không (BR10).
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

    // Sinh mã bệnh nhân mới: BN + 6 chữ số (BN000001, BN000002, ...)
    // UPDLOCK, HOLDLOCK: khóa lại để 2 người bấm Lưu cùng lúc không bị trùng mã
    public String taoMaBenhNhanMoi(Connection ketNoi) throws SQLException {
        String sql = "SELECT ISNULL(MAX(CAST(SUBSTRING(maBenhNhan, 3, 6) AS INT)), 0) + 1 "
                + "FROM dbo.BenhNhan WITH (UPDLOCK, HOLDLOCK) "
                + "WHERE maBenhNhan LIKE 'BN[0-9][0-9][0-9][0-9][0-9][0-9]'";
        return taoMa(ketNoi, sql, "BN");
    }

    // Sinh mã hồ sơ bệnh án mới: HS + 6 chữ số (HS000001, ...)
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
            return tienTo + String.format("%06d", soTiepTheo); // ví dụ 7 → BN000007
        }
    }

    // Thêm 1 dòng vào bảng BenhNhan
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

    // Thêm hồ sơ bệnh án trống (chưa có tiền sử / dị ứng)
    public void themHoSoBenhAn(Connection ketNoi, String maHoSo, String maBenhNhan) throws SQLException {
        themHoSoBenhAn(ketNoi, maHoSo, maBenhNhan, null, null);
    }

    // Thêm hồ sơ bệnh án cho bệnh nhân (mỗi bệnh nhân có đúng 1 hồ sơ),
    // kèm tiền sử bệnh + dị ứng (có thể null)
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

    // Cập nhật thông tin bệnh nhân, trả về true nếu có dòng được sửa
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

    // Ghi nhật ký thao tác, không có nội dung chi tiết
    public void ghiNhatKy(Connection ketNoi, String maTaiKhoan, String hanhDong, String maBenhNhan)
            throws SQLException {
        ghiNhatKy(ketNoi, maTaiKhoan, hanhDong, maBenhNhan, null);
    }

    // Ghi nhật ký thao tác kèm nội dung (ví dụ khi sửa: giá trị CŨ → MỚI, theo BR07 và UC003 A1)
    public void ghiNhatKy(Connection ketNoi, String maTaiKhoan, String hanhDong, String maBenhNhan,
                          String noiDung) throws SQLException {
        // Cột noiDung chỉ chứa được 1000 ký tự → dài hơn thì cắt bớt
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

    // ===================== Hàm dùng chung =====================

    // Tạo PreparedStatement kèm thời gian chờ tối đa.
    // Nếu đặt thời gian chờ bị lỗi thì đóng câu lệnh lại ngay rồi báo lỗi.
    // public static: phần Lịch hẹn (LichHenDao) dùng lại hàm này.
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

    // Đọc 1 dòng kết quả thành 1 đối tượng BenhNhan (thứ tự cột giống hằng CAC_COT ở trên)
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

    // Đọc cột 1..8 như trên, rồi đọc thêm cột 9 (tiền sử bệnh) và cột 10 (dị ứng)
    private BenhNhan docBenhNhanKemTienSu(ResultSet ketQua) throws SQLException {
        BenhNhan benhNhan = docMotBenhNhan(ketQua);
        benhNhan.setTienSuBenh(ketQua.getNString(9));
        benhNhan.setDiUng(ketQua.getNString(10));
        return benhNhan;
    }
}
