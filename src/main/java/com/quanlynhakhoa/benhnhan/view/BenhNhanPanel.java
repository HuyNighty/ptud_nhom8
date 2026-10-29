package com.quanlynhakhoa.benhnhan.view;

import com.quanlynhakhoa.benhnhan.controller.BenhNhanController;
import com.quanlynhakhoa.benhnhan.dao.BenhNhanDao;
import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.benhnhan.model.GioiTinh;
import com.quanlynhakhoa.benhnhan.service.BenhNhanService;
import com.quanlynhakhoa.benhnhan.service.KetNoiCsdl;
import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Màn hình Danh sách bệnh nhân. Panel gốc dùng BorderLayout, chia vùng theo HƯỚNG:
 *
 *   ┌──────────────── pnlNorth ────────────────┐
 *   │ lblTieuDe / lblMoTa / pnlNorthSouth (lọc)│
 *   ├────────── pnlCenter ─────────┬ pnlEast ──┤
 *   │ scrDanhSach → tblBenhNhan    │ chi tiết  │
 *   ├────────────── pnlSouth ──────┴───────────┤
 *   │ lblDemSoDong      lblTrangThai           │
 *   └──────────────────────────────────────────┘
 *   (WEST không dùng)
 *
 * Panel chỉ hiển thị và chuyển sự kiện cho BenhNhanController; không gọi SQL.
 */
public final class BenhNhanPanel extends JPanel {
    // Màu và font lấy theo AccountPanel / DashboardPanel hiện có.
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(97, 115, 137);
    private static final Color BORDER = new Color(220, 231, 242);
    private static final Color BACKGROUND = new Color(245, 248, 252);
    private static final Color ERROR = new Color(176, 32, 32);
    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final BenhNhanController controller;

    // ===== NORTH: tiêu đề + hàng lọc =====
    private final JPanel pnlNorth = new JPanel(new BorderLayout(0, 7));
    private final JLabel lblTieuDe = new JLabel("Hồ sơ bệnh nhân");
    private final JLabel lblMoTa = new JLabel("Tìm theo SĐT, mã BN hoặc họ tên · chọn một dòng để xem chi tiết");
    private final JPanel pnlNorthSouth = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
    private final JTextField txtTimKiem = new JTextField(24);
    private final JComboBox<GioiTinh> cmbLocGioiTinh = new JComboBox<>();
    private final JButton btnLoc = new JButton("Tìm kiếm");
    private final JButton btnXoaLoc = new JButton("Xóa lọc");
    private final JButton btnTaiLai = new JButton("Làm mới");

    // ===== CENTER: bảng danh sách =====
    private final JPanel pnlCenter = new JPanel(new BorderLayout());
    private final BenhNhanTableModel mdlBenhNhan = new BenhNhanTableModel();
    private final JTable tblBenhNhan = new JTable(mdlBenhNhan);
    private final JScrollPane scrDanhSach = new JScrollPane(tblBenhNhan);

    // ===== EAST: chi tiết bệnh nhân đang chọn =====
    private final JPanel pnlEast = new JPanel(new BorderLayout());
    private final JPanel pnlEastNorth = new JPanel(new BorderLayout(0, 4));
    private final JLabel lblTenBenhNhan = new JLabel();
    private final JLabel lblMaBenhNhan = new JLabel();
    private final JTabbedPane tabChiTiet = new JTabbedPane();
    private final JPanel pnlThongTin = new JPanel(new GridLayout(0, 2, 8, 10));
    private final JLabel lblNgaySinh = new JLabel();
    private final JLabel lblGioiTinh = new JLabel();
    private final JLabel lblSoDienThoai = new JLabel();
    private final JLabel lblDiaChi = new JLabel();
    private final JLabel lblNguoiLienHe = new JLabel();
    private final JLabel lblNgayTao = new JLabel();

    // ===== SOUTH: số dòng + trạng thái =====
    private final JPanel pnlSouth = new JPanel(new BorderLayout(16, 0));
    private final JLabel lblDemSoDong = new JLabel(" ");
    private final JLabel lblTrangThai = new JLabel("Chưa tải dữ liệu.");

    private BenhNhan benhNhanDangChon;

    /** Dùng khi Core gắn vào menu: .register(Module.PATIENTS, BenhNhanPanel::new) */
    public BenhNhanPanel(DesktopSession phien) {
        this(phien, TeamDevConnection::open, TeamDevConnection.hasLocalConfig());
    }

    /** Dùng cho chạy thử / kiểm thử với nguồn kết nối khác. */
    public BenhNhanPanel(DesktopSession phien, KetNoiCsdl ketNoiCsdl) {
        this(phien, ketNoiCsdl, true);
    }

    private BenhNhanPanel(DesktopSession phien, KetNoiCsdl ketNoiCsdl, boolean daCauHinh) {
        this.controller = new BenhNhanController(new BenhNhanService(
                Objects.requireNonNull(phien, "phien"), Objects.requireNonNull(ketNoiCsdl, "ketNoiCsdl")));
        setLayout(new BorderLayout(14, 14));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 26, 24, 26));

        add(taoNorth(), BorderLayout.NORTH);
        add(taoCenter(), BorderLayout.CENTER);
        add(taoEast(), BorderLayout.EAST);
        add(taoSouth(), BorderLayout.SOUTH);

        ganSuKien();
        controller.ganPanel(this);
        hienChiTiet(null);

        if (daCauHinh) {
            // Tải sau khi panel dựng xong; SQL chạy trong SwingWorker của Controller.
            SwingUtilities.invokeLater(this::locDanhSach);
        } else {
            btnLoc.setEnabled(false);
            btnTaiLai.setEnabled(false);
            btnXoaLoc.setEnabled(false);
            lblTrangThai.setText("Chưa có cấu hình SQL Server development trong worktree.");
        }
    }

    // =====================================================================
    // Dựng từng vùng
    // =====================================================================

    private JPanel taoNorth() {
        pnlNorth.setOpaque(false);
        lblTieuDe.setFont(new Font("SansSerif", Font.BOLD, 23));
        lblTieuDe.setForeground(NAVY);
        lblMoTa.setForeground(MUTED);

        pnlNorthSouth.setBackground(Color.WHITE);
        pnlNorthSouth.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(7, 10, 7, 10)));
        cmbLocGioiTinh.addItem(null);
        for (GioiTinh gt : GioiTinh.values()) cmbLocGioiTinh.addItem(gt);
        cmbLocGioiTinh.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setText(value == null ? "Tất cả giới tính" : ((GioiTinh) value).tenHienThi());
                return this;
            }
        });
        pnlNorthSouth.add(new JLabel("Tìm:"));
        pnlNorthSouth.add(txtTimKiem);
        pnlNorthSouth.add(cmbLocGioiTinh);
        pnlNorthSouth.add(btnLoc);
        pnlNorthSouth.add(btnXoaLoc);
        pnlNorthSouth.add(btnTaiLai);

        pnlNorth.add(lblTieuDe, BorderLayout.NORTH);
        pnlNorth.add(lblMoTa, BorderLayout.CENTER);
        pnlNorth.add(pnlNorthSouth, BorderLayout.SOUTH);
        return pnlNorth;
    }

    private JPanel taoCenter() {
        pnlCenter.setOpaque(false);
        tblBenhNhan.setRowHeight(30);
        tblBenhNhan.setAutoCreateRowSorter(true);
        tblBenhNhan.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblBenhNhan.setFillsViewportHeight(true);
        tblBenhNhan.getTableHeader().setReorderingAllowed(false);
        tblBenhNhan.getColumnModel().getColumn(0).setPreferredWidth(90);
        tblBenhNhan.getColumnModel().getColumn(1).setPreferredWidth(210);
        scrDanhSach.setBorder(BorderFactory.createLineBorder(BORDER));
        pnlCenter.add(scrDanhSach, BorderLayout.CENTER);
        return pnlCenter;
    }

    private JPanel taoEast() {
        pnlEast.setBackground(Color.WHITE);
        pnlEast.setBorder(BorderFactory.createLineBorder(BORDER));
        pnlEast.setPreferredSize(new Dimension(360, 10));

        // EAST → NORTH: tên + mã bệnh nhân
        pnlEastNorth.setOpaque(false);
        pnlEastNorth.setBorder(new EmptyBorder(16, 16, 10, 16));
        lblTenBenhNhan.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblTenBenhNhan.setForeground(NAVY);
        lblMaBenhNhan.setForeground(MUTED);
        pnlEastNorth.add(lblTenBenhNhan, BorderLayout.NORTH);
        pnlEastNorth.add(lblMaBenhNhan, BorderLayout.SOUTH);

        // EAST → CENTER: tab "Thông tin"
        pnlThongTin.setBackground(Color.WHITE);
        pnlThongTin.setBorder(new EmptyBorder(12, 14, 12, 14));
        themDong("Ngày sinh", lblNgaySinh);
        themDong("Giới tính", lblGioiTinh);
        themDong("Số điện thoại", lblSoDienThoai);
        themDong("Địa chỉ", lblDiaChi);
        themDong("Người liên hệ", lblNguoiLienHe);
        themDong("Ngày tạo", lblNgayTao);
        JPanel pnlThongTinNorth = new JPanel(new BorderLayout());
        pnlThongTinNorth.setBackground(Color.WHITE);
        pnlThongTinNorth.add(pnlThongTin, BorderLayout.NORTH); // giữ các dòng sát phía trên
        tabChiTiet.addTab("Thông tin", pnlThongTinNorth);

        pnlEast.add(pnlEastNorth, BorderLayout.NORTH);
        pnlEast.add(tabChiTiet, BorderLayout.CENTER);
        return pnlEast;
    }

    private void themDong(String nhan, JLabel lblGiaTri) {
        JLabel lblNhan = new JLabel(nhan);
        lblNhan.setForeground(MUTED);
        lblGiaTri.setForeground(NAVY);
        lblGiaTri.setFont(new Font("SansSerif", Font.BOLD, 13));
        pnlThongTin.add(lblNhan);
        pnlThongTin.add(lblGiaTri);
    }

    private JPanel taoSouth() {
        pnlSouth.setOpaque(false);
        lblDemSoDong.setForeground(NAVY);
        lblDemSoDong.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblTrangThai.setForeground(MUTED);
        pnlSouth.add(lblDemSoDong, BorderLayout.WEST);
        pnlSouth.add(lblTrangThai, BorderLayout.CENTER);
        return pnlSouth;
    }

    // =====================================================================
    // Sự kiện → Controller
    // =====================================================================

    private void ganSuKien() {
        btnLoc.addActionListener(e -> locDanhSach());
        txtTimKiem.addActionListener(e -> locDanhSach());
        btnTaiLai.addActionListener(e -> locDanhSach());
        btnXoaLoc.addActionListener(e -> {
            txtTimKiem.setText("");
            cmbLocGioiTinh.setSelectedIndex(0);
            locDanhSach();
        });
        tblBenhNhan.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int dongXem = tblBenhNhan.getSelectedRow();
            BenhNhan bn = dongXem < 0 ? null
                    : mdlBenhNhan.layDong(tblBenhNhan.convertRowIndexToModel(dongXem));
            controller.chonBenhNhan(bn == null ? null : bn.maBenhNhan());
        });
    }

    private void locDanhSach() {
        controller.taiDanhSach(txtTimKiem.getText(), (GioiTinh) cmbLocGioiTinh.getSelectedItem());
    }

    // =====================================================================
    // Controller gọi để hiển thị (luôn trên luồng giao diện)
    // =====================================================================

    public void hienDangTai(boolean dangTai, String thongBao) {
        btnLoc.setEnabled(!dangTai);
        btnTaiLai.setEnabled(!dangTai);
        btnXoaLoc.setEnabled(!dangTai);
        if (thongBao != null) {
            lblTrangThai.setForeground(MUTED);
            lblTrangThai.setText(thongBao);
        }
    }

    public void hienDanhSach(BenhNhanDao.KetQuaTimKiem ketQua) {
        String maDangChon = benhNhanDangChon == null ? null : benhNhanDangChon.maBenhNhan();
        mdlBenhNhan.datDuLieu(ketQua.danhSach());
        lblDemSoDong.setText(ketQua.danhSach().size() + " bệnh nhân");
        lblTrangThai.setForeground(MUTED);
        lblTrangThai.setText(ketQua.biCatBot()
                ? "Chỉ hiển thị " + BenhNhanDao.GIOI_HAN_DONG + " dòng đầu — hãy nhập từ khóa cụ thể hơn."
                : "Đọc từ SQL Server · hồ sơ bệnh nhân không bị xóa.");
        int dong = maDangChon == null ? -1 : mdlBenhNhan.timDong(maDangChon);
        if (dong >= 0) {
            int dongXem = tblBenhNhan.convertRowIndexToView(dong);
            tblBenhNhan.setRowSelectionInterval(dongXem, dongXem);
        } else {
            hienChiTiet(null);
        }
    }

    public void hienChiTiet(BenhNhan bn) {
        benhNhanDangChon = bn;
        if (bn == null) {
            lblTenBenhNhan.setText("Chưa chọn bệnh nhân");
            lblMaBenhNhan.setText("Chọn một dòng trong danh sách.");
            for (JLabel lbl : new JLabel[]{lblNgaySinh, lblGioiTinh, lblSoDienThoai,
                    lblDiaChi, lblNguoiLienHe, lblNgayTao}) {
                lbl.setText("—");
            }
            return;
        }
        lblTenBenhNhan.setText(bn.hoTen());
        lblMaBenhNhan.setText("Mã bệnh nhân: " + bn.maBenhNhan());
        lblNgaySinh.setText(bn.ngaySinh() == null ? "—" : BenhNhanTableModel.NGAY.format(bn.ngaySinh()));
        lblGioiTinh.setText(bn.gioiTinh().tenHienThi());
        lblSoDienThoai.setText(BenhNhanTableModel.dinhDangSoDienThoai(bn.soDienThoai()));
        lblDiaChi.setText(hien(bn.diaChi()));
        lblNguoiLienHe.setText(hien(bn.nguoiLienHe()));
        lblNgayTao.setText(bn.ngayTao() == null ? "—" : NGAY_GIO.format(bn.ngayTao()));
    }

    public void hienLoi(String thongBao) {
        lblTrangThai.setForeground(ERROR);
        lblTrangThai.setText(thongBao);
    }

    private static String hien(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
