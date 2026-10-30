package com.quanlynhakhoa.lichhen.view;

import com.quanlynhakhoa.benhnhan.view.BieuTuong;
import com.quanlynhakhoa.lichhen.controller.LichHenController;
import com.quanlynhakhoa.lichhen.model.LichHen;
import com.quanlynhakhoa.lichhen.service.LichHenService;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

// Màn hình LỊCH HẸN (menu Lịch hẹn → Danh sách lịch hẹn), theo UC004 / UC005 trong báo cáo docs/tuan_cuoi.
// View chỉ tạo giao diện và hiển thị. Sự kiện do LichHenController xử lý.
//
//  ┌──────────────────────────── pnlNorth ─────────────────────────────┐
//  │ Lịch hẹn                                                          │
//  │ Ngày [..] Chọn... Hôm nay   Tìm (...) [.......] Tìm kiếm  +Đặt lịch hẹn │
//  ├──── pnlWest ────┬──────────────── pnlCenter ──────────────────────┤
//  │ Lịch hẹn đang   │ bảng lịch hẹn trong ngày                        │
//  │ chọn: mã, giờ,  │                                                 │
//  │ bệnh nhân...    │                                                 │
//  │ [Xác nhận đến]  │                                                 │
//  │ [Đổi lịch...]   │                                                 │
//  │ [Hủy lịch...]   │                                                 │
//  ├─────────────────┴──────────── pnlSouth ───────────────────────────┤
//  │ dòng trạng thái                                                   │
//  └───────────────────────────────────────────────────────────────────┘
public class LichHenPanel extends JPanel {
    // Màu giống BenhNhanPanel / AccountPanel
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color XAM = new Color(97, 115, 137);
    private static final Color VIEN = new Color(220, 231, 242);
    private static final Color NEN = new Color(245, 248, 252);
    private static final Color DO = new Color(176, 32, 32);
    private static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DINH_DANG_GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DINH_DANG_NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ----- NORTH: tiêu đề + thanh lọc -----
    private JPanel pnlNorth;
    private JLabel lblTieuDe;
    private JTextField txtNgayXem;       // ngày đang xem, chỉ hiển thị (chọn bằng nút "Chọn...")
    private JButton btnChonNgayXem;
    private JButton btnHomNay;
    private JTextField txtTimKiem;
    private JButton btnTimKiem;
    private JButton btnDatLich;

    // ----- WEST: lịch hẹn đang chọn + nút thao tác -----
    private JPanel pnlWest;
    private JLabel lblMaLichHen;
    private JLabel lblThoiGianHen;
    private JLabel lblBenhNhan;
    private JLabel lblSoDienThoai;
    private JLabel lblBacSi;
    private JLabel lblTrangThaiLichHen;
    private JTextArea txaLyDo;
    private JButton btnXacNhanDen;
    private JButton btnDoiLich;
    private JButton btnHuyLich;
    private boolean duocQuanLyLich;      // có quyền đổi / hủy lịch không
    private boolean duocXacNhanDen;      // có quyền xác nhận đến không

    // ----- CENTER: bảng lịch hẹn -----
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
        taoWest();
        taoCenter();
        taoSouth();
        add(pnlNorth, BorderLayout.NORTH);
        add(pnlWest, BorderLayout.WEST);
        add(pnlCenter, BorderLayout.CENTER);
        add(pnlSouth, BorderLayout.SOUTH);

        datNgayDangXem(LocalDate.now());
        hienChiTiet(null);

        // Tạo Service và Controller, Controller sẽ gắn sự kiện cho các nút
        new LichHenController(this, new LichHenService(phienDangNhap));
    }

    // ===================== Tạo các vùng =====================

    private void taoNorth() {
        pnlNorth = new JPanel(new BorderLayout(0, 10));
        pnlNorth.setOpaque(false);

        lblTieuDe = new JLabel("Lịch hẹn");
        lblTieuDe.setFont(new Font("SansSerif", Font.BOLD, 23));
        lblTieuDe.setForeground(NAVY);

        txtNgayXem = new JTextField(9);
        txtNgayXem.setEditable(false);
        btnChonNgayXem = new JButton("Chọn...");
        btnHomNay = new JButton("Hôm nay");
        txtTimKiem = new JTextField(16);
        btnTimKiem = new JButton("Tìm kiếm");
        btnTimKiem.setIcon(new BieuTuong(BieuTuong.TIM_KIEM));   // dùng lại icon kính lúp
        btnDatLich = new JButton("+ Đặt lịch hẹn");

        // Thanh lọc 1 dòng (SOUTH của pnlNorth)
        JPanel pnlNorthSouth = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
        pnlNorthSouth.setBackground(Color.WHITE);
        pnlNorthSouth.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlNorthSouth.add(new JLabel("Ngày:"));
        pnlNorthSouth.add(txtNgayXem);
        pnlNorthSouth.add(btnChonNgayXem);
        pnlNorthSouth.add(btnHomNay);
        pnlNorthSouth.add(new JLabel("Tìm (mã LH, mã BN, tên, SĐT):"));
        pnlNorthSouth.add(txtTimKiem);
        pnlNorthSouth.add(btnTimKiem);
        pnlNorthSouth.add(btnDatLich);

        pnlNorth.add(lblTieuDe, BorderLayout.NORTH);
        pnlNorth.add(pnlNorthSouth, BorderLayout.SOUTH);
    }

    private void taoWest() {
        pnlWest = new JPanel(new BorderLayout());
        pnlWest.setBackground(Color.WHITE);
        pnlWest.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlWest.setPreferredSize(new Dimension(270, 0));

        // Box dọc chứa thông tin lịch hẹn đang chọn
        Box boxThongTin = Box.createVerticalBox();
        boxThongTin.setBorder(new EmptyBorder(14, 14, 10, 14));

        JLabel lblTieuDeKhung = new JLabel("Lịch hẹn đang chọn");
        lblTieuDeKhung.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblTieuDeKhung.setForeground(NAVY);
        boxThongTin.add(dongTrai(lblTieuDeKhung));
        boxThongTin.add(Box.createVerticalStrut(12));

        lblMaLichHen = taoGiaTri();
        lblThoiGianHen = taoGiaTri();
        lblBenhNhan = taoGiaTri();
        lblSoDienThoai = taoGiaTri();
        lblBacSi = taoGiaTri();
        lblTrangThaiLichHen = taoGiaTri();
        txaLyDo = new JTextArea("—");
        txaLyDo.setEditable(false);
        txaLyDo.setLineWrap(true);
        txaLyDo.setWrapStyleWord(true);
        txaLyDo.setOpaque(false);
        txaLyDo.setFont(new Font("SansSerif", Font.BOLD, 13));
        txaLyDo.setForeground(NAVY);

        themDong(boxThongTin, "Mã lịch hẹn:", lblMaLichHen);
        themDong(boxThongTin, "Giờ hẹn:", lblThoiGianHen);
        themDong(boxThongTin, "Bệnh nhân:", lblBenhNhan);
        themDong(boxThongTin, "SĐT:", lblSoDienThoai);
        themDong(boxThongTin, "Bác sĩ:", lblBacSi);
        themDong(boxThongTin, "Trạng thái:", lblTrangThaiLichHen);

        // Lý do: nhãn 1 dòng, nội dung nhiều dòng bên dưới
        JLabel lblNhanLyDo = new JLabel("Lý do khám:");
        lblNhanLyDo.setForeground(XAM);
        boxThongTin.add(dongTrai(lblNhanLyDo));
        boxThongTin.add(Box.createVerticalStrut(4));
        boxThongTin.add(txaLyDo);

        // 3 nút thao tác xếp dọc, rộng bằng nhau (SOUTH của pnlWest)
        btnXacNhanDen = new JButton("Xác nhận đến");
        btnDoiLich = new JButton("Đổi lịch...");
        btnHuyLich = new JButton("Hủy lịch...");
        JPanel pnlNutThaoTac = new JPanel(new GridLayout(3, 1, 0, 8));
        pnlNutThaoTac.setOpaque(false);
        pnlNutThaoTac.setBorder(new EmptyBorder(10, 14, 14, 14));
        pnlNutThaoTac.add(btnXacNhanDen);
        pnlNutThaoTac.add(btnDoiLich);
        pnlNutThaoTac.add(btnHuyLich);

        pnlWest.add(boxThongTin, BorderLayout.NORTH);
        pnlWest.add(pnlNutThaoTac, BorderLayout.SOUTH);
    }

    private void taoCenter() {
        pnlCenter = new JPanel(new BorderLayout());
        pnlCenter.setOpaque(false);

        String[] tenCacCot = {"Mã lịch hẹn", "Giờ hẹn", "Bệnh nhân", "SĐT", "Bác sĩ", "Lý do", "Trạng thái"};
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
        tblLichHen.getColumnModel().getColumn(2).setPreferredWidth(170);   // Bệnh nhân
        tblLichHen.getColumnModel().getColumn(3).setPreferredWidth(100);   // SĐT
        tblLichHen.getColumnModel().getColumn(4).setPreferredWidth(140);   // Bác sĩ
        tblLichHen.getColumnModel().getColumn(5).setPreferredWidth(200);   // Lý do
        tblLichHen.getColumnModel().getColumn(6).setPreferredWidth(110);   // Trạng thái

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

    // ===================== Hàm tiện ích tạo giao diện =====================

    private JLabel taoGiaTri() {
        JLabel lblGiaTri = new JLabel("—");
        lblGiaTri.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblGiaTri.setForeground(NAVY);
        return lblGiaTri;
    }

    // 1 dòng chỉ có 1 nhãn, nằm sát bên trái
    private Box dongTrai(JLabel lblNoiDung) {
        Box boxDong = Box.createHorizontalBox();
        boxDong.add(lblNoiDung);
        boxDong.add(Box.createHorizontalGlue());
        return boxDong;
    }

    // Thêm 1 dòng "nhãn : giá trị" vào Box dọc
    private void themDong(Box boxCha, String noiDungNhan, JLabel lblGiaTri) {
        JLabel lblNhan = new JLabel(noiDungNhan);
        lblNhan.setForeground(XAM);
        lblNhan.setPreferredSize(new Dimension(85, lblNhan.getPreferredSize().height));
        Box boxDong = Box.createHorizontalBox();
        boxDong.add(lblNhan);
        boxDong.add(Box.createHorizontalStrut(6));
        boxDong.add(lblGiaTri);
        boxDong.add(Box.createHorizontalGlue());
        boxCha.add(boxDong);
        boxCha.add(Box.createVerticalStrut(9));
    }

    // ===================== Hàm hiển thị (Controller gọi) =====================

    // Đổ danh sách lịch hẹn vào bảng
    public void hienDanhSachLichHen(ArrayList<LichHen> danhSachMoi) {
        danhSachLichHen = danhSachMoi;
        modelLichHen.setRowCount(0);   // xóa các dòng cũ
        for (LichHen lichHen : danhSachLichHen) {
            modelLichHen.addRow(new Object[]{
                    lichHen.getMaLichHen(),
                    lichHen.getThoiGianHen().format(DINH_DANG_GIO),
                    lichHen.getTenBenhNhan(),
                    lichHen.getSoDienThoaiBenhNhan(),
                    lichHen.getTenBacSi(),
                    chuoiRongNeuNull(lichHen.getLyDoHen()),
                    lichHen.getTrangThaiHienThi()
            });
        }
        lblTrangThai.setForeground(XAM);
        lblTrangThai.setText("Ngày " + ngayDangXem.format(DINH_DANG_NGAY) + ": có "
                + danhSachLichHen.size() + " lịch hẹn. Mỗi lịch hẹn dài " + LichHenService.THOI_LUONG_PHUT
                + " phút. Chọn 1 lịch hẹn để xác nhận đến, đổi hoặc hủy.");
        hienChiTiet(null);
    }

    // Hiện lịch hẹn đang chọn ở pnlWest và bật / tắt 3 nút theo trạng thái + quyền (null = chưa chọn)
    public void hienChiTiet(LichHen lichHen) {
        if (lichHen == null) {
            lblMaLichHen.setText("—");
            lblThoiGianHen.setText("—");
            lblBenhNhan.setText("Chưa chọn");
            lblSoDienThoai.setText("—");
            lblBacSi.setText("—");
            lblTrangThaiLichHen.setText("—");
            txaLyDo.setText("—");
            btnXacNhanDen.setEnabled(false);
            btnDoiLich.setEnabled(false);
            btnHuyLich.setEnabled(false);
            return;
        }
        lblMaLichHen.setText(lichHen.getMaLichHen());
        lblThoiGianHen.setText(lichHen.getThoiGianHen().format(DINH_DANG_NGAY_GIO));
        lblBenhNhan.setText(lichHen.getTenBenhNhan());
        lblSoDienThoai.setText(lichHen.getSoDienThoaiBenhNhan());
        lblBacSi.setText(lichHen.getTenBacSi());
        lblTrangThaiLichHen.setText(lichHen.getTrangThaiHienThi());
        if (lichHen.getLyDoHen() == null) {
            txaLyDo.setText("—");
        } else {
            txaLyDo.setText(lichHen.getLyDoHen());
        }

        // BR03: chỉ lịch "Đã đặt" mới đổi / hủy; xác nhận đến chỉ cho lịch "Đã đặt" của hôm nay
        btnXacNhanDen.setEnabled(duocXacNhanDen && lichHen.duocXacNhanDen());
        btnDoiLich.setEnabled(duocQuanLyLich && lichHen.laDaDat());
        btnHuyLich.setEnabled(duocQuanLyLich && lichHen.laDaDat());
    }

    // Chọn dòng có mã lịch hẹn này (dùng sau khi lưu xong) và hiện ở pnlWest
    public void chonDongTheoMa(String maLichHen) {
        for (int dong = 0; dong < modelLichHen.getRowCount(); dong++) {
            if (modelLichHen.getValueAt(dong, 0).equals(maLichHen)) {
                tblLichHen.setRowSelectionInterval(dong, dong);
                tblLichHen.scrollRectToVisible(tblLichHen.getCellRect(dong, 0, true));
                hienChiTiet(danhSachLichHen.get(dong));
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

    // Lưu quyền của người đăng nhập (Controller gọi 1 lần khi mở màn hình). Service vẫn kiểm tra lại.
    public void datQuyen(boolean duocQuanLyLich, boolean duocXacNhanDen) {
        this.duocQuanLyLich = duocQuanLyLich;
        this.duocXacNhanDen = duocXacNhanDen;
        btnDatLich.setEnabled(duocQuanLyLich);
        hienChiTiet(layLichHenDangChon());
    }

    // Hiện lỗi: dòng chữ đỏ ở SOUTH + hộp thông báo giữa màn hình
    public void hienLoi(String thongBao) {
        lblTrangThai.setForeground(DO);
        lblTrangThai.setText(thongBao);
        JOptionPane.showMessageDialog(this, thongBao, "Thông báo lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private String chuoiRongNeuNull(String chuoi) {
        if (chuoi == null) {
            return "";
        }
        return chuoi;
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

    public JTable getTblLichHen() {
        return tblLichHen;
    }

    public JButton getBtnXacNhanDen() {
        return btnXacNhanDen;
    }

    public JButton getBtnDoiLich() {
        return btnDoiLich;
    }

    public JButton getBtnHuyLich() {
        return btnHuyLich;
    }
}
