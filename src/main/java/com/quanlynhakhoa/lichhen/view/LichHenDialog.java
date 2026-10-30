package com.quanlynhakhoa.lichhen.view;

import com.quanlynhakhoa.benhnhan.hopdong.BenhNhanTomTat;
import com.quanlynhakhoa.benhnhan.view.BieuTuong;
import com.quanlynhakhoa.benhnhan.view.ChonNgayDialog;
import com.quanlynhakhoa.benhnhan.view.MauNut;
import com.quanlynhakhoa.lichhen.model.BacSi;
import com.quanlynhakhoa.lichhen.model.LichHen;
import com.quanlynhakhoa.lichhen.service.LichHenService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

// Hộp thoại ĐẶT LỊCH / ĐỔI LỊCH (UC004). Chỉ tạo giao diện và đọc dữ liệu trên form.
// Các nút do LichHenController xử lý.
//   lichHenCanDoi = null  → form ĐẶT LỊCH (tìm và chọn bệnh nhân)
//   lichHenCanDoi != null → form ĐỔI LỊCH (bệnh nhân giữ nguyên, chỉ đổi bác sĩ / ngày / giờ)
//
//  ┌──────── pnlNorth ────────┐
//  │ tiêu đề                  │
//  ├──────── pnlCenter ───────┤
//  │ 1. Bệnh nhân             │
//  │ 2. Bác sĩ, ngày, giờ     │
//  │ 3. Lý do khám            │
//  ├──────── pnlSouth ────────┤
//  │                Hủy  Lưu  │
//  └──────────────────────────┘
public class LichHenDialog extends JDialog {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color XAM = new Color(97, 115, 137);
    private static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private LichHen lichHenCanDoi;     // null = đặt mới

    private JPanel pnlNorth;
    private JLabel lblTieuDe;

    private JPanel pnlCenter;
    private JTextField txtTimBenhNhan;
    private JButton btnTimBenhNhan;
    private JComboBox<BenhNhanTomTat> cmbBenhNhan;
    private JComboBox<BacSi> cmbBacSi;
    private JTextField txtNgayHen;           // chỉ hiển thị ngày đã chọn, không cho gõ
    private JButton btnChonNgay;
    private JComboBox<LocalTime> cmbGioHen;  // các giờ còn trống (LocalTime hiện dạng 08:30)
    private JButton btnXemGioTrong;
    private JLabel lblGhiChuGio;
    private JTextArea txaLyDo;
    private JScrollPane scrLyDo;

    private JPanel pnlSouth;
    private JButton btnLuu;
    private JButton btnHuy;

    public LichHenDialog(Window cuaSoCha, LichHen lichHenCanDoi) {
        super(cuaSoCha, ModalityType.APPLICATION_MODAL);   // phải đóng form mới bấm được màn hình chính
        this.lichHenCanDoi = lichHenCanDoi;

        String tieuDe;
        if (lichHenCanDoi == null) {
            tieuDe = "Đặt lịch hẹn";
        } else {
            tieuDe = "Đổi lịch hẹn " + lichHenCanDoi.getMaLichHen();
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

        if (lichHenCanDoi != null) {
            doDuLieuLichHenCu();
        }

        setSize(600, 470);
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

        // Dòng tìm bệnh nhân: ô từ khóa + nút Tìm
        txtTimBenhNhan = new JTextField();
        btnTimBenhNhan = new JButton("Tìm");
        btnTimBenhNhan.setIcon(new BieuTuong(BieuTuong.TIM_KIEM));
        Box boxTimBenhNhan = Box.createHorizontalBox();
        boxTimBenhNhan.add(txtTimBenhNhan);
        boxTimBenhNhan.add(Box.createHorizontalStrut(6));
        boxTimBenhNhan.add(btnTimBenhNhan);

        cmbBenhNhan = new JComboBox<>();
        cmbBacSi = new JComboBox<>();

        // Dòng ngày hẹn: ô chỉ xem + nút "Chọn" mở lịch (dùng lại ChonNgayDialog)
        txtNgayHen = new JTextField();
        txtNgayHen.setEditable(false);
        btnChonNgay = new JButton("Chọn");
        Box boxNgayHen = Box.createHorizontalBox();
        boxNgayHen.add(txtNgayHen);
        boxNgayHen.add(Box.createHorizontalStrut(6));
        boxNgayHen.add(btnChonNgay);

        // Dòng giờ hẹn: ô chọn giờ trống + nút "Xem giờ trống"
        cmbGioHen = new JComboBox<>();
        btnXemGioTrong = new JButton("Xem giờ trống");
        Box boxGioHen = Box.createHorizontalBox();
        boxGioHen.add(cmbGioHen);
        boxGioHen.add(Box.createHorizontalStrut(6));
        boxGioHen.add(btnXemGioTrong);

        lblGhiChuGio = new JLabel("Chọn bác sĩ và ngày, rồi bấm \"Xem giờ trống\". Mỗi lịch hẹn dài "
                + LichHenService.THOI_LUONG_PHUT + " phút.");
        lblGhiChuGio.setForeground(XAM);

        txaLyDo = new JTextArea(3, 20);
        txaLyDo.setLineWrap(true);
        txaLyDo.setWrapStyleWord(true);
        scrLyDo = new JScrollPane(txaLyDo);

        JLabel lblNhanTimBenhNhan = new JLabel("Tìm bệnh nhân:");
        JLabel lblNhanBenhNhan = new JLabel("Bệnh nhân (*):");
        JLabel lblNhanBacSi = new JLabel("Bác sĩ (*):");
        JLabel lblNhanNgayHen = new JLabel("Ngày hẹn (*):");
        JLabel lblNhanGioHen = new JLabel("Giờ hẹn (*):");
        JLabel lblNhanLyDo = new JLabel("Lý do khám:");

        // Cho các nhãn rộng bằng nhau
        Dimension doRongNhan = lblNhanTimBenhNhan.getPreferredSize();
        lblNhanBenhNhan.setPreferredSize(doRongNhan);
        lblNhanBacSi.setPreferredSize(doRongNhan);
        lblNhanNgayHen.setPreferredSize(doRongNhan);
        lblNhanGioHen.setPreferredSize(doRongNhan);
        lblNhanLyDo.setPreferredSize(doRongNhan);
        lblNhanLyDo.setAlignmentY(TOP_ALIGNMENT);
        scrLyDo.setAlignmentY(TOP_ALIGNMENT);

        Box boxForm = Box.createVerticalBox();
        themDong(boxForm, lblNhanTimBenhNhan, boxTimBenhNhan);
        themDong(boxForm, lblNhanBenhNhan, cmbBenhNhan);
        themDong(boxForm, lblNhanBacSi, cmbBacSi);
        themDong(boxForm, lblNhanNgayHen, boxNgayHen);
        themDong(boxForm, lblNhanGioHen, boxGioHen);

        Box boxGhiChu = Box.createHorizontalBox();
        boxGhiChu.add(lblGhiChuGio);
        boxGhiChu.add(Box.createHorizontalGlue());
        boxForm.add(boxGhiChu);
        boxForm.add(Box.createVerticalStrut(10));

        themDong(boxForm, lblNhanLyDo, scrLyDo);

        pnlCenter.add(boxForm, BorderLayout.NORTH);
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
        MauNut.toXanhDuong(btnLuu);   // nút lưu màu xanh dương
        btnHuy = new JButton("Hủy");
        MauNut.toDo(btnHuy);   // nút hủy màu đỏ
        pnlSouth.add(btnHuy);
        pnlSouth.add(btnLuu);
    }

    // Form ĐỔI LỊCH: bệnh nhân cố định (không tìm được), điền sẵn ngày và lý do cũ
    private void doDuLieuLichHenCu() {
        BenhNhanTomTat benhNhanCu = new BenhNhanTomTat(lichHenCanDoi.getMaBenhNhan(),
                lichHenCanDoi.getTenBenhNhan(), null, null, lichHenCanDoi.getSoDienThoaiBenhNhan());
        cmbBenhNhan.addItem(benhNhanCu);
        cmbBenhNhan.setEnabled(false);
        txtTimBenhNhan.setEnabled(false);
        btnTimBenhNhan.setEnabled(false);

        // Ngày cũ đã qua thì mặc định hôm nay
        LocalDate ngayCu = lichHenCanDoi.getThoiGianHen().toLocalDate();
        if (ngayCu.isBefore(LocalDate.now())) {
            ngayCu = LocalDate.now();
        }
        txtNgayHen.setText(ngayCu.format(DINH_DANG_NGAY));

        if (lichHenCanDoi.getLyDoHen() != null) {
            txaLyDo.setText(lichHenCanDoi.getLyDoHen());
        }
    }

    // ===================== Hàm hiển thị (Controller gọi) =====================

    // Kết quả tìm bệnh nhân → đổ vào ô chọn bệnh nhân
    public void hienDanhSachBenhNhan(ArrayList<BenhNhanTomTat> danhSachBenhNhan) {
        cmbBenhNhan.removeAllItems();
        for (BenhNhanTomTat benhNhan : danhSachBenhNhan) {
            cmbBenhNhan.addItem(benhNhan);
        }
        if (danhSachBenhNhan.isEmpty()) {
            hienLoi("Không tìm thấy bệnh nhân. Nếu là bệnh nhân mới, hãy tạo hồ sơ ở màn hình "
                    + "Bệnh nhân → Danh sách bệnh nhân rồi đặt lịch lại.");
        }
    }

    // Đổ danh sách bác sĩ vào ô chọn; maBacSiChonSan khác null thì chọn sẵn bác sĩ đó
    public void hienDanhSachBacSi(ArrayList<BacSi> danhSachBacSi, String maBacSiChonSan) {
        cmbBacSi.removeAllItems();
        for (BacSi bacSi : danhSachBacSi) {
            cmbBacSi.addItem(bacSi);
            if (bacSi.getMaNhanVien().equals(maBacSiChonSan)) {
                cmbBacSi.setSelectedItem(bacSi);
            }
        }
    }

    // Đổ các giờ còn trống vào ô chọn giờ
    public void hienGioTrong(ArrayList<LocalTime> danhSachGioTrong) {
        cmbGioHen.removeAllItems();
        for (LocalTime gioTrong : danhSachGioTrong) {
            cmbGioHen.addItem(gioTrong);
        }
        if (danhSachGioTrong.isEmpty()) {
            lblGhiChuGio.setText("Bác sĩ không có ca làm việc hoặc đã kín lịch ngày này. Chọn bác sĩ / ngày khác.");
        } else {
            lblGhiChuGio.setText("Còn " + danhSachGioTrong.size() + " giờ trống. Mỗi lịch hẹn dài "
                    + LichHenService.THOI_LUONG_PHUT + " phút.");
        }
    }

    // Đổi bác sĩ hoặc ngày thì danh sách giờ cũ không còn đúng → xóa đi, bắt bấm "Xem giờ trống" lại
    public void xoaGioTrong() {
        cmbGioHen.removeAllItems();
        lblGhiChuGio.setText("Chọn bác sĩ và ngày, rồi bấm \"Xem giờ trống\". Mỗi lịch hẹn dài "
                + LichHenService.THOI_LUONG_PHUT + " phút.");
    }

    // Bấm "Chọn" → mở lịch (từ hôm nay tới 1 năm sau) → điền ngày đã chọn
    public void moLichChonNgayHen() {
        LocalDate homNay = LocalDate.now();
        LocalDate ngayDaChon = ChonNgayDialog.chon(this, "Chọn ngày hẹn", layNgayHen(),
                homNay, homNay.plusYears(LichHenService.SO_NAM_DAT_TRUOC_TOI_DA));
        if (ngayDaChon != null) {          // null = bấm Hủy → giữ nguyên
            txtNgayHen.setText(ngayDaChon.format(DINH_DANG_NGAY));
            xoaGioTrong();
        }
    }

    // ===================== Đọc dữ liệu trên form =====================

    public BenhNhanTomTat layBenhNhanDaChon() {
        return (BenhNhanTomTat) cmbBenhNhan.getSelectedItem();
    }

    public BacSi layBacSiDaChon() {
        return (BacSi) cmbBacSi.getSelectedItem();
    }

    // Mã bác sĩ đang chọn, chưa chọn thì null
    public String layMaBacSiDaChon() {
        BacSi bacSi = layBacSiDaChon();
        if (bacSi == null) {
            return null;
        }
        return bacSi.getMaNhanVien();
    }

    // Ngày hẹn đang chọn, chưa chọn thì null
    public LocalDate layNgayHen() {
        if (txtNgayHen.getText().isEmpty()) {
            return null;
        }
        return LocalDate.parse(txtNgayHen.getText(), DINH_DANG_NGAY);
    }

    // Ngày + giờ hẹn, chưa chọn đủ thì null
    public LocalDateTime layThoiGianHen() {
        LocalDate ngayHen = layNgayHen();
        LocalTime gioHen = (LocalTime) cmbGioHen.getSelectedItem();
        if (ngayHen == null || gioHen == null) {
            return null;
        }
        return ngayHen.atTime(gioHen);
    }

    public String layLyDo() {
        return txaLyDo.getText();
    }

    public LichHen getLichHenCanDoi() {
        return lichHenCanDoi;
    }

    // ===================== Trạng thái đang xử lý =====================

    // Đang đọc / ghi database: khóa các nút để không bấm 2 lần
    public void datDangXuLy(boolean dangXuLy) {
        boolean choPhepThaoTac = !dangXuLy;
        btnLuu.setEnabled(choPhepThaoTac);
        btnHuy.setEnabled(choPhepThaoTac);
        btnXemGioTrong.setEnabled(choPhepThaoTac);
        btnChonNgay.setEnabled(choPhepThaoTac);
        cmbBacSi.setEnabled(choPhepThaoTac);
        if (lichHenCanDoi == null) {
            // Form đổi lịch luôn khóa phần bệnh nhân
            btnTimBenhNhan.setEnabled(choPhepThaoTac);
        }

        if (dangXuLy) {
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    // Hiện lỗi thành hộp thông báo giữa màn hình (form vẫn mở để sửa lại)
    public void hienLoi(String thongBao) {
        JOptionPane.showMessageDialog(this, thongBao, "Thông báo", JOptionPane.WARNING_MESSAGE);
    }

    // ===================== Getter cho Controller =====================

    public JTextField getTxtTimBenhNhan() {
        return txtTimBenhNhan;
    }

    public JButton getBtnTimBenhNhan() {
        return btnTimBenhNhan;
    }

    public JComboBox<BacSi> getCmbBacSi() {
        return cmbBacSi;
    }

    public JButton getBtnChonNgay() {
        return btnChonNgay;
    }

    public JButton getBtnXemGioTrong() {
        return btnXemGioTrong;
    }

    public JButton getBtnLuu() {
        return btnLuu;
    }

    public JButton getBtnHuy() {
        return btnHuy;
    }
}
