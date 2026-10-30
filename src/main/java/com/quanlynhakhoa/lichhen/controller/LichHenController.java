package com.quanlynhakhoa.lichhen.controller;

import com.quanlynhakhoa.benhnhan.hopdong.BenhNhanTomTat;
import com.quanlynhakhoa.benhnhan.service.DuLieuDaThayDoiException;
import com.quanlynhakhoa.benhnhan.view.ChonNgayDialog;
import com.quanlynhakhoa.lichhen.model.BacSi;
import com.quanlynhakhoa.lichhen.model.LichHen;
import com.quanlynhakhoa.lichhen.service.LichHenService;
import com.quanlynhakhoa.lichhen.view.LichHenDialog;
import com.quanlynhakhoa.lichhen.view.LichHenPanel;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.Permission;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class LichHenController implements ActionListener, MouseListener {
    private LichHenPanel view;
    private LichHenService lichHenService;

    private LichHenDialog winLichHen;
    private boolean dangTaiDanhSach;
    private boolean dangXuLyForm;

    public LichHenController(LichHenPanel view, LichHenService lichHenService) {
        this.view = view;
        this.lichHenService = lichHenService;

        view.getBtnChonNgayXem().addActionListener(this);
        view.getBtnHomNay().addActionListener(this);
        view.getTxtTimKiem().addActionListener(this);
        view.getBtnTimKiem().addActionListener(this);
        view.getBtnDatLich().addActionListener(this);
        view.getBtnTiepDon().addActionListener(this);
        view.getBtnDoiLich().addActionListener(this);
        view.getBtnHuyLich().addActionListener(this);
        view.getTblLichHen().addMouseListener(this);

        boolean duocQuanLyLich = lichHenService.coQuyen(Permission.APPOINTMENT_MANAGE);
        boolean duocTiepDon = lichHenService.coQuyen(Permission.PATIENT_CHECK_IN);
        view.datQuyen(duocQuanLyLich, duocTiepDon);

        taiDanhSach(null);
    }

    @Override
    public void actionPerformed(ActionEvent suKien) {
        Object nguonSuKien = suKien.getSource();

        if (nguonSuKien.equals(view.getBtnTimKiem()) || nguonSuKien.equals(view.getTxtTimKiem())) {
            taiDanhSach(null);
        }
        if (nguonSuKien.equals(view.getBtnHomNay())) {
            view.datNgayDangXem(LocalDate.now());
            taiDanhSach(null);
        }
        if (nguonSuKien.equals(view.getBtnChonNgayXem())) {
            chonNgayXem();
        }
        if (nguonSuKien.equals(view.getBtnDatLich())) {
            moForm(null);
        }
        if (nguonSuKien.equals(view.getBtnTiepDon())) {
            tiepDonBenhNhan();
        }
        if (nguonSuKien.equals(view.getBtnDoiLich())) {
            LichHen lichHen = layLichHenDaDatDangChon("đổi");
            if (lichHen != null) {
                moForm(lichHen);
            }
        }
        if (nguonSuKien.equals(view.getBtnHuyLich())) {
            LichHen lichHen = layLichHenDaDatDangChon("hủy");
            if (lichHen != null) {
                huyLich(lichHen);
            }
        }

        if (winLichHen == null) {
            return;
        }
        if (nguonSuKien.equals(winLichHen.getBtnTimBenhNhan()) || nguonSuKien.equals(winLichHen.getTxtTimBenhNhan())) {
            timBenhNhan();
        }
        if (nguonSuKien.equals(winLichHen.getCmbBacSi())) {
            winLichHen.xoaGioTrong();
        }
        if (nguonSuKien.equals(winLichHen.getBtnChonNgay())) {
            winLichHen.moLichChonNgayHen();
        }
        if (nguonSuKien.equals(winLichHen.getBtnXemGioTrong())) {
            xemGioTrong();
        }
        if (nguonSuKien.equals(winLichHen.getBtnLuu())) {
            luu();
        }
        if (nguonSuKien.equals(winLichHen.getBtnHuy())) {
            winLichHen.dispose();
        }
    }

    @Override
    public void mouseClicked(MouseEvent suKien) {
        view.hienChiTiet(view.layLichHenDangChon());
    }

    @Override
    public void mousePressed(MouseEvent suKien) {
    }

    @Override
    public void mouseReleased(MouseEvent suKien) {
    }

    @Override
    public void mouseEntered(MouseEvent suKien) {
    }

    @Override
    public void mouseExited(MouseEvent suKien) {
    }

    private void taiDanhSach(String maCanChon) {
        if (dangTaiDanhSach) {
            return;
        }
        dangTaiDanhSach = true;
        LocalDate ngay = view.getNgayDangXem();
        String tuKhoa = view.getTxtTimKiem().getText();
        view.datDangTai(true);

        new SwingWorker<ArrayList<LichHen>, Void>() {
            @Override
            protected ArrayList<LichHen> doInBackground() throws Exception {
                return lichHenService.layDanhSachLichHen(ngay, tuKhoa);
            }

            @Override
            protected void done() {
                dangTaiDanhSach = false;
                view.datDangTai(false);
                try {
                    view.hienDanhSachLichHen(get());
                    if (maCanChon != null) {
                        view.chonDongTheoMa(maCanChon);
                    }
                } catch (Exception loi) {
                    view.hienLoi(thongBaoLoi(loi));
                }
            }
        }.execute();
    }

    private void chonNgayXem() {
        LocalDate homNay = LocalDate.now();
        LocalDate ngayDaChon = ChonNgayDialog.chon(SwingUtilities.getWindowAncestor(view), "Chọn ngày xem lịch hẹn",
                view.getNgayDangXem(), homNay.minusYears(5), homNay.plusYears(LichHenService.SO_NAM_DAT_TRUOC_TOI_DA));
        if (ngayDaChon != null) {
            view.datNgayDangXem(ngayDaChon);
            taiDanhSach(null);
        }
    }

    private LichHen layLichHenDaDatDangChon(String viecCanLam) {
        LichHen lichHen = view.layLichHenDangChon();
        if (lichHen == null) {
            view.hienLoi("Chọn 1 lịch hẹn trong bảng để " + viecCanLam + ".");
            return null;
        }
        if (!lichHen.laDaDat()) {
            view.hienLoi("Lịch hẹn " + lichHen.getMaLichHen() + " đang ở trạng thái \""
                    + lichHen.getTrangThaiHienThi() + "\". Chỉ lịch \"Đã đặt\" mới được " + viecCanLam + ".");
            return null;
        }
        return lichHen;
    }

    private void tiepDonBenhNhan() {
        LichHen lichHen = view.layLichHenDangChon();
        if (lichHen == null) {
            view.hienLoi("Chọn 1 lịch hẹn trong bảng để tiếp đón bệnh nhân.");
            return;
        }
        if (!lichHen.duocTiepDon()) {
            view.hienLoi("Chỉ tiếp đón bệnh nhân có lịch hẹn \"Đã đặt\" của hôm nay.");
            return;
        }
        String cauHoi = "Tiếp đón bệnh nhân " + lichHen.getTenBenhNhan() + "?\n"
                + "Lịch hẹn chuyển \"Đã đến\" và hệ thống tạo lượt khám \"Chờ khám\" với bác sĩ "
                + lichHen.getTenBacSi() + ".";
        int luaChon = JOptionPane.showConfirmDialog(view, cauHoi, "Tiếp đón bệnh nhân", JOptionPane.YES_NO_OPTION);
        if (luaChon != JOptionPane.YES_OPTION) {
            return;
        }
        String maLichHen = lichHen.getMaLichHen();
        String tenBacSi = lichHen.getTenBacSi();
        view.datDangTai(true);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return lichHenService.tiepDonBenhNhan(maLichHen);
            }

            @Override
            protected void done() {
                view.datDangTai(false);
                try {
                    String maPhieuKham = get();
                    JOptionPane.showMessageDialog(view, "Đã tạo lượt khám " + maPhieuKham
                            + " (Chờ khám). Bệnh nhân vào danh sách chờ của bác sĩ " + tenBacSi + ".");
                } catch (Exception loi) {
                    view.hienLoi(thongBaoLoi(loi));
                }
                taiDanhSach(maLichHen);
            }
        }.execute();
    }

    private void huyLich(LichHen lichHen) {
        String cauHoi = "Hủy lịch hẹn " + lichHen.getMaLichHen() + " của " + lichHen.getTenBenhNhan()
                + "?\nNhập lý do hủy (bắt buộc):";
        String lyDoHuy = JOptionPane.showInputDialog(view, cauHoi, "Hủy lịch hẹn", JOptionPane.QUESTION_MESSAGE);
        if (lyDoHuy == null) {
            return;
        }
        String maLichHen = lichHen.getMaLichHen();
        view.datDangTai(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                lichHenService.huyLich(maLichHen, lyDoHuy);
                return null;
            }

            @Override
            protected void done() {
                view.datDangTai(false);
                try {
                    get();
                    JOptionPane.showMessageDialog(view, "Đã hủy lịch hẹn " + maLichHen + ".");
                } catch (Exception loi) {
                    view.hienLoi(thongBaoLoi(loi));
                }
                taiDanhSach(maLichHen);
            }
        }.execute();
    }

    private void moForm(LichHen lichHenCanDoi) {
        winLichHen = new LichHenDialog(SwingUtilities.getWindowAncestor(view), lichHenCanDoi);
        winLichHen.getTxtTimBenhNhan().addActionListener(this);
        winLichHen.getBtnTimBenhNhan().addActionListener(this);
        winLichHen.getBtnChonNgay().addActionListener(this);
        winLichHen.getBtnXemGioTrong().addActionListener(this);
        winLichHen.getBtnLuu().addActionListener(this);
        winLichHen.getBtnHuy().addActionListener(this);

        String maBacSiChonSan = null;
        if (lichHenCanDoi != null) {
            maBacSiChonSan = lichHenCanDoi.getMaBacSi();
        }
        taiDanhSachBacSi(winLichHen, maBacSiChonSan);

        try {
            winLichHen.setVisible(true);
        } finally {
            winLichHen = null;
            dangXuLyForm = false;
        }
    }

    private void taiDanhSachBacSi(LichHenDialog winDangMo, String maBacSiChonSan) {
        dangXuLyForm = true;
        winDangMo.datDangXuLy(true);

        new SwingWorker<ArrayList<BacSi>, Void>() {
            @Override
            protected ArrayList<BacSi> doInBackground() throws Exception {
                return lichHenService.layDanhSachBacSi();
            }

            @Override
            protected void done() {
                dangXuLyForm = false;
                if (!winDangMo.isDisplayable()) {
                    return;
                }
                winDangMo.datDangXuLy(false);
                try {
                    winDangMo.hienDanhSachBacSi(get(), maBacSiChonSan);
                } catch (Exception loi) {
                    winDangMo.hienLoi(thongBaoLoi(loi));
                }
                winDangMo.getCmbBacSi().addActionListener(LichHenController.this);
            }
        }.execute();
    }

    private void timBenhNhan() {
        if (dangXuLyForm) {
            return;
        }
        dangXuLyForm = true;
        LichHenDialog winDangMo = winLichHen;
        String tuKhoa = winDangMo.getTxtTimBenhNhan().getText();
        winDangMo.datDangXuLy(true);

        new SwingWorker<ArrayList<BenhNhanTomTat>, Void>() {
            @Override
            protected ArrayList<BenhNhanTomTat> doInBackground() throws Exception {
                return lichHenService.timBenhNhan(tuKhoa);
            }

            @Override
            protected void done() {
                dangXuLyForm = false;
                if (!winDangMo.isDisplayable()) {
                    return;
                }
                winDangMo.datDangXuLy(false);
                try {
                    winDangMo.hienDanhSachBenhNhan(get());
                } catch (Exception loi) {
                    winDangMo.hienLoi(thongBaoLoi(loi));
                }
            }
        }.execute();
    }

    private void xemGioTrong() {
        if (dangXuLyForm) {
            return;
        }
        dangXuLyForm = true;
        LichHenDialog winDangMo = winLichHen;
        String maBacSi = winDangMo.layMaBacSiDaChon();
        LocalDate ngayHen = winDangMo.layNgayHen();
        String maLichHenDangDoi = null;
        if (winDangMo.getLichHenCanDoi() != null) {
            maLichHenDangDoi = winDangMo.getLichHenCanDoi().getMaLichHen();
        }
        String maBoQua = maLichHenDangDoi;
        winDangMo.datDangXuLy(true);

        new SwingWorker<ArrayList<LocalTime>, Void>() {
            @Override
            protected ArrayList<LocalTime> doInBackground() throws Exception {
                return lichHenService.layGioTrong(maBacSi, ngayHen, maBoQua);
            }

            @Override
            protected void done() {
                dangXuLyForm = false;
                if (!winDangMo.isDisplayable()) {
                    return;
                }
                winDangMo.datDangXuLy(false);
                try {
                    winDangMo.hienGioTrong(get());
                } catch (Exception loi) {
                    winDangMo.hienLoi(thongBaoLoi(loi));
                }
            }
        }.execute();
    }

    private void luu() {
        if (dangXuLyForm) {
            return;
        }
        dangXuLyForm = true;
        LichHenDialog winDangMo = winLichHen;

        LichHen lichHenCanDoi = winDangMo.getLichHenCanDoi();
        BenhNhanTomTat benhNhan = winDangMo.layBenhNhanDaChon();
        String maBenhNhan = null;
        if (benhNhan != null) {
            maBenhNhan = benhNhan.getMaBenhNhan();
        }
        String maBenhNhanDaChon = maBenhNhan;
        String maBacSi = winDangMo.layMaBacSiDaChon();
        LocalDateTime thoiGianHen = winDangMo.layThoiGianHen();
        String lyDo = winDangMo.layLyDo();
        winDangMo.datDangXuLy(true);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                if (lichHenCanDoi == null) {
                    return lichHenService.datLich(maBenhNhanDaChon, maBacSi, thoiGianHen, lyDo);
                }
                return lichHenService.doiLich(lichHenCanDoi.getMaLichHen(), maBacSi, thoiGianHen, lyDo);
            }

            @Override
            protected void done() {
                dangXuLyForm = false;
                if (winDangMo.isDisplayable()) {
                    winDangMo.datDangXuLy(false);
                }

                String maLichHenMoi;
                try {
                    maLichHenMoi = get();
                } catch (Exception loi) {
                    winDangMo.hienLoi(thongBaoLoi(loi));
                    Throwable loiGoc = layLoiGoc(loi);
                    if (loiGoc instanceof DuLieuDaThayDoiException) {
                        if (winDangMo.isDisplayable()) {
                            xemGioTrong();
                        }
                        taiDanhSach(null);
                    }
                    return;
                }

                String thongBao;
                if (lichHenCanDoi == null) {
                    thongBao = "Đã đặt lịch hẹn " + maLichHenMoi + ".";
                } else {
                    thongBao = "Đã đổi lịch " + lichHenCanDoi.getMaLichHen() + " sang lịch mới " + maLichHenMoi + ".";
                }
                JOptionPane.showMessageDialog(winDangMo, thongBao);
                winDangMo.dispose();

                view.datNgayDangXem(thoiGianHen.toLocalDate());
                taiDanhSach(maLichHenMoi);
            }
        }.execute();
    }

    private Throwable layLoiGoc(Throwable loi) {
        while (loi instanceof ExecutionException && loi.getCause() != null) {
            loi = loi.getCause();
        }
        return loi;
    }

    private String thongBaoLoi(Throwable loi) {
        Throwable loiGoc = layLoiGoc(loi);
        if (loiGoc instanceof AccessDeniedException) {
            return "Tài khoản không có quyền thực hiện thao tác này.";
        }
        if (loiGoc instanceof IllegalArgumentException) {
            return loiGoc.getMessage();
        }
        if (loiGoc instanceof DuLieuDaThayDoiException) {
            return loiGoc.getMessage();
        }
        if (loiGoc instanceof SQLTimeoutException) {
            return "SQL Server phản hồi quá lâu. Vui lòng thử lại.";
        }
        if (loiGoc instanceof SQLException) {
            return "Lỗi khi làm việc với SQL Server. Kiểm tra kết nối và quyền của tài khoản database rồi thử lại.";
        }
        return "Có lỗi xảy ra, vui lòng thử lại.";
    }
}
