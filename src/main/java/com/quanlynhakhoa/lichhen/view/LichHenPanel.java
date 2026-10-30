package com.quanlynhakhoa.lichhen.view;

import com.quanlynhakhoa.benhnhan.view.BieuTuong;
import com.quanlynhakhoa.benhnhan.view.MauNut;
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

public class LichHenPanel extends JPanel {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color XAM = new Color(97, 115, 137);
    private static final Color VIEN = new Color(220, 231, 242);
    private static final Color NEN = new Color(245, 248, 252);
    private static final Color DO = new Color(176, 32, 32);
    private static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DINH_DANG_GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DINH_DANG_NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private JPanel pnlNorth;
    private JLabel lblTieuDe;
    private JTextField txtNgayXem;
    private JButton btnChonNgayXem;
    private JButton btnHomNay;
    private JTextField txtTimKiem;
    private JButton btnTimKiem;
    private JButton btnDatLich;

    private JPanel pnlEast;
    private JLabel lblMaLichHen;
    private JLabel lblThoiGianHen;
    private JLabel lblBenhNhan;
    private JLabel lblSoDienThoai;
    private JLabel lblBacSi;
    private JLabel lblTrangThaiLichHen;
    private JTextArea txaLyDo;
    private JButton btnTiepDon;
    private JButton btnDoiLich;
    private JButton btnHuyLich;
    private boolean duocQuanLyLich;
    private boolean duocTiepDon;

    private JPanel pnlCenter;
    private DefaultTableModel modelLichHen;
    private JTable tblLichHen;
    private JScrollPane scrLichHen;
    private ArrayList<LichHen> danhSachLichHen = new ArrayList<>();

    private JPanel pnlSouth;
    private JLabel lblTrangThai;

    private LocalDate ngayDangXem = LocalDate.now();

    public LichHenPanel(DesktopSession phienDangNhap) {
        setLayout(new BorderLayout(14, 14));
        setBackground(NEN);
        setBorder(new EmptyBorder(24, 26, 24, 26));

        taoNorth();
        taoCenter();
        taoEast();
        taoSouth();
        add(pnlNorth, BorderLayout.NORTH);
        add(pnlCenter, BorderLayout.CENTER);
        add(pnlEast, BorderLayout.EAST);
        add(pnlSouth, BorderLayout.SOUTH);

        datNgayDangXem(LocalDate.now());
        hienChiTiet(null);

        new LichHenController(this, new LichHenService(phienDangNhap));
    }

    private void taoNorth() {
        pnlNorth = new JPanel(new BorderLayout(0, 10));
        pnlNorth.setOpaque(false);

        lblTieuDe = new JLabel("Lịch hẹn");
        lblTieuDe.setFont(new Font("SansSerif", Font.BOLD, 23));
        lblTieuDe.setForeground(NAVY);

        txtNgayXem = new JTextField(9);
        txtNgayXem.setEditable(false);
        btnChonNgayXem = new JButton("Chọn");
        btnHomNay = new JButton("Hôm nay");
        txtTimKiem = new JTextField(16);
        btnTimKiem = new JButton("Tìm kiếm");
        btnTimKiem.setIcon(new BieuTuong(BieuTuong.TIM_KIEM));
        btnDatLich = new JButton("+ Đặt lịch hẹn");
        MauNut.toXanhDuong(btnDatLich);

        JPanel pnlNorthSouth = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
        pnlNorthSouth.setBackground(Color.WHITE);
        pnlNorthSouth.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlNorthSouth.add(new JLabel("Ngày:"));
        pnlNorthSouth.add(txtNgayXem);
        pnlNorthSouth.add(btnChonNgayXem);
        pnlNorthSouth.add(btnHomNay);
        pnlNorthSouth.add(new JLabel("Tìm:"));
        pnlNorthSouth.add(txtTimKiem);
        pnlNorthSouth.add(btnTimKiem);
        pnlNorthSouth.add(btnDatLich);

        pnlNorth.add(lblTieuDe, BorderLayout.NORTH);
        pnlNorth.add(pnlNorthSouth, BorderLayout.SOUTH);
    }

    private void taoEast() {
        pnlEast = new JPanel(new BorderLayout());
        pnlEast.setBackground(Color.WHITE);
        pnlEast.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlEast.setPreferredSize(new Dimension(270, 0));

        Box boxThongTin = Box.createVerticalBox();
        boxThongTin.setBorder(new EmptyBorder(14, 14, 10, 14));

        JLabel lblTieuDeKhung = new JLabel("Thông tin lịch hẹn");
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

        JLabel lblNhanLyDo = new JLabel("Lý do khám:");
        lblNhanLyDo.setForeground(XAM);
        boxThongTin.add(dongTrai(lblNhanLyDo));
        boxThongTin.add(Box.createVerticalStrut(4));
        boxThongTin.add(txaLyDo);

        btnTiepDon = new JButton("Tiếp đón bệnh nhân");
        MauNut.toXanhLa(btnTiepDon);
        btnDoiLich = new JButton("Đổi lịch");
        MauNut.toCam(btnDoiLich);
        btnHuyLich = new JButton("Hủy lịch");
        MauNut.toDo(btnHuyLich);
        JPanel pnlNutThaoTac = new JPanel(new GridLayout(3, 1, 0, 8));
        pnlNutThaoTac.setOpaque(false);
        pnlNutThaoTac.setBorder(new EmptyBorder(10, 14, 14, 14));
        pnlNutThaoTac.add(btnTiepDon);
        pnlNutThaoTac.add(btnDoiLich);
        pnlNutThaoTac.add(btnHuyLich);

        pnlEast.add(boxThongTin, BorderLayout.NORTH);
        pnlEast.add(pnlNutThaoTac, BorderLayout.SOUTH);
    }

    private void taoCenter() {
        pnlCenter = new JPanel(new BorderLayout());
        pnlCenter.setOpaque(false);

        String[] tenCacCot = {"Mã lịch hẹn", "Giờ hẹn", "Bệnh nhân", "SĐT", "Bác sĩ", "Lý do", "Trạng thái"};
        modelLichHen = new DefaultTableModel(tenCacCot, 0) {
            @Override
            public boolean isCellEditable(int dong, int cot) {
                return false;
            }
        };
        tblLichHen = new JTable(modelLichHen);
        tblLichHen.setRowHeight(30);
        tblLichHen.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblLichHen.getTableHeader().setReorderingAllowed(false);
        tblLichHen.setFillsViewportHeight(true);

        tblLichHen.getColumnModel().getColumn(0).setPreferredWidth(90);
        tblLichHen.getColumnModel().getColumn(1).setPreferredWidth(60);
        tblLichHen.getColumnModel().getColumn(2).setPreferredWidth(170);
        tblLichHen.getColumnModel().getColumn(3).setPreferredWidth(100);
        tblLichHen.getColumnModel().getColumn(4).setPreferredWidth(140);
        tblLichHen.getColumnModel().getColumn(5).setPreferredWidth(200);
        tblLichHen.getColumnModel().getColumn(6).setPreferredWidth(110);

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

    private JLabel taoGiaTri() {
        JLabel lblGiaTri = new JLabel("—");
        lblGiaTri.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblGiaTri.setForeground(NAVY);
        return lblGiaTri;
    }

    private Box dongTrai(JLabel lblNoiDung) {
        Box boxDong = Box.createHorizontalBox();
        boxDong.add(lblNoiDung);
        boxDong.add(Box.createHorizontalGlue());
        return boxDong;
    }

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

    public void hienDanhSachLichHen(ArrayList<LichHen> danhSachMoi) {
        danhSachLichHen = danhSachMoi;
        modelLichHen.setRowCount(0);
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
        lblTrangThai.setText("Có " + danhSachLichHen.size() + " lịch hẹn.");
        hienChiTiet(null);
    }

    public void hienChiTiet(LichHen lichHen) {
        if (lichHen == null) {
            lblMaLichHen.setText("—");
            lblThoiGianHen.setText("—");
            lblBenhNhan.setText("—");
            lblSoDienThoai.setText("—");
            lblBacSi.setText("—");
            lblTrangThaiLichHen.setText("—");
            txaLyDo.setText("—");
            btnTiepDon.setEnabled(false);
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

        btnTiepDon.setEnabled(duocTiepDon && lichHen.duocTiepDon());
        btnDoiLich.setEnabled(duocQuanLyLich && lichHen.laDaDat());
        btnHuyLich.setEnabled(duocQuanLyLich && lichHen.laDaDat());
    }

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

    public LichHen layLichHenDangChon() {
        int dongDangChon = tblLichHen.getSelectedRow();
        if (dongDangChon < 0 || dongDangChon >= danhSachLichHen.size()) {
            return null;
        }
        return danhSachLichHen.get(dongDangChon);
    }

    public void datNgayDangXem(LocalDate ngay) {
        ngayDangXem = ngay;
        txtNgayXem.setText(ngay.format(DINH_DANG_NGAY));
    }

    public LocalDate getNgayDangXem() {
        return ngayDangXem;
    }

    public void datDangTai(boolean dangTai) {
        btnTimKiem.setEnabled(!dangTai);
        btnHomNay.setEnabled(!dangTai);
        btnChonNgayXem.setEnabled(!dangTai);
        if (dangTai) {
            lblTrangThai.setForeground(XAM);
            lblTrangThai.setText("Đang tải dữ liệu...");
        }
    }

    public void datQuyen(boolean duocQuanLyLich, boolean duocTiepDon) {
        this.duocQuanLyLich = duocQuanLyLich;
        this.duocTiepDon = duocTiepDon;
        btnDatLich.setEnabled(duocQuanLyLich);
        hienChiTiet(layLichHenDangChon());
    }

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

    public JButton getBtnTiepDon() {
        return btnTiepDon;
    }

    public JButton getBtnDoiLich() {
        return btnDoiLich;
    }

    public JButton getBtnHuyLich() {
        return btnHuyLich;
    }
}
