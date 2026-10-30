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
    private static final Color DO = new Color(200, 40, 40);    // viền ô nhập bị lỗi
    private static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
    private JTextArea txaTienSuBenh;       // tiền sử bệnh (nhiều dòng, không bắt buộc)
    private JScrollPane scrTienSuBenh;
    private JTextArea txaDiUng;            // dị ứng (nhiều dòng, không bắt buộc)
    private JScrollPane scrDiUng;

    private JPanel pnlSouth;
    private JButton btnLuu;
    private JButton btnHuy;

    // Viền ban đầu của từng ô nhập, để bỏ viền đỏ thì trả lại đúng viền cũ
    private HashMap<JComponent, Border> vienBanDau = new HashMap<>();

    // benhNhanCanSua = null → form THÊM; khác null → form SỬA (đổ sẵn dữ liệu)
    public BenhNhanDialog(Window cuaSoCha, BenhNhan benhNhanCanSua) {
        super(cuaSoCha, ModalityType.APPLICATION_MODAL); // mở form thì phải đóng mới bấm được màn hình chính
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
        txaTienSuBenh = taoOChuNhieuDong();
        scrTienSuBenh = new JScrollPane(txaTienSuBenh);
        txaDiUng = taoOChuNhieuDong();
        scrDiUng = new JScrollPane(txaDiUng);

        // Ghi nhớ viền ban đầu của các ô có thể bị tô đỏ
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

        // Cho các nhãn rộng bằng nhau (giống code cũ)
        Dimension doRongNhan = lblNhanSoDienThoai.getPreferredSize();
        lblNhanMa.setPreferredSize(doRongNhan);
        lblNhanHoTen.setPreferredSize(doRongNhan);
        lblNhanNgaySinh.setPreferredSize(doRongNhan);
        lblNhanGioiTinh.setPreferredSize(doRongNhan);
        lblNhanDiaChi.setPreferredSize(doRongNhan);
        lblNhanNguoiLienHe.setPreferredSize(doRongNhan);
        lblNhanTienSu.setPreferredSize(doRongNhan);
        lblNhanDiUng.setPreferredSize(doRongNhan);
        lblNhanTienSu.setAlignmentY(TOP_ALIGNMENT);   // nhãn nằm sát trên ô nhiều dòng
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

        JLabel lblGhiChu = new JLabel("(*) bắt buộc · SĐT gồm 10 số, bắt đầu bằng 0 · tiền sử tối đa "
                + BenhNhanService.TIEN_SU_TOI_DA + ", dị ứng tối đa " + BenhNhanService.DI_UNG_TOI_DA + " ký tự");
        lblGhiChu.setForeground(XAM);
        Box boxGhiChu = Box.createHorizontalBox();
        boxGhiChu.add(lblGhiChu);
        boxGhiChu.add(Box.createHorizontalGlue());
        boxForm.add(boxGhiChu);

        pnlCenter.add(boxForm, BorderLayout.NORTH);
    }

    // Ô nhập nhiều dòng, tự xuống dòng theo từ
    private JTextArea taoOChuNhieuDong() {
        JTextArea txaNhieuDong = new JTextArea(3, 20);
        txaNhieuDong.setLineWrap(true);
        txaNhieuDong.setWrapStyleWord(true);
        return txaNhieuDong;
    }

    // Thêm 1 dòng "nhãn : ô nhập" vào form
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
        btnHuy = new JButton("Hủy");
        pnlSouth.add(btnHuy);
        pnlSouth.add(btnLuu);
    }

    // Form SỬA: đổ dữ liệu bệnh nhân lên các ô
    private void doDuLieuLenForm(BenhNhan benhNhan) {
        txtMaBenhNhan.setText(benhNhan.getMaBenhNhan());
        txtHoTen.setText(benhNhan.getHoTen());
        txtNgaySinh.setText(benhNhan.getNgaySinh().format(DINH_DANG_NGAY));

        // Dữ liệu cũ có giới tính "Khác": thêm lựa chọn "Khác" riêng cho bệnh nhân này,
        // nếu không ô chọn sẽ tự hiện "Nam" và bấm Lưu là giới tính bị đổi mà không ai biết.
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

    // Bấm "Chọn..." → mở lịch → điền ngày đã chọn vào ô ngày sinh
    public void moLichChonNgaySinh() {
        LocalDate ngayDangCo = null;
        if (!txtNgaySinh.getText().isEmpty()) {
            ngayDangCo = LocalDate.parse(txtNgaySinh.getText(), DINH_DANG_NGAY);
        }
        LocalDate ngayDaChon = ChonNgayDialog.chon(this, ngayDangCo);
        if (ngayDaChon != null) {           // null = người dùng bấm Hủy → giữ nguyên
            txtNgaySinh.setText(ngayDaChon.format(DINH_DANG_NGAY));
        }
    }

    // Đọc dữ liệu trên form thành 1 đối tượng BenhNhan.
    // Việc kiểm tra quy tắc (bắt buộc, regex...) do Service làm.
    public BenhNhan layBenhNhanTuForm(String maBenhNhan) {
        // Ngày sinh dạng dd/MM/yyyy → Service kiểm tra bằng regex rồi đổi sang LocalDate
        LocalDate ngaySinh = BenhNhanService.docNgaySinh(txtNgaySinh.getText());

        // Đổi chữ trong ô chọn sang mã lưu trong database
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

    // ===================== Tô đỏ ô lỗi (UC003 E1) =====================

    // Tô đỏ ô có tên tenO (tên ô lấy từ LoiNhapLieuException) và đưa con trỏ vào ô đó
    public void toDoO(String tenO) {
        JComponent oBiLoi = layONhapTheoTen(tenO);
        if (oBiLoi == null) {
            return;
        }
        boToDoTatCa();
        oBiLoi.setBorder(BorderFactory.createLineBorder(DO, 2));

        // Ô nhiều dòng nằm trong JScrollPane → đưa con trỏ vào ô chữ bên trong
        if (oBiLoi == scrTienSuBenh) {
            txaTienSuBenh.requestFocusInWindow();
        } else if (oBiLoi == scrDiUng) {
            txaDiUng.requestFocusInWindow();
        } else {
            oBiLoi.requestFocusInWindow();
        }
    }

    // Trả lại viền ban đầu cho tất cả các ô
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

    // ===================== Trạng thái đang lưu =====================

    // Đang lưu: khóa các ô và nút để không bấm Lưu 2 lần hay đóng form giữa chừng
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
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);           // không cho bấm X
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    // Hiện lỗi thành hộp thông báo giữa màn hình (form vẫn mở để sửa lại)
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
