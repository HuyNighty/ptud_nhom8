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

// Controller màn hình Hồ sơ bệnh án (UC009): nhận sự kiện từ View → gọi Service → đưa kết quả về View.
// Đọc database bằng SwingWorker (luồng nền) để app không bị đơ.
public class HoSoBenhAnController implements ActionListener, MouseListener {
    private HoSoBenhAnPanel view;
    private BenhNhanService benhNhanService;   // tìm bệnh nhân (bước 1)
    private HoSoBenhAnService hoSoService;     // mở hồ sơ + ghi nhật ký (bước 2)
    private boolean dangTim;                   // đang tìm bệnh nhân → bỏ qua bấm thêm
    private long lanMoHoSo;                    // số thứ tự lần mở hồ sơ, dùng để bỏ kết quả cũ

    public HoSoBenhAnController(HoSoBenhAnPanel view, BenhNhanService benhNhanService,
                                HoSoBenhAnService hoSoService) {
        this.view = view;
        this.benhNhanService = benhNhanService;
        this.hoSoService = hoSoService;

        // Gắn sự kiện cho nút, ô tìm kiếm (Enter) và 2 bảng
        view.getBtnTimKiem().addActionListener(this);
        view.getBtnLamMoi().addActionListener(this);
        view.getTxtTimKiem().addActionListener(this);
        view.getTblBenhNhan().addMouseListener(this);
        view.getTblLuotKham().addMouseListener(this);

        // Mở màn hình là hiện danh sách bệnh nhân luôn
        timBenhNhan();
    }

    // Bấm Tìm kiếm / Làm mới / Enter
    @Override
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        if (o.equals(view.getBtnLamMoi())) {
            view.getTxtTimKiem().setText("");
        }
        timBenhNhan();
    }

    // Click vào bảng bệnh nhân → mở hồ sơ; click vào bảng lần khám → xem chi tiết
    @Override
    public void mouseClicked(MouseEvent e) {
        Object o = e.getSource();
        if (o.equals(view.getTblBenhNhan())) {
            BenhNhan bn = view.layBenhNhanDangChon();
            if (bn != null) {
                moHoSo(bn);
            }
        }
        if (o.equals(view.getTblLuotKham())) {
            // Chi tiết đã có sẵn trong danh sách vừa đọc → không cần đọc lại database
            view.hienChiTietLuotKham(view.layLuotKhamDangChon());
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    // ===================== Bước 1: tìm bệnh nhân =====================

    private void timBenhNhan() {
        if (dangTim) {
            return;
        }
        dangTim = true;
        lanMoHoSo++;                                   // bỏ kết quả mở hồ sơ đang chờ (nếu có)
        String tuKhoa = view.getTxtTimKiem().getText();
        view.datDangTai(true, "Đang tìm bệnh nhân...");
        new SwingWorker<ArrayList<BenhNhan>, Void>() {
            @Override
            protected ArrayList<BenhNhan> doInBackground() throws Exception {
                return benhNhanService.timKiem(tuKhoa, "");   // "" = mọi giới tính
            }

            @Override
            protected void done() {
                dangTim = false;
                view.datDangTai(false, null);
                try {
                    view.hienDsBenhNhan(get());
                } catch (Exception ex) {
                    view.hienLoi(thongBaoLoi(ex));
                }
            }
        }.execute();
    }

    // ===================== Bước 2: mở hồ sơ (ghi nhật ký + đọc các lần khám) =====================

    private void moHoSo(BenhNhan bn) {
        long lanNay = ++lanMoHoSo;
        view.hienHoSo(null, null);
        view.datDangTai(true, "Đang mở hồ sơ bệnh án của " + bn.getHoTen() + "...");
        new SwingWorker<HoSoBenhAn, Void>() {
            @Override
            protected HoSoBenhAn doInBackground() throws Exception {
                return hoSoService.moHoSo(bn.getMaBenhNhan());
            }

            @Override
            protected void done() {
                if (lanNay != lanMoHoSo) {
                    return;                            // người dùng đã chọn bệnh nhân khác → bỏ kết quả này
                }
                view.datDangTai(false, null);
                try {
                    view.hienHoSo(bn, get());
                } catch (Exception ex) {
                    view.hienLoi(thongBaoLoi(ex));
                }
            }
        }.execute();
    }

    // Đổi lỗi thành câu dễ hiểu (không hiện câu SQL ra màn hình)
    private String thongBaoLoi(Throwable ex) {
        while (ex instanceof ExecutionException && ex.getCause() != null) {
            ex = ex.getCause();
        }
        if (ex instanceof AccessDeniedException) {
            return "Tài khoản không có quyền xem hồ sơ bệnh án.";
        }
        if (ex instanceof IllegalArgumentException) {
            return ex.getMessage();
        }
        if (ex instanceof SQLTimeoutException) {
            return "SQL Server phản hồi quá lâu. Vui lòng thử lại.";
        }
        if (ex instanceof SQLException) {
            return "Lỗi khi làm việc với SQL Server. Kiểm tra kết nối rồi thử lại.";
        }
        return "Có lỗi xảy ra, vui lòng thử lại.";
    }
}
