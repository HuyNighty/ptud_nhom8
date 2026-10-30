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
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class BenhNhanService {
    private static final String MOT_TU = "\\p{Lu}\\p{Ll}*";

    public static final String REGEX_HO_TEN = MOT_TU + "( " + MOT_TU + ")*";

    public static final String REGEX_NGAY_SINH = "(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/[0-9]{4}";

    public static final String REGEX_SO_DIEN_THOAI = "0[0-9]{9}";

    public static final int HO_TEN_TOI_DA = 100;
    public static final int DIA_CHI_TOI_DA = 255;
    public static final int NGUOI_LIEN_HE_TOI_DA = 200;
    public static final int TIEN_SU_TOI_DA = 500;
    public static final int DI_UNG_TOI_DA = 300;

    private static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DesktopSession phienDangNhap;
    private final AuthorizationService phanQuyen = new AuthorizationService(new RolePermissionPolicy());
    private final BenhNhanDao benhNhanDao;
    private final HoSoBenhAnDao hoSoBenhAnDao;
    private final BenhNhanDao.ConnectionProvider cachMoKetNoi;

    public BenhNhanService(DesktopSession phienDangNhap) {
        this(phienDangNhap, TeamDevConnection::open);
    }

    public BenhNhanService(DesktopSession phienDangNhap, BenhNhanDao.ConnectionProvider cachMoKetNoi) {
        this.phienDangNhap = java.util.Objects.requireNonNull(phienDangNhap, "phienDangNhap");
        this.cachMoKetNoi = java.util.Objects.requireNonNull(cachMoKetNoi, "cachMoKetNoi");
        this.benhNhanDao = new BenhNhanDao(cachMoKetNoi);
        this.hoSoBenhAnDao = new HoSoBenhAnDao(cachMoKetNoi);
    }

    public boolean coQuyen(Permission quyen) {
        return phanQuyen.can(phienDangNhap, quyen);
    }

    public ArrayList<BenhNhan> timKiem(String tuKhoa, String gioiTinh) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.PATIENT_READ);

        tuKhoa = tuKhoa.trim();
        if (tuKhoa.length() > 100) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        }
        String tuKhoaKhongKhoangTrang = tuKhoa.replace(" ", "");
        if (!tuKhoaKhongKhoangTrang.isEmpty() && tuKhoaKhongKhoangTrang.matches("[0-9]+")) {
            if (tuKhoaKhongKhoangTrang.length() > 10) {
                throw new IllegalArgumentException("Số điện thoại không hợp lệ: gồm 10 chữ số và bắt đầu bằng 0.");
            }
            tuKhoa = tuKhoaKhongKhoangTrang;
        }

        if (tuKhoa.isEmpty() && gioiTinh.isEmpty()) {
            return benhNhanDao.getAllBenhNhan();
        }
        return benhNhanDao.timKiem(tuKhoa, gioiTinh);
    }

    public BenhNhan xemChiTiet(String maBenhNhan) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.PATIENT_READ);
        return benhNhanDao.timTheoMa(maBenhNhan);
    }

    public void kiemTraDuLieu(BenhNhan benhNhan) {
        String hoTen = "";
        if (benhNhan.getHoTen() != null) {
            hoTen = benhNhan.getHoTen().trim().replaceAll("\\s+", " ");
        }
        hoTen = Normalizer.normalize(hoTen, Normalizer.Form.NFC);
        if (hoTen.isEmpty()) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_HO_TEN, "Nhập họ tên.");
        }
        if (hoTen.length() < 2 || hoTen.length() > HO_TEN_TOI_DA) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_HO_TEN,
                    "Họ tên dài từ 2 đến " + HO_TEN_TOI_DA + " ký tự.");
        }
        if (!hoTen.matches(REGEX_HO_TEN)) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_HO_TEN,
                    "Họ tên phải viết hoa chữ cái đầu mỗi từ, chỉ gồm chữ cái (ví dụ: Nguyễn Thị Lan).");
        }
        benhNhan.setHoTen(hoTen);

        LocalDate ngaySinh = benhNhan.getNgaySinh();
        if (ngaySinh == null) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_NGAY_SINH, "Chọn ngày sinh.");
        }
        if (ngaySinh.isAfter(LocalDate.now())) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_NGAY_SINH, "Ngày sinh không được ở tương lai.");
        }
        if (ngaySinh.isBefore(LocalDate.now().minusYears(120))) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_NGAY_SINH, "Ngày sinh không hợp lệ (quá 120 tuổi).");
        }

        String gioiTinh = benhNhan.getGioiTinh();
        if (!"NAM".equals(gioiTinh) && !"NU".equals(gioiTinh) && !"KHAC".equals(gioiTinh)) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_GIOI_TINH, "Chọn giới tính.");
        }

        String soDienThoai = "";
        if (benhNhan.getSoDienThoai() != null) {
            soDienThoai = benhNhan.getSoDienThoai().trim();
        }
        if (!soDienThoai.matches(REGEX_SO_DIEN_THOAI)) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_SO_DIEN_THOAI,
                    "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.");
        }
        benhNhan.setSoDienThoai(soDienThoai);

        benhNhan.setDiaChi(doiChuoiTrongThanhNull(benhNhan.getDiaChi()));
        benhNhan.setNguoiLienHe(doiChuoiTrongThanhNull(benhNhan.getNguoiLienHe()));
        if (benhNhan.getDiaChi() != null && benhNhan.getDiaChi().length() > DIA_CHI_TOI_DA) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_DIA_CHI,
                    "Địa chỉ tối đa " + DIA_CHI_TOI_DA + " ký tự.");
        }
        if (benhNhan.getNguoiLienHe() != null && benhNhan.getNguoiLienHe().length() > NGUOI_LIEN_HE_TOI_DA) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_NGUOI_LIEN_HE,
                    "Người liên hệ tối đa " + NGUOI_LIEN_HE_TOI_DA + " ký tự.");
        }

        benhNhan.setTienSuBenh(doiChuoiTrongThanhNull(benhNhan.getTienSuBenh()));
        benhNhan.setDiUng(doiChuoiTrongThanhNull(benhNhan.getDiUng()));
        if (benhNhan.getTienSuBenh() != null && benhNhan.getTienSuBenh().length() > TIEN_SU_TOI_DA) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_TIEN_SU_BENH,
                    "Tiền sử bệnh tối đa " + TIEN_SU_TOI_DA + " ký tự "
                    + "(đang nhập " + benhNhan.getTienSuBenh().length() + ").");
        }
        if (benhNhan.getDiUng() != null && benhNhan.getDiUng().length() > DI_UNG_TOI_DA) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_DI_UNG,
                    "Dị ứng tối đa " + DI_UNG_TOI_DA + " ký tự "
                    + "(đang nhập " + benhNhan.getDiUng().length() + ").");
        }
    }

    public static LocalDate docNgaySinh(String chuoiNgaySinh) {
        if (!chuoiNgaySinh.matches(REGEX_NGAY_SINH)) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_NGAY_SINH,
                    "Chọn ngày sinh (bấm nút \"Chọn\").");
        }
        String[] ngayThangNam = chuoiNgaySinh.split("/");
        int ngay = Integer.parseInt(ngayThangNam[0]);
        int thang = Integer.parseInt(ngayThangNam[1]);
        int nam = Integer.parseInt(ngayThangNam[2]);
        try {
            return LocalDate.of(nam, thang, ngay);
        } catch (DateTimeException loi) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_NGAY_SINH,
                    "Ngày sinh không tồn tại (ví dụ 31/02).");
        }
    }

    private String doiChuoiTrongThanhNull(String chuoi) {
        if (chuoi == null || chuoi.trim().isEmpty()) {
            return null;
        }
        return chuoi.trim();
    }

    public ArrayList<BenhNhan> timNghiTrung(BenhNhan benhNhan) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.PATIENT_READ);
        kiemTraDuLieu(benhNhan);
        return benhNhanDao.timNghiTrung(benhNhan.getSoDienThoai(), benhNhan.getHoTen(),
                benhNhan.getNgaySinh(), benhNhan.getMaBenhNhan());
    }

    public String themBenhNhan(BenhNhan benhNhan) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.PATIENT_CREATE);
        kiemTraDuLieu(benhNhan);

        try (Connection ketNoi = cachMoKetNoi.open()) {
            if (!ketNoi.getAutoCommit()) {
                throw new SQLException("Expected a fresh JDBC connection");
            }
            ketNoi.setAutoCommit(false);
            try {
                String maBenhNhan = benhNhanDao.taoMaBenhNhanMoi(ketNoi);
                benhNhan.setMaBenhNhan(maBenhNhan);
                benhNhanDao.themBenhNhan(ketNoi, benhNhan, phienDangNhap.accountId());

                String maHoSo = benhNhanDao.taoMaHoSoMoi(ketNoi);
                benhNhanDao.themHoSoBenhAn(ketNoi, maHoSo, maBenhNhan,
                        benhNhan.getTienSuBenh(), benhNhan.getDiUng());

                benhNhanDao.ghiNhatKy(ketNoi, phienDangNhap.accountId(), "Thêm bệnh nhân", maBenhNhan);
                phanQuyen.require(phienDangNhap, Permission.PATIENT_CREATE);

                ketNoi.commit();
                return maBenhNhan;
            } catch (SQLException | RuntimeException loi) {
                ketNoi.rollback();
                benhNhan.setMaBenhNhan(null);
                throw loi;
            }
        }
    }

    public void capNhatBenhNhan(BenhNhan benhNhanMoi, BenhNhan benhNhanLucMoForm) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.PATIENT_UPDATE);
        if (benhNhanMoi.getMaBenhNhan() == null) {
            throw new IllegalArgumentException("Chưa chọn bệnh nhân cần sửa.");
        }
        if (benhNhanLucMoForm == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu lúc mở form sửa, hãy mở lại form.");
        }
        kiemTraDuLieu(benhNhanMoi);
        String maBenhNhan = benhNhanMoi.getMaBenhNhan();

        try (Connection ketNoi = cachMoKetNoi.open()) {
            if (!ketNoi.getAutoCommit()) {
                throw new SQLException("Expected a fresh JDBC connection");
            }
            ketNoi.setAutoCommit(false);
            try {
                BenhNhan benhNhanTrongDatabase = benhNhanDao.timTheoMaDeSua(ketNoi, maBenhNhan);
                if (benhNhanTrongDatabase == null) {
                    throw new IllegalArgumentException("Không tìm thấy bệnh nhân " + maBenhNhan + ".");
                }

                if (!giongNhau(benhNhanTrongDatabase, benhNhanLucMoForm)) {
                    throw new DuLieuDaThayDoiException("Bệnh nhân " + maBenhNhan
                            + " vừa được người khác sửa. Dữ liệu mới nhất sẽ được tải lại, "
                            + "hãy kiểm tra rồi sửa lại.");
                }

                if (!benhNhanDao.capNhatBenhNhan(ketNoi, benhNhanMoi)) {
                    throw new IllegalArgumentException("Không tìm thấy bệnh nhân " + maBenhNhan + ".");
                }
                String maHoSo = hoSoBenhAnDao.timMaHoSo(ketNoi, maBenhNhan);
                if (maHoSo == null) {
                    String maHoSoMoi = benhNhanDao.taoMaHoSoMoi(ketNoi);
                    benhNhanDao.themHoSoBenhAn(ketNoi, maHoSoMoi, maBenhNhan,
                            benhNhanMoi.getTienSuBenh(), benhNhanMoi.getDiUng());
                } else {
                    hoSoBenhAnDao.capNhatTienSu(ketNoi, maHoSo,
                            benhNhanMoi.getTienSuBenh(), benhNhanMoi.getDiUng());
                }

                String noiDungThayDoi = moTaThayDoi(benhNhanTrongDatabase, benhNhanMoi);
                benhNhanDao.ghiNhatKy(ketNoi, phienDangNhap.accountId(), "Sửa thông tin bệnh nhân",
                        maBenhNhan, noiDungThayDoi);

                phanQuyen.require(phienDangNhap, Permission.PATIENT_UPDATE);
                ketNoi.commit();
            } catch (SQLException | RuntimeException loi) {
                ketNoi.rollback();
                throw loi;
            }
        }
    }

    private boolean giongNhau(BenhNhan benhNhanThuNhat, BenhNhan benhNhanThuHai) {
        if (!haiChuoiGiongNhau(benhNhanThuNhat.getHoTen(), benhNhanThuHai.getHoTen())) {
            return false;
        }
        if (!java.util.Objects.equals(benhNhanThuNhat.getNgaySinh(), benhNhanThuHai.getNgaySinh())) {
            return false;
        }
        if (!haiChuoiGiongNhau(benhNhanThuNhat.getGioiTinh(), benhNhanThuHai.getGioiTinh())) {
            return false;
        }
        if (!haiChuoiGiongNhau(benhNhanThuNhat.getSoDienThoai(), benhNhanThuHai.getSoDienThoai())) {
            return false;
        }
        if (!haiChuoiGiongNhau(benhNhanThuNhat.getDiaChi(), benhNhanThuHai.getDiaChi())) {
            return false;
        }
        if (!haiChuoiGiongNhau(benhNhanThuNhat.getNguoiLienHe(), benhNhanThuHai.getNguoiLienHe())) {
            return false;
        }
        if (!haiChuoiGiongNhau(benhNhanThuNhat.getTienSuBenh(), benhNhanThuHai.getTienSuBenh())) {
            return false;
        }
        if (!haiChuoiGiongNhau(benhNhanThuNhat.getDiUng(), benhNhanThuHai.getDiUng())) {
            return false;
        }
        return true;
    }

    private boolean haiChuoiGiongNhau(String chuoiThuNhat, String chuoiThuHai) {
        String giaTriThuNhat = doiChuoiTrongThanhNull(chuoiThuNhat);
        String giaTriThuHai = doiChuoiTrongThanhNull(chuoiThuHai);
        if (giaTriThuNhat == null && giaTriThuHai == null) {
            return true;
        }
        if (giaTriThuNhat == null || giaTriThuHai == null) {
            return false;
        }
        return giaTriThuNhat.equals(giaTriThuHai);
    }

    private String moTaThayDoi(BenhNhan benhNhanCu, BenhNhan benhNhanMoi) {
        StringBuilder noiDung = new StringBuilder();
        themMotThayDoi(noiDung, "Họ tên", benhNhanCu.getHoTen(), benhNhanMoi.getHoTen());
        themMotThayDoi(noiDung, "Ngày sinh", benhNhanCu.getNgaySinh().format(DINH_DANG_NGAY),
                benhNhanMoi.getNgaySinh().format(DINH_DANG_NGAY));
        themMotThayDoi(noiDung, "Giới tính", benhNhanCu.getGioiTinh(), benhNhanMoi.getGioiTinh());
        themMotThayDoi(noiDung, "SĐT", benhNhanCu.getSoDienThoai(), benhNhanMoi.getSoDienThoai());
        themMotThayDoi(noiDung, "Địa chỉ", benhNhanCu.getDiaChi(), benhNhanMoi.getDiaChi());
        themMotThayDoi(noiDung, "Người liên hệ", benhNhanCu.getNguoiLienHe(), benhNhanMoi.getNguoiLienHe());
        themMotThayDoi(noiDung, "Tiền sử bệnh", benhNhanCu.getTienSuBenh(), benhNhanMoi.getTienSuBenh());
        themMotThayDoi(noiDung, "Dị ứng", benhNhanCu.getDiUng(), benhNhanMoi.getDiUng());
        if (noiDung.length() == 0) {
            return "Không thay đổi thông tin.";
        }
        return noiDung.toString();
    }

    private void themMotThayDoi(StringBuilder noiDung, String tenO, String giaTriCu, String giaTriMoi) {
        if (haiChuoiGiongNhau(giaTriCu, giaTriMoi)) {
            return;
        }
        if (noiDung.length() > 0) {
            noiDung.append("; ");
        }
        noiDung.append(tenO).append(": '").append(hienKhongCoNeuTrong(giaTriCu))
                .append("' → '").append(hienKhongCoNeuTrong(giaTriMoi)).append("'");
    }

    private String hienKhongCoNeuTrong(String giaTri) {
        if (giaTri == null || giaTri.trim().isEmpty()) {
            return "(trống)";
        }
        return giaTri;
    }
}
