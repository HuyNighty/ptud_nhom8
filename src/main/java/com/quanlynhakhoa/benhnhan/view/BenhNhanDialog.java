package com.quanlynhakhoa.benhnhan.view;

import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.benhnhan.service.BenhNhanService;
import com.quanlynhakhoa.benhnhan.service.LoiNhapLieuException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

public class BenhNhanDialog extends JDialog {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color XAM = new Color(97, 115, 137);
    private static final Color DO = new Color(200, 40, 40);
    private static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private JPanel pnlNorth;
    private JLabel lblTieuDe;

    private JPanel pnlCenter;
    private JTextField txtMaBenhNhan;
    private JTextField txtHoTen;
    private JTextField txtNgaySinh;
    private JButton btnChonNgay;
    private JComboBox<String> cmbGioiTinh;
    private JTextField txtSoDienThoai;
    private JTextField txtDiaChi;
    private JTextField txtNguoiLienHe;
    private JTextArea txaTienSuBenh;
    private JScrollPane scrTienSuBenh;
    private JTextArea txaDiUng;
    private JScrollPane scrDiUng;

    private JPanel pnlSouth;
    private JButton btnLuu;
    private JButton btnHuy;

    private HashMap<JComponent, Border> vienBanDau = new HashMap<>();

    public BenhNhanDialog(Window cuaSoCha, BenhNhan benhNhanCanSua) {
        super(cuaSoCha, ModalityType.APPLICATION_MODAL);
        String tieuDe;
        if (benhNhanCanSua == null) {
            tieuDe = "Thêm bệnh nhân";
        } else {
            tieuDe = "Sửa thông tin bệnh nhân";
        }
        setTitle(tieuDe);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel pnlNoiDung = new JPanel(new BorderLayout(0, 12));
        pnlNoiDung.setBorder(new EmptyBorder(18, 20, 16, 20));
        setContentPane(pnlNoiDung);

        taoNorth(tieuDe);
        taoCenter();
        taoSouth();
        pnlNoiDung.add(pnlNorth, BorderLayout.NORTH);
        pnlNoiDung.add(pnlCenter, BorderLayout.CENTER);
        pnlNoiDung.add(pnlSouth, BorderLayout.SOUTH);

        if (benhNhanCanSua != null) {
            doDuLieuLenForm(benhNhanCanSua);
        }

        setSize(560, 560);
        setLocationRelativeTo(cuaSoCha);
    }

    private void taoNorth(String tieuDe) {
        pnlNorth = new JPanel(new BorderLayout());
        lblTieuDe = new JLabel(tieuDe);
        lblTieuDe.setFont(new Font("SansSerif", Font.BOLD, 19));
        lblTieuDe.setForeground(NAVY);
        pnlNorth.add(lblTieuDe, BorderLayout.WEST);
    }

    private void taoCenter() {
        pnlCenter = new JPanel(new BorderLayout());

        txtMaBenhNhan = new JTextField();
        txtMaBenhNhan.setEditable(false);
        txtHoTen = new JTextField();

        txtNgaySinh = new JTextField();
        txtNgaySinh.setEditable(false);
        btnChonNgay = new JButton("Chọn");
        Box boxNgaySinh = Box.createHorizontalBox();
        boxNgaySinh.add(txtNgaySinh);
        boxNgaySinh.add(Box.createHorizontalStrut(6));
        boxNgaySinh.add(btnChonNgay);

        cmbGioiTinh = new JComboBox<>(new String[]{"Nam", "Nữ"});
        txtSoDienThoai = new JTextField();
        txtDiaChi = new JTextField();
        txtNguoiLienHe = new JTextField();
        txaTienSuBenh = taoOChuNhieuDong();
        scrTienSuBenh = new JScrollPane(txaTienSuBenh);
        txaDiUng = taoOChuNhieuDong();
        scrDiUng = new JScrollPane(txaDiUng);

        vienBanDau.put(txtHoTen, txtHoTen.getBorder());
        vienBanDau.put(txtNgaySinh, txtNgaySinh.getBorder());
        vienBanDau.put(cmbGioiTinh, cmbGioiTinh.getBorder());
        vienBanDau.put(txtSoDienThoai, txtSoDienThoai.getBorder());
        vienBanDau.put(txtDiaChi, txtDiaChi.getBorder());
        vienBanDau.put(txtNguoiLienHe, txtNguoiLienHe.getBorder());
        vienBanDau.put(scrTienSuBenh, scrTienSuBenh.getBorder());
        vienBanDau.put(scrDiUng, scrDiUng.getBorder());

        JLabel lblNhanMa = new JLabel("Mã bệnh nhân:");
        JLabel lblNhanHoTen = new JLabel("Họ tên (*):");
        JLabel lblNhanNgaySinh = new JLabel("Ngày sinh (*):");
        JLabel lblNhanGioiTinh = new JLabel("Giới tính (*):");
        JLabel lblNhanSoDienThoai = new JLabel("Số điện thoại (*):");
        JLabel lblNhanDiaChi = new JLabel("Địa chỉ:");
        JLabel lblNhanNguoiLienHe = new JLabel("Người liên hệ:");
        JLabel lblNhanTienSu = new JLabel("Tiền sử bệnh:");
        JLabel lblNhanDiUng = new JLabel("Dị ứng:");

        Dimension doRongNhan = lblNhanSoDienThoai.getPreferredSize();
        lblNhanMa.setPreferredSize(doRongNhan);
        lblNhanHoTen.setPreferredSize(doRongNhan);
        lblNhanNgaySinh.setPreferredSize(doRongNhan);
        lblNhanGioiTinh.setPreferredSize(doRongNhan);
        lblNhanDiaChi.setPreferredSize(doRongNhan);
        lblNhanNguoiLienHe.setPreferredSize(doRongNhan);
        lblNhanTienSu.setPreferredSize(doRongNhan);
        lblNhanDiUng.setPreferredSize(doRongNhan);
        lblNhanTienSu.setAlignmentY(TOP_ALIGNMENT);
        lblNhanDiUng.setAlignmentY(TOP_ALIGNMENT);
        scrTienSuBenh.setAlignmentY(TOP_ALIGNMENT);
        scrDiUng.setAlignmentY(TOP_ALIGNMENT);

        Box boxForm = Box.createVerticalBox();
        themDong(boxForm, lblNhanMa, txtMaBenhNhan);
        themDong(boxForm, lblNhanHoTen, txtHoTen);
        themDong(boxForm, lblNhanNgaySinh, boxNgaySinh);
        themDong(boxForm, lblNhanGioiTinh, cmbGioiTinh);
        themDong(boxForm, lblNhanSoDienThoai, txtSoDienThoai);
        themDong(boxForm, lblNhanDiaChi, txtDiaChi);
        themDong(boxForm, lblNhanNguoiLienHe, txtNguoiLienHe);
        themDong(boxForm, lblNhanTienSu, scrTienSuBenh);
        themDong(boxForm, lblNhanDiUng, scrDiUng);

        pnlCenter.add(boxForm, BorderLayout.NORTH);
    }

    private JTextArea taoOChuNhieuDong() {
        JTextArea txaNhieuDong = new JTextArea(3, 20);
        txaNhieuDong.setLineWrap(true);
        txaNhieuDong.setWrapStyleWord(true);
        return txaNhieuDong;
    }

    private void themDong(Box boxForm, JLabel lblNhan, Component oNhap) {
        Box boxDong = Box.createHorizontalBox();
        boxDong.add(lblNhan);
        boxDong.add(Box.createHorizontalStrut(10));
        boxDong.add(oNhap);
        boxForm.add(boxDong);
        boxForm.add(Box.createVerticalStrut(10));
    }

    private void taoSouth() {
        pnlSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnLuu = new JButton("Lưu");
        MauNut.toXanhDuong(btnLuu);
        btnHuy = new JButton("Hủy");
        MauNut.toDo(btnHuy);
        pnlSouth.add(btnHuy);
        pnlSouth.add(btnLuu);
    }

    private void doDuLieuLenForm(BenhNhan benhNhan) {
        txtMaBenhNhan.setText(benhNhan.getMaBenhNhan());
        txtHoTen.setText(benhNhan.getHoTen());
        txtNgaySinh.setText(benhNhan.getNgaySinh().format(DINH_DANG_NGAY));

        if ("KHAC".equals(benhNhan.getGioiTinh())) {
            cmbGioiTinh.addItem("Khác");
        }
        cmbGioiTinh.setSelectedItem(benhNhan.getGioiTinhHienThi());

        txtSoDienThoai.setText(benhNhan.getSoDienThoai());
        txtDiaChi.setText(chuoiRongNeuNull(benhNhan.getDiaChi()));
        txtNguoiLienHe.setText(chuoiRongNeuNull(benhNhan.getNguoiLienHe()));
        txaTienSuBenh.setText(chuoiRongNeuNull(benhNhan.getTienSuBenh()));
        txaDiUng.setText(chuoiRongNeuNull(benhNhan.getDiUng()));
    }

    private String chuoiRongNeuNull(String chuoi) {
        if (chuoi == null) {
            return "";
        }
        return chuoi;
    }

    public void moLichChonNgaySinh() {
        LocalDate ngayDangCo = null;
        if (!txtNgaySinh.getText().isEmpty()) {
            ngayDangCo = LocalDate.parse(txtNgaySinh.getText(), DINH_DANG_NGAY);
        }
        LocalDate ngayDaChon = ChonNgayDialog.chon(this, ngayDangCo);
        if (ngayDaChon != null) {
            txtNgaySinh.setText(ngayDaChon.format(DINH_DANG_NGAY));
        }
    }

    public BenhNhan layBenhNhanTuForm(String maBenhNhan) {
        LocalDate ngaySinh = BenhNhanService.docNgaySinh(txtNgaySinh.getText());

        String gioiTinh;
        Object luaChonGioiTinh = cmbGioiTinh.getSelectedItem();
        if ("Nữ".equals(luaChonGioiTinh)) {
            gioiTinh = "NU";
        } else if ("Khác".equals(luaChonGioiTinh)) {
            gioiTinh = "KHAC";
        } else {
            gioiTinh = "NAM";
        }

        BenhNhan benhNhan = new BenhNhan(maBenhNhan);
        benhNhan.setHoTen(txtHoTen.getText());
        benhNhan.setNgaySinh(ngaySinh);
        benhNhan.setGioiTinh(gioiTinh);
        benhNhan.setSoDienThoai(txtSoDienThoai.getText());
        benhNhan.setDiaChi(txtDiaChi.getText());
        benhNhan.setNguoiLienHe(txtNguoiLienHe.getText());
        benhNhan.setTienSuBenh(txaTienSuBenh.getText());
        benhNhan.setDiUng(txaDiUng.getText());
        return benhNhan;
    }

    public void toDoO(String tenO) {
        JComponent oBiLoi = layONhapTheoTen(tenO);
        if (oBiLoi == null) {
            return;
        }
        boToDoTatCa();
        oBiLoi.setBorder(BorderFactory.createLineBorder(DO, 2));

        if (oBiLoi == scrTienSuBenh) {
            txaTienSuBenh.requestFocusInWindow();
        } else if (oBiLoi == scrDiUng) {
            txaDiUng.requestFocusInWindow();
        } else {
            oBiLoi.requestFocusInWindow();
        }
    }

    public void boToDoTatCa() {
        for (JComponent oNhap : vienBanDau.keySet()) {
            oNhap.setBorder(vienBanDau.get(oNhap));
        }
    }

    private JComponent layONhapTheoTen(String tenO) {
        if (LoiNhapLieuException.O_HO_TEN.equals(tenO)) {
            return txtHoTen;
        }
        if (LoiNhapLieuException.O_NGAY_SINH.equals(tenO)) {
            return txtNgaySinh;
        }
        if (LoiNhapLieuException.O_GIOI_TINH.equals(tenO)) {
            return cmbGioiTinh;
        }
        if (LoiNhapLieuException.O_SO_DIEN_THOAI.equals(tenO)) {
            return txtSoDienThoai;
        }
        if (LoiNhapLieuException.O_DIA_CHI.equals(tenO)) {
            return txtDiaChi;
        }
        if (LoiNhapLieuException.O_NGUOI_LIEN_HE.equals(tenO)) {
            return txtNguoiLienHe;
        }
        if (LoiNhapLieuException.O_TIEN_SU_BENH.equals(tenO)) {
            return scrTienSuBenh;
        }
        if (LoiNhapLieuException.O_DI_UNG.equals(tenO)) {
            return scrDiUng;
        }
        return null;
    }

    public void datDangLuu(boolean dangLuu) {
        boolean choPhepThaoTac = !dangLuu;
        btnLuu.setEnabled(choPhepThaoTac);
        btnHuy.setEnabled(choPhepThaoTac);
        btnChonNgay.setEnabled(choPhepThaoTac);
        txtHoTen.setEnabled(choPhepThaoTac);
        txtSoDienThoai.setEnabled(choPhepThaoTac);
        txtDiaChi.setEnabled(choPhepThaoTac);
        txtNguoiLienHe.setEnabled(choPhepThaoTac);
        txaTienSuBenh.setEnabled(choPhepThaoTac);
        txaDiUng.setEnabled(choPhepThaoTac);
        cmbGioiTinh.setEnabled(choPhepThaoTac);

        if (dangLuu) {
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    public void hienLoi(String thongBao) {
        JOptionPane.showMessageDialog(this, thongBao, "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
    }

    public JButton getBtnChonNgay() {
        return btnChonNgay;
    }

    public JButton getBtnLuu() {
        return btnLuu;
    }

    public JButton getBtnHuy() {
        return btnHuy;
    }
}
