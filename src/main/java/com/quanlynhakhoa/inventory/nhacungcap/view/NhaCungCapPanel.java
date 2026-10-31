package com.quanlynhakhoa.inventory.nhacungcap.view;

import com.quanlynhakhoa.inventory.nhacungcap.controller.NhaCungCapController;
import com.quanlynhakhoa.inventory.nhacungcap.model.NhaCungCap;
import com.quanlynhakhoa.inventory.nhacungcap.service.NhaCungCapService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
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

/** View danh sách nhà cung cấp; không chứa SQL hoặc luật nghiệp vụ. */
public final class NhaCungCapPanel extends JPanel {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(97, 115, 137);
    private static final Color BORDER = new Color(220, 231, 242);
    private static final Color BACKGROUND = new Color(245, 248, 252);

    private final JTextField txtTimKiem = new JTextField(20);
    private final JComboBox<String> cmbTrangThai = new JComboBox<>(
            new String[]{"Tất cả trạng thái", "Đang hợp tác", "Ngừng hợp tác"});
    private final JButton btnTim = new JButton("Tìm kiếm");
    private final JButton btnTaiLai = new JButton("Làm mới");
    private final JButton btnThem = new JButton("+ Thêm nhà cung cấp");
    private final JButton btnSua = new JButton("Sửa thông tin");
    private final JButton btnDoiTrangThai = new JButton("Ngừng hợp tác");
    private final JLabel lblTrangThai = new JLabel("Đang tải danh sách...");
    private final DefaultTableModel tableModel;
    private final JTable tblNhaCungCap;
    private List<NhaCungCap> dsNhaCungCap = new ArrayList<>();
    private boolean coTheQuanLy;
    private boolean dangTai;

    public NhaCungCapPanel(DesktopSession phien) {
        setLayout(new BorderLayout(12, 12));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 26, 20, 26));

        JLabel title = new JLabel("Danh sách nhà cung cấp");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        title.setForeground(NAVY);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createLineBorder(BORDER));
        toolbar.add(new JLabel("Tìm mã/tên/điện thoại/email:"));
        toolbar.add(txtTimKiem);
        toolbar.add(cmbTrangThai);
        toolbar.add(btnTim);
        toolbar.add(btnTaiLai);
        toolbar.add(btnThem);
        toolbar.add(btnSua);
        toolbar.add(btnDoiTrangThai);

        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setOpaque(false);
        north.add(title, BorderLayout.NORTH);
        north.add(toolbar, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        String[] columns = {"Mã NCC", "Tên nhà cung cấp", "Điện thoại", "Email", "Địa chỉ", "Trạng thái"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblNhaCungCap = new JTable(tableModel);
        tblNhaCungCap.setRowHeight(29);
        tblNhaCungCap.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblNhaCungCap.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) capNhatTrangThaiNut();
        });
        tblNhaCungCap.getTableHeader().setReorderingAllowed(false);
        tblNhaCungCap.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(tblNhaCungCap);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 5));
        south.setOpaque(false);
        lblTrangThai.setForeground(MUTED);
        south.add(lblTrangThai, BorderLayout.NORTH);
        JLabel note = new JLabel("Ngừng hợp tác chỉ đổi trạng thái; không xóa nhà cung cấp để giữ nguyên tham chiếu lô hàng/chứng từ.", SwingConstants.LEFT);
        note.setForeground(MUTED);
        note.setFont(note.getFont().deriveFont(11f));
        south.add(note, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        NhaCungCapService service = new NhaCungCapService(phien);
        new NhaCungCapController(this, service);
    }

    public void hienDanhSach(List<NhaCungCap> danhSach) {
        dsNhaCungCap = new ArrayList<>(danhSach);
        tableModel.setRowCount(0);
        for (NhaCungCap ncc : dsNhaCungCap) {
            tableModel.addRow(new Object[]{ncc.getMaNhaCungCap(), ncc.getTenNhaCungCap(),
                    ncc.getSoDienThoai(), ncc.getEmail() == null ? "—" : ncc.getEmail(),
                    ncc.getDiaChi() == null ? "—" : ncc.getDiaChi(), ncc.getTrangThaiHienThi()});
        }
        setTrangThai("Hiển thị " + dsNhaCungCap.size() + " nhà cung cấp (tối đa 500).", MUTED);
        capNhatTrangThaiNut();
    }

    public NhaCungCap layNhaCungCapDangChon() {
        int row = tblNhaCungCap.getSelectedRow();
        if (row < 0) return null;
        int modelRow = tblNhaCungCap.convertRowIndexToModel(row);
        return modelRow >= 0 && modelRow < dsNhaCungCap.size() ? dsNhaCungCap.get(modelRow) : null;
    }

    public void datQuyen(boolean coTheQuanLy) {
        this.coTheQuanLy = coTheQuanLy;
        capNhatTrangThaiNut();
    }

    public void datDangTai(boolean dangTai) {
        this.dangTai = dangTai;
        btnTim.setEnabled(!dangTai);
        btnTaiLai.setEnabled(!dangTai);
        cmbTrangThai.setEnabled(!dangTai);
        txtTimKiem.setEnabled(!dangTai);
        capNhatTrangThaiNut();
        if (dangTai) setTrangThai("Đang tải danh sách nhà cung cấp...", MUTED);
    }

    private void capNhatTrangThaiNut() {
        NhaCungCap selected = layNhaCungCapDangChon();
        btnThem.setEnabled(coTheQuanLy && !dangTai);
        btnSua.setEnabled(coTheQuanLy && !dangTai && selected != null);
        btnDoiTrangThai.setEnabled(coTheQuanLy && !dangTai && selected != null);
        if (selected == null) btnDoiTrangThai.setText("Ngừng hợp tác");
        else btnDoiTrangThai.setText(NhaCungCap.NGUNG_HOP_TAC.equals(selected.getTrangThai())
                ? "Khôi phục hợp tác" : "Ngừng hợp tác");
    }

    public String getTuKhoa() { return txtTimKiem.getText(); }
    public String getTrangThaiLoc() {
        return switch (cmbTrangThai.getSelectedIndex()) {
            case 1 -> NhaCungCap.DANG_HOP_TAC;
            case 2 -> NhaCungCap.NGUNG_HOP_TAC;
            default -> "";
        };
    }
    public JButton getBtnTim() {
        return btnTim; }
    public JButton getBtnTaiLai() {
        return btnTaiLai; }
    public JButton getBtnThem() {
        return btnThem; }
    public JButton getBtnSua() {
        return btnSua; }
    public JButton getBtnDoiTrangThai() {
        return btnDoiTrangThai; }
    public JTextField getTxtTimKiem() {
        return txtTimKiem; }
    public JTable getTblNhaCungCap() {
        return tblNhaCungCap; }

    public void setTrangThai(String message, Color color) {
        lblTrangThai.setText(message);
        lblTrangThai.setForeground(color);
    }

    public void hienLoi(String message) {
        setTrangThai(message, new Color(176, 32, 32));
        JOptionPane.showMessageDialog(this, message, "Thông báo", JOptionPane.ERROR_MESSAGE);
    }
}
