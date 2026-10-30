package com.quanlynhakhoa.benhnhan.controller;

import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.benhnhan.model.HoSoBenhAn;
import com.quanlynhakhoa.benhnhan.service.BenhNhanService;
import com.quanlynhakhoa.benhnhan.service.HoSoBenhAnService;
import com.quanlynhakhoa.benhnhan.view.HoSoBenhAnPanel;
import com.quanlynhakhoa.security.AccessDeniedException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

public class HoSoBenhAnController implements ActionListener, MouseListener {
    private HoSoBenhAnPanel view;
    private BenhNhanService benhNhanService;
    private HoSoBenhAnService hoSoBenhAnService;
    private boolean dangTimBenhNhan;
    private long soThuTuLanMoHoSo;

    public HoSoBenhAnController(HoSoBenhAnPanel view, BenhNhanService benhNhanService,
                                HoSoBenhAnService hoSoBenhAnService) {
        this.view = view;
        this.benhNhanService = benhNhanService;
        this.hoSoBenhAnService = hoSoBenhAnService;

        view.getBtnTimKiem().addActionListener(this);
        view.getBtnLamMoi().addActionListener(this);
        view.getTxtTimKiem().addActionListener(this);
        view.getTblBenhNhan().addMouseListener(this);
        view.getTblLuotKham().addMouseListener(this);

        timBenhNhan();
    }

    @Override
    public void actionPerformed(ActionEvent suKien) {
        Object nguonSuKien = suKien.getSource();
        if (nguonSuKien.equals(view.getBtnLamMoi())) {
            view.getTxtTimKiem().setText("");
        }
        timBenhNhan();
    }

    @Override
    public void mouseClicked(MouseEvent suKien) {
        Object nguonSuKien = suKien.getSource();
        if (nguonSuKien.equals(view.getTblBenhNhan())) {
            BenhNhan benhNhan = view.layBenhNhanDangChon();
            if (benhNhan != null) {
                moHoSo(benhNhan);
            }
        }
        if (nguonSuKien.equals(view.getTblLuotKham())) {
            view.hienChiTietLuotKham(view.layLuotKhamDangChon());
        }
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

    private void timBenhNhan() {
        if (dangTimBenhNhan) {
            return;
        }
        dangTimBenhNhan = true;
        soThuTuLanMoHoSo++;
        String tuKhoa = view.getTxtTimKiem().getText();
        view.datDangTai(true, "Đang tìm bệnh nhân...");

        new SwingWorker<ArrayList<BenhNhan>, Void>() {
            @Override
            protected ArrayList<BenhNhan> doInBackground() throws Exception {
                return benhNhanService.timKiem(tuKhoa, "");
            }

            @Override
            protected void done() {
                dangTimBenhNhan = false;
                view.datDangTai(false, null);
                try {
                    view.hienDanhSachBenhNhan(get());
                } catch (Exception loi) {
                    view.hienLoi(thongBaoLoi(loi));
                }
            }
        }.execute();
    }

    private void moHoSo(BenhNhan benhNhan) {
        soThuTuLanMoHoSo++;
        long lanMoNay = soThuTuLanMoHoSo;
        view.hienHoSo(null, null);
        view.datDangTai(true, "Đang mở hồ sơ bệnh án của " + benhNhan.getHoTen() + "...");

        new SwingWorker<HoSoBenhAn, Void>() {
            @Override
            protected HoSoBenhAn doInBackground() throws Exception {
                return hoSoBenhAnService.moHoSo(benhNhan.getMaBenhNhan());
            }

            @Override
            protected void done() {
                if (lanMoNay != soThuTuLanMoHoSo) {
                    return;
                }
                view.datDangTai(false, null);
                try {
                    view.hienHoSo(benhNhan, get());
                } catch (Exception loi) {
                    view.hienLoi(thongBaoLoi(loi));
                }
            }
        }.execute();
    }

    private String thongBaoLoi(Throwable loi) {
        while (loi instanceof ExecutionException && loi.getCause() != null) {
            loi = loi.getCause();
        }
        if (loi instanceof AccessDeniedException) {
            return "Tài khoản không có quyền xem hồ sơ bệnh án.";
        }
        if (loi instanceof IllegalArgumentException) {
            return loi.getMessage();
        }
        if (loi instanceof SQLTimeoutException) {
            return "SQL Server phản hồi quá lâu. Vui lòng thử lại.";
        }
        if (loi instanceof SQLException) {
            return "Lỗi khi làm việc với SQL Server. Kiểm tra kết nối rồi thử lại.";
        }
        return "Có lỗi xảy ra, vui lòng thử lại.";
    }
}
