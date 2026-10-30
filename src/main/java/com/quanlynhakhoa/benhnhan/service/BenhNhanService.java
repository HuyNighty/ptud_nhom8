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

    // Độ dài tối đa, giống độ dài cột trong database (V5)
    public static final int HO_TEN_TOI_DA = 100;
    public static final int DIA_CHI_TOI_DA = 255;
    public static final int NGUOI_LIEN_HE_TOI_DA = 200;
    public static final int TIEN_SU_TOI_DA = 500;
    public static final int DI_UNG_TOI_DA = 300;

    private static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DesktopSession phienDangNhap;   // người đang đăng nhập
    private final AuthorizationService phanQuyen = new AuthorizationService(new RolePermissionPolicy());
    private final BenhNhanDao benhNhanDao;
    private final HoSoBenhAnDao hoSoBenhAnDao;     // tiền sử bệnh + dị ứng lưu ở bảng HoSoBenhAn
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

    // Người đang đăng nhập có quyền này không (View dùng để làm mờ nút)
    public boolean coQuyen(Permission quyen) {
        return phanQuyen.can(phienDangNhap, quyen);
    }

    // ===================== XEM / TÌM KIẾM =====================

    public ArrayList<BenhNhan> timKiem(String tuKhoa, String gioiTinh) throws SQLException {
        // 1. Kiểm tra quyền: không có quyền xem bệnh nhân thì báo lỗi
        phanQuyen.require(phienDangNhap, Permission.PATIENT_READ);

        // 2. Kiểm tra dữ liệu nhập
        tuKhoa = tuKhoa.trim();
        if (tuKhoa.length() > 100) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        }
        // Nếu người dùng gõ số điện thoại có khoảng trắng (0909 123 456) thì bỏ khoảng trắng
        String tuKhoaKhongKhoangTrang = tuKhoa.replace(" ", "");
        if (!tuKhoaKhongKhoangTrang.isEmpty() && tuKhoaKhongKhoangTrang.matches("[0-9]+")) {
            if (tuKhoaKhongKhoangTrang.length() > 10) {
                throw new IllegalArgumentException("Số điện thoại không hợp lệ: gồm 10 chữ số và bắt đầu bằng 0.");
            }
            tuKhoa = tuKhoaKhongKhoangTrang;
        }

        // 3. Gọi DAO lấy dữ liệu
        if (tuKhoa.isEmpty() && gioiTinh.isEmpty()) {
            return benhNhanDao.getAllBenhNhan();       // không lọc gì → lấy tất cả
        }
        return benhNhanDao.timKiem(tuKhoa, gioiTinh);
    }

    public BenhNhan xemChiTiet(String maBenhNhan) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.PATIENT_READ);
        return benhNhanDao.timTheoMa(maBenhNhan);
    }

    // ===================== KIỂM TRA DỮ LIỆU =====================

    // Kiểm tra và chuẩn hóa dữ liệu trên form.
    // Sai ô nào thì ném LoiNhapLieuException kèm TÊN Ô đó để form tô đỏ (UC003 E1).
    public void kiemTraDuLieu(BenhNhan benhNhan) {
        // Họ tên: bắt buộc, 2–100 ký tự, bỏ khoảng trắng thừa
        String hoTen = "";
        if (benhNhan.getHoTen() != null) {
            hoTen = benhNhan.getHoTen().trim().replaceAll("\\s+", " ");
        }
        // Đưa dấu tiếng Việt về 1 dạng chuẩn (NFC) để so trùng họ tên cho đúng
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

        // Ngày sinh: bắt buộc, không ở tương lai, không quá 120 tuổi
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

        // Giới tính: NAM hoặc NU. KHAC chỉ còn ở dữ liệu cũ (form thêm mới không cho chọn),
        // vẫn cho lưu để khi sửa bệnh nhân cũ thì giới tính không bị tự đổi thành Nam.
        String gioiTinh = benhNhan.getGioiTinh();
        if (!"NAM".equals(gioiTinh) && !"NU".equals(gioiTinh) && !"KHAC".equals(gioiTinh)) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_GIOI_TINH, "Chọn giới tính.");
        }

        // SĐT: chỉ bỏ khoảng trắng 2 đầu; bên trong phải toàn là chữ số (10 số, bắt đầu bằng 0)
        String soDienThoai = "";
        if (benhNhan.getSoDienThoai() != null) {
            soDienThoai = benhNhan.getSoDienThoai().trim();
        }
        if (!soDienThoai.matches(REGEX_SO_DIEN_THOAI)) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_SO_DIEN_THOAI,
                    "Số điện thoại chỉ được nhập chữ số: đúng 10 số, bắt đầu bằng 0 "
                    + "(không nhập chữ, khoảng trắng hay dấu chấm).");
        }
        benhNhan.setSoDienThoai(soDienThoai);

        // Địa chỉ, người liên hệ: không bắt buộc; để trống thì lưu NULL
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

        // Tiền sử bệnh, dị ứng: không bắt buộc; để trống thì lưu NULL
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

    // Đổi chuỗi ngày sinh "dd/MM/yyyy" (lấy từ ô ngày sinh đã chọn trên lịch) thành LocalDate.
    // Kiểm tra bằng regex trước, sau đó kiểm tra ngày có thật không (ví dụ 31/02 là sai).
    public static LocalDate docNgaySinh(String chuoiNgaySinh) {
        if (!chuoiNgaySinh.matches(REGEX_NGAY_SINH)) {
            throw new LoiNhapLieuException(LoiNhapLieuException.O_NGAY_SINH,
                    "Chọn ngày sinh (bấm nút \"Chọn...\").");
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

    // Chuỗi null hoặc toàn khoảng trắng → null; còn lại thì bỏ khoảng trắng 2 đầu
    private String doiChuoiTrongThanhNull(String chuoi) {
        if (chuoi == null || chuoi.trim().isEmpty()) {
            return null;
        }
        return chuoi.trim();
    }

    // Tìm hồ sơ nghi trùng theo BR01: cùng họ tên + ngày sinh + số điện thoại.
    // Chỉ để CẢNH BÁO, không chặn: lễ tân quyết định dùng hồ sơ cũ hay vẫn tạo mới (UC003 A2).
    public ArrayList<BenhNhan> timNghiTrung(BenhNhan benhNhan) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.PATIENT_READ);
        kiemTraDuLieu(benhNhan);
        return benhNhanDao.timNghiTrung(benhNhan.getSoDienThoai(), benhNhan.getHoTen(),
                benhNhan.getNgaySinh(), benhNhan.getMaBenhNhan());
    }

    // ===================== THÊM =====================

    // Thêm bệnh nhân mới + tạo hồ sơ bệnh án + ghi nhật ký, trong CÙNG 1 transaction.
    // Trả về mã bệnh nhân mới (ví dụ BN000007).
    public String themBenhNhan(BenhNhan benhNhan) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.PATIENT_CREATE);
        kiemTraDuLieu(benhNhan);

        try (Connection ketNoi = cachMoKetNoi.open()) {
            if (!ketNoi.getAutoCommit()) {
                throw new SQLException("Expected a fresh JDBC connection");
            }
            ketNoi.setAutoCommit(false);          // bắt đầu transaction
            try {
                String maBenhNhan = benhNhanDao.taoMaBenhNhanMoi(ketNoi);
                benhNhan.setMaBenhNhan(maBenhNhan);
                benhNhanDao.themBenhNhan(ketNoi, benhNhan, phienDangNhap.accountId());

                String maHoSo = benhNhanDao.taoMaHoSoMoi(ketNoi);
                benhNhanDao.themHoSoBenhAn(ketNoi, maHoSo, maBenhNhan,
                        benhNhan.getTienSuBenh(), benhNhan.getDiUng());

                benhNhanDao.ghiNhatKy(ketNoi, phienDangNhap.accountId(), "Thêm bệnh nhân", maBenhNhan);
                phanQuyen.require(phienDangNhap, Permission.PATIENT_CREATE);   // kiểm tra lại trước khi lưu thật

                ketNoi.commit();                  // tất cả thành công → lưu thật
                return maBenhNhan;
            } catch (SQLException | RuntimeException loi) {
                ketNoi.rollback();                // có lỗi → hủy hết, không lưu nửa chừng
                benhNhan.setMaBenhNhan(null);
                throw loi;
            }
        }
    }

    // ===================== SỬA =====================

    // Sửa thông tin bệnh nhân + tiền sử / dị ứng + ghi nhật ký, trong cùng 1 transaction.
    //   benhNhanMoi:        dữ liệu trên form lúc bấm Lưu
    //   benhNhanLucMoForm:  dữ liệu lúc mở form Sửa (đọc từ database lúc đó)
    // Các bước:
    //   1. Đọc lại bệnh nhân trong database và khóa dòng đó
    //   2. BR10: nếu khác dữ liệu lúc mở form → người khác vừa sửa → báo lỗi, KHÔNG ghi đè
    //   3. Cập nhật bệnh nhân, tiền sử, dị ứng
    //   4. BR07 / UC003 A1: ghi nhật ký kèm giá trị CŨ → MỚI
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
                // Bước 1
                BenhNhan benhNhanTrongDatabase = benhNhanDao.timTheoMaDeSua(ketNoi, maBenhNhan);
                if (benhNhanTrongDatabase == null) {
                    throw new IllegalArgumentException("Không tìm thấy bệnh nhân " + maBenhNhan + ".");
                }

                // Bước 2
                if (!giongNhau(benhNhanTrongDatabase, benhNhanLucMoForm)) {
                    throw new DuLieuDaThayDoiException("Bệnh nhân " + maBenhNhan
                            + " vừa được người khác sửa. Dữ liệu mới nhất sẽ được tải lại, "
                            + "hãy kiểm tra rồi sửa lại.");
                }

                // Bước 3
                if (!benhNhanDao.capNhatBenhNhan(ketNoi, benhNhanMoi)) {
                    throw new IllegalArgumentException("Không tìm thấy bệnh nhân " + maBenhNhan + ".");
                }
                // Tiền sử bệnh + dị ứng nằm ở HoSoBenhAn; bệnh nhân chưa có hồ sơ thì tạo mới
                String maHoSo = hoSoBenhAnDao.timMaHoSo(ketNoi, maBenhNhan);
                if (maHoSo == null) {
                    String maHoSoMoi = benhNhanDao.taoMaHoSoMoi(ketNoi);
                    benhNhanDao.themHoSoBenhAn(ketNoi, maHoSoMoi, maBenhNhan,
                            benhNhanMoi.getTienSuBenh(), benhNhanMoi.getDiUng());
                } else {
                    hoSoBenhAnDao.capNhatTienSu(ketNoi, maHoSo,
                            benhNhanMoi.getTienSuBenh(), benhNhanMoi.getDiUng());
                }

                // Bước 4
                String noiDungThayDoi = moTaThayDoi(benhNhanTrongDatabase, benhNhanMoi);
                benhNhanDao.ghiNhatKy(ketNoi, phienDangNhap.accountId(), "Sửa thông tin bệnh nhân",
                        maBenhNhan, noiDungThayDoi);

                phanQuyen.require(phienDangNhap, Permission.PATIENT_UPDATE);   // kiểm tra lại trước khi lưu thật
                ketNoi.commit();
            } catch (SQLException | RuntimeException loi) {
                ketNoi.rollback();
                throw loi;
            }
        }
    }

    // So 2 bệnh nhân: giống nhau ở TẤT CẢ thông tin được sửa thì trả về true
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

    // So 2 chuỗi; null và chuỗi rỗng xem như giống nhau (đều là "không có")
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

    // Viết nội dung nhật ký: chỉ liệt kê những ô có thay đổi, dạng "Tên ô: 'cũ' → 'mới'"
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
            return;   // không đổi thì không ghi
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
