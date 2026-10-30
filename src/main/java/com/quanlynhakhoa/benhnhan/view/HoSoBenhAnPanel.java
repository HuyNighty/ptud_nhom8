package com.quanlynhakhoa.benhnhan.view;

import com.quanlynhakhoa.benhnhan.controller.HoSoBenhAnController;
import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.benhnhan.model.HoSoBenhAn;
import com.quanlynhakhoa.benhnhan.model.LuotKham;
import com.quanlynhakhoa.benhnhan.service.BenhNhanService;
import com.quanlynhakhoa.benhnhan.service.HoSoBenhAnService;
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

// Màn hình HỒ SƠ BỆNH ÁN (menu Bệnh nhân → Hồ sơ bệnh án), theo UC009 trong báo cáo docs/tuan_cuoi:
//   1. Tìm bệnh nhân  →  2. chọn bệnh nhân: ghi nhật ký + hiện các lần khám theo thời gian
//   3. chọn 1 lần khám  →  4. hiện chi tiết lần khám đó.   CHỈ XEM.
// View chỉ tạo giao diện và hiển thị. Sự kiện do HoSoBenhAnController xử lý.
//
//  ┌──────────────────── pnlNorth ────────────────────┐
//  │ tiêu đề + ô tìm kiếm + nút                       │
//  ├─ pnlWest ─┬──────── pnlCenter ───────┬─ pnlEast ─┤
//  │ bệnh nhân │ thông tin hồ sơ          │ chi tiết  │
//  │ tìm được  │ bảng các lần khám        │ lần khám  │
//  ├───────────┴──────── pnlSouth ────────┴───────────┤
//  │ dòng trạng thái                                  │
//  └──────────────────────────────────────────────────┘
public class HoSoBenhAnPanel extends JPanel {
    // Màu giống BenhNhanPanel / AccountPanel
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color XAM = new Color(97, 115, 137);
    private static final Color VIEN = new Color(220, 231, 242);
    private static final Color NEN = new Color(245, 248, 252);
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ----- NORTH -----
    private JPanel pnlNorth;
    private JLabel lblTieuDe;
    private JTextField txtTimKiem;
    private JButton btnTimKiem;
    private JButton btnLamMoi;

    // ----- WEST: danh sách bệnh nhân -----
    private JPanel pnlWest;
    private DefaultTableModel modelBenhNhan;
    private JTable tblBenhNhan;
    private JScrollPane scrBenhNhan;
    private ArrayList<BenhNhan> dsBenhNhan = new ArrayList<>();

    // ----- CENTER: thông tin hồ sơ + các lần khám -----
    private JPanel pnlCenter;
    private JLabel lblTenBenhNhan;
    private JLabel lblThongTinHoSo;
    private JTextArea txaTienSuBenh;
    private JTextArea txaDiUng;
    private DefaultTableModel modelLuotKham;
    private JTable tblLuotKham;
    private JScrollPane scrLuotKham;
    private ArrayList<LuotKham> dsLuotKham = new ArrayList<>();

    // ----- EAST: chi tiết lần khám -----
    private JPanel pnlEast;
    private JLabel lblMaPhieu;
    private JLabel lblNgayKham;
    private JLabel lblBacSi;
    private JLabel lblTrangThaiKham;
    private JTextArea txaTrieuChung;
    private JTextArea txaChanDoan;
    private JTextArea txaKetLuan;

    // ----- SOUTH -----
    private JPanel pnlSouth;
    private JLabel lblTrangThai;

    // Hàm khởi tạo: Core gắn vào menu bằng .register(Module.MEDICAL_RECORDS, HoSoBenhAnPanel::new)
    public HoSoBenhAnPanel(DesktopSession phien) {
        setLayout(new BorderLayout(14, 14));
        setBackground(NEN);
        setBorder(new EmptyBorder(24, 26, 24, 26));

        taoNorth();
        taoWest();
        taoCenter();
        taoEast();
        taoSouth();

        add(pnlNorth, BorderLayout.NORTH);
        add(pnlWest, BorderLayout.WEST);
        add(pnlCenter, BorderLayout.CENTER);
        add(pnlEast, BorderLayout.EAST);
        add(pnlSouth, BorderLayout.SOUTH);

        hienHoSo(null, null);
        hienChiTietLuotKham(null);

        // Tạo Service và Controller, Controller sẽ gắn sự kiện cho các nút
        new HoSoBenhAnController(this, new BenhNhanService(phien), new HoSoBenhAnService(phien));
    }

    // ===================== Tạo các vùng =====================

    private void taoNorth() {
        pnlNorth = new JPanel(new BorderLayout(0, 10));
        pnlNorth.setOpaque(false);

        lblTieuDe = new JLabel("Hồ sơ bệnh án");
        lblTieuDe.setFont(new Font("SansSerif", Font.BOLD, 23));
        lblTieuDe.setForeground(NAVY);
        JLabel lblMoTa = new JLabel("Tìm bệnh nhân → chọn bệnh nhân → chọn một lần khám để xem chi tiết. "
                + "Mỗi lần mở hồ sơ đều được ghi nhật ký.");
        lblMoTa.setForeground(XAM);

        txtTimKiem = new JTextField(24);
        btnTimKiem = new JButton("Tìm kiếm");
        btnTimKiem.setIcon(new BieuTuong(BieuTuong.TIM_KIEM));
        btnLamMoi = new JButton("Làm mới");
        btnLamMoi.setIcon(new BieuTuong(BieuTuong.LAM_MOI));

        JPanel pnlNorthSouth = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
        pnlNorthSouth.setBackground(Color.WHITE);
        pnlNorthSouth.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlNorthSouth.add(new JLabel("Tìm bệnh nhân (SĐT, mã BN, họ tên):"));
        pnlNorthSouth.add(txtTimKiem);
        pnlNorthSouth.add(btnTimKiem);
        pnlNorthSouth.add(btnLamMoi);

        JPanel pnlNorthNorth = new JPanel(new BorderLayout(0, 4));
        pnlNorthNorth.setOpaque(false);
        pnlNorthNorth.add(lblTieuDe, BorderLayout.NORTH);
        pnlNorthNorth.add(lblMoTa, BorderLayout.SOUTH);

        pnlNorth.add(pnlNorthNorth, BorderLayout.NORTH);
        pnlNorth.add(pnlNorthSouth, BorderLayout.SOUTH);
    }

    private void taoWest() {
        pnlWest = new JPanel(new BorderLayout(0, 6));
        pnlWest.setOpaque(false);
        pnlWest.setPreferredSize(new Dimension(300, 0));

        modelBenhNhan = new DefaultTableModel("Mã BN;Họ tên;SĐT".split(";"), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblBenhNhan = taoBang(modelBenhNhan);
        tblBenhNhan.getColumnModel().getColumn(0).setPreferredWidth(70);
        tblBenhNhan.getColumnModel().getColumn(1).setPreferredWidth(130);
        scrBenhNhan = new JScrollPane(tblBenhNhan);
        scrBenhNhan.setBorder(BorderFactory.createLineBorder(VIEN));

        pnlWest.add(taoTieuDeKhung("1. Chọn bệnh nhân"), BorderLayout.NORTH);
        pnlWest.add(scrBenhNhan, BorderLayout.CENTER);
    }

    private void taoCenter() {
        pnlCenter = new JPanel(new BorderLayout(0, 6));
        pnlCenter.setOpaque(false);

        // CENTER → NORTH: tên bệnh nhân, mã hồ sơ, tiền sử, dị ứng
        lblTenBenhNhan = new JLabel();
        lblTenBenhNhan.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblTenBenhNhan.setForeground(NAVY);
        lblThongTinHoSo = new JLabel();
        lblThongTinHoSo.setForeground(XAM);
        txaTienSuBenh = taoOChuChiXem();
        txaDiUng = taoOChuChiXem();

        Box boxHoSo = Box.createVerticalBox();
        boxHoSo.add(dongTrai(lblTenBenhNhan));
        boxHoSo.add(Box.createVerticalStrut(2));
        boxHoSo.add(dongTrai(lblThongTinHoSo));
        boxHoSo.add(Box.createVerticalStrut(8));
        themDongNhieuChu(boxHoSo, "Tiền sử bệnh:", txaTienSuBenh);
        themDongNhieuChu(boxHoSo, "Dị ứng:", txaDiUng);

        // CENTER → CENTER: bảng các lần khám
        modelLuotKham = new DefaultTableModel("Mã phiếu;Ngày khám;Bác sĩ;Chẩn đoán;Trạng thái".split(";"), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblLuotKham = taoBang(modelLuotKham);
        tblLuotKham.getColumnModel().getColumn(0).setPreferredWidth(80);
        tblLuotKham.getColumnModel().getColumn(1).setPreferredWidth(115);
        tblLuotKham.getColumnModel().getColumn(3).setPreferredWidth(170);
        scrLuotKham = new JScrollPane(tblLuotKham);
        scrLuotKham.setBorder(BorderFactory.createLineBorder(VIEN));

        JPanel pnlCenterCenter = new JPanel(new BorderLayout(0, 6));
        pnlCenterCenter.setOpaque(false);
        pnlCenterCenter.add(taoTieuDeKhung("2. Chọn một lần khám (mới nhất ở trên)"), BorderLayout.NORTH);
        pnlCenterCenter.add(scrLuotKham, BorderLayout.CENTER);

        // Box không tự tô nền → bọc trong JPanel nền trắng có viền
        JPanel pnlCenterNorth = new JPanel(new BorderLayout());
        pnlCenterNorth.setBackground(Color.WHITE);
        pnlCenterNorth.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VIEN), new EmptyBorder(10, 12, 4, 12)));
        pnlCenterNorth.add(boxHoSo, BorderLayout.CENTER);

        pnlCenter.add(pnlCenterNorth, BorderLayout.NORTH);
        pnlCenter.add(pnlCenterCenter, BorderLayout.CENTER);
    }

    private void taoEast() {
        pnlEast = new JPanel(new BorderLayout());
        pnlEast.setBackground(Color.WHITE);
        pnlEast.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlEast.setPreferredSize(new Dimension(320, 0));

        lblMaPhieu = taoGiaTri();
        lblNgayKham = taoGiaTri();
        lblBacSi = taoGiaTri();
        lblTrangThaiKham = taoGiaTri();
        txaTrieuChung = taoOChuChiXem();
        txaChanDoan = taoOChuChiXem();
        txaKetLuan = taoOChuChiXem();

        Box boxChiTiet = Box.createVerticalBox();
        boxChiTiet.setBorder(new EmptyBorder(14, 14, 14, 14));
        JLabel lblTieuDeChiTiet = new JLabel("3. Chi tiết lần khám");
        lblTieuDeChiTiet.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblTieuDeChiTiet.setForeground(NAVY);
        boxChiTiet.add(dongTrai(lblTieuDeChiTiet));
        boxChiTiet.add(Box.createVerticalStrut(12));
        themDong(boxChiTiet, "Mã phiếu:", lblMaPhieu);
        themDong(boxChiTiet, "Ngày khám:", lblNgayKham);
        themDong(boxChiTiet, "Bác sĩ:", lblBacSi);
        themDong(boxChiTiet, "Trạng thái:", lblTrangThaiKham);
        themDongNhieuChu(boxChiTiet, "Triệu chứng:", txaTrieuChung);
        themDongNhieuChu(boxChiTiet, "Chẩn đoán:", txaChanDoan);
        themDongNhieuChu(boxChiTiet, "Kết luận:", txaKetLuan);

        pnlEast.add(boxChiTiet, BorderLayout.NORTH);   // NORTH để các dòng nằm sát phía trên
    }

    private void taoSouth() {
        pnlSouth = new JPanel(new BorderLayout());
        pnlSouth.setOpaque(false);
        lblTrangThai = new JLabel("Chưa tải dữ liệu.");
        lblTrangThai.setForeground(XAM);
        pnlSouth.add(lblTrangThai, BorderLayout.CENTER);
    }

    // ===================== Hàm tiện ích tạo giao diện =====================

    private JTable taoBang(DefaultTableModel model) {
        JTable tbl = new JTable(model);
        tbl.setRowHeight(28);
        tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tbl.getTableHeader().setReorderingAllowed(false);
        tbl.setFillsViewportHeight(true);
        return tbl;
    }

    private JLabel taoTieuDeKhung(String chu) {
        JLabel lbl = new JLabel(chu);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(NAVY);
        return lbl;
    }

    private JLabel taoGiaTri() {
        JLabel lbl = new JLabel("—");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(NAVY);
        return lbl;
    }

    // Ô chữ nhiều dòng CHỈ ĐỂ XEM: tự xuống dòng, không viền, nền trong suốt giống JLabel
    private JTextArea taoOChuChiXem() {
        JTextArea txa = new JTextArea("—");
        txa.setEditable(false);
        txa.setLineWrap(true);
        txa.setWrapStyleWord(true);
        txa.setOpaque(false);
        txa.setBorder(null);
        txa.setFont(new Font("SansSerif", Font.BOLD, 13));
        txa.setForeground(NAVY);
        return txa;
    }

    private Box dongTrai(JLabel lbl) {
        Box box = Box.createHorizontalBox();
        box.add(lbl);
        box.add(Box.createHorizontalGlue());
        return box;
    }

    // Thêm 1 dòng "nhãn : giá trị" vào Box dọc
    private void themDong(Box box, String nhan, JLabel lblGiaTri) {
        JLabel lblNhan = new JLabel(nhan);
        lblNhan.setForeground(XAM);
        lblNhan.setPreferredSize(new Dimension(95, lblNhan.getPreferredSize().height));
        Box boxDong = Box.createHorizontalBox();
        boxDong.add(lblNhan);
        boxDong.add(Box.createHorizontalStrut(8));
        boxDong.add(lblGiaTri);
        boxDong.add(Box.createHorizontalGlue());
        box.add(boxDong);
        box.add(Box.createVerticalStrut(10));
    }

    // Thêm 1 dòng "nhãn : nội dung nhiều dòng" vào Box dọc
    private void themDongNhieuChu(Box box, String nhan, JTextArea txaGiaTri) {
        JLabel lblNhan = new JLabel(nhan);
        lblNhan.setForeground(XAM);
        lblNhan.setPreferredSize(new Dimension(95, lblNhan.getPreferredSize().height));
        lblNhan.setAlignmentY(TOP_ALIGNMENT);
        txaGiaTri.setAlignmentY(TOP_ALIGNMENT);
        Box boxDong = Box.createHorizontalBox();
        boxDong.add(lblNhan);
        boxDong.add(Box.createHorizontalStrut(8));
        boxDong.add(txaGiaTri);
        box.add(boxDong);
        box.add(Box.createVerticalStrut(10));
    }

    // ===================== Hàm hiển thị (Controller gọi) =====================

    // Bước 1: đổ danh sách bệnh nhân tìm được vào bảng bên trái
    public void hienDsBenhNhan(ArrayList<BenhNhan> ds) {
        dsBenhNhan = ds;
        modelBenhNhan.setRowCount(0);
        for (BenhNhan bn : ds) {
            modelBenhNhan.addRow(new Object[]{bn.getMaBenhNhan(), bn.getHoTen(), bn.getSoDienThoai()});
        }
        lblTrangThai.setForeground(XAM);
        lblTrangThai.setText("Tìm được " + ds.size() + " bệnh nhân. Chọn một bệnh nhân để mở hồ sơ.");
        hienHoSo(null, null);
    }

    // Bước 2: hiện hồ sơ + danh sách các lần khám (bn = null → xóa trắng)
    public void hienHoSo(BenhNhan bn, HoSoBenhAn hoSo) {
        modelLuotKham.setRowCount(0);
        dsLuotKham = new ArrayList<>();
        hienChiTietLuotKham(null);
        if (bn == null || hoSo == null) {
            lblTenBenhNhan.setText("Chưa chọn bệnh nhân");
            lblThongTinHoSo.setText("Chọn một bệnh nhân ở danh sách bên trái.");
            txaTienSuBenh.setText("—");
            txaDiUng.setText("—");
            return;
        }
        lblTenBenhNhan.setText(bn.getHoTen() + " (" + bn.getMaBenhNhan() + ")");
        lblThongTinHoSo.setText(hoSo.getMaHoSo() == null
                ? "Chưa có hồ sơ bệnh án"
                : "Mã hồ sơ " + hoSo.getMaHoSo() + " · Lập ngày " + hoSo.getNgayLap().format(NGAY));
        txaTienSuBenh.setText(hoSo.getTienSuBenh() == null ? "Không có" : hoSo.getTienSuBenh());
        txaDiUng.setText(hoSo.getDiUng() == null ? "Không có" : hoSo.getDiUng());

        dsLuotKham = hoSo.getDsLuotKham();
        for (LuotKham lk : dsLuotKham) {
            modelLuotKham.addRow(new Object[]{
                    lk.getMaPhieuKham(),
                    lk.getThoiGianTiepNhan() == null ? "" : lk.getThoiGianTiepNhan().format(NGAY_GIO),
                    hien(lk.getTenBacSi()),
                    hien(lk.getChanDoan()),
                    lk.getTrangThaiHienThi()
            });
        }
        lblTrangThai.setForeground(XAM);
        lblTrangThai.setText(dsLuotKham.isEmpty()
                ? "Bệnh nhân chưa có lần khám nào."
                : "Có " + dsLuotKham.size() + " lần khám. Chọn một lần khám để xem chi tiết.");
    }

    // Bước 3–4: hiện chi tiết 1 lần khám (null → xóa trắng)
    public void hienChiTietLuotKham(LuotKham lk) {
        if (lk == null) {
            lblMaPhieu.setText("—");
            lblNgayKham.setText("—");
            lblBacSi.setText("—");
            lblTrangThaiKham.setText("—");
            txaTrieuChung.setText("—");
            txaChanDoan.setText("—");
            txaKetLuan.setText("—");
            return;
        }
        lblMaPhieu.setText(lk.getMaPhieuKham());
        lblNgayKham.setText(lk.getThoiGianTiepNhan() == null ? "—" : lk.getThoiGianTiepNhan().format(NGAY_GIO));
        lblBacSi.setText(hien(lk.getTenBacSi()));
        lblTrangThaiKham.setText(lk.getTrangThaiHienThi());
        txaTrieuChung.setText(hien(lk.getTrieuChung()));
        txaChanDoan.setText(hien(lk.getChanDoan()));
        txaKetLuan.setText(hien(lk.getKetLuan()));
    }

    // Đang đọc database: làm mờ nút để không bấm chồng
    public void datDangTai(boolean dangTai, String thongBao) {
        btnTimKiem.setEnabled(!dangTai);
        btnLamMoi.setEnabled(!dangTai);
        if (dangTai) {
            lblTrangThai.setForeground(XAM);
            lblTrangThai.setText(thongBao);
        }
    }

    // Hiện lỗi: dòng chữ đỏ ở SOUTH + hộp thông báo giữa màn hình
    public void hienLoi(String thongBao) {
        lblTrangThai.setForeground(new Color(176, 32, 32));
        lblTrangThai.setText(thongBao);
        JOptionPane.showMessageDialog(this, thongBao, "Thông báo lỗi", JOptionPane.ERROR_MESSAGE);
    }

    // Bệnh nhân / lần khám ở dòng đang chọn (null nếu chưa chọn)
    public BenhNhan layBenhNhanDangChon() {
        int row = tblBenhNhan.getSelectedRow();
        return row < 0 || row >= dsBenhNhan.size() ? null : dsBenhNhan.get(row);
    }

    public LuotKham layLuotKhamDangChon() {
        int row = tblLuotKham.getSelectedRow();
        return row < 0 || row >= dsLuotKham.size() ? null : dsLuotKham.get(row);
    }

    private String hien(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    // ===================== Getter cho Controller =====================

    public JTextField getTxtTimKiem() { return txtTimKiem; }
    public JButton getBtnTimKiem() { return btnTimKiem; }
    public JButton getBtnLamMoi() { return btnLamMoi; }
    public JTable getTblBenhNhan() { return tblBenhNhan; }
    public JTable getTblLuotKham() { return tblLuotKham; }
}
