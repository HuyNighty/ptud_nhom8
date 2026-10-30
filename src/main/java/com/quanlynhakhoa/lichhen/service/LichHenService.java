package com.quanlynhakhoa.lichhen.service;

import com.quanlynhakhoa.benhnhan.hopdong.BenhNhanTomTat;
import com.quanlynhakhoa.benhnhan.hopdong.TraCuuBenhNhan;
import com.quanlynhakhoa.benhnhan.hopdong.TraCuuBenhNhanService;
import com.quanlynhakhoa.benhnhan.dao.HoSoBenhAnDao;
import com.quanlynhakhoa.benhnhan.service.DuLieuDaThayDoiException;
import com.quanlynhakhoa.lichhen.dao.LichHenDao;
import com.quanlynhakhoa.lichhen.model.BacSi;
import com.quanlynhakhoa.lichhen.model.LichHen;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;

// Service LỊCH HẸN (UC004 trong báo cáo docs/tuan_cuoi): kiểm tra QUYỀN và QUY TẮC, rồi mới gọi DAO.
//   Xem danh sách:          quyền APPOINTMENT_READ
//   Đặt / đổi / hủy lịch:   quyền APPOINTMENT_MANAGE (lễ tân, admin)
//   Tiếp đón bệnh nhân:     quyền PATIENT_CHECK_IN (lễ tân, admin)
// Quy tắc:
//   BR02  lịch không chồng giờ với lịch khác của cùng bác sĩ, và nằm trọn trong ca làm việc
//   BR03  chỉ lịch "Đã đặt" mới được đổi hoặc hủy; hủy phải ghi lý do
//   BR09  bệnh nhân không có 2 lượt khám chưa hoàn tất cùng lúc (khi tiếp đón bệnh nhân)
//   BR10  kiểm tra lại NGAY LÚC LƯU (trong transaction, có khóa) — người khác vừa đặt thì báo lỗi
public class LichHenService {
    // Database V5 không có cột thời lượng / giờ kết thúc → mỗi lịch hẹn tạm tính 30 phút.
    // (Báo cáo cho chọn 30 / 60 / 90 phút: cần thêm cột trong database mới làm được.)
    public static final int THOI_LUONG_PHUT = 30;

    // Được đặt lịch xa nhất 1 năm kể từ hôm nay
    public static final int SO_NAM_DAT_TRUOC_TOI_DA = 1;

    public static final int LY_DO_TOI_DA = 300;   // giống cột lyDoHen, lyDoHuy (NVARCHAR(300))

    private static final DateTimeFormatter DINH_DANG_NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DesktopSession phienDangNhap;
    private final AuthorizationService phanQuyen = new AuthorizationService(new RolePermissionPolicy());
    private final LichHenDao lichHenDao = new LichHenDao();
    private final HoSoBenhAnDao hoSoBenhAnDao = new HoSoBenhAnDao();   // dùng lại: tìm mã hồ sơ của bệnh nhân
    private final TraCuuBenhNhan traCuuBenhNhan;   // hợp đồng với phần Bệnh nhân

    public LichHenService(DesktopSession phienDangNhap) {
        this.phienDangNhap = phienDangNhap;
        this.traCuuBenhNhan = new TraCuuBenhNhanService(phienDangNhap);
    }

    // Người đang đăng nhập có quyền này không (View dùng để làm mờ nút)
    public boolean coQuyen(Permission quyen) {
        return phanQuyen.can(phienDangNhap, quyen);
    }

    // ===================== XEM =====================

    public ArrayList<LichHen> layDanhSachLichHen(LocalDate ngay, String tuKhoa) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.APPOINTMENT_READ);
        if (ngay == null) {
            throw new IllegalArgumentException("Chọn ngày cần xem.");
        }
        if (tuKhoa == null) {
            tuKhoa = "";
        }
        tuKhoa = tuKhoa.trim();
        if (tuKhoa.length() > 100) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        }
        return lichHenDao.layDanhSachTheoNgay(ngay, tuKhoa);
    }

    public ArrayList<BacSi> layDanhSachBacSi() throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.APPOINTMENT_READ);
        return lichHenDao.layDanhSachBacSi();
    }

    // UC004 bước 1: tìm bệnh nhân (dùng hợp đồng TraCuuBenhNhan, không tự đọc bảng BenhNhan)
    public ArrayList<BenhNhanTomTat> timBenhNhan(String tuKhoa) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.APPOINTMENT_MANAGE);
        return traCuuBenhNhan.timBenhNhan(tuKhoa);
    }

    // UC004 bước 3: các giờ CÒN TRỐNG trong ca của bác sĩ ở 1 ngày, mỗi giờ cách nhau 30 phút.
    // maLichHenDangDoi: khi đổi lịch thì không tính chính lịch đó là "đã có người hẹn"; đặt mới thì null.
    public ArrayList<LocalTime> layGioTrong(String maBacSi, LocalDate ngay, String maLichHenDangDoi)
            throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.APPOINTMENT_MANAGE);
        kiemTraBacSiVaNgay(maBacSi, ngay);

        ArrayList<LocalTime> danhSachGioTrong = new ArrayList<>();
        try (Connection ketNoi = lichHenDao.moKetNoi()) {
            ArrayList<LocalTime[]> danhSachCa = lichHenDao.layCaLamViec(ketNoi, maBacSi, ngay);
            ArrayList<LocalDateTime> danhSachGioDaHen = lichHenDao.layGioDaHen(ketNoi, maBacSi, ngay, maLichHenDangDoi);

            for (LocalTime[] motCa : danhSachCa) {
                LocalTime gioBatDauCa = motCa[0];
                LocalTime gioKetThucCa = motCa[1];

                // Đi từ đầu ca, mỗi bước 30 phút, tới khi lịch hẹn không còn nằm trọn trong ca
                LocalDateTime gioDangXet = ngay.atTime(gioBatDauCa);
                LocalDateTime ketThucCa = ngay.atTime(gioKetThucCa);
                while (!gioDangXet.plusMinutes(THOI_LUONG_PHUT).isAfter(ketThucCa)) {
                    boolean daQuaGio = gioDangXet.isBefore(LocalDateTime.now());
                    boolean biChong = biChongGio(gioDangXet, danhSachGioDaHen);
                    boolean daCoTrongDanhSach = danhSachGioTrong.contains(gioDangXet.toLocalTime());
                    if (!daQuaGio && !biChong && !daCoTrongDanhSach) {
                        danhSachGioTrong.add(gioDangXet.toLocalTime());
                    }
                    gioDangXet = gioDangXet.plusMinutes(THOI_LUONG_PHUT);
                }
            }
        }
        Collections.sort(danhSachGioTrong);   // nhiều ca thì sắp lại theo giờ
        return danhSachGioTrong;
    }

    // Giờ bắt đầu mới có chồng lên lịch đã hẹn nào không (mỗi lịch dài THOI_LUONG_PHUT phút)
    private boolean biChongGio(LocalDateTime gioBatDauMoi, ArrayList<LocalDateTime> danhSachGioDaHen) {
        LocalDateTime gioKetThucMoi = gioBatDauMoi.plusMinutes(THOI_LUONG_PHUT);
        for (LocalDateTime gioDaHen : danhSachGioDaHen) {
            LocalDateTime gioKetThucDaHen = gioDaHen.plusMinutes(THOI_LUONG_PHUT);
            // 2 khoảng [a, b) và [c, d) chồng nhau khi a < d và c < b
            if (gioBatDauMoi.isBefore(gioKetThucDaHen) && gioDaHen.isBefore(gioKetThucMoi)) {
                return true;
            }
        }
        return false;
    }

    // ===================== ĐẶT LỊCH (UC004 luồng chính) =====================

    // Trả về mã lịch hẹn mới (ví dụ LH000007)
    public String datLich(String maBenhNhan, String maBacSi, LocalDateTime thoiGianHen, String lyDoHen)
            throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.APPOINTMENT_MANAGE);
        if (maBenhNhan == null || maBenhNhan.isBlank()) {
            throw new IllegalArgumentException("Chọn bệnh nhân.");
        }
        kiemTraThoiGianHen(maBacSi, thoiGianHen);
        lyDoHen = kiemTraLyDo(lyDoHen, false);

        // BR10: bệnh nhân còn tồn tại không (đọc qua hợp đồng với phần Bệnh nhân)
        BenhNhanTomTat benhNhan = traCuuBenhNhan.layTheoMa(maBenhNhan);
        if (benhNhan == null) {
            throw new IllegalArgumentException("Bệnh nhân " + maBenhNhan + " không còn tồn tại, hãy chọn lại.");
        }

        try (Connection ketNoi = lichHenDao.moKetNoi()) {
            ketNoi.setAutoCommit(false);          // bắt đầu transaction
            try {
                // UC004 bước 5: kiểm tra lại ngay lúc lưu (BR02)
                kiemTraKhungGioConTrong(ketNoi, maBacSi, thoiGianHen, null);

                LichHen lichHenMoi = new LichHen(lichHenDao.taoMaLichHenMoi(ketNoi));
                lichHenMoi.setMaBenhNhan(maBenhNhan);
                lichHenMoi.setMaBacSi(maBacSi);
                lichHenMoi.setThoiGianHen(thoiGianHen);
                lichHenMoi.setLyDoHen(lyDoHen);
                lichHenDao.themLichHen(ketNoi, lichHenMoi, phienDangNhap.accountId());

                lichHenDao.ghiNhatKy(ketNoi, phienDangNhap.accountId(), "Đặt lịch hẹn", lichHenMoi.getMaLichHen(),
                        "Bệnh nhân " + maBenhNhan + ", bác sĩ " + maBacSi
                        + ", lúc " + thoiGianHen.format(DINH_DANG_NGAY_GIO));

                phanQuyen.require(phienDangNhap, Permission.APPOINTMENT_MANAGE);   // kiểm tra lại trước khi lưu thật
                ketNoi.commit();                  // tất cả thành công → lưu thật
                return lichHenMoi.getMaLichHen();
            } catch (SQLException | RuntimeException loi) {
                ketNoi.rollback();                // có lỗi → hủy hết
                throw loi;
            }
        }
    }

    // ===================== ĐỔI LỊCH (UC004 A1) =====================

    // Lịch cũ chuyển "Đã đổi lịch", tạo lịch mới (trỏ về lịch cũ bằng maLichHenGoc). Trả về mã lịch mới.
    public String doiLich(String maLichHenCu, String maBacSiMoi, LocalDateTime thoiGianMoi, String lyDoHen)
            throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.APPOINTMENT_MANAGE);
        if (maLichHenCu == null || maLichHenCu.isBlank()) {
            throw new IllegalArgumentException("Chọn lịch hẹn cần đổi.");
        }
        kiemTraThoiGianHen(maBacSiMoi, thoiGianMoi);
        lyDoHen = kiemTraLyDo(lyDoHen, false);

        try (Connection ketNoi = lichHenDao.moKetNoi()) {
            ketNoi.setAutoCommit(false);
            try {
                // Đọc lại lịch cũ + khóa; không còn "Đã đặt" thì không cho đổi (UC004 E2, BR03)
                LichHen lichHenCu = layLichHenConDaDat(ketNoi, maLichHenCu);

                // Kiểm tra giờ mới (không tính chính lịch cũ)
                kiemTraKhungGioConTrong(ketNoi, maBacSiMoi, thoiGianMoi, maLichHenCu);

                lichHenDao.doiTrangThai(ketNoi, maLichHenCu, LichHen.DA_DOI_LICH, null);

                LichHen lichHenMoi = new LichHen(lichHenDao.taoMaLichHenMoi(ketNoi));
                lichHenMoi.setMaBenhNhan(lichHenCu.getMaBenhNhan());   // đổi lịch không đổi bệnh nhân
                lichHenMoi.setMaBacSi(maBacSiMoi);
                lichHenMoi.setThoiGianHen(thoiGianMoi);
                lichHenMoi.setLyDoHen(lyDoHen);
                lichHenMoi.setMaLichHenGoc(maLichHenCu);
                lichHenDao.themLichHen(ketNoi, lichHenMoi, phienDangNhap.accountId());

                lichHenDao.ghiNhatKy(ketNoi, phienDangNhap.accountId(), "Đổi lịch hẹn", lichHenMoi.getMaLichHen(),
                        "Từ " + maLichHenCu + " (bác sĩ " + lichHenCu.getMaBacSi()
                        + ", " + lichHenCu.getThoiGianHen().format(DINH_DANG_NGAY_GIO) + ") → "
                        + "bác sĩ " + maBacSiMoi + ", " + thoiGianMoi.format(DINH_DANG_NGAY_GIO));

                phanQuyen.require(phienDangNhap, Permission.APPOINTMENT_MANAGE);
                ketNoi.commit();
                return lichHenMoi.getMaLichHen();
            } catch (SQLException | RuntimeException loi) {
                ketNoi.rollback();
                throw loi;
            }
        }
    }

    // ===================== HỦY LỊCH (UC004 A2) =====================

    public void huyLich(String maLichHen, String lyDoHuy) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.APPOINTMENT_MANAGE);
        if (maLichHen == null || maLichHen.isBlank()) {
            throw new IllegalArgumentException("Chọn lịch hẹn cần hủy.");
        }
        lyDoHuy = kiemTraLyDo(lyDoHuy, true);   // BR03: hủy phải ghi lý do

        try (Connection ketNoi = lichHenDao.moKetNoi()) {
            ketNoi.setAutoCommit(false);
            try {
                layLichHenConDaDat(ketNoi, maLichHen);   // không còn "Đã đặt" → báo lỗi
                lichHenDao.doiTrangThai(ketNoi, maLichHen, LichHen.DA_HUY, lyDoHuy);
                lichHenDao.ghiNhatKy(ketNoi, phienDangNhap.accountId(), "Hủy lịch hẹn", maLichHen,
                        "Lý do: " + lyDoHuy);

                phanQuyen.require(phienDangNhap, Permission.APPOINTMENT_MANAGE);
                ketNoi.commit();
            } catch (SQLException | RuntimeException loi) {
                ketNoi.rollback();
                throw loi;
            }
        }
    }

    // ===================== TIẾP ĐÓN BỆNH NHÂN (UC005, bệnh nhân có lịch hẹn) =====================

    // Bệnh nhân có lịch hẹn hôm nay đã tới phòng khám:
    //   1. tạo lượt khám "Chờ khám" (bảng PhieuKham) với bác sĩ trong lịch hẹn
    //   2. chuyển lịch hẹn sang "Đã đến"
    // Trả về mã phiếu khám mới (ví dụ PK000012).
    public String tiepDonBenhNhan(String maLichHen) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.PATIENT_CHECK_IN);
        if (maLichHen == null || maLichHen.isBlank()) {
            throw new IllegalArgumentException("Chọn lịch hẹn của bệnh nhân cần tiếp đón.");
        }

        try (Connection ketNoi = lichHenDao.moKetNoi()) {
            ketNoi.setAutoCommit(false);
            try {
                // Đọc lại lịch hẹn + khóa; phải còn "Đã đặt" (BR10)
                LichHen lichHen = layLichHenConDaDat(ketNoi, maLichHen);
                if (!lichHen.getThoiGianHen().toLocalDate().equals(LocalDate.now())) {
                    throw new IllegalArgumentException("Chỉ tiếp đón bệnh nhân có lịch hẹn hôm nay (lịch này hẹn ngày "
                            + lichHen.getThoiGianHen().format(DINH_DANG_NGAY_GIO) + ").");
                }

                // BR09 / UC005 E1: đang có lượt khám chưa hoàn tất thì không tạo thêm
                if (lichHenDao.demLuotKhamDangMo(ketNoi, lichHen.getMaBenhNhan()) > 0) {
                    throw new IllegalArgumentException("Bệnh nhân đang có lượt khám chưa hoàn tất.");
                }

                // Lượt khám phải gắn với hồ sơ bệnh án của bệnh nhân
                String maHoSo = hoSoBenhAnDao.timMaHoSo(ketNoi, lichHen.getMaBenhNhan());
                if (maHoSo == null) {
                    throw new IllegalArgumentException("Bệnh nhân " + lichHen.getMaBenhNhan()
                            + " chưa có hồ sơ bệnh án. Mở màn hình Bệnh nhân, bấm Sửa rồi Lưu để tạo hồ sơ.");
                }

                String maPhieuKham = lichHenDao.taoMaPhieuKhamMoi(ketNoi);
                lichHenDao.themLuotKhamChoKham(ketNoi, maPhieuKham, lichHen, maHoSo, LocalDateTime.now());
                lichHenDao.doiTrangThai(ketNoi, maLichHen, LichHen.DA_DEN, null);
                lichHenDao.ghiNhatKy(ketNoi, phienDangNhap.accountId(), "Tiếp đón bệnh nhân", maLichHen,
                        "Tạo lượt khám " + maPhieuKham + " (Chờ khám), bác sĩ " + lichHen.getMaBacSi());

                phanQuyen.require(phienDangNhap, Permission.PATIENT_CHECK_IN);
                ketNoi.commit();
                return maPhieuKham;
            } catch (SQLException | RuntimeException loi) {
                ketNoi.rollback();
                throw loi;
            }
        }
    }

    // ===================== Các hàm kiểm tra dùng chung =====================

    private void kiemTraBacSiVaNgay(String maBacSi, LocalDate ngay) {
        if (maBacSi == null || maBacSi.isBlank()) {
            throw new IllegalArgumentException("Chọn bác sĩ.");
        }
        if (ngay == null) {
            throw new IllegalArgumentException("Chọn ngày hẹn.");
        }
        if (ngay.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày hẹn không được ở quá khứ.");
        }
        if (ngay.isAfter(LocalDate.now().plusYears(SO_NAM_DAT_TRUOC_TOI_DA))) {
            throw new IllegalArgumentException("Chỉ được đặt lịch trước tối đa " + SO_NAM_DAT_TRUOC_TOI_DA + " năm.");
        }
    }

    private void kiemTraThoiGianHen(String maBacSi, LocalDateTime thoiGianHen) {
        if (thoiGianHen == null) {
            throw new IllegalArgumentException("Chọn giờ hẹn (bấm \"Xem giờ trống\").");
        }
        kiemTraBacSiVaNgay(maBacSi, thoiGianHen.toLocalDate());
        if (thoiGianHen.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Giờ hẹn đã qua, hãy chọn giờ khác.");
        }
    }

    // Bỏ khoảng trắng 2 đầu; để trống → null (hoặc báo lỗi nếu batBuoc); dài quá 300 → báo lỗi
    private String kiemTraLyDo(String lyDo, boolean batBuoc) {
        if (lyDo == null || lyDo.trim().isEmpty()) {
            if (batBuoc) {
                throw new IllegalArgumentException("Nhập lý do.");
            }
            return null;
        }
        lyDo = lyDo.trim();
        if (lyDo.length() > LY_DO_TOI_DA) {
            throw new IllegalArgumentException("Lý do tối đa " + LY_DO_TOI_DA + " ký tự (đang nhập "
                    + lyDo.length() + ").");
        }
        return lyDo;
    }

    // BR02, kiểm tra trong transaction (có khóa):
    //   1. lịch nằm trọn trong 1 ca làm việc của bác sĩ
    //   2. không chồng giờ với lịch còn hiệu lực khác của bác sĩ
    // Sai → DuLieuDaThayDoiException ("Khung giờ không còn trống", UC004 E1) để Controller tải lại giờ trống.
    private void kiemTraKhungGioConTrong(Connection ketNoi, String maBacSi, LocalDateTime batDau,
                                         String maBoQua) throws SQLException {
        LocalDateTime ketThuc = batDau.plusMinutes(THOI_LUONG_PHUT);

        boolean namTrongCa = false;
        ArrayList<LocalTime[]> danhSachCa = lichHenDao.layCaLamViec(ketNoi, maBacSi, batDau.toLocalDate());
        for (LocalTime[] motCa : danhSachCa) {
            LocalDateTime batDauCa = batDau.toLocalDate().atTime(motCa[0]);
            LocalDateTime ketThucCa = batDau.toLocalDate().atTime(motCa[1]);
            if (!batDau.isBefore(batDauCa) && !ketThuc.isAfter(ketThucCa)) {
                namTrongCa = true;
            }
        }
        if (!namTrongCa) {
            throw new DuLieuDaThayDoiException("Khung giờ không còn trống: giờ hẹn không nằm trong ca làm việc "
                    + "của bác sĩ. Hãy chọn lại giờ.");
        }

        int soLichChong = lichHenDao.demLichChongGio(ketNoi, maBacSi, batDau, ketThuc, THOI_LUONG_PHUT, maBoQua);
        if (soLichChong > 0) {
            throw new DuLieuDaThayDoiException("Khung giờ không còn trống: bác sĩ vừa có lịch hẹn khác "
                    + "trùng giờ này. Hãy chọn lại giờ.");
        }
    }

    // Đọc lại lịch hẹn (có khóa). Không có → báo lỗi; không còn "Đã đặt" → DuLieuDaThayDoiException (UC004 E2)
    private LichHen layLichHenConDaDat(Connection ketNoi, String maLichHen) throws SQLException {
        LichHen lichHen = lichHenDao.timTheoMaDeSua(ketNoi, maLichHen);
        if (lichHen == null) {
            throw new IllegalArgumentException("Không tìm thấy lịch hẹn " + maLichHen + ".");
        }
        if (!lichHen.laDaDat()) {
            throw new DuLieuDaThayDoiException("Lịch hẹn " + maLichHen + " đang ở trạng thái \""
                    + lichHen.getTrangThaiHienThi() + "\", chỉ lịch \"Đã đặt\" mới được đổi hoặc hủy.");
        }
        return lichHen;
    }
}
