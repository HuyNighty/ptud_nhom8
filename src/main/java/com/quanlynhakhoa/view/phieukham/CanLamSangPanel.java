package com.quanlynhakhoa.view.phieukham;

import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.controller.phieukham.CanLamSangController;
import com.quanlynhakhoa.model.phieukham.ThongTinChiDinhCanLamSang;
import com.quanlynhakhoa.model.phieukham.DichVuCanLamSangChon;
import com.quanlynhakhoa.model.phieukham.DuLieuCanLamSang;
import com.quanlynhakhoa.model.phieukham.ThongTinPhieuKham;
import com.quanlynhakhoa.service.phieukham.CanLamSangService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;

public final class CanLamSangPanel extends JPanel {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(97, 115, 137);
    private static final Color BORDER = new Color(225, 233, 241);
    private static final Color BACKGROUND = new Color(245, 248, 252);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DesktopSession session;
    private final JComboBox<DichVuCanLamSangChon> cmbDichVu = new JComboBox<>();
    private final JTextArea txaYeuCau = textArea(3);
    private final JTextArea txaYeuCauChiTiet = textArea(2);
    private final JTextArea txaKetQua = textArea(5);
    private final JButton btnTaoChiDinh = button("Tạo chỉ định", true);
    private final JButton btnLuuKetQua = button("Lưu kết quả", true);
    private final JLabel lblTrangThai = label("Chọn phiếu khám để tải dữ liệu cận lâm sàng.", 11, MUTED, false);
    private final JLabel lblMaPhieu = label("Phiếu khám: —", 11, NAVY, true);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Mã chỉ định", "Dịch vụ", "Thời gian", "Trạng thái"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable tblChiDinh = new JTable(tableModel);
    private final List<ThongTinChiDinhCanLamSang> rows = new ArrayList<>();
    private final CanLamSangController controller;
    private ThongTinPhieuKham current;
    private String selectedDoctorId = "";
    private boolean loading;

    public CanLamSangPanel(DesktopSession session) {
        this.session = java.util.Objects.requireNonNull(session, "session");
        setLayout(new BorderLayout(0, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(12, 8, 8, 8));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        tblChiDinh.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblChiDinh.setRowHeight(28);
        tblChiDinh.setFillsViewportHeight(true);
        tblChiDinh.setShowVerticalLines(false);
        tblChiDinh.setGridColor(BORDER);
        tblChiDinh.getTableHeader().setReorderingAllowed(false);
        tblChiDinh.getColumnModel().getColumn(0).setPreferredWidth(90);
        tblChiDinh.getColumnModel().getColumn(1).setPreferredWidth(180);
        tblChiDinh.getColumnModel().getColumn(2).setPreferredWidth(120);
        tblChiDinh.getColumnModel().getColumn(3).setPreferredWidth(105);
        txaKetQua.setEditable(false);
        txaYeuCauChiTiet.setEditable(false);
        btnTaoChiDinh.setEnabled(false);
        btnLuuKetQua.setEnabled(false);
        controller = new CanLamSangController(this, new CanLamSangService(session));
        btnTaoChiDinh.addActionListener(event -> controller.taoChiDinh());
        btnLuuKetQua.addActionListener(event -> controller.luuKetQua());
        tblChiDinh.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) hienChiTietDangChon();
        });
        updateControls();
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setOpaque(false);
        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.add(label("CHỈ ĐỊNH CẬN LÂM SÀNG", 12, NAVY, true));
        title.add(Box.createVerticalStrut(4));
        title.add(lblMaPhieu);
        panel.add(title, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(8, 10));
        body.setOpaque(false);
        body.add(buildOrderForm(), BorderLayout.NORTH);
        body.add(buildOrderList(), BorderLayout.CENTER);
        body.add(buildResultForm(), BorderLayout.SOUTH);
        return body;
    }

    private JPanel buildOrderForm() {
        JPanel card = card(new BorderLayout(8, 8));
        card.add(label("TẠO CHỈ ĐỊNH MỚI", 11, NAVY, true), BorderLayout.NORTH);
        JPanel fields = new JPanel(new BorderLayout(0, 6));
        fields.setOpaque(false);
        cmbDichVu.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cmbDichVu.setPreferredSize(new Dimension(220, 30));
        fields.add(field("Dịch vụ cận lâm sàng", cmbDichVu), BorderLayout.NORTH);
        JScrollPane requestScroll = new JScrollPane(txaYeuCau);
        requestScroll.setPreferredSize(new Dimension(260, 62));
        fields.add(field("Yêu cầu / ghi chú (tối đa 300 ký tự)", requestScroll), BorderLayout.CENTER);
        card.add(fields, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(btnTaoChiDinh);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildOrderList() {
        JPanel card = card(new BorderLayout(0, 7));
        card.add(label("DANH SÁCH CHỈ ĐỊNH", 11, NAVY, true), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(tblChiDinh);
        scroll.setPreferredSize(new Dimension(420, 125));
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildResultForm() {
        JPanel card = card(new BorderLayout(8, 6));
        card.add(label("KẾT QUẢ CHỈ ĐỊNH ĐANG CHỌN", 11, NAVY, true), BorderLayout.NORTH);
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JScrollPane requestScroll = new JScrollPane(txaYeuCauChiTiet);
        requestScroll.setPreferredSize(new Dimension(260, 48));
        JScrollPane resultScroll = new JScrollPane(txaKetQua);
        resultScroll.setPreferredSize(new Dimension(260, 78));
        content.add(field("Yêu cầu đã gửi", requestScroll));
        content.add(Box.createVerticalStrut(6));
        content.add(field("Kết quả (tối đa 1000 ký tự)", resultScroll));
        card.add(content, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(btnLuuKetQua);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(lblTrangThai, BorderLayout.CENTER);
        return footer;
    }

    private void hienChiTietDangChon() {
        int selected = tblChiDinh.getSelectedRow();
        if (selected < 0) {
            txaYeuCauChiTiet.setText("");
            txaKetQua.setText("");
            txaYeuCauChiTiet.setEditable(false);
            txaKetQua.setEditable(false);
            btnLuuKetQua.setEnabled(false);
            return;
        }
        int modelRow = tblChiDinh.convertRowIndexToModel(selected);
        if (modelRow < 0 || modelRow >= rows.size()) return;
        ThongTinChiDinhCanLamSang order = rows.get(modelRow);
        txaYeuCauChiTiet.setText(safe(order.yeuCau()));
        txaKetQua.setText(order.ketQua() == null ? "" : order.ketQua());
        boolean pending = "CHO_THUC_HIEN".equals(order.trangThai());
        txaYeuCauChiTiet.setEditable(false);
        txaKetQua.setEditable(pending && canEdit() && isOpenVisit() && !loading);
        btnLuuKetQua.setEnabled(pending && canEdit() && isOpenVisit() && !loading);
    }

    public void setContext(ThongTinPhieuKham phieu, String maBacSiPhuTrach) {
        current = phieu;
        selectedDoctorId = maBacSiPhuTrach == null ? "" : maBacSiPhuTrach.trim();
        if (phieu == null) {
            controller.invalidateLoads();
            loading = false;
            lblMaPhieu.setText("Phiếu khám: —");
            clearData();
            lblTrangThai.setText("Chọn phiếu khám để tải dữ liệu cận lâm sàng.");
            updateControls();
            return;
        }
        lblMaPhieu.setText("Phiếu khám: " + phieu.maPhieuKham()
                + "  ·  Bác sĩ: " + safe(phieu.tenBacSi()));
        clearData();
        if (controller != null) controller.taiDuLieu();
    }

    /** Nhận lựa chọn bác sĩ hiện tại từ màn hình Phiếu khám. Service vẫn xác minh với DB. */
    public void setMaBacSiPhuTrach(String maBacSiPhuTrach) {
        selectedDoctorId = maBacSiPhuTrach == null ? "" : maBacSiPhuTrach.trim();
        updateControls();
    }

    private void clearData() {
        cmbDichVu.removeAllItems();
        rows.clear();
        tableModel.setRowCount(0);
        txaYeuCau.setText("");
        txaYeuCauChiTiet.setText("");
        txaKetQua.setText("");
        txaYeuCauChiTiet.setEditable(false);
        txaKetQua.setEditable(false);
        btnLuuKetQua.setEnabled(false);
    }

    public void hienDuLieu(DuLieuCanLamSang data) {
        cmbDichVu.removeAllItems();
        for (DichVuCanLamSangChon service : data.dichVu()) cmbDichVu.addItem(service);
        rows.clear();
        rows.addAll(data.chiDinh());
        tableModel.setRowCount(0);
        for (ThongTinChiDinhCanLamSang order : rows) {
            LocalDateTime orderedAt = order.thoiGianChiDinh();
            tableModel.addRow(new Object[]{
                    order.maChiDinh(), order.tenDichVu(),
                    orderedAt == null ? "—" : DATE_TIME.format(orderedAt),
                    tenTrangThai(order.trangThai())});
        }
        if (!rows.isEmpty()) tblChiDinh.setRowSelectionInterval(0, 0);
        else hienChiTietDangChon();
        updateControls();
    }

    public void setLoading(boolean value, String message) {
        loading = value;
        lblTrangThai.setText(message == null ? "" : message);
        updateControls();
    }

    public void showMessage(String message) { lblTrangThai.setText(message); }
    public void clearRequestInput() { txaYeuCau.setText(""); }

    public void showError(String message) {
        lblTrangThai.setText(message);
        javax.swing.JOptionPane.showMessageDialog(this, message, "Cận lâm sàng",
                javax.swing.JOptionPane.WARNING_MESSAGE);
    }

    private void updateControls() {
        boolean open = isOpenVisit();
        boolean writable = canEdit() && open && current != null && !loading;
        cmbDichVu.setEnabled(writable && cmbDichVu.getItemCount() > 0);
        txaYeuCau.setEditable(writable);
        btnTaoChiDinh.setEnabled(writable && cmbDichVu.getItemCount() > 0);
        if (tblChiDinh.getSelectedRow() >= 0) hienChiTietDangChon();
        else btnLuuKetQua.setEnabled(false);
        if (!open && current != null) {
            lblTrangThai.setText("Phiếu đã kết thúc; chỉ xem lịch sử chỉ định.");
        }
    }

    private boolean isOpenVisit() {
        return current != null && ("CHO_KHAM".equals(current.trangThai())
                || "DANG_KHAM".equals(current.trangThai()));
    }

    private boolean canEdit() {
        if (!session.isActive()) return false;
        if (session.role() == VaiTroHeThong.DOCTOR) return true;
        return session.role() == VaiTroHeThong.SYSTEM_ADMIN && current != null
                && selectedDoctorId.equals(current.maNhanVien());
    }

    public String getMaPhieuKham() { return current == null ? "" : current.maPhieuKham(); }
    public String getMaBacSiPhuTrach() { return selectedDoctorId; }
    public String getMaDichVu() {
        DichVuCanLamSangChon selected = (DichVuCanLamSangChon) cmbDichVu.getSelectedItem();
        return selected == null ? "" : selected.maDichVu();
    }
    public String getYeuCau() { return txaYeuCau.getText(); }
    public String getKetQua() { return txaKetQua.getText(); }
    public String getMaChiDinhDangChon() {
        int selected = tblChiDinh.getSelectedRow();
        if (selected < 0) return "";
        int modelRow = tblChiDinh.convertRowIndexToModel(selected);
        return modelRow >= 0 && modelRow < rows.size() ? rows.get(modelRow).maChiDinh() : "";
    }
    public boolean xacNhan(String message) {
        return javax.swing.JOptionPane.showConfirmDialog(this, message, "Xác nhận",
                javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE)
                == javax.swing.JOptionPane.YES_OPTION;
    }

    private static JPanel card(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(9, 10, 9, 10)));
        return panel;
    }
    private static JPanel field(String title, java.awt.Component component) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(label(title, 10, MUTED, true), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }
    private static JTextArea textArea(int rows) {
        JTextArea area = new JTextArea(rows, 24);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("SansSerif", Font.PLAIN, 12));
        area.setBorder(new EmptyBorder(6, 7, 6, 7));
        return area;
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
    private static String tenTrangThai(String status) {
        return switch (status == null ? "" : status) {
            case "CHO_THUC_HIEN" -> "Chờ thực hiện";
            case "DA_CO_KET_QUA" -> "Đã có kết quả";
            default -> "Không xác định";
        };
    }
    private static String safe(String value) { return value == null || value.isBlank() ? "—" : value; }
}
