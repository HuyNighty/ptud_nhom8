package com.quanlynhakhoa.inventory.lohang.view;

import com.quanlynhakhoa.inventory.lohang.controller.LoHangController;
import com.quanlynhakhoa.inventory.lohang.model.LoHang;
import com.quanlynhakhoa.inventory.lohang.service.LoHangService;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** View tra cứu LoHang; không cho sửa số tồn ngoài chứng từ kho. */
public final class LoHangPanel extends JPanel {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(97, 115, 137);
    private static final Color BORDER = new Color(220, 231, 242);
    private static final Color BACKGROUND = new Color(245, 248, 252);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField txtTimKiem = new JTextField(20);
    private final JComboBox<String> cmbLoai = new JComboBox<>(new String[]{"Tất cả mặt hàng", "Thuốc", "Vật tư"});
    private final JComboBox<String> cmbHan = new JComboBox<>(new String[]{"Mọi hạn dùng", "Đã hết hạn", "Còn hạn / không có HSD"});
    private final JButton btnTim = new JButton("Tìm kiếm");
    private final JButton btnTaiLai = new JButton("Làm mới");
    private final JLabel lblTrangThai = new JLabel("Đang tải danh sách lô...");
    private final DefaultTableModel tableModel;
    private final JTable tblLoHang;
    private List<LoHang> dsLoHang = new ArrayList<>();

    public LoHangPanel(DesktopSession phien) {
        setLayout(new BorderLayout(12, 12));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 26, 20, 26));

        JLabel title = new JLabel("Tra cứu lô hàng");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        title.setForeground(NAVY);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createLineBorder(BORDER));
        toolbar.add(new JLabel("Mã lô / số lô / mặt hàng / NCC:"));
        toolbar.add(txtTimKiem);
        toolbar.add(cmbLoai);
        toolbar.add(cmbHan);
        toolbar.add(btnTim);
        toolbar.add(btnTaiLai);

        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setOpaque(false);
        north.add(title, BorderLayout.NORTH);
        north.add(toolbar, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        String[] columns = {"Mã lô", "Số lô", "Loại", "Mã hàng", "Tên hàng", "Nhà cung cấp", "Ngày SX", "Hạn sử dụng", "Tồn", "Tình trạng hạn"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblLoHang = new JTable(tableModel);
        tblLoHang.setRowHeight(29);
        tblLoHang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblLoHang.getTableHeader().setReorderingAllowed(false);
        tblLoHang.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(tblLoHang);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 5));
        south.setOpaque(false);
        lblTrangThai.setForeground(MUTED);
        south.add(lblTrangThai, BorderLayout.NORTH);
        JLabel note = new JLabel("Chỉ xem dữ liệu. Số tồn được cập nhật cùng transaction của phiếu nhập/xuất/điều chỉnh; không sửa trực tiếp tại danh sách.", SwingConstants.LEFT);
        note.setForeground(MUTED);
        note.setFont(note.getFont().deriveFont(11f));
        south.add(note, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        new LoHangController(this, new LoHangService(phien));
    }

    public void hienDanhSach(List<LoHang> danhSach) {
        dsLoHang = new ArrayList<>(danhSach);
        tableModel.setRowCount(0);
        for (LoHang lo : dsLoHang) {
            String maHang = lo.getMaThuoc() != null ? lo.getMaThuoc() : lo.getMaVatTu();
            tableModel.addRow(new Object[]{lo.getMaLo(), lo.getSoLo(), lo.getLoaiHangHienThi(), maHang,
                    lo.getTenHang(), lo.getTenNhaCungCap() + " (" + lo.getMaNhaCungCap() + ")",
                    dinhDang(lo.getNgaySanXuat()), dinhDang(lo.getHanSuDung()), lo.getSoLuongTon(),
                    lo.getTinhTrangHan()});
        }
        setTrangThai("Hiển thị " + dsLoHang.size() + " lô (tối đa 1.000).", MUTED);
    }

    private String dinhDang(java.time.LocalDate ngay) { return ngay == null ? "—" : ngay.format(DATE); }

    public String getTuKhoa() { return txtTimKiem.getText(); }
    public String getLoaiLoc() {
        return switch (cmbLoai.getSelectedIndex()) {
            case 1 -> LoHang.LOAI_THUOC;
            case 2 -> LoHang.LOAI_VAT_TU;
            default -> LoHang.LOAI_TAT_CA;
        };
    }
    public String getHanLoc() {
        return switch (cmbHan.getSelectedIndex()) {
            case 1 -> LoHang.HAN_HET_HAN;
            case 2 -> LoHang.HAN_CON_HAN;
            default -> LoHang.HAN_TAT_CA;
        };
    }
    public JButton getBtnTim() { return btnTim; }
    public JButton getBtnTaiLai() { return btnTaiLai; }
    public JTextField getTxtTimKiem() { return txtTimKiem; }

    public void datDangTai(boolean dangTai) {
        txtTimKiem.setEnabled(!dangTai);
        cmbLoai.setEnabled(!dangTai);
        cmbHan.setEnabled(!dangTai);
        btnTim.setEnabled(!dangTai);
        btnTaiLai.setEnabled(!dangTai);
        if (dangTai) setTrangThai("Đang tải dữ liệu lô hàng...", MUTED);
    }

    public void setTrangThai(String message, Color color) {
        lblTrangThai.setText(message);
        lblTrangThai.setForeground(color);
    }

    public void hienLoi(String message) {
        setTrangThai(message, new Color(176, 32, 32));
        JOptionPane.showMessageDialog(this, message, "Không thể tải lô hàng", JOptionPane.ERROR_MESSAGE);
    }
}
