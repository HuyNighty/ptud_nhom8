package com.quanlynhakhoa.benhnhan.controller;

import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.benhnhan.service.BenhNhanService;
import com.quanlynhakhoa.benhnhan.view.BenhNhanPanel;
import com.quanlynhakhoa.security.AccessDeniedException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.SQLException;
import java.util.ArrayList;

// Controller: nhận sự kiện từ View → gọi Service → đưa kết quả về View
public class BenhNhanController implements ActionListener, MouseListener {
    private BenhNhanPanel view;
    private BenhNhanService service;

    public BenhNhanController(BenhNhanPanel view, BenhNhanService service) {
        this.view = view;
        this.service = service;

        // Gắn sự kiện cho các nút và ô tìm kiếm (nhấn Enter)
        view.getBtnLoc().addActionListener(this);
        view.getBtnXoaLoc().addActionListener(this);
        view.getBtnTaiLai().addActionListener(this);
        view.getTxtTimKiem().addActionListener(this);
        // Gắn sự kiện click chuột vào bảng
        view.getTblBenhNhan().addMouseListener(this);

        // Mở màn hình là đọc dữ liệu từ database vào bảng luôn
        docDuLieuVaoTable();
    }

    // Xử lý khi bấm nút hoặc nhấn Enter
    @Override
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        if (o.equals(view.getBtnLoc()) || o.equals(view.getTxtTimKiem())) {
            docDuLieuVaoTable();
        }
        if (o.equals(view.getBtnXoaLoc())) {
            view.xoaTrang();
            docDuLieuVaoTable();
        }
        if (o.equals(view.getBtnTaiLai())) {
            docDuLieuVaoTable();
        }
    }

    // Click 1 dòng trong bảng → đọc lại bệnh nhân từ database → hiện ở khung bên phải
    @Override
    public void mouseClicked(MouseEvent e) {
        int row = view.getTblBenhNhan().getSelectedRow();
        if (row < 0) {
            return;
        }
        String maBenhNhan = view.getTblBenhNhan().getValueAt(row, 0).toString();
        try {
            BenhNhan bn = service.xemChiTiet(maBenhNhan);
            view.hienChiTiet(bn);
        } catch (Exception ex) {
            view.hienLoi(thongBaoLoi(ex));
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

    // Lấy điều kiện lọc trên View → gọi Service → đổ kết quả vào bảng
    private void docDuLieuVaoTable() {
        try {
            String tuKhoa = view.getTxtTimKiem().getText();
            String gioiTinh = view.layGioiTinhDangChon();
            ArrayList<BenhNhan> dsBenhNhan = service.timKiem(tuKhoa, gioiTinh);
            view.docDuLieuVaoTable(dsBenhNhan);
        } catch (Exception ex) {
            view.hienLoi(thongBaoLoi(ex));
        }
    }

    // Đổi lỗi thành câu dễ hiểu (không hiện câu SQL ra màn hình)
    private String thongBaoLoi(Exception ex) {
        if (ex instanceof AccessDeniedException) {
            return "Tài khoản không có quyền xem bệnh nhân.";
        }
        if (ex instanceof IllegalArgumentException) {
            return ex.getMessage();
        }
        if (ex instanceof SQLException) {
            return "Không đọc được dữ liệu từ SQL Server. Kiểm tra kết nối rồi thử lại.";
        }
        return "Có lỗi xảy ra, vui lòng thử lại.";
    }
}
