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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

// View: chỉ tạo giao diện và hiển thị dữ liệu. Sự kiện do BenhNhanController xử lý.
//
//  ┌────────────── pnlNorth ──────────────┐
//  │ tiêu đề + ô tìm kiếm + các nút       │
//  ├──────── pnlCenter ──────┬─ pnlEast ──┤
//  │ bảng bệnh nhân          │ thông tin  │
//  ├────────────── pnlSouth ─┴────────────┤
//  │ dòng trạng thái                      │
//  └──────────────────────────────────────┘
public class BenhNhanPanel extends JPanel {
    // Màu giống AccountPanel / DashboardPanel có sẵn
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
    private JComboBox<String> cmbGioiTinh;
    private JButton btnLoc;
    private JButton btnXoaLoc;
    private JButton btnTaiLai;
    private JButton btnThem;

    // ----- CENTER -----
    private JPanel pnlCenter;
    private DefaultTableModel tableModel;
    private JTable tblBenhNhan;
    private JScrollPane scrBenhNhan;

    // ----- EAST -----
    private JPanel pnlEast;
    private JLabel lblTenBenhNhan;
    private JLabel lblMaBenhNhan;
    private JLabel lblNgaySinh;
    private JLabel lblGioiTinh;
    private JLabel lblSoDienThoai;
    private JLabel lblDiaChi;
    private JLabel lblNguoiLienHe;
    private JLabel lblNgayTao;
    private JButton btnSua;
    private BenhNhan bnDangXem;     // bệnh nhân đang hiện ở khung bên phải
    private boolean duocSua;        // người đăng nhập có quyền sửa không

    // ----- SOUTH -----
    private JPanel pnlSouth;
    private JLabel lblTrangThai;

    // Hàm khởi tạo: Core gắn vào menu bằng .register(Module.PATIENTS, BenhNhanPanel::new)
    public BenhNhanPanel(DesktopSession phien) {
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

        // Tạo Service và Controller, Controller sẽ gắn sự kiện cho các nút
        BenhNhanService service = new BenhNhanService(phien);
        new BenhNhanController(this, service);
    }

    // ===================== Tạo 4 vùng =====================

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
        btnLoc.setIcon(new BieuTuong(BieuTuong.TIM_KIEM));     // hình kính lúp
        btnTaiLai.setIcon(new BieuTuong(BieuTuong.LAM_MOI));   // hình mũi tên vòng tròn
        btnThem = new JButton("+ Thêm bệnh nhân");

        // Hàng tìm kiếm nằm ở phía dưới (SOUTH) của pnlNorth
        JPanel pnlNorthSouth = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
        pnlNorthSouth.setBackground(Color.WHITE);
        pnlNorthSouth.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlNorthSouth.add(new JLabel("Tìm (SĐT, mã BN, họ tên):"));
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

        String[] headers = "Mã BN;Họ tên;Ngày sinh;Giới tính;SĐT".split(";");
        tableModel = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // bảng chỉ để xem, không sửa trực tiếp
            }
        };
        tblBenhNhan = new JTable(tableModel);
        tblBenhNhan.setRowHeight(30);
        tblBenhNhan.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblBenhNhan.getTableHeader().setReorderingAllowed(false);
        tblBenhNhan.setFillsViewportHeight(true); // phần trống của bảng cũng nền trắng

        scrBenhNhan = new JScrollPane(tblBenhNhan);
        scrBenhNhan.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlCenter.add(scrBenhNhan, BorderLayout.CENTER);
    }

    private void taoEast() {
        pnlEast = new JPanel(new BorderLayout());
        pnlEast.setBackground(Color.WHITE);
        pnlEast.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlEast.setPreferredSize(new Dimension(340, 0));

        // Box dọc chứa các dòng thông tin, mỗi dòng là 1 Box ngang (giống code cũ)
        Box boxThongTin = Box.createVerticalBox();
        boxThongTin.setBorder(new EmptyBorder(16, 16, 16, 16));

        lblTenBenhNhan = new JLabel("Chưa chọn bệnh nhân");
        lblTenBenhNhan.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblTenBenhNhan.setForeground(NAVY);
        lblMaBenhNhan = new JLabel("Chọn một dòng trong bảng");
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

        // Nhãn bên trái (lblNhan...) và giá trị bên phải (lbl...)
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

        // Cho các nhãn rộng bằng nhau để giá trị thẳng cột (giống code cũ)
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

        pnlEast.add(boxThongTin, BorderLayout.NORTH); // NORTH để các dòng nằm sát phía trên

        // SOUTH của pnlEast: nút Sửa (thao tác trên bệnh nhân đang chọn)
        btnSua = new JButton("Sửa thông tin...");
        btnSua.setEnabled(false);
        JPanel pnlEastSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        pnlEastSouth.setOpaque(false);
        pnlEastSouth.add(btnSua);
        pnlEast.add(pnlEastSouth, BorderLayout.SOUTH);
    }

    // Thêm 1 dòng "nhãn : giá trị" vào Box dọc
    private void themDong(Box boxThongTin, JLabel lblNhan, JLabel lblGiaTri) {
        Box boxDong = Box.createHorizontalBox();
        boxDong.add(lblNhan);
        boxDong.add(Box.createHorizontalStrut(10));
        boxDong.add(lblGiaTri);
        boxDong.add(Box.createHorizontalGlue());
        boxThongTin.add(boxDong);
        boxThongTin.add(Box.createVerticalStrut(12));
    }

    private JLabel taoNhan(String chu) {
        JLabel lbl = new JLabel(chu);
        lbl.setForeground(XAM);
        return lbl;
    }

    private JLabel taoGiaTri() {
        JLabel lbl = new JLabel("—");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(NAVY);
        return lbl;
    }

    private void taoSouth() {
        pnlSouth = new JPanel(new BorderLayout());
        pnlSouth.setOpaque(false);
        lblTrangThai = new JLabel("Chưa tải dữ liệu.");
        lblTrangThai.setForeground(XAM);
        pnlSouth.add(lblTrangThai, BorderLayout.CENTER);
    }

    // ===================== Hàm hiển thị (Controller gọi) =====================

    // Đổ danh sách bệnh nhân vào bảng
    public void docDuLieuVaoTable(ArrayList<BenhNhan> dsBenhNhan) {
        tableModel.setRowCount(0); // xóa các dòng cũ trên bảng
        for (BenhNhan bn : dsBenhNhan) {
            tableModel.addRow(new Object[]{
                    bn.getMaBenhNhan(),
                    bn.getHoTen(),
                    bn.getNgaySinh().format(NGAY),
                    bn.getGioiTinhHienThi(),
                    bn.getSoDienThoai()
            });
        }
        lblTrangThai.setForeground(XAM);
        lblTrangThai.setText("Có " + dsBenhNhan.size() + " bệnh nhân.");
        hienChiTiet(null);
    }

    // Hiện thông tin 1 bệnh nhân ở khung bên phải (null = chưa chọn)
    public void hienChiTiet(BenhNhan bn) {
        bnDangXem = bn;
        btnSua.setEnabled(bn != null && duocSua); // chưa chọn ai hoặc không có quyền → làm mờ
        if (bn == null) {
            lblTenBenhNhan.setText("Chưa chọn bệnh nhân");
            lblMaBenhNhan.setText("Chọn một dòng trong bảng");
            lblNgaySinh.setText("—");
            lblGioiTinh.setText("—");
            lblSoDienThoai.setText("—");
            lblDiaChi.setText("—");
            lblNguoiLienHe.setText("—");
            lblNgayTao.setText("—");
            return;
        }
        lblTenBenhNhan.setText(bn.getHoTen());
        lblMaBenhNhan.setText("Mã bệnh nhân: " + bn.getMaBenhNhan());
        lblNgaySinh.setText(bn.getNgaySinh().format(NGAY));
        lblGioiTinh.setText(bn.getGioiTinhHienThi());
        lblSoDienThoai.setText(bn.getSoDienThoai());
        lblDiaChi.setText(bn.getDiaChi() == null ? "—" : bn.getDiaChi());
        lblNguoiLienHe.setText(bn.getNguoiLienHe() == null ? "—" : bn.getNguoiLienHe());
        lblNgayTao.setText(bn.getNgayTao() == null ? "—" : bn.getNgayTao().format(NGAY_GIO));
    }

    // Đang đọc database: làm mờ các nút tìm/làm mới để không bấm chồng, hiện chữ "Đang tải..."
    public void datDangTai(boolean dangTai) {
        btnLoc.setEnabled(!dangTai);
        btnXoaLoc.setEnabled(!dangTai);
        btnTaiLai.setEnabled(!dangTai);
        if (dangTai) {
            lblTrangThai.setForeground(XAM);
            lblTrangThai.setText("Đang tải dữ liệu...");
        }
    }

    // Hiện lỗi: dòng chữ đỏ ở SOUTH + hộp thông báo giữa màn hình
    public void hienLoi(String thongBao) {
        lblTrangThai.setForeground(new Color(176, 32, 32));
        lblTrangThai.setText(thongBao);
        javax.swing.JOptionPane.showMessageDialog(this, thongBao, "Thông báo lỗi",
                javax.swing.JOptionPane.ERROR_MESSAGE);
    }

    // Bật/tắt nút theo quyền của người đăng nhập (Controller gọi 1 lần khi mở màn hình)
    public void datQuyen(boolean duocThem, boolean duocSua) {
        this.duocSua = duocSua;
        btnThem.setEnabled(duocThem);
        btnSua.setEnabled(bnDangXem != null && duocSua);
    }

    // Chọn dòng có mã bệnh nhân này trong bảng (dùng sau khi lưu xong)
    public void chonDongTheoMa(String maBenhNhan) {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (tableModel.getValueAt(row, 0).equals(maBenhNhan)) {
                tblBenhNhan.setRowSelectionInterval(row, row);
                tblBenhNhan.scrollRectToVisible(tblBenhNhan.getCellRect(row, 0, true));
                return;
            }
        }
    }

    public BenhNhan getBnDangXem() { return bnDangXem; }

    // Xóa trắng ô tìm kiếm và đưa giới tính về "Tất cả"
    public void xoaTrang() {
        txtTimKiem.setText("");
        cmbGioiTinh.setSelectedIndex(0);
        txtTimKiem.requestFocus();
    }

    // Đổi lựa chọn trong combo sang mã lưu trong database ("" = tất cả)
    public String layGioiTinhDangChon() {
        String chon = (String) cmbGioiTinh.getSelectedItem();
        if ("Nam".equals(chon)) return "NAM";
        if ("Nữ".equals(chon)) return "NU";
        return "";
    }

    // ===================== Getter cho Controller =====================

    public JTextField getTxtTimKiem() { return txtTimKiem; }
    public JComboBox<String> getCmbGioiTinh() { return cmbGioiTinh; }
    public JButton getBtnLoc() { return btnLoc; }
    public JButton getBtnXoaLoc() { return btnXoaLoc; }
    public JButton getBtnTaiLai() { return btnTaiLai; }
    public JTable getTblBenhNhan() { return tblBenhNhan; }
    public JButton getBtnThem() { return btnThem; }
    public JButton getBtnSua() { return btnSua; }
}
