package com.quanlynhakhoa.lichhen.controller;

import com.quanlynhakhoa.benhnhan.hopdong.BenhNhanTomTat;
import com.quanlynhakhoa.benhnhan.service.DuLieuDaThayDoiException;
import com.quanlynhakhoa.lichhen.model.BacSi;
import com.quanlynhakhoa.lichhen.model.LichHen;
import com.quanlynhakhoa.lichhen.service.LichHenService;
import com.quanlynhakhoa.lichhen.view.TiepNhanDialog;
import com.quanlynhakhoa.lichhen.view.TiepNhanPanel;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.Permission;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class TiepNhanController implements ActionListener, MouseListener {
    private TiepNhanPanel view;
    private LichHenService lichHenService;

    private TiepNhanDialog winTiepNhan;
    private boolean dangTaiDanhSach;
    private boolean dangXuLyForm;

    public TiepNhanController(TiepNhanPanel view, LichHenService lichHenService) {
        this.view = view;
        this.lichHenService = lichHenService;

        view.getTxtTimKiem().addActionListener(this);
        view.getBtnTimKiem().addActionListener(this);
        view.getBtnLamMoi().addActionListener(this);
        view.getBtnTiepNhanKhongHen().addActionListener(this);
        view.getBtnCheckIn().addActionListener(this);
        view.getTblLichHen().addMouseListener(this);

        view.datQuyen(lichHenService.coQuyen(Permission.PATIENT_CHECK_IN));

        taiDanhSach(null);
    }

    @Override
    public void actionPerformed(ActionEvent suKien) {
        Object nguonSuKien = suKien.getSource();

        if (nguonSuKien.equals(view.getBtnTimKiem()) || nguonSuKien.equals(view.getTxtTimKiem())) {
            taiDanhSach(null);
        }
        if (nguonSuKien.equals(view.getBtnLamMoi())) {
            view.getTxtTimKiem().setText("");
            taiDanhSach(null);
        }
        if (nguonSuKien.equals(view.getBtnCheckIn())) {
            checkIn();
        }
        if (nguonSuKien.equals(view.getBtnTiepNhanKhongHen())) {
            moFormTiepNhan();
        }

        if (winTiepNhan == null) {
            return;
        }
        if (nguonSuKien.equals(winTiepNhan.getBtnTimBenhNhan()) || nguonSuKien.equals(winTiepNhan.getTxtTimBenhNhan())) {
            timBenhNhan();
        }
        if (nguonSuKien.equals(winTiepNhan.getBtnTiepNhan())) {
            luuTiepNhan();
        }
        if (nguonSuKien.equals(winTiepNhan.getBtnHuy())) {
            winTiepNhan.dispose();
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
        String tuKhoa = view.getTxtTimKiem().getText();
        view.datDangTai(true);

        new SwingWorker<ArrayList<LichHen>, Void>() {
            @Override
            protected ArrayList<LichHen> doInBackground() throws Exception {
                return lichHenService.layLichHenHomNay(tuKhoa);
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

    private void checkIn() {
        LichHen lichHen = view.layLichHenDangChon();
        if (lichHen == null) {
            view.hienLoi("Chọn 1 lịch hẹn trong bảng để check-in.");
            return;
        }
        if (!lichHen.duocTiepDon()) {
            view.hienLoi("Lịch hẹn đang ở trạng thái \"" + lichHen.getTrangThaiHienThi() + "\", không check-in được.");
            return;
        }
        String cauHoi = "Xác nhận bệnh nhân " + lichHen.getTenBenhNhan() + " đã đến phòng khám?\n"
                + "Lịch hẹn chuyển \"Đã đến\" và bệnh nhân vào danh sách chờ của bác sĩ " + lichHen.getTenBacSi() + ".";
        int luaChon = JOptionPane.showConfirmDialog(view, cauHoi, "Check-in", JOptionPane.YES_NO_OPTION);
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
                    JOptionPane.showMessageDialog(view, "Check-in thành công. Đã tạo lượt khám " + maPhieuKham
                            + " (Chờ khám) với bác sĩ " + tenBacSi + ".");
                } catch (Exception loi) {
                    view.hienLoi(thongBaoLoi(loi));
                }
                taiDanhSach(maLichHen);
            }
        }.execute();
    }

    private void moFormTiepNhan() {
        winTiepNhan = new TiepNhanDialog(SwingUtilities.getWindowAncestor(view));
        winTiepNhan.getTxtTimBenhNhan().addActionListener(this);
        winTiepNhan.getBtnTimBenhNhan().addActionListener(this);
        winTiepNhan.getBtnTiepNhan().addActionListener(this);
        winTiepNhan.getBtnHuy().addActionListener(this);

        taiBacSiDangTrongCa(winTiepNhan);

        try {
            winTiepNhan.setVisible(true);
        } finally {
            winTiepNhan = null;
            dangXuLyForm = false;
        }
    }

    private void taiBacSiDangTrongCa(TiepNhanDialog winDangMo) {
        dangXuLyForm = true;
        winDangMo.datDangXuLy(true);

        new SwingWorker<ArrayList<BacSi>, Void>() {
            @Override
            protected ArrayList<BacSi> doInBackground() throws Exception {
                return lichHenService.layBacSiDangTrongCa();
            }

            @Override
            protected void done() {
                dangXuLyForm = false;
                if (!winDangMo.isDisplayable()) {
                    return;
                }
                winDangMo.datDangXuLy(false);
                try {
                    winDangMo.hienDanhSachBacSi(get());
                } catch (Exception loi) {
                    winDangMo.hienLoi(thongBaoLoi(loi));
                }
            }
        }.execute();
    }

    private void timBenhNhan() {
        if (dangXuLyForm) {
            return;
        }
        dangXuLyForm = true;
        TiepNhanDialog winDangMo = winTiepNhan;
        String tuKhoa = winDangMo.getTxtTimBenhNhan().getText();
        winDangMo.datDangXuLy(true);

        new SwingWorker<ArrayList<BenhNhanTomTat>, Void>() {
            @Override
            protected ArrayList<BenhNhanTomTat> doInBackground() throws Exception {
                return lichHenService.timBenhNhanDeTiepNhan(tuKhoa);
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

    private void luuTiepNhan() {
        if (dangXuLyForm) {
            return;
        }
        TiepNhanDialog winDangMo = winTiepNhan;
        BenhNhanTomTat benhNhan = winDangMo.layBenhNhanDaChon();
        BacSi bacSi = winDangMo.layBacSiDaChon();
        if (benhNhan == null) {
            winDangMo.hienLoi("Chọn bệnh nhân.");
            return;
        }
        if (bacSi == null) {
            winDangMo.hienLoi("Chọn bác sĩ.");
            return;
        }
        String maBenhNhan = benhNhan.getMaBenhNhan();
        String maBacSi = bacSi.getMaNhanVien();
        String tenBacSi = bacSi.getHoTen();
        String lyDoDen = winDangMo.layLyDoDen();
        dangXuLyForm = true;
        winDangMo.datDangXuLy(true);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return lichHenService.tiepNhanKhongHen(maBenhNhan, maBacSi, lyDoDen);
            }

            @Override
            protected void done() {
                dangXuLyForm = false;
                if (winDangMo.isDisplayable()) {
                    winDangMo.datDangXuLy(false);
                }

                String maPhieuKham;
                try {
                    maPhieuKham = get();
                } catch (Exception loi) {
                    winDangMo.hienLoi(thongBaoLoi(loi));
                    return;
                }

                JOptionPane.showMessageDialog(winDangMo, "Đã tạo lượt khám " + maPhieuKham
                        + " (Chờ khám). Bệnh nhân vào danh sách chờ của bác sĩ " + tenBacSi + ".");
                winDangMo.dispose();
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
