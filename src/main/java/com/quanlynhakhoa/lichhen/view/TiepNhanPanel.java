package com.quanlynhakhoa.lichhen.view;

import com.quanlynhakhoa.benhnhan.view.BieuTuong;
import com.quanlynhakhoa.benhnhan.view.MauNut;
import com.quanlynhakhoa.lichhen.controller.TiepNhanController;
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

public class TiepNhanPanel extends JPanel {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color XAM = new Color(97, 115, 137);
    private static final Color VIEN = new Color(220, 231, 242);
    private static final Color NEN = new Color(245, 248, 252);
    private static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DINH_DANG_GIO = DateTimeFormatter.ofPattern("HH:mm");

    private JPanel pnlNorth;
    private JLabel lblTieuDe;
    private JLabel lblNgayHomNay;
    private JTextField txtTimKiem;
    private JButton btnTimKiem;
    private JButton btnLamMoi;
    private JButton btnTiepNhanKhongHen;

    private JPanel pnlCenter;
    private DefaultTableModel modelLichHen;
    private JTable tblLichHen;
    private JScrollPane scrLichHen;
    private ArrayList<LichHen> danhSachLichHen = new ArrayList<>();

    private JPanel pnlEast;
    private JLabel lblMaLichHen;
    private JLabel lblGioHen;
    private JLabel lblBenhNhan;
    private JLabel lblSoDienThoai;
    private JLabel lblBacSi;
    private JLabel lblTrangThai;
    private JTextArea txaLyDo;
    private JButton btnCheckIn;
    private boolean duocTiepNhan;

    public TiepNhanPanel(DesktopSession phienDangNhap) {
        setLayout(new BorderLayout(14, 14));
        setBackground(NEN);
        setBorder(new EmptyBorder(24, 26, 24, 26));

        taoNorth();
        taoCenter();
        taoEast();
        add(pnlNorth, BorderLayout.NORTH);
        add(pnlCenter, BorderLayout.CENTER);
        add(pnlEast, BorderLayout.EAST);

        hienChiTiet(null);

        new TiepNhanController(this, new LichHenService(phienDangNhap));
    }

    private void taoNorth() {
        pnlNorth = new JPanel(new BorderLayout(0, 10));
        pnlNorth.setOpaque(false);

        lblTieuDe = new JLabel("Tiếp nhận bệnh nhân");
        lblTieuDe.setFont(new Font("SansSerif", Font.BOLD, 23));
        lblTieuDe.setForeground(NAVY);

        lblNgayHomNay = new JLabel("Lịch hẹn ngày " + LocalDate.now().format(DINH_DANG_NGAY));
        lblNgayHomNay.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblNgayHomNay.setForeground(NAVY);
        txtTimKiem = new JTextField(16);
        btnTimKiem = new JButton("Tìm kiếm");
        btnTimKiem.setIcon(new BieuTuong(BieuTuong.TIM_KIEM));
        btnLamMoi = new JButton("Làm mới");
        btnTiepNhanKhongHen = new JButton("+ Tiếp nhận không hẹn");
        MauNut.toXanhDuong(btnTiepNhanKhongHen);

        JPanel pnlNorthSouth = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
        pnlNorthSouth.setBackground(Color.WHITE);
        pnlNorthSouth.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlNorthSouth.add(lblNgayHomNay);
        pnlNorthSouth.add(new JLabel("Tìm:"));
        pnlNorthSouth.add(txtTimKiem);
        pnlNorthSouth.add(btnTimKiem);
        pnlNorthSouth.add(btnLamMoi);
        pnlNorthSouth.add(btnTiepNhanKhongHen);

        pnlNorth.add(lblTieuDe, BorderLayout.NORTH);
        pnlNorth.add(pnlNorthSouth, BorderLayout.SOUTH);
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
        lblGioHen = taoGiaTri();
        lblBenhNhan = taoGiaTri();
        lblSoDienThoai = taoGiaTri();
        lblBacSi = taoGiaTri();
        lblTrangThai = taoGiaTri();
        txaLyDo = new JTextArea("—");
        txaLyDo.setEditable(false);
        txaLyDo.setLineWrap(true);
        txaLyDo.setWrapStyleWord(true);
        txaLyDo.setOpaque(false);
        txaLyDo.setFont(new Font("SansSerif", Font.BOLD, 13));
        txaLyDo.setForeground(NAVY);

        themDong(boxThongTin, "Mã lịch hẹn:", lblMaLichHen);
        themDong(boxThongTin, "Giờ hẹn:", lblGioHen);
        themDong(boxThongTin, "Bệnh nhân:", lblBenhNhan);
        themDong(boxThongTin, "SĐT:", lblSoDienThoai);
        themDong(boxThongTin, "Bác sĩ:", lblBacSi);
        themDong(boxThongTin, "Trạng thái:", lblTrangThai);

        JLabel lblNhanLyDo = new JLabel("Lý do khám:");
        lblNhanLyDo.setForeground(XAM);
        boxThongTin.add(dongTrai(lblNhanLyDo));
        boxThongTin.add(Box.createVerticalStrut(4));
        boxThongTin.add(txaLyDo);

        btnCheckIn = new JButton("Check-in");
        MauNut.toXanhLa(btnCheckIn);
        JPanel pnlNutThaoTac = new JPanel(new GridLayout(1, 1, 0, 8));
        pnlNutThaoTac.setOpaque(false);
        pnlNutThaoTac.setBorder(new EmptyBorder(10, 14, 14, 14));
        pnlNutThaoTac.add(btnCheckIn);

        pnlEast.add(boxThongTin, BorderLayout.NORTH);
        pnlEast.add(pnlNutThaoTac, BorderLayout.SOUTH);
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
            String lyDo = lichHen.getLyDoHen();
            if (lyDo == null) {
                lyDo = "";
            }
            modelLichHen.addRow(new Object[]{
                    lichHen.getMaLichHen(),
                    lichHen.getThoiGianHen().format(DINH_DANG_GIO),
                    lichHen.getTenBenhNhan(),
                    lichHen.getSoDienThoaiBenhNhan(),
                    lichHen.getTenBacSi(),
                    lyDo,
                    lichHen.getTrangThaiHienThi()
            });
        }
        hienChiTiet(null);
    }

    public void hienChiTiet(LichHen lichHen) {
        if (lichHen == null) {
            lblMaLichHen.setText("—");
            lblGioHen.setText("—");
            lblBenhNhan.setText("—");
            lblSoDienThoai.setText("—");
            lblBacSi.setText("—");
            lblTrangThai.setText("—");
            txaLyDo.setText("—");
            btnCheckIn.setEnabled(false);
            return;
        }
        lblMaLichHen.setText(lichHen.getMaLichHen());
        lblGioHen.setText(lichHen.getThoiGianHen().format(DINH_DANG_GIO));
        lblBenhNhan.setText(lichHen.getTenBenhNhan());
        lblSoDienThoai.setText(lichHen.getSoDienThoaiBenhNhan());
        lblBacSi.setText(lichHen.getTenBacSi());
        lblTrangThai.setText(lichHen.getTrangThaiHienThi());
        if (lichHen.getLyDoHen() == null) {
            txaLyDo.setText("—");
        } else {
            txaLyDo.setText(lichHen.getLyDoHen());
        }
        btnCheckIn.setEnabled(duocTiepNhan && lichHen.duocTiepDon());
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

    public void datDangTai(boolean dangTai) {
        btnTimKiem.setEnabled(!dangTai);
        btnLamMoi.setEnabled(!dangTai);
    }

    public void datQuyen(boolean duocTiepNhan) {
        this.duocTiepNhan = duocTiepNhan;
        btnTiepNhanKhongHen.setEnabled(duocTiepNhan);
        hienChiTiet(layLichHenDangChon());
    }

    public void hienLoi(String thongBao) {
        JOptionPane.showMessageDialog(this, thongBao, "Thông báo lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public JTextField getTxtTimKiem() {
        return txtTimKiem;
    }

    public JButton getBtnTimKiem() {
        return btnTimKiem;
    }

    public JButton getBtnLamMoi() {
        return btnLamMoi;
    }

    public JButton getBtnTiepNhanKhongHen() {
        return btnTiepNhanKhongHen;
    }

    public JTable getTblLichHen() {
        return tblLichHen;
    }

    public JButton getBtnCheckIn() {
        return btnCheckIn;
    }
}
