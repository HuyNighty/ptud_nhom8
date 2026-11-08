package com.quanlynhakhoa.nghiepvu.phieukham.view;

import com.quanlynhakhoa.quantri.vaitro.entity.Role;
import com.quanlynhakhoa.nghiepvu.phieukham.controller.DonThuocController;
import com.quanlynhakhoa.nghiepvu.phieukham.entity.ChiTietDonThuoc;
import com.quanlynhakhoa.nghiepvu.phieukham.entity.DonThuocData;
import com.quanlynhakhoa.nghiepvu.phieukham.entity.PhieuKham;
import com.quanlynhakhoa.nghiepvu.phieukham.entity.Thuoc;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;

public final class DonThuocPanel extends JPanel {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(97, 115, 137);
    private static final Color BORDER = new Color(225, 233, 241);
    private static final Color BACKGROUND = new Color(245, 248, 252);

    private final DesktopSession session;
    private final JComboBox<Thuoc> cmbThuoc = new JComboBox<>();
    private final JSpinner spnSoLuong = new JSpinner(new SpinnerNumberModel(1, 1, 10000, 1));
    private final JTextField txtLieuDung = new JTextField(18);
    private final JTextField txtCachDung = new JTextField(22);
    private final JSpinner spnSoNgayDung = new JSpinner(new SpinnerNumberModel(1, 1, 365, 1));
    private final JTextArea txaLoiDan = new JTextArea(2, 30);
    private final JButton btnThem = button("Thêm thuốc", false);
    private final JButton btnXoa = button("Xóa thuốc", false);
    private final JButton btnLuu = button("Lưu đơn thuốc", true);
    private final JLabel lblTrangThai = label("Chọn phiếu khám để tải danh mục thuốc.", 11, MUTED, false);
    private final JLabel lblPhieu = label("Phiếu khám: —", 11, NAVY, true);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Mã thuốc", "Tên thuốc", "SL", "Liều dùng", "Cách dùng", "Số ngày"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable tblChiTiet = new JTable(tableModel);
    private final List<ChiTietDonThuoc> cart = new ArrayList<>();
    private final DonThuocController controller;

    private PhieuKham current;
    private String selectedDoctorId = "";
    private boolean loading;
    private boolean alreadyPrescribed;

    public DonThuocPanel(DesktopSession session) {
        this.session = java.util.Objects.requireNonNull(session, "session");
        setLayout(new BorderLayout(0, 9));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(12, 8, 8, 8));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        tblChiTiet.setRowHeight(27);
        tblChiTiet.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblChiTiet.setFillsViewportHeight(true);
        tblChiTiet.setShowVerticalLines(false);
        tblChiTiet.setGridColor(BORDER);
        tblChiTiet.getTableHeader().setReorderingAllowed(false);
        tblChiTiet.getColumnModel().getColumn(0).setPreferredWidth(75);
        tblChiTiet.getColumnModel().getColumn(1).setPreferredWidth(150);
        tblChiTiet.getColumnModel().getColumn(2).setPreferredWidth(38);
        tblChiTiet.getColumnModel().getColumn(3).setPreferredWidth(105);
        tblChiTiet.getColumnModel().getColumn(4).setPreferredWidth(150);
        tblChiTiet.getColumnModel().getColumn(5).setPreferredWidth(55);
        tblChiTiet.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) updateControls();
        });

        txaLoiDan.setLineWrap(true);
        txaLoiDan.setWrapStyleWord(true);
        txaLoiDan.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txaLoiDan.setBorder(new EmptyBorder(6, 7, 6, 7));

        controller = new DonThuocController(this,
                new com.quanlynhakhoa.nghiepvu.phieukham.service.DonThuocService(session));
        btnThem.addActionListener(event -> controller.themThuoc());
        btnXoa.addActionListener(event -> xoaThuocDangChon());
        btnLuu.addActionListener(event -> controller.luuDonThuoc());
        updateControls();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.add(label("KÊ ĐƠN THUỐC", 12, NAVY, true));
        title.add(Box.createVerticalStrut(4));
        title.add(lblPhieu);
        header.add(title, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(8, 9));
        body.setOpaque(false);
        body.add(buildMedicineForm(), BorderLayout.NORTH);
        body.add(buildMedicineList(), BorderLayout.CENTER);
        body.add(buildInstructions(), BorderLayout.SOUTH);
        return body;
    }

    private JPanel buildMedicineForm() {
        JPanel card = card(new BorderLayout(7, 7));
        card.add(label("THÊM THUỐC VÀO ĐƠN", 11, NAVY, true), BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        cmbThuoc.setPreferredSize(new Dimension(280, 29));
        fields.add(field("Thuốc", cmbThuoc));
        fields.add(Box.createVerticalStrut(5));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        row.setOpaque(false);
        row.add(field("Số lượng", spnSoLuong));
        row.add(field("Liều dùng", txtLieuDung));
        row.add(field("Số ngày", spnSoNgayDung));
        fields.add(row);
        fields.add(Box.createVerticalStrut(5));
        fields.add(field("Cách dùng", txtCachDung));
        card.add(fields, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(btnThem);
        actions.add(btnXoa);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildMedicineList() {
        JPanel card = card(new BorderLayout(0, 6));
        card.add(label("CHI TIẾT ĐƠN THUỐC", 11, NAVY, true), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(tblChiTiet);
        scroll.setPreferredSize(new Dimension(540, 150));
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildInstructions() {
        JPanel card = card(new BorderLayout(0, 5));
        card.add(label("LỜI DẶN (tối đa 500 ký tự)", 11, NAVY, true), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(txaLoiDan);
        scroll.setPreferredSize(new Dimension(400, 54));
        card.add(scroll, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(btnLuu);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(lblTrangThai, BorderLayout.CENTER);
        return footer;
    }

    private JPanel field(String title, java.awt.Component component) {
        JPanel panel = new JPanel(new BorderLayout(0, 3));
        panel.setOpaque(false);
        panel.add(label(title, 10, MUTED, true), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    public void setContext(PhieuKham visit, String doctorId) {
        current = visit;
        selectedDoctorId = doctorId == null ? "" : doctorId.trim();
        if (visit == null) {
            controller.invalidateLoads();
            loading = false;
            alreadyPrescribed = false;
            lblPhieu.setText("Phiếu khám: —");
            clearData();
            lblTrangThai.setText("Chọn phiếu khám để tải danh mục thuốc.");
            updateControls();
            return;
        }
        lblPhieu.setText("Phiếu khám: " + visit.maPhieuKham()
                + "  ·  Bác sĩ: " + safe(visit.tenBacSi()));
        clearData();
        controller.taiDuLieu();
    }

    public void setMaBacSiPhuTrach(String doctorId) {
        selectedDoctorId = doctorId == null ? "" : doctorId.trim();
        updateControls();
    }

    private void clearData() {
        cmbThuoc.removeAllItems();
        cart.clear();
        tableModel.setRowCount(0);
        txaLoiDan.setText("");
        alreadyPrescribed = false;
        updateControls();
    }

    public void hienDuLieu(DonThuocData data) {
        cmbThuoc.removeAllItems();
        for (Thuoc medicine : data.danhSachThuoc()) cmbThuoc.addItem(medicine);
        cart.clear();
        cart.addAll(data.chiTiet());
        tableModel.setRowCount(0);
        for (ChiTietDonThuoc item : cart) addTableRow(item);
        txaLoiDan.setText(data.loiDan());
        alreadyPrescribed = data.daKeDon();
        if (alreadyPrescribed) {
            lblTrangThai.setText("Phiếu khám đã có đơn thuốc. Đơn hiện tại chỉ được xem.");
        } else if (data.danhSachThuoc().isEmpty()) {
            lblTrangThai.setText("Danh mục thuốc hiện chưa có dữ liệu.");
        } else {
            lblTrangThai.setText("Đã tải " + data.danhSachThuoc().size() + " thuốc.");
        }
        updateControls();
    }

    private void addTableRow(ChiTietDonThuoc item) {
        tableModel.addRow(new Object[]{item.maThuoc(), item.tenThuoc(), item.soLuong(),
                item.lieuDung(), item.cachDung(), item.soNgayDung()});
    }

    public ChiTietDonThuoc taoChiTietDangNhap() {
        Thuoc medicine = (Thuoc) cmbThuoc.getSelectedItem();
        if (medicine == null) throw new IllegalArgumentException("Chọn thuốc cần kê.");
        String dosage = txtLieuDung.getText() == null ? "" : txtLieuDung.getText().trim();
        String usage = txtCachDung.getText() == null ? "" : txtCachDung.getText().trim();
        if (dosage.isEmpty()) throw new IllegalArgumentException("Nhập liều dùng.");
        if (usage.isEmpty()) throw new IllegalArgumentException("Nhập cách dùng.");
        return new ChiTietDonThuoc(medicine.maThuoc(), medicine.tenThuoc(), medicine.donViTinh(),
                ((Number) spnSoLuong.getValue()).intValue(), dosage, usage,
                ((Number) spnSoNgayDung.getValue()).intValue(),
                medicine.kiemSoatDacBiet(), medicine.hoatChat());
    }

    public void themVaoDanhSach(ChiTietDonThuoc item) {
        for (ChiTietDonThuoc old : cart) {
            if (old.maThuoc().equals(item.maThuoc())) {
                throw new IllegalArgumentException("Thuốc này đã có trong đơn. Hãy xóa dòng cũ nếu muốn thay đổi.");
            }
        }
        cart.add(item);
        addTableRow(item);
        txtLieuDung.setText("");
        txtCachDung.setText("");
        spnSoLuong.setValue(1);
        spnSoNgayDung.setValue(1);
        updateControls();
    }

    private void xoaThuocDangChon() {
        int selected = tblChiTiet.getSelectedRow();
        if (selected < 0 || selected >= cart.size()) return;
        cart.remove(selected);
        tableModel.removeRow(selected);
        updateControls();
    }

    public List<ChiTietDonThuoc> layChiTietNhap() {
        return List.copyOf(cart);
    }

    public String getMaPhieuKham() { return current == null ? "" : current.maPhieuKham(); }
    public String getMaBacSiPhuTrach() { return selectedDoctorId; }
    public String getLoiDan() { return txaLoiDan.getText(); }

    public void setLoading(boolean value, String message) {
        loading = value;
        lblTrangThai.setText(message == null ? "" : message);
        updateControls();
    }

    public void showMessage(String message) { lblTrangThai.setText(message); }

    public void showError(String message) {
        lblTrangThai.setText(message);
        JOptionPane.showMessageDialog(this, message, "Đơn thuốc", JOptionPane.WARNING_MESSAGE);
    }

    public boolean xacNhanCanhBao(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Xác nhận cảnh báo thuốc",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void updateControls() {
        boolean open = current != null && ("CHO_KHAM".equals(current.trangThai())
                || "DANG_KHAM".equals(current.trangThai()));
        boolean roleAllowed = session.isActive() && (session.role() == Role.DOCTOR
                || (session.role() == Role.SYSTEM_ADMIN && current != null
                && selectedDoctorId.equals(current.maNhanVien())));
        boolean editable = open && roleAllowed && !loading && !alreadyPrescribed;
        cmbThuoc.setEnabled(editable && cmbThuoc.getItemCount() > 0);
        spnSoLuong.setEnabled(editable);
        txtLieuDung.setEnabled(editable);
        txtCachDung.setEnabled(editable);
        spnSoNgayDung.setEnabled(editable);
        txaLoiDan.setEditable(editable);
        btnThem.setEnabled(editable && cmbThuoc.getItemCount() > 0);
        btnXoa.setEnabled(editable && tblChiTiet.getSelectedRow() >= 0);
        btnLuu.setEnabled(editable && !cart.isEmpty());
    }

    private JPanel card(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(8, 9, 8, 9)));
        return panel;
    }

    private static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.putClientProperty("theme.primary", primary);
        button.setFont(new Font("SansSerif", Font.BOLD, 11));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBackground(primary ? NAVY : Color.WHITE);
        button.setForeground(primary ? Color.WHITE : NAVY);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? NAVY : BORDER),
                new EmptyBorder(6, 10, 6, 10)));
        return button;
    }

    private static JLabel label(String text, int size, Color color, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
