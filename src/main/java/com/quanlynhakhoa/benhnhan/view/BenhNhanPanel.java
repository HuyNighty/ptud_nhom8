package com.quanlynhakhoa.benhnhan.view;

import com.quanlynhakhoa.benhnhan.controller.BenhNhanController;
import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.benhnhan.service.BenhNhanService;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

public class BenhNhanPanel extends JPanel {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color XAM = new Color(97, 115, 137);
    private static final Color VIEN = new Color(220, 231, 242);
    private static final Color NEN = new Color(245, 248, 252);
    private static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DINH_DANG_NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private JPanel pnlNorth;
    private JLabel lblTieuDe;
    private JTextField txtTimKiem;
    private JComboBox<String> cmbGioiTinh;
    private JButton btnLoc;
    private JButton btnXoaLoc;
    private JButton btnTaiLai;
    private JButton btnThem;

    private JPanel pnlCenter;
    private DefaultTableModel modelBenhNhan;
    private JTable tblBenhNhan;
    private JScrollPane scrBenhNhan;

    private JPanel pnlEast;
    private JLabel lblTenBenhNhan;
    private JLabel lblMaBenhNhan;
    private JLabel lblNgaySinh;
    private JLabel lblGioiTinh;
    private JLabel lblSoDienThoai;
    private JLabel lblDiaChi;
    private JLabel lblNguoiLienHe;
    private JLabel lblNgayTao;
    private JTextArea txaTienSuBenh;
    private JTextArea txaDiUng;
    private JButton btnSua;
    private BenhNhan benhNhanDangXem;
    private boolean duocSua;

    private JPanel pnlSouth;
    private JLabel lblTrangThai;

    public BenhNhanPanel(DesktopSession phienDangNhap) {
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

        new BenhNhanController(this, new BenhNhanService(phienDangNhap));
    }

    private void taoNorth() {
        pnlNorth = new JPanel(new BorderLayout(0, 10));
        pnlNorth.setOpaque(false);

        lblTieuDe = new JLabel("Danh sách bệnh nhân");
        lblTieuDe.setFont(new Font("SansSerif", Font.BOLD, 23));
        lblTieuDe.setForeground(NAVY);

        txtTimKiem = new JTextField(24);
        cmbGioiTinh = new JComboBox<>(new String[]{"Tất cả", "Nam", "Nữ"});
        btnLoc = new JButton("Tìm kiếm");
        btnXoaLoc = new JButton("Xóa lọc");
        btnTaiLai = new JButton("Làm mới");
        btnLoc.setIcon(new BieuTuong(BieuTuong.TIM_KIEM));
        btnTaiLai.setIcon(new BieuTuong(BieuTuong.LAM_MOI));
        btnThem = new JButton("+ Thêm bệnh nhân");
        MauNut.toXanhDuong(btnThem);

        JPanel pnlNorthSouth = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
        pnlNorthSouth.setBackground(Color.WHITE);
        pnlNorthSouth.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlNorthSouth.add(new JLabel("Tìm:"));
        pnlNorthSouth.add(txtTimKiem);
        pnlNorthSouth.add(new JLabel("Giới tính:"));
        pnlNorthSouth.add(cmbGioiTinh);
        pnlNorthSouth.add(btnLoc);
        pnlNorthSouth.add(btnXoaLoc);
        pnlNorthSouth.add(btnTaiLai);
        pnlNorthSouth.add(btnThem);

        pnlNorth.add(lblTieuDe, BorderLayout.NORTH);
        pnlNorth.add(pnlNorthSouth, BorderLayout.SOUTH);
    }

    private void taoCenter() {
        pnlCenter = new JPanel(new BorderLayout());
        pnlCenter.setOpaque(false);

        String[] tenCacCot = {"Mã BN", "Họ tên", "Ngày sinh", "Giới tính", "SĐT"};
        modelBenhNhan = new DefaultTableModel(tenCacCot, 0) {
            @Override
            public boolean isCellEditable(int dong, int cot) {
                return false;
            }
        };
        tblBenhNhan = new JTable(modelBenhNhan);
        tblBenhNhan.setRowHeight(30);
        tblBenhNhan.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblBenhNhan.getTableHeader().setReorderingAllowed(false);
        tblBenhNhan.setFillsViewportHeight(true);

        scrBenhNhan = new JScrollPane(tblBenhNhan);
        scrBenhNhan.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlCenter.add(scrBenhNhan, BorderLayout.CENTER);
    }

    private void taoEast() {
        pnlEast = new JPanel(new BorderLayout());
        pnlEast.setBackground(Color.WHITE);
        pnlEast.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlEast.setPreferredSize(new Dimension(360, 0));

        Box boxThongTin = Box.createVerticalBox();
        boxThongTin.setBorder(new EmptyBorder(16, 16, 16, 16));

        lblTenBenhNhan = new JLabel("Chưa chọn bệnh nhân");
        lblTenBenhNhan.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblTenBenhNhan.setForeground(NAVY);
        lblMaBenhNhan = new JLabel("");
        lblMaBenhNhan.setForeground(XAM);

        Box boxTen = Box.createHorizontalBox();
        boxTen.add(lblTenBenhNhan);
        boxTen.add(Box.createHorizontalGlue());
        Box boxMa = Box.createHorizontalBox();
        boxMa.add(lblMaBenhNhan);
        boxMa.add(Box.createHorizontalGlue());
        boxThongTin.add(boxTen);
        boxThongTin.add(Box.createVerticalStrut(4));
        boxThongTin.add(boxMa);
        boxThongTin.add(Box.createVerticalStrut(18));

        JLabel lblNhanNgaySinh = taoNhan("Ngày sinh:");
        JLabel lblNhanGioiTinh = taoNhan("Giới tính:");
        JLabel lblNhanSoDienThoai = taoNhan("Số điện thoại:");
        JLabel lblNhanDiaChi = taoNhan("Địa chỉ:");
        JLabel lblNhanNguoiLienHe = taoNhan("Người liên hệ:");
        JLabel lblNhanNgayTao = taoNhan("Ngày tạo:");

        lblNgaySinh = taoGiaTri();
        lblGioiTinh = taoGiaTri();
        lblSoDienThoai = taoGiaTri();
        lblDiaChi = taoGiaTri();
        lblNguoiLienHe = taoGiaTri();
        lblNgayTao = taoGiaTri();

        lblNhanNgaySinh.setPreferredSize(lblNhanNguoiLienHe.getPreferredSize());
        lblNhanGioiTinh.setPreferredSize(lblNhanNguoiLienHe.getPreferredSize());
        lblNhanSoDienThoai.setPreferredSize(lblNhanNguoiLienHe.getPreferredSize());
        lblNhanDiaChi.setPreferredSize(lblNhanNguoiLienHe.getPreferredSize());
        lblNhanNgayTao.setPreferredSize(lblNhanNguoiLienHe.getPreferredSize());

        themDong(boxThongTin, lblNhanNgaySinh, lblNgaySinh);
        themDong(boxThongTin, lblNhanGioiTinh, lblGioiTinh);
        themDong(boxThongTin, lblNhanSoDienThoai, lblSoDienThoai);
        themDong(boxThongTin, lblNhanDiaChi, lblDiaChi);
        themDong(boxThongTin, lblNhanNguoiLienHe, lblNguoiLienHe);
        themDong(boxThongTin, lblNhanNgayTao, lblNgayTao);

        JLabel lblNhanTienSu = taoNhan("Tiền sử bệnh:");
        JLabel lblNhanDiUng = taoNhan("Dị ứng:");
        lblNhanTienSu.setPreferredSize(lblNhanNguoiLienHe.getPreferredSize());
        lblNhanDiUng.setPreferredSize(lblNhanNguoiLienHe.getPreferredSize());
        txaTienSuBenh = taoOChuChiXem();
        txaDiUng = taoOChuChiXem();
        themDongNhieuChu(boxThongTin, lblNhanTienSu, txaTienSuBenh);
        themDongNhieuChu(boxThongTin, lblNhanDiUng, txaDiUng);

        pnlEast.add(boxThongTin, BorderLayout.NORTH);

        btnSua = new JButton("Sửa thông tin");
        MauNut.toCam(btnSua);
        btnSua.setEnabled(false);
        JPanel pnlEastSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        pnlEastSouth.setOpaque(false);
        pnlEastSouth.add(btnSua);
        pnlEast.add(pnlEastSouth, BorderLayout.SOUTH);
    }

    private JTextArea taoOChuChiXem() {
        JTextArea txaChiXem = new JTextArea("—");
        txaChiXem.setEditable(false);
        txaChiXem.setLineWrap(true);
        txaChiXem.setWrapStyleWord(true);
        txaChiXem.setOpaque(false);
        txaChiXem.setBorder(null);
        txaChiXem.setFont(new Font("SansSerif", Font.BOLD, 13));
        txaChiXem.setForeground(NAVY);
        return txaChiXem;
    }

    private void themDongNhieuChu(Box boxThongTin, JLabel lblNhan, JTextArea txaGiaTri) {
        Box boxDong = Box.createHorizontalBox();
        lblNhan.setAlignmentY(TOP_ALIGNMENT);
        txaGiaTri.setAlignmentY(TOP_ALIGNMENT);
        boxDong.add(lblNhan);
        boxDong.add(Box.createHorizontalStrut(10));
        boxDong.add(txaGiaTri);
        boxThongTin.add(boxDong);
        boxThongTin.add(Box.createVerticalStrut(12));
    }

    private void themDong(Box boxThongTin, JLabel lblNhan, JLabel lblGiaTri) {
        Box boxDong = Box.createHorizontalBox();
        boxDong.add(lblNhan);
        boxDong.add(Box.createHorizontalStrut(10));
        boxDong.add(lblGiaTri);
        boxDong.add(Box.createHorizontalGlue());
        boxThongTin.add(boxDong);
        boxThongTin.add(Box.createVerticalStrut(12));
    }

    private JLabel taoNhan(String noiDung) {
        JLabel lblNhan = new JLabel(noiDung);
        lblNhan.setForeground(XAM);
        return lblNhan;
    }

    private JLabel taoGiaTri() {
        JLabel lblGiaTri = new JLabel("—");
        lblGiaTri.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblGiaTri.setForeground(NAVY);
        return lblGiaTri;
    }

    private void taoSouth() {
        pnlSouth = new JPanel(new BorderLayout());
        pnlSouth.setOpaque(false);
        lblTrangThai = new JLabel("Chưa tải dữ liệu.");
        lblTrangThai.setForeground(XAM);
        pnlSouth.add(lblTrangThai, BorderLayout.CENTER);
    }

    public void docDuLieuVaoTable(ArrayList<BenhNhan> danhSachBenhNhan) {
        modelBenhNhan.setRowCount(0);
        for (BenhNhan benhNhan : danhSachBenhNhan) {
            modelBenhNhan.addRow(new Object[]{
                    benhNhan.getMaBenhNhan(),
                    benhNhan.getHoTen(),
                    benhNhan.getNgaySinh().format(DINH_DANG_NGAY),
                    benhNhan.getGioiTinhHienThi(),
                    benhNhan.getSoDienThoai()
            });
        }
        lblTrangThai.setForeground(XAM);
        lblTrangThai.setText("Có " + danhSachBenhNhan.size() + " bệnh nhân.");
        hienChiTiet(null);
    }

    public void hienChiTiet(BenhNhan benhNhan) {
        benhNhanDangXem = benhNhan;
        if (benhNhan == null) {
            btnSua.setEnabled(false);
            lblTenBenhNhan.setText("Chưa chọn bệnh nhân");
            lblMaBenhNhan.setText("");
            lblNgaySinh.setText("—");
            lblGioiTinh.setText("—");
            lblSoDienThoai.setText("—");
            lblDiaChi.setText("—");
            lblNguoiLienHe.setText("—");
            lblNgayTao.setText("—");
            txaTienSuBenh.setText("—");
            txaDiUng.setText("—");
            return;
        }
        btnSua.setEnabled(duocSua);
        lblTenBenhNhan.setText(benhNhan.getHoTen());
        lblMaBenhNhan.setText("Mã bệnh nhân: " + benhNhan.getMaBenhNhan());
        lblNgaySinh.setText(benhNhan.getNgaySinh().format(DINH_DANG_NGAY));
        lblGioiTinh.setText(benhNhan.getGioiTinhHienThi());
        lblSoDienThoai.setText(benhNhan.getSoDienThoai());
        lblDiaChi.setText(hienGachNgangNeuTrong(benhNhan.getDiaChi()));
        lblNguoiLienHe.setText(hienGachNgangNeuTrong(benhNhan.getNguoiLienHe()));
        if (benhNhan.getNgayTao() == null) {
            lblNgayTao.setText("—");
        } else {
            lblNgayTao.setText(benhNhan.getNgayTao().format(DINH_DANG_NGAY_GIO));
        }
        txaTienSuBenh.setText(hienKhongCoNeuTrong(benhNhan.getTienSuBenh()));
        txaDiUng.setText(hienKhongCoNeuTrong(benhNhan.getDiUng()));
    }

    public void datDangTai(boolean dangTai) {
        btnLoc.setEnabled(!dangTai);
        btnXoaLoc.setEnabled(!dangTai);
        btnTaiLai.setEnabled(!dangTai);
        if (dangTai) {
            lblTrangThai.setForeground(XAM);
            lblTrangThai.setText("Đang tải dữ liệu...");
        }
    }

    public void hienLoi(String thongBao) {
        lblTrangThai.setForeground(new Color(176, 32, 32));
        lblTrangThai.setText(thongBao);
        JOptionPane.showMessageDialog(this, thongBao, "Thông báo lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private String hienGachNgangNeuTrong(String giaTri) {
        if (giaTri == null) {
            return "—";
        }
        return giaTri;
    }

    private String hienKhongCoNeuTrong(String giaTri) {
        if (giaTri == null) {
            return "Không có";
        }
        return giaTri;
    }

    public void datQuyen(boolean duocThem, boolean duocSua) {
        this.duocSua = duocSua;
        btnThem.setEnabled(duocThem);
        btnSua.setEnabled(benhNhanDangXem != null && duocSua);
    }

    public void chonDongTheoMa(String maBenhNhan) {
        for (int dong = 0; dong < modelBenhNhan.getRowCount(); dong++) {
            if (modelBenhNhan.getValueAt(dong, 0).equals(maBenhNhan)) {
                tblBenhNhan.setRowSelectionInterval(dong, dong);
                tblBenhNhan.scrollRectToVisible(tblBenhNhan.getCellRect(dong, 0, true));
                return;
            }
        }
    }

    public BenhNhan getBenhNhanDangXem() {
        return benhNhanDangXem;
    }

    public void xoaTrang() {
        txtTimKiem.setText("");
        cmbGioiTinh.setSelectedIndex(0);
        txtTimKiem.requestFocus();
    }

    public String layGioiTinhDangChon() {
        Object luaChon = cmbGioiTinh.getSelectedItem();
        if ("Nam".equals(luaChon)) {
            return "NAM";
        }
        if ("Nữ".equals(luaChon)) {
            return "NU";
        }
        return "";
    }

    public JTextField getTxtTimKiem() {
        return txtTimKiem;
    }

    public JComboBox<String> getCmbGioiTinh() {
        return cmbGioiTinh;
    }

    public JButton getBtnLoc() {
        return btnLoc;
    }

    public JButton getBtnXoaLoc() {
        return btnXoaLoc;
    }

    public JButton getBtnTaiLai() {
        return btnTaiLai;
    }

    public JTable getTblBenhNhan() {
        return tblBenhNhan;
    }

    public JButton getBtnThem() {
        return btnThem;
    }

    public JButton getBtnSua() {
        return btnSua;
    }
}
