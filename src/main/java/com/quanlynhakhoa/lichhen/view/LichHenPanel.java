package com.quanlynhakhoa.lichhen.view;

import com.quanlynhakhoa.benhnhan.view.BieuTuong;
import com.quanlynhakhoa.lichhen.controller.LichHenController;
import com.quanlynhakhoa.lichhen.model.LichHen;
import com.quanlynhakhoa.lichhen.service.LichHenService;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

// Màn hình LỊCH HẸN (menu Lịch hẹn → Danh sách lịch hẹn), theo UC004 trong báo cáo docs/tuan_cuoi.
// View chỉ tạo giao diện và hiển thị. Sự kiện do LichHenController xử lý.
//
//  ┌────────────────────────── pnlNorth ──────────────────────────┐
//  │ Lịch hẹn                                                     │  ← tiêu đề
//  │ ┌──────────────── khung trắng (pnlNorthSouth) ─────────────┐ │
//  │ │ Ngày [30/10/2026] Chọn... Hôm nay   Tìm [.......] Tìm kiếm │ │  ← dòng 1: lọc + tìm
//  │ │ ──────────────────────────────────────────────────────── │ │
//  │ │ [+ Đặt lịch hẹn] [Đổi lịch...] [Hủy lịch...]              │ │  ← dòng 2: nút thao tác
//  │ └──────────────────────────────────────────────────────────┘ │
//  ├────────────────────────── pnlCenter ─────────────────────────┤
//  │ bảng lịch hẹn trong ngày                                     │
//  ├────────────────────────── pnlSouth ──────────────────────────┤
//  │ dòng trạng thái                                              │
//  └──────────────────────────────────────────────────────────────┘
public class LichHenPanel extends JPanel {
    // Màu giống BenhNhanPanel / AccountPanel
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color XAM = new Color(97, 115, 137);
    private static final Color VIEN = new Color(220, 231, 242);
    private static final Color NEN = new Color(245, 248, 252);
    private static final Color DO = new Color(176, 32, 32);
    private static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DINH_DANG_GIO = DateTimeFormatter.ofPattern("HH:mm");

    // ----- NORTH -----
    private JPanel pnlNorth;
    private JLabel lblTieuDe;
    private JButton btnDatLich;          // 3 nút thao tác: dòng 2 của khung tìm kiếm
    private JButton btnDoiLich;
    private JButton btnHuyLich;
    private JTextField txtNgayXem;       // ngày đang xem, chỉ hiển thị (chọn bằng nút "Chọn...")
    private JButton btnChonNgayXem;
    private JButton btnHomNay;
    private JTextField txtTimKiem;
    private JButton btnTimKiem;

    // ----- CENTER -----
    private JPanel pnlCenter;
    private DefaultTableModel modelLichHen;
    private JTable tblLichHen;
    private JScrollPane scrLichHen;
    private ArrayList<LichHen> danhSachLichHen = new ArrayList<>();   // dữ liệu của bảng

    // ----- SOUTH -----
    private JPanel pnlSouth;
    private JLabel lblTrangThai;

    private LocalDate ngayDangXem = LocalDate.now();

    // Hàm khởi tạo: Core gắn vào menu bằng .register(Module.APPOINTMENTS, LichHenPanel::new)
    public LichHenPanel(DesktopSession phienDangNhap) {
        setLayout(new BorderLayout(14, 14));
        setBackground(NEN);
        setBorder(new EmptyBorder(24, 26, 24, 26));

        taoNorth();
        taoCenter();
        taoSouth();
        add(pnlNorth, BorderLayout.NORTH);
        add(pnlCenter, BorderLayout.CENTER);
        add(pnlSouth, BorderLayout.SOUTH);

        datNgayDangXem(LocalDate.now());

        // Tạo Service và Controller, Controller sẽ gắn sự kiện cho các nút
        new LichHenController(this, new LichHenService(phienDangNhap));
    }

    // ===================== Tạo các vùng =====================

    private void taoNorth() {
        pnlNorth = new JPanel(new BorderLayout(0, 10));
        pnlNorth.setOpaque(false);

        // Tiêu đề (NORTH của pnlNorth)
        lblTieuDe = new JLabel("Lịch hẹn");
        lblTieuDe.setFont(new Font("SansSerif", Font.BOLD, 23));
        lblTieuDe.setForeground(NAVY);

        // ----- Dòng 1: lọc theo ngày + tìm kiếm -----
        txtNgayXem = new JTextField(9);
        txtNgayXem.setEditable(false);
        btnChonNgayXem = new JButton("Chọn...");
        btnHomNay = new JButton("Hôm nay");
        txtTimKiem = new JTextField(20);
        btnTimKiem = new JButton("Tìm kiếm");
        btnTimKiem.setIcon(new BieuTuong(BieuTuong.TIM_KIEM));   // dùng lại icon kính lúp

        JPanel pnlDongLoc = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
        pnlDongLoc.setOpaque(false);
        pnlDongLoc.add(new JLabel("Ngày:"));
        pnlDongLoc.add(txtNgayXem);
        pnlDongLoc.add(btnChonNgayXem);
        pnlDongLoc.add(btnHomNay);
        pnlDongLoc.add(Box.createHorizontalStrut(18));   // khoảng cách giữa nhóm Ngày và nhóm Tìm
        pnlDongLoc.add(new JLabel("Tìm (mã LH, mã BN, tên, SĐT):"));
        pnlDongLoc.add(txtTimKiem);
        pnlDongLoc.add(btnTimKiem);

        // ----- Dòng 2: nút thao tác, có 1 đường kẻ mảnh phía trên để tách với dòng 1 -----
        btnDatLich = new JButton("+ Đặt lịch hẹn");
        btnDoiLich = new JButton("Đổi lịch...");
        btnHuyLich = new JButton("Hủy lịch...");

        JPanel pnlDongThaoTac = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
        pnlDongThaoTac.setOpaque(false);
        pnlDongThaoTac.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, VIEN));   // đường kẻ trên
        pnlDongThaoTac.add(btnDatLich);
        pnlDongThaoTac.add(btnDoiLich);
        pnlDongThaoTac.add(btnHuyLich);

        // Khung trắng chứa 2 dòng (SOUTH của pnlNorth)
        JPanel pnlNorthSouth = new JPanel(new BorderLayout());
        pnlNorthSouth.setBackground(Color.WHITE);
        pnlNorthSouth.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlNorthSouth.add(pnlDongLoc, BorderLayout.NORTH);
        pnlNorthSouth.add(pnlDongThaoTac, BorderLayout.SOUTH);

        pnlNorth.add(lblTieuDe, BorderLayout.NORTH);
        pnlNorth.add(pnlNorthSouth, BorderLayout.SOUTH);
    }

    private void taoCenter() {
        pnlCenter = new JPanel(new BorderLayout());
        pnlCenter.setOpaque(false);

        String[] tenCacCot = {"Mã lịch hẹn", "Giờ hẹn", "Mã BN", "Bệnh nhân", "SĐT", "Bác sĩ", "Lý do", "Trạng thái"};
        modelLichHen = new DefaultTableModel(tenCacCot, 0) {
            @Override
            public boolean isCellEditable(int dong, int cot) {
                return false; // bảng chỉ để xem
            }
        };
        tblLichHen = new JTable(modelLichHen);
        tblLichHen.setRowHeight(30);
        tblLichHen.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblLichHen.getTableHeader().setReorderingAllowed(false);
        tblLichHen.setFillsViewportHeight(true);

        // Độ rộng cột: cột ngắn (mã, giờ, SĐT, trạng thái) hẹp lại, cột tên / lý do rộng hơn
        tblLichHen.getColumnModel().getColumn(0).setPreferredWidth(90);    // Mã lịch hẹn
        tblLichHen.getColumnModel().getColumn(1).setPreferredWidth(60);    // Giờ hẹn
        tblLichHen.getColumnModel().getColumn(2).setPreferredWidth(80);    // Mã BN
        tblLichHen.getColumnModel().getColumn(3).setPreferredWidth(170);   // Bệnh nhân
        tblLichHen.getColumnModel().getColumn(4).setPreferredWidth(100);   // SĐT
        tblLichHen.getColumnModel().getColumn(5).setPreferredWidth(150);   // Bác sĩ
        tblLichHen.getColumnModel().getColumn(6).setPreferredWidth(220);   // Lý do
        tblLichHen.getColumnModel().getColumn(7).setPreferredWidth(110);   // Trạng thái

        scrLichHen = new JScrollPane(tblLichHen);
        scrLichHen.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlCenter.add(scrLichHen, BorderLayout.CENTER);
    }

    private void taoSouth() {
        pnlSouth = new JPanel(new BorderLayout());
        pnlSouth.setOpaque(false);
        lblTrangThai = new JLabel("Chưa tải dữ liệu.");
        lblTrangThai.setForeground(XAM);
        pnlSouth.add(lblTrangThai, BorderLayout.CENTER);
    }

    // ===================== Hàm hiển thị (Controller gọi) =====================

    // Đổ danh sách lịch hẹn vào bảng
    public void hienDanhSachLichHen(ArrayList<LichHen> danhSachMoi) {
        danhSachLichHen = danhSachMoi;
        modelLichHen.setRowCount(0);   // xóa các dòng cũ
        for (LichHen lichHen : danhSachLichHen) {
            String lyDo = lichHen.getLyDoHen();
            if (lyDo == null) {
                lyDo = "";
            }
            modelLichHen.addRow(new Object[]{
                    lichHen.getMaLichHen(),
                    lichHen.getThoiGianHen().format(DINH_DANG_GIO),
                    lichHen.getMaBenhNhan(),
                    lichHen.getTenBenhNhan(),
                    lichHen.getSoDienThoaiBenhNhan(),
                    lichHen.getTenBacSi(),
                    lyDo,
                    lichHen.getTrangThaiHienThi()
            });
        }
        lblTrangThai.setForeground(XAM);
        lblTrangThai.setText("Ngày " + ngayDangXem.format(DINH_DANG_NGAY) + ": có "
                + danhSachLichHen.size() + " lịch hẹn. Mỗi lịch hẹn dài " + LichHenService.THOI_LUONG_PHUT + " phút.");
    }

    // Chọn dòng có mã lịch hẹn này (dùng sau khi đặt / đổi lịch xong)
    public void chonDongTheoMa(String maLichHen) {
        for (int dong = 0; dong < modelLichHen.getRowCount(); dong++) {
            if (modelLichHen.getValueAt(dong, 0).equals(maLichHen)) {
                tblLichHen.setRowSelectionInterval(dong, dong);
                tblLichHen.scrollRectToVisible(tblLichHen.getCellRect(dong, 0, true));
                return;
            }
        }
    }

    // Lịch hẹn ở dòng đang chọn (null nếu chưa chọn)
    public LichHen layLichHenDangChon() {
        int dongDangChon = tblLichHen.getSelectedRow();
        if (dongDangChon < 0 || dongDangChon >= danhSachLichHen.size()) {
            return null;
        }
        return danhSachLichHen.get(dongDangChon);
    }

    // Đặt ngày đang xem và hiện lên ô ngày
    public void datNgayDangXem(LocalDate ngay) {
        ngayDangXem = ngay;
        txtNgayXem.setText(ngay.format(DINH_DANG_NGAY));
    }

    public LocalDate getNgayDangXem() {
        return ngayDangXem;
    }

    // Đang đọc / ghi database: làm mờ nút để không bấm chồng
    public void datDangTai(boolean dangTai) {
        btnTimKiem.setEnabled(!dangTai);
        btnHomNay.setEnabled(!dangTai);
        btnChonNgayXem.setEnabled(!dangTai);
        if (dangTai) {
            lblTrangThai.setForeground(XAM);
            lblTrangThai.setText("Đang tải dữ liệu...");
        }
    }

    // Không có quyền đặt / đổi / hủy lịch thì làm mờ các nút đó (Service vẫn kiểm tra lại)
    public void datQuyen(boolean duocQuanLyLich) {
        btnDatLich.setEnabled(duocQuanLyLich);
        btnDoiLich.setEnabled(duocQuanLyLich);
        btnHuyLich.setEnabled(duocQuanLyLich);
    }

    // Hiện lỗi: dòng chữ đỏ ở SOUTH + hộp thông báo giữa màn hình
    public void hienLoi(String thongBao) {
        lblTrangThai.setForeground(DO);
        lblTrangThai.setText(thongBao);
        JOptionPane.showMessageDialog(this, thongBao, "Thông báo lỗi", JOptionPane.ERROR_MESSAGE);
    }

    // ===================== Getter cho Controller =====================

    public JButton getBtnChonNgayXem() {
        return btnChonNgayXem;
    }

    public JButton getBtnHomNay() {
        return btnHomNay;
    }

    public JTextField getTxtTimKiem() {
        return txtTimKiem;
    }

    public JButton getBtnTimKiem() {
        return btnTimKiem;
    }

    public JButton getBtnDatLich() {
        return btnDatLich;
    }

    public JButton getBtnDoiLich() {
        return btnDoiLich;
    }

    public JButton getBtnHuyLich() {
        return btnHuyLich;
    }
}
