package com.quanlynhakhoa.lichhen.service;

import com.quanlynhakhoa.benhnhan.hopdong.BenhNhanTomTat;
import com.quanlynhakhoa.benhnhan.hopdong.TraCuuBenhNhan;
import com.quanlynhakhoa.benhnhan.hopdong.TraCuuBenhNhanService;
import com.quanlynhakhoa.benhnhan.dao.BenhNhanDao;
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

public class LichHenService {
    public static final int THOI_LUONG_PHUT = 30;

    public static final int SO_NAM_DAT_TRUOC_TOI_DA = 1;

    public static final int LY_DO_TOI_DA = 300;

    private static final DateTimeFormatter DINH_DANG_NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DesktopSession phienDangNhap;
    private final AuthorizationService phanQuyen = new AuthorizationService(new RolePermissionPolicy());
    private final LichHenDao lichHenDao = new LichHenDao();
    private final HoSoBenhAnDao hoSoBenhAnDao = new HoSoBenhAnDao();
    private final BenhNhanDao benhNhanDao = new BenhNhanDao();
    private final TraCuuBenhNhan traCuuBenhNhan;

    public LichHenService(DesktopSession phienDangNhap) {
        this.phienDangNhap = phienDangNhap;
        this.traCuuBenhNhan = new TraCuuBenhNhanService(phienDangNhap);
    }

    public boolean coQuyen(Permission quyen) {
        return phanQuyen.can(phienDangNhap, quyen);
    }

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

    public ArrayList<BenhNhanTomTat> timBenhNhan(String tuKhoa) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.APPOINTMENT_MANAGE);
        return traCuuBenhNhan.timBenhNhan(tuKhoa);
    }

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
        Collections.sort(danhSachGioTrong);
        return danhSachGioTrong;
    }

    private boolean biChongGio(LocalDateTime gioBatDauMoi, ArrayList<LocalDateTime> danhSachGioDaHen) {
        LocalDateTime gioKetThucMoi = gioBatDauMoi.plusMinutes(THOI_LUONG_PHUT);
        for (LocalDateTime gioDaHen : danhSachGioDaHen) {
            LocalDateTime gioKetThucDaHen = gioDaHen.plusMinutes(THOI_LUONG_PHUT);
            if (gioBatDauMoi.isBefore(gioKetThucDaHen) && gioDaHen.isBefore(gioKetThucMoi)) {
                return true;
            }
        }
        return false;
    }

    public String datLich(String maBenhNhan, String maBacSi, LocalDateTime thoiGianHen, String lyDoHen)
            throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.APPOINTMENT_MANAGE);
        if (maBenhNhan == null || maBenhNhan.isBlank()) {
            throw new IllegalArgumentException("Chọn bệnh nhân.");
        }
        kiemTraThoiGianHen(maBacSi, thoiGianHen);
        lyDoHen = kiemTraLyDo(lyDoHen, false);

        BenhNhanTomTat benhNhan = traCuuBenhNhan.layTheoMa(maBenhNhan);
        if (benhNhan == null) {
            throw new IllegalArgumentException("Bệnh nhân " + maBenhNhan + " không còn tồn tại, hãy chọn lại.");
        }

        try (Connection ketNoi = lichHenDao.moKetNoi()) {
            ketNoi.setAutoCommit(false);
            try {
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

                phanQuyen.require(phienDangNhap, Permission.APPOINTMENT_MANAGE);
                ketNoi.commit();
                return lichHenMoi.getMaLichHen();
            } catch (SQLException | RuntimeException loi) {
                ketNoi.rollback();
                throw loi;
            }
        }
    }

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
                LichHen lichHenCu = layLichHenConDaDat(ketNoi, maLichHenCu);

                kiemTraKhungGioConTrong(ketNoi, maBacSiMoi, thoiGianMoi, maLichHenCu);

                yeuCauDaCapNhatTrangThai(lichHenDao.doiTrangThai(ketNoi, maLichHenCu, LichHen.DA_DOI_LICH, null), maLichHenCu);

                LichHen lichHenMoi = new LichHen(lichHenDao.taoMaLichHenMoi(ketNoi));
                lichHenMoi.setMaBenhNhan(lichHenCu.getMaBenhNhan());
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

    public void huyLich(String maLichHen, String lyDoHuy) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.APPOINTMENT_MANAGE);
        if (maLichHen == null || maLichHen.isBlank()) {
            throw new IllegalArgumentException("Chọn lịch hẹn cần hủy.");
        }
        lyDoHuy = kiemTraLyDo(lyDoHuy, true);

        try (Connection ketNoi = lichHenDao.moKetNoi()) {
            ketNoi.setAutoCommit(false);
            try {
                layLichHenConDaDat(ketNoi, maLichHen);
                yeuCauDaCapNhatTrangThai(lichHenDao.doiTrangThai(ketNoi, maLichHen, LichHen.DA_HUY, lyDoHuy), maLichHen);
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

    public void xacNhanLichHen(String maLichHen) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.APPOINTMENT_MANAGE);
        if (maLichHen == null || maLichHen.isBlank()) {
            throw new IllegalArgumentException("Chọn lịch hẹn cần xác nhận.");
        }

        try (Connection ketNoi = lichHenDao.moKetNoi()) {
            ketNoi.setAutoCommit(false);
            try {
                LichHen lichHen = layLichHenConDaDat(ketNoi, maLichHen);
                if (!lichHen.laChoXacNhan()) {
                    throw new DuLieuDaThayDoiException("Lịch hẹn " + maLichHen + " đã được xác nhận trước đó.");
                }
                yeuCauDaCapNhatTrangThai(lichHenDao.doiTrangThai(ketNoi, maLichHen, LichHen.DA_XAC_NHAN, null), maLichHen);
                lichHenDao.ghiNhatKy(ketNoi, phienDangNhap.accountId(), "Xác nhận lịch hẹn", maLichHen,
                        "Đã gọi bệnh nhân, bệnh nhân xác nhận sẽ đến");

                phanQuyen.require(phienDangNhap, Permission.APPOINTMENT_MANAGE);
                ketNoi.commit();
            } catch (SQLException | RuntimeException loi) {
                ketNoi.rollback();
                throw loi;
            }
        }
    }

    public ArrayList<LichHen> layLichHenHomNay(String tuKhoa) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.PATIENT_CHECK_IN);
        if (tuKhoa == null) {
            tuKhoa = "";
        }
        tuKhoa = tuKhoa.trim();
        if (tuKhoa.length() > 100) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        }
        ArrayList<LichHen> tatCaLichHen = lichHenDao.layDanhSachTheoNgay(LocalDate.now(), tuKhoa);
        ArrayList<LichHen> danhSachCanTiepNhan = new ArrayList<>();
        for (LichHen lichHen : tatCaLichHen) {
            boolean daHuy = LichHen.DA_HUY.equals(lichHen.getTrangThai());
            boolean daDoiLich = LichHen.DA_DOI_LICH.equals(lichHen.getTrangThai());
            if (!daHuy && !daDoiLich) {
                danhSachCanTiepNhan.add(lichHen);
            }
        }
        return danhSachCanTiepNhan;
    }

    public String tiepDonBenhNhan(String maLichHen) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.PATIENT_CHECK_IN);
        if (maLichHen == null || maLichHen.isBlank()) {
            throw new IllegalArgumentException("Chọn lịch hẹn của bệnh nhân cần tiếp đón.");
        }

        try (Connection ketNoi = lichHenDao.moKetNoi()) {
            ketNoi.setAutoCommit(false);
            try {
                LichHen lichHen = layLichHenConDaDat(ketNoi, maLichHen);
                if (!lichHen.getThoiGianHen().toLocalDate().equals(LocalDate.now())) {
                    throw new IllegalArgumentException("Chỉ check-in bệnh nhân có lịch hẹn hôm nay (lịch này hẹn ngày "
                            + lichHen.getThoiGianHen().format(DINH_DANG_NGAY_GIO) + ").");
                }

                if (lichHenDao.demLuotKhamDangMo(ketNoi, lichHen.getMaBenhNhan()) > 0) {
                    throw new IllegalArgumentException("Bệnh nhân đang có lượt khám chưa hoàn tất.");
                }

                String maHoSo = layHoacTaoMaHoSo(ketNoi, lichHen.getMaBenhNhan());

                String maPhieuKham = lichHenDao.taoMaPhieuKhamMoi(ketNoi);
                lichHenDao.themLuotKhamChoKham(ketNoi, maPhieuKham, lichHen.getMaBenhNhan(), maHoSo,
                        lichHen.getMaBacSi(), maLichHen, LocalDateTime.now(), lichHen.getLyDoHen());
                yeuCauDaCapNhatTrangThai(lichHenDao.doiTrangThai(ketNoi, maLichHen, LichHen.DA_DEN, null), maLichHen);
                lichHenDao.ghiNhatKy(ketNoi, phienDangNhap.accountId(), "Check-in bệnh nhân", maLichHen,
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

    public ArrayList<BenhNhanTomTat> timBenhNhanDeTiepNhan(String tuKhoa) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.PATIENT_CHECK_IN);
        return traCuuBenhNhan.timBenhNhan(tuKhoa);
    }

    public ArrayList<BacSi> layBacSiDangTrongCa() throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.PATIENT_CHECK_IN);
        ArrayList<BacSi> tatCaBacSi = lichHenDao.layDanhSachBacSi();
        ArrayList<BacSi> danhSachBacSiTrongCa = new ArrayList<>();
        LocalDateTime bayGio = LocalDateTime.now();

        try (Connection ketNoi = lichHenDao.moKetNoi()) {
            for (BacSi bacSi : tatCaBacSi) {
                if (dangTrongCa(ketNoi, bacSi.getMaNhanVien(), bayGio)) {
                    int soNguoiDangCho = lichHenDao.demNguoiDangCho(ketNoi, bacSi.getMaNhanVien(), bayGio.toLocalDate());
                    bacSi.setSoNguoiDangCho(soNguoiDangCho);
                    danhSachBacSiTrongCa.add(bacSi);
                }
            }
        }
        return danhSachBacSiTrongCa;
    }

    public String tiepNhanKhongHen(String maBenhNhan, String maBacSi, String lyDoDen) throws SQLException {
        phanQuyen.require(phienDangNhap, Permission.PATIENT_CHECK_IN);
        if (maBenhNhan == null || maBenhNhan.isBlank()) {
            throw new IllegalArgumentException("Chọn bệnh nhân.");
        }
        if (maBacSi == null || maBacSi.isBlank()) {
            throw new IllegalArgumentException("Chọn bác sĩ.");
        }
        lyDoDen = kiemTraLyDo(lyDoDen, true);

        BenhNhanTomTat benhNhan = traCuuBenhNhan.layTheoMa(maBenhNhan);
        if (benhNhan == null) {
            throw new IllegalArgumentException("Bệnh nhân " + maBenhNhan + " không còn tồn tại, hãy chọn lại.");
        }

        LocalDateTime bayGio = LocalDateTime.now();
        try (Connection ketNoi = lichHenDao.moKetNoi()) {
            ketNoi.setAutoCommit(false);
            try {
                if (!dangTrongCa(ketNoi, maBacSi, bayGio)) {
                    throw new IllegalArgumentException("Bác sĩ đã chọn hiện không trong ca làm việc. Hãy chọn bác sĩ khác.");
                }

                String maLichHenHomNay = lichHenDao.timLichHenChuaDenTrongNgay(ketNoi, maBenhNhan, bayGio.toLocalDate());
                if (maLichHenHomNay != null) {
                    throw new IllegalArgumentException("Bệnh nhân có lịch hẹn hôm nay (" + maLichHenHomNay
                            + "). Hãy chọn lịch hẹn đó trong bảng và bấm \"Check-in\".");
                }

                if (lichHenDao.demLuotKhamDangMo(ketNoi, maBenhNhan) > 0) {
                    throw new IllegalArgumentException("Bệnh nhân đang có lượt khám chưa hoàn tất.");
                }

                String maHoSo = layHoacTaoMaHoSo(ketNoi, maBenhNhan);
                String maPhieuKham = lichHenDao.taoMaPhieuKhamMoi(ketNoi);
                lichHenDao.themLuotKhamChoKham(ketNoi, maPhieuKham, maBenhNhan, maHoSo, maBacSi, null, bayGio, lyDoDen);
                lichHenDao.ghiNhatKyTiepNhan(ketNoi, phienDangNhap.accountId(), maPhieuKham,
                        "Bệnh nhân " + maBenhNhan + ", bác sĩ " + maBacSi + ", lý do: " + lyDoDen);

                phanQuyen.require(phienDangNhap, Permission.PATIENT_CHECK_IN);
                ketNoi.commit();
                return maPhieuKham;
            } catch (SQLException | RuntimeException loi) {
                ketNoi.rollback();
                throw loi;
            }
        }
    }

    private boolean dangTrongCa(Connection ketNoi, String maBacSi, LocalDateTime thoiDiem) throws SQLException {
        ArrayList<LocalTime[]> danhSachCa = lichHenDao.layCaLamViec(ketNoi, maBacSi, thoiDiem.toLocalDate());
        for (LocalTime[] motCa : danhSachCa) {
            LocalTime gioBatDauCa = motCa[0];
            LocalTime gioKetThucCa = motCa[1];
            LocalTime gioHienTai = thoiDiem.toLocalTime();
            if (!gioHienTai.isBefore(gioBatDauCa) && gioHienTai.isBefore(gioKetThucCa)) {
                return true;
            }
        }
        return false;
    }

    private String layHoacTaoMaHoSo(Connection ketNoi, String maBenhNhan) throws SQLException {
        String maHoSo = hoSoBenhAnDao.timMaHoSo(ketNoi, maBenhNhan);
        if (maHoSo == null) {
            maHoSo = benhNhanDao.taoMaHoSoMoi(ketNoi);
            benhNhanDao.themHoSoBenhAn(ketNoi, maHoSo, maBenhNhan);
        }
        return maHoSo;
    }

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

    static void yeuCauDaCapNhatTrangThai(boolean daCapNhat, String maLichHen) {
        if (!daCapNhat) {
            throw new DuLieuDaThayDoiException(
                    "Lịch hẹn " + maLichHen
                    + " đã thay đổi trạng thái. Hãy tải lại danh sách và thử lại.");
        }
    }
    private LichHen layLichHenConDaDat(Connection ketNoi, String maLichHen) throws SQLException {
        LichHen lichHen = lichHenDao.timTheoMaDeSua(ketNoi, maLichHen);
        if (lichHen == null) {
            throw new IllegalArgumentException("Không tìm thấy lịch hẹn " + maLichHen + ".");
        }
        if (!lichHen.laDaDat()) {
            throw new DuLieuDaThayDoiException("Lịch hẹn " + maLichHen + " đang ở trạng thái \""
                    + lichHen.getTrangThaiHienThi() + "\", chỉ lịch \"Chờ xác nhận\" hoặc \"Đã xác nhận\" mới được thao tác.");
        }
        return lichHen;
    }
}
