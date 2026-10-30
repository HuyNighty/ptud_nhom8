package com.quanlynhakhoa.dichvu.ui;

import com.quanlynhakhoa.billing.dao.ConnectionProvider;
import com.quanlynhakhoa.billing.ui.BillingUi;
import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.dichvu.controller.DichVuController;
import com.quanlynhakhoa.dichvu.model.DichVu;
import com.quanlynhakhoa.dichvu.service.DichVuService;
import com.quanlynhakhoa.security.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

public final class DichVuPanel extends JPanel implements com.quanlynhakhoa.features.RefreshableFeature {
    public enum EditorMode { COMBINED, SERVICES, PRICES }
    private final EditorMode mode;
    private final JTextField txtTuKhoa = new JTextField(18);
    private final JComboBox<String> cmbTrangThai = new JComboBox<>(new String[]{"Tất cả", "Đang áp dụng", "Ngừng áp dụng"});
    private final JButton btnTim = new JButton("Tìm"), btnTaiLai = new JButton("Tải lại");
    private final JTextField txtMaDichVu = new JTextField(), txtTenDichVu = new JTextField(), txtDonGia = new JTextField();
    private final JTextArea txaMoTa = new JTextArea(3, 22);
    private final JCheckBox chkLaCanLamSang = new JCheckBox("Cận lâm sàng"), chkDangApDung = new JCheckBox("Đang áp dụng", true);
    private final JButton btnThem = new JButton("Thêm"), btnXoaTrang = new JButton("Xóa trắng"), btnLuu = new JButton("Lưu");
    private final JButton btnNgungApDung = new JButton("Ngừng áp dụng"), btnApDungLai = new JButton("Áp dụng lại");
    private final JLabel lblTrangThai = new JLabel("Bấm Tải lại để đọc database.");
    private final DefaultTableModel tableModel = BillingUi.model("Mã", "Tên dịch vụ", "Đơn giá", "Cận lâm sàng", "Trạng thái");
    private final JTable tblDichVu = BillingUi.table(tableModel);
    private final DichVuController controller;
    private List<DichVu> rows = List.of();
    private DichVu selected;
    private boolean loaded;

    public DichVuPanel(DesktopSession session) { this(session, TeamDevConnection::open); }
    public DichVuPanel(DesktopSession session, ConnectionProvider connections) {
        this(session,connections,EditorMode.COMBINED);
    }
    public DichVuPanel(DesktopSession session, ConnectionProvider connections,EditorMode mode) {
        this.mode=mode;
        new AuthorizationService(new RolePermissionPolicy()).require(session, Permission.SERVICE_CATALOG_MANAGE);
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel pnlTop = new JPanel(new BorderLayout(0, 8));
        JLabel lblTitle = new JLabel(mode==EditorMode.COMBINED?"DỊCH VỤ & BẢNG GIÁ":mode==EditorMode.SERVICES?"DỊCH VỤ":"BẢNG GIÁ DỊCH VỤ");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 22f));
        pnlTop.add(lblTitle, BorderLayout.NORTH);
        JPanel pnlTim = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlTim.add(new JLabel("Mã / tên")); pnlTim.add(txtTuKhoa); pnlTim.add(cmbTrangThai); pnlTim.add(btnTim); pnlTim.add(btnTaiLai);
        pnlTop.add(pnlTim, BorderLayout.SOUTH); add(pnlTop, BorderLayout.NORTH);
        JPanel pnlForm = new JPanel(new BorderLayout(8, 8));
        JPanel pnlFields = new JPanel(new GridLayout(0, 1, 4, 4));
        pnlFields.add(new JLabel("Mã (1–10 ký tự ASCII, khóa khi sửa)")); pnlFields.add(txtMaDichVu);
        pnlFields.add(new JLabel("Tên (tối đa 100 ký tự)")); pnlFields.add(txtTenDichVu);
        pnlFields.add(new JLabel("Đơn giá VND: chỉ chữ số, không dấu phân cách")); pnlFields.add(txtDonGia);
        pnlFields.add(chkLaCanLamSang); pnlFields.add(chkDangApDung);
        pnlForm.add(pnlFields, BorderLayout.NORTH);
        txaMoTa.setLineWrap(true); txaMoTa.setWrapStyleWord(true);
        JScrollPane scrMoTa = new JScrollPane(txaMoTa);
        scrMoTa.setBorder(BorderFactory.createTitledBorder("Mô tả (tối đa 300 ký tự)"));
        pnlForm.add(scrMoTa, BorderLayout.CENTER);
        JPanel pnlActions = new JPanel(new GridLayout(0, 2, 6, 6));
        if(mode!=EditorMode.PRICES)pnlActions.add(btnThem);
        pnlActions.add(btnXoaTrang); pnlActions.add(btnLuu);
        pnlActions.add(btnNgungApDung); pnlActions.add(btnApDungLai);
        btnThem.setVisible(mode!=EditorMode.PRICES);
        pnlForm.add(pnlActions, BorderLayout.SOUTH);
        JScrollPane scrDanhSach = BillingUi.scroll(tblDichVu);
        scrDanhSach.setPreferredSize(new Dimension(650, 360));
        JPanel pnlBody = new JPanel(new BorderLayout(12, 0));
        pnlBody.add(scrDanhSach, BorderLayout.CENTER); pnlBody.add(pnlForm, BorderLayout.EAST); add(pnlBody, BorderLayout.CENTER);
        JPanel pnlBottom = new JPanel(new GridLayout(0, 1, 0, 6));
        pnlBottom.add(new JLabel("Ngừng áp dụng thay cho xóa; giữ liên kết và giá trên hóa đơn lịch sử."));
        pnlBottom.add(new JLabel(mode==EditorMode.SERVICES?"Dịch vụ mới có giá 0; đặt đơn giá tại Danh mục → Bảng giá dịch vụ.":"Giá hiện tại dùng chung trạng thái áp dụng của dịch vụ; schema chưa có ngày hiệu lực hoặc lịch sử bảng giá."));
        pnlBottom.add(lblTrangThai); add(pnlBottom, BorderLayout.SOUTH);
        controller = new DichVuController(this, session, new DichVuService(session, connections));
        clearForm();
        BillingUi.style(this);
    }
    @Override public void addNotify() {
        super.addNotify();
        if (!loaded) { loaded = true; SwingUtilities.invokeLater(new Runnable() { public void run() { controller.load(false); } }); }
    }
    public void onSearch(ActionListener listener) { btnTim.addActionListener(listener); txtTuKhoa.addActionListener(listener); cmbTrangThai.addActionListener(listener); }
    public void onReload(ActionListener listener) { btnTaiLai.addActionListener(listener); }
    public void onNew(ActionListener listener) { btnThem.addActionListener(listener); }
    public void onClear(ActionListener listener) { btnXoaTrang.addActionListener(listener); }
    public void focusNewCode() { txtMaDichVu.requestFocusInWindow(); }
    public EditorMode editorMode(){return mode;}
    @Override public void refreshData(){controller.load(false);}
    public void onSave(ActionListener listener) { btnLuu.addActionListener(listener); }
    public void onDeactivate(ActionListener listener) { btnNgungApDung.addActionListener(listener); }
    public void onActivate(ActionListener listener) { btnApDungLai.addActionListener(listener); }
    public void onSelect(ListSelectionListener listener) { tblDichVu.getSelectionModel().addListSelectionListener(listener); }
    public String keyword() { return txtTuKhoa.getText(); }
    public Boolean activeFilter() { return cmbTrangThai.getSelectedIndex() == 0 ? null : cmbTrangThai.getSelectedIndex() == 1; }
    public boolean creating() { return selected == null; }
    public DichVu selected() { return selected; }
    public DichVu input() {
        return new DichVu(selected == null ? txtMaDichVu.getText() : selected.maDichVu(), txtTenDichVu.getText(),
        mode==EditorMode.SERVICES?(selected==null?java.math.BigDecimal.ZERO:selected.donGia()):DichVuService.parsePrice(txtDonGia.getText()), txaMoTa.getText(), chkLaCanLamSang.isSelected(), chkDangApDung.isSelected());
    }
    public void showRows(List<DichVu> values) {
        rows = List.of(); tableModel.setRowCount(0); rows = List.copyOf(values);
        for (DichVu value : rows) tableModel.addRow(new Object[]{value.maDichVu(), value.tenDichVu(), value.donGia(),
                value.laCanLamSang() ? "Có" : "Không", value.dangApDung() ? "Đang áp dụng" : "Ngừng áp dụng"});
        clearForm();
    }
    public void selectRow() {
        int index = tblDichVu.getSelectedRow();
        if (index < 0) return;
        index = tblDichVu.convertRowIndexToModel(index);
        if (index >= 0 && index < rows.size()) showForm(rows.get(index));
    }
    public void showForm(DichVu value) {
        selected = value;
        txtMaDichVu.setText(value.maDichVu()); txtMaDichVu.setEditable(false);
        txtTenDichVu.setText(value.tenDichVu()); txtDonGia.setText(value.donGia().toPlainString());
        txaMoTa.setText(value.moTa()); chkLaCanLamSang.setSelected(value.laCanLamSang()); chkDangApDung.setSelected(value.dangApDung());
        setBusy(false);
    }
    public void clearForm() {
        tblDichVu.clearSelection(); selected = null;
        txtMaDichVu.setText(""); txtTenDichVu.setText(""); txtDonGia.setText(mode==EditorMode.SERVICES?"0":""); txaMoTa.setText("");
        chkLaCanLamSang.setSelected(false); chkDangApDung.setSelected(true); setBusy(false);
    }
    public void setBusy(boolean busy) {
        for (Component control : new Component[]{txtTuKhoa, cmbTrangThai, btnTim, btnTaiLai, txtMaDichVu,
                txtTenDichVu, txtDonGia, txaMoTa, chkLaCanLamSang, chkDangApDung, btnThem, btnXoaTrang, btnLuu, tblDichVu}) control.setEnabled(!busy);
        txtMaDichVu.setEditable(!busy && selected == null);
        if(mode==EditorMode.SERVICES)txtDonGia.setEnabled(false);
        if(mode==EditorMode.PRICES){txtMaDichVu.setEditable(false);txtTenDichVu.setEnabled(false);txaMoTa.setEnabled(false);chkLaCanLamSang.setEnabled(false);btnLuu.setEnabled(!busy&&selected!=null);}
        btnNgungApDung.setEnabled(!busy && selected != null && selected.dangApDung());
        btnApDungLai.setEnabled(!busy && selected != null && !selected.dangApDung());
    }
    public void status(String text) { lblTrangThai.setText(text); }
    public void expired() { showRows(List.of()); setBusy(true); status("Phiên đã đăng xuất."); }
}
