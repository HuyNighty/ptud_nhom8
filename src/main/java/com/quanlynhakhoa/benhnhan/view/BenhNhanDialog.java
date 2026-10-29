package com.quanlynhakhoa.benhnhan.view;

import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.benhnhan.service.BenhNhanService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

// Hộp thoại Thêm / Sửa bệnh nhân. Chỉ tạo giao diện và đọc dữ liệu trên form.
// Các nút (Chọn ngày, Lưu, Hủy) do BenhNhanController xử lý.
//
//  ┌──────── pnlNorth ────────┐
//  │ tiêu đề                  │
//  ├──────── pnlCenter ───────┤
//  │ các ô nhập               │
//  ├──────── pnlSouth ────────┤
//  │                Hủy  Lưu  │
//  └──────────────────────────┘
public class BenhNhanDialog extends JDialog {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color XAM = new Color(97, 115, 137);
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private JPanel pnlNorth;
    private JLabel lblTieuDe;

    private JPanel pnlCenter;
    private JTextField txtMaBenhNhan;
    private JTextField txtHoTen;
    private JTextField txtNgaySinh;        // chỉ hiển thị ngày đã chọn, không cho gõ
    private JButton btnChonNgay;           // mở lịch để chọn ngày sinh
    private JComboBox<String> cmbGioiTinh;
    private JTextField txtSoDienThoai;
    private JTextField txtDiaChi;
    private JTextField txtNguoiLienHe;

    private JPanel pnlSouth;
    private JButton btnLuu;
    private JButton btnHuy;

    // bnCanSua = null → form THÊM; khác null → form SỬA (đổ sẵn dữ liệu)
    public BenhNhanDialog(Window chuSoHuu, BenhNhan bnCanSua) {
        super(chuSoHuu, ModalityType.APPLICATION_MODAL); // mở form thì phải đóng mới bấm được màn hình chính
        setTitle(bnCanSua == null ? "Thêm bệnh nhân" : "Sửa thông tin bệnh nhân");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel pnlNoiDung = new JPanel(new BorderLayout(0, 12));
        pnlNoiDung.setBorder(new EmptyBorder(18, 20, 16, 20));
        setContentPane(pnlNoiDung);

        taoNorth(bnCanSua == null ? "Thêm bệnh nhân" : "Sửa thông tin bệnh nhân");
        taoCenter();
        taoSouth();
        pnlNoiDung.add(pnlNorth, BorderLayout.NORTH);
        pnlNoiDung.add(pnlCenter, BorderLayout.CENTER);
        pnlNoiDung.add(pnlSouth, BorderLayout.SOUTH);

        if (bnCanSua != null) {
            doDuLieuLenForm(bnCanSua);
        }

        setSize(560, 400);
        setLocationRelativeTo(chuSoHuu);
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

        txtMaBenhNhan = new JTextField("(tự sinh khi lưu)");
        txtMaBenhNhan.setEditable(false);   // mã do hệ thống sinh, không cho nhập
        txtHoTen = new JTextField();

        // Ngày sinh: ô chỉ xem + nút "Chọn..." mở lịch
        txtNgaySinh = new JTextField();
        txtNgaySinh.setEditable(false);
        btnChonNgay = new JButton("Chọn...");
        Box boxNgaySinh = Box.createHorizontalBox();
        boxNgaySinh.add(txtNgaySinh);
        boxNgaySinh.add(Box.createHorizontalStrut(6));
        boxNgaySinh.add(btnChonNgay);

        cmbGioiTinh = new JComboBox<>(new String[]{"Nam", "Nữ"});
        txtSoDienThoai = new JTextField();
        txtDiaChi = new JTextField();
        txtNguoiLienHe = new JTextField();

        JLabel lblNhanMa = new JLabel("Mã bệnh nhân:");
        JLabel lblNhanHoTen = new JLabel("Họ tên (*):");
        JLabel lblNhanNgaySinh = new JLabel("Ngày sinh (*):");
        JLabel lblNhanGioiTinh = new JLabel("Giới tính (*):");
        JLabel lblNhanSoDienThoai = new JLabel("Số điện thoại (*):");
        JLabel lblNhanDiaChi = new JLabel("Địa chỉ:");
        JLabel lblNhanNguoiLienHe = new JLabel("Người liên hệ:");

        // Cho các nhãn rộng bằng nhau (giống code cũ)
        Dimension rongNhan = lblNhanSoDienThoai.getPreferredSize();
        lblNhanMa.setPreferredSize(rongNhan);
        lblNhanHoTen.setPreferredSize(rongNhan);
        lblNhanNgaySinh.setPreferredSize(rongNhan);
        lblNhanGioiTinh.setPreferredSize(rongNhan);
        lblNhanDiaChi.setPreferredSize(rongNhan);
        lblNhanNguoiLienHe.setPreferredSize(rongNhan);

        Box boxForm = Box.createVerticalBox();
        themDong(boxForm, lblNhanMa, txtMaBenhNhan);
        themDong(boxForm, lblNhanHoTen, txtHoTen);
        themDong(boxForm, lblNhanNgaySinh, boxNgaySinh);
        themDong(boxForm, lblNhanGioiTinh, cmbGioiTinh);
        themDong(boxForm, lblNhanSoDienThoai, txtSoDienThoai);
        themDong(boxForm, lblNhanDiaChi, txtDiaChi);
        themDong(boxForm, lblNhanNguoiLienHe, txtNguoiLienHe);

        JLabel lblGhiChu = new JLabel("(*) bắt buộc · SĐT gồm 10 số, bắt đầu bằng 0");
        lblGhiChu.setForeground(XAM);
        Box boxGhiChu = Box.createHorizontalBox();
        boxGhiChu.add(lblGhiChu);
        boxGhiChu.add(Box.createHorizontalGlue());
        boxForm.add(boxGhiChu);

        pnlCenter.add(boxForm, BorderLayout.NORTH);
    }

    // Thêm 1 dòng "nhãn : ô nhập" vào form
    private void themDong(Box boxForm, JLabel lblNhan, java.awt.Component oNhap) {
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
        btnHuy = new JButton("Hủy");
        pnlSouth.add(btnHuy);
        pnlSouth.add(btnLuu);
    }

    // Form SỬA: đổ dữ liệu bệnh nhân lên các ô
    private void doDuLieuLenForm(BenhNhan bn) {
        txtMaBenhNhan.setText(bn.getMaBenhNhan());
        txtHoTen.setText(bn.getHoTen());
        txtNgaySinh.setText(bn.getNgaySinh().format(NGAY));
        cmbGioiTinh.setSelectedItem(bn.getGioiTinhHienThi());
        txtSoDienThoai.setText(bn.getSoDienThoai());
        txtDiaChi.setText(bn.getDiaChi() == null ? "" : bn.getDiaChi());
        txtNguoiLienHe.setText(bn.getNguoiLienHe() == null ? "" : bn.getNguoiLienHe());
    }

    // Bấm "Chọn..." → mở lịch → điền ngày đã chọn vào ô ngày sinh
    public void moLichChonNgaySinh() {
        LocalDate ngayDangCo = null;
        if (!txtNgaySinh.getText().isEmpty()) {
            ngayDangCo = LocalDate.parse(txtNgaySinh.getText(), NGAY);
        }
        LocalDate ngayChon = ChonNgayDialog.chon(this, ngayDangCo);
        if (ngayChon != null) {           // null = người dùng bấm Hủy → giữ nguyên
            txtNgaySinh.setText(ngayChon.format(NGAY));
        }
    }

    // Đọc dữ liệu trên form thành 1 đối tượng BenhNhan.
    // Việc kiểm tra quy tắc (bắt buộc, regex...) do Service làm.
    public BenhNhan layBenhNhanTuForm(String maBenhNhan) {
        // Ngày sinh dạng dd/MM/yyyy → Service kiểm tra bằng regex rồi đổi sang LocalDate
        LocalDate ngaySinh = BenhNhanService.docNgaySinh(txtNgaySinh.getText());

        // Chỉ có 2 lựa chọn: Nam → NAM, Nữ → NU
        String gioiTinh = "NAM";
        if ("Nữ".equals(cmbGioiTinh.getSelectedItem())) gioiTinh = "NU";

        BenhNhan bn = new BenhNhan(maBenhNhan);
        bn.setHoTen(txtHoTen.getText());
        bn.setNgaySinh(ngaySinh);
        bn.setGioiTinh(gioiTinh);
        bn.setSoDienThoai(txtSoDienThoai.getText());
        bn.setDiaChi(txtDiaChi.getText());
        bn.setNguoiLienHe(txtNguoiLienHe.getText());
        return bn;
    }

    // Hiện lỗi thành hộp thông báo giữa màn hình (form vẫn mở để sửa lại)
    public void hienLoi(String thongBao) {
        JOptionPane.showMessageDialog(this, thongBao, "Dữ liệu chưa hợp lệ", JOptionPane.WARNING_MESSAGE);
    }

    public JButton getBtnChonNgay() { return btnChonNgay; }
    public JButton getBtnLuu() { return btnLuu; }
    public JButton getBtnHuy() { return btnHuy; }
}
