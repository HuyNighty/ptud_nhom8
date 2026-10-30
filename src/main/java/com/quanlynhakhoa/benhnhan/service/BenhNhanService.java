package com.quanlynhakhoa.benhnhan.service;

import com.quanlynhakhoa.benhnhan.dao.BenhNhanDao;
import com.quanlynhakhoa.benhnhan.dao.HoSoBenhAnDao;
import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;

// Service: kiểm tra QUYỀN và kiểm tra DỮ LIỆU, rồi mới gọi DAO
public class BenhNhanService {
    // ===================== REGEX kiểm tra dữ liệu =====================
    // (String.matches() luôn so CẢ chuỗi nên không cần ^ và $)

    // 1 TỪ trong họ tên: 1 chữ IN HOA + các chữ thường phía sau.   Ví dụ: "Nguyễn", "Lan", "Đỗ"
    //   \p{Lu} = 1 chữ in hoa (kể cả có dấu: Đ, Á, Ô...)
    //   \p{Ll}* = 0 hoặc nhiều chữ thường (kể cả có dấu: ễ, ư, ị...)
    private static final String MOT_TU = "\\p{Lu}\\p{Ll}*";

    // HỌ TÊN = 1 từ, rồi lặp lại ( khoảng trắng + 1 từ ).   Ví dụ: "Nguyễn" + " Thị" + " Lan"
    public static final String REGEX_HO_TEN = MOT_TU + "( " + MOT_TU + ")*";

    // NGÀY SINH dd/MM/yyyy: ngày 01–31 / tháng 01–12 / năm 4 chữ số.   Ví dụ: "12/05/1998"
    public static final String REGEX_NGAY_SINH = "(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/[0-9]{4}";

    // SỐ ĐIỆN THOẠI: số 0 + đúng 9 chữ số nữa (tổng 10 số, chỉ có chữ số).   Ví dụ: "0909123456"
    public static final String REGEX_SO_DIEN_THOAI = "0[0-9]{9}";

    // TIỀN SỬ BỆNH / DỊ ỨNG: không bắt buộc, độ dài giống cột trong bảng HoSoBenhAn (V5)
    public static final int TIEN_SU_TOI_DA = 500;
    public static final int DI_UNG_TOI_DA = 300;

    private final DesktopSession phien;
    private final AuthorizationService phanQuyen = new AuthorizationService(new RolePermissionPolicy());
    private final BenhNhanDao dao;
    private final HoSoBenhAnDao hoSoDao;   // tiền sử bệnh + dị ứng lưu ở bảng HoSoBenhAn
    private final BenhNhanDao.ConnectionProvider connections;

    public BenhNhanService(DesktopSession phien) {
        this(phien, TeamDevConnection::open);
    }

    public BenhNhanService(DesktopSession phien, BenhNhanDao.ConnectionProvider connections) {
        this.phien = java.util.Objects.requireNonNull(phien, "phien");
        this.connections = java.util.Objects.requireNonNull(connections, "connections");
        this.dao = new BenhNhanDao(connections);
        this.hoSoDao = new HoSoBenhAnDao(connections);
    }

    // Người đang đăng nhập có quyền này không (View dùng để làm mờ nút)
    public boolean coQuyen(Permission quyen) {
        return phanQuyen.can(phien, quyen);
    }

    // ===================== XEM / TÌM KIẾM =====================

    public ArrayList<BenhNhan> timKiem(String tuKhoa, String gioiTinh) throws SQLException {
        // 1. Kiểm tra quyền: không có quyền xem bệnh nhân thì báo lỗi
        phanQuyen.require(phien, Permission.PATIENT_READ);

        // 2. Kiểm tra dữ liệu nhập
        tuKhoa = tuKhoa.trim();
        if (tuKhoa.length() > 100) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        }
        // Nếu người dùng gõ số điện thoại có khoảng trắng (0909 123 456) thì bỏ khoảng trắng
        String boKhoangTrang = tuKhoa.replace(" ", "");
        if (!boKhoangTrang.isEmpty() && boKhoangTrang.matches("[0-9]+")) {
            if (boKhoangTrang.length() > 10) {
                throw new IllegalArgumentException("Số điện thoại không hợp lệ: gồm 10 chữ số và bắt đầu bằng 0.");
            }
            tuKhoa = boKhoangTrang;
        }

        // 3. Gọi DAO lấy dữ liệu
        if (tuKhoa.isEmpty() && gioiTinh.isEmpty()) {
            return dao.getAllBenhNhan();       // không lọc gì → lấy tất cả
        }
        return dao.timKiem(tuKhoa, gioiTinh);
    }

    public BenhNhan xemChiTiet(String maBenhNhan) throws SQLException {
        phanQuyen.require(phien, Permission.PATIENT_READ);
        return dao.timTheoMa(maBenhNhan);
    }

    // ===================== KIỂM TRA DỮ LIỆU =====================

    // Kiểm tra và chuẩn hóa dữ liệu trên form. Sai thì ném IllegalArgumentException kèm câu báo lỗi.
    public void kiemTraDuLieu(BenhNhan bn) {
        // Họ tên: bắt buộc, tối đa 100 ký tự, bỏ khoảng trắng thừa
        String hoTen = bn.getHoTen() == null ? "" : bn.getHoTen().trim().replaceAll("\\s+", " ");
        // Đưa dấu tiếng Việt về 1 dạng chuẩn (NFC) để so trùng họ tên cho đúng
        hoTen = java.text.Normalizer.normalize(hoTen, java.text.Normalizer.Form.NFC);
        if (hoTen.isEmpty()) {
            throw new IllegalArgumentException("Nhập họ tên.");
        }
        if (hoTen.length() < 2 || hoTen.length() > 100) {
            throw new IllegalArgumentException("Họ tên dài từ 2 đến 100 ký tự.");
        }
        if (!hoTen.matches(REGEX_HO_TEN)) {
            throw new IllegalArgumentException("Họ tên phải viết hoa chữ cái đầu mỗi từ, chỉ gồm chữ cái "
                    + "(ví dụ: Nguyễn Thị Lan).");
        }
        bn.setHoTen(hoTen);

        // Ngày sinh: bắt buộc, không ở tương lai, không quá 120 tuổi
        LocalDate ngaySinh = bn.getNgaySinh();
        if (ngaySinh == null) {
            throw new IllegalArgumentException("Nhập ngày sinh.");
        }
        if (ngaySinh.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh không được ở tương lai.");
        }
        if (ngaySinh.isBefore(LocalDate.now().minusYears(120))) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ (quá 120 tuổi).");
        }

        // Giới tính: chỉ NAM hoặc NU (ứng dụng không cho chọn "Khác")
        String gioiTinh = bn.getGioiTinh();
        if (!"NAM".equals(gioiTinh) && !"NU".equals(gioiTinh)) {
            throw new IllegalArgumentException("Chọn giới tính.");
        }

        // SĐT: chỉ bỏ khoảng trắng 2 đầu; bên trong phải toàn là chữ số (10 số, bắt đầu bằng 0)
        String sdt = bn.getSoDienThoai() == null ? "" : bn.getSoDienThoai().trim();
        if (!sdt.matches(REGEX_SO_DIEN_THOAI)) {
            throw new IllegalArgumentException("Số điện thoại chỉ được nhập chữ số: đúng 10 số, bắt đầu bằng 0 "
                    + "(không nhập chữ, khoảng trắng hay dấu chấm).");
        }
        bn.setSoDienThoai(sdt);

        // Địa chỉ, người liên hệ: không bắt buộc; để trống thì lưu NULL
        bn.setDiaChi(boTrongThanhNull(bn.getDiaChi()));
        bn.setNguoiLienHe(boTrongThanhNull(bn.getNguoiLienHe()));
        if (bn.getDiaChi() != null && bn.getDiaChi().length() > 255) {
            throw new IllegalArgumentException("Địa chỉ tối đa 255 ký tự.");
        }
        if (bn.getNguoiLienHe() != null && bn.getNguoiLienHe().length() > 200) {
            throw new IllegalArgumentException("Người liên hệ tối đa 200 ký tự.");
        }

        // Tiền sử bệnh, dị ứng: không bắt buộc; để trống thì lưu NULL
        bn.setTienSuBenh(boTrongThanhNull(bn.getTienSuBenh()));
        bn.setDiUng(boTrongThanhNull(bn.getDiUng()));
        if (bn.getTienSuBenh() != null && bn.getTienSuBenh().length() > TIEN_SU_TOI_DA) {
            throw new IllegalArgumentException("Tiền sử bệnh tối đa " + TIEN_SU_TOI_DA + " ký tự "
                    + "(đang nhập " + bn.getTienSuBenh().length() + ").");
        }
        if (bn.getDiUng() != null && bn.getDiUng().length() > DI_UNG_TOI_DA) {
            throw new IllegalArgumentException("Dị ứng tối đa " + DI_UNG_TOI_DA + " ký tự "
                    + "(đang nhập " + bn.getDiUng().length() + ").");
        }
    }

    // Đổi chuỗi ngày sinh "dd/MM/yyyy" (lấy từ ô ngày sinh đã chọn trên lịch) thành LocalDate.
    // Kiểm tra bằng regex trước, sau đó kiểm tra ngày có thật không (ví dụ 31/02 là sai).
    public static LocalDate docNgaySinh(String chuoi) {
        if (!chuoi.matches(REGEX_NGAY_SINH)) {
            throw new IllegalArgumentException("Chọn ngày sinh (bấm nút \"Chọn...\").");
        }
        String[] phan = chuoi.split("/");
        try {
            return LocalDate.of(Integer.parseInt(phan[2]), Integer.parseInt(phan[1]), Integer.parseInt(phan[0]));
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Ngày sinh không tồn tại (ví dụ 31/02).");
        }
    }

    private String boTrongThanhNull(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        return s.trim();
    }

    // Tìm hồ sơ nghi trùng (cùng SĐT, hoặc cùng họ tên + ngày sinh). Chỉ để CẢNH BÁO, không chặn.
    public ArrayList<BenhNhan> timNghiTrung(BenhNhan bn) throws SQLException {
        phanQuyen.require(phien, Permission.PATIENT_READ);
        kiemTraDuLieu(bn);
        return dao.timNghiTrung(bn.getSoDienThoai(), bn.getHoTen(), bn.getNgaySinh(), bn.getMaBenhNhan());
    }

    // ===================== THÊM / SỬA =====================

    // Thêm bệnh nhân mới + tạo hồ sơ bệnh án trống + ghi nhật ký, trong CÙNG 1 transaction.
    // Trả về mã bệnh nhân mới (ví dụ BN000007).
    public String themBenhNhan(BenhNhan bn) throws SQLException {
        phanQuyen.require(phien, Permission.PATIENT_CREATE);
        kiemTraDuLieu(bn);

        try (Connection con = connections.open()) {
            if (!con.getAutoCommit()) throw new SQLException("Expected a fresh JDBC connection");
            con.setAutoCommit(false);          // bắt đầu transaction
            try {
                String maBenhNhan = dao.taoMaBenhNhanMoi(con);
                bn.setMaBenhNhan(maBenhNhan);
                dao.themBenhNhan(con, bn, phien.accountId());

                String maHoSo = dao.taoMaHoSoMoi(con);
                dao.themHoSoBenhAn(con, maHoSo, maBenhNhan, bn.getTienSuBenh(), bn.getDiUng());

                dao.ghiNhatKy(con, phien.accountId(), "Thêm bệnh nhân", maBenhNhan);
                phanQuyen.require(phien, Permission.PATIENT_CREATE);

                con.commit();                  // tất cả thành công → lưu thật
                return maBenhNhan;
            } catch (SQLException | RuntimeException loi) {
                con.rollback();                // có lỗi → hủy hết, không lưu nửa chừng
                bn.setMaBenhNhan(null);
                throw loi;
            }
        }
    }

    // Sửa thông tin bệnh nhân + ghi nhật ký, trong cùng 1 transaction
    public void capNhatBenhNhan(BenhNhan bn) throws SQLException {
        phanQuyen.require(phien, Permission.PATIENT_UPDATE);
        if (bn.getMaBenhNhan() == null) {
            throw new IllegalArgumentException("Chưa chọn bệnh nhân cần sửa.");
        }
        kiemTraDuLieu(bn);

        try (Connection con = connections.open()) {
            if (!con.getAutoCommit()) throw new SQLException("Expected a fresh JDBC connection");
            con.setAutoCommit(false);
            try {
                if (!dao.capNhatBenhNhan(con, bn)) {
                    throw new IllegalArgumentException("Không tìm thấy bệnh nhân " + bn.getMaBenhNhan() + ".");
                }
                // Tiền sử bệnh + dị ứng nằm ở HoSoBenhAn; bệnh nhân chưa có hồ sơ thì tạo mới
                String maHoSo = hoSoDao.timMaHoSo(con, bn.getMaBenhNhan());
                if (maHoSo == null) {
                    dao.themHoSoBenhAn(con, dao.taoMaHoSoMoi(con), bn.getMaBenhNhan(),
                            bn.getTienSuBenh(), bn.getDiUng());
                } else {
                    hoSoDao.capNhatTienSu(con, maHoSo, bn.getTienSuBenh(), bn.getDiUng());
                }
                dao.ghiNhatKy(con, phien.accountId(), "Sửa thông tin bệnh nhân", bn.getMaBenhNhan());
                phanQuyen.require(phien, Permission.PATIENT_UPDATE);
                con.commit();
            } catch (SQLException | RuntimeException loi) {
                con.rollback();
                throw loi;
            }
        }
    }
}
