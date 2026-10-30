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
    private BenhNhanService benhNhanService;       // tìm bệnh nhân (bước 1)
    private HoSoBenhAnService hoSoBenhAnService;   // mở hồ sơ + ghi nhật ký (bước 2)
    private boolean dangTimBenhNhan;               // đang tìm bệnh nhân → bỏ qua bấm thêm
    private long soThuTuLanMoHoSo;                 // số thứ tự lần mở hồ sơ, dùng để bỏ kết quả cũ

    public HoSoBenhAnController(HoSoBenhAnPanel view, BenhNhanService benhNhanService,
                                HoSoBenhAnService hoSoBenhAnService) {
        this.view = view;
        this.benhNhanService = benhNhanService;
        this.hoSoBenhAnService = hoSoBenhAnService;

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
    public void actionPerformed(ActionEvent suKien) {
        Object nguonSuKien = suKien.getSource();
        if (nguonSuKien.equals(view.getBtnLamMoi())) {
            view.getTxtTimKiem().setText("");
        }
        timBenhNhan();
    }

    // Click vào bảng bệnh nhân → mở hồ sơ; click vào bảng lần khám → xem chi tiết
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
            // Chi tiết đã có sẵn trong danh sách vừa đọc → không cần đọc lại database
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

    // ===================== Bước 1: tìm bệnh nhân =====================

    private void timBenhNhan() {
        if (dangTimBenhNhan) {
            return;
        }
        dangTimBenhNhan = true;
        soThuTuLanMoHoSo++;                            // bỏ kết quả mở hồ sơ đang chờ (nếu có)
        String tuKhoa = view.getTxtTimKiem().getText();
        view.datDangTai(true, "Đang tìm bệnh nhân...");

        new SwingWorker<ArrayList<BenhNhan>, Void>() {
            @Override
            protected ArrayList<BenhNhan> doInBackground() throws Exception {
                return benhNhanService.timKiem(tuKhoa, "");   // "" = mọi giới tính
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

    // ===================== Bước 2: mở hồ sơ (ghi nhật ký + đọc các lần khám) =====================

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
                    return;      // người dùng đã chọn bệnh nhân khác → bỏ kết quả này
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

    // Đổi lỗi thành câu dễ hiểu (không hiện câu SQL ra màn hình)
    private String thongBaoLoi(Throwable loi) {
        // SwingWorker bọc lỗi thật bên trong ExecutionException → lấy lỗi thật ra
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
