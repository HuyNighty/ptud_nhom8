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
    private final JButton btnTim = new JButton("Tìm"), btnTaiLai = new JButton("Làm mới");
    private final JTextField txtMaDichVu = new JTextField(), txtTenDichVu = new JTextField(), txtDonGia = new JTextField();
    private final JTextArea txaMoTa = new JTextArea(3, 22);
    private final JCheckBox chkLaCanLamSang = new JCheckBox("Cận lâm sàng"), chkDangApDung = new JCheckBox("Đang áp dụng", true);
    private final JButton btnThem = new JButton("Thêm mới"), btnXoaTrang = new JButton("Xóa trắng"), btnLuu = new JButton("Lưu dịch vụ");
    private final JButton btnNgungApDung = new JButton("Ngừng áp dụng"), btnApDungLai = new JButton("Áp dụng lại");
    private final JLabel lblTrangThai = new JLabel("Chọn dịch vụ để sửa hoặc bấm Thêm mới.");
    private final JPanel pnlForm = new JPanel(new BorderLayout(8,8));
    private final DefaultTableModel tableModel = BillingUi.model("Mã dịch vụ", "Tên dịch vụ", "Giá hiện hành", "Cận lâm sàng", "Trạng thái");
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
        JPanel pnlTim = BillingUi.filters(BillingUi.field("Mã hoặc tên dịch vụ",txtTuKhoa),BillingUi.field("Trạng thái sử dụng",cmbTrangThai),BillingUi.field(" ",BillingUi.actions(btnTim,btnTaiLai)));
        pnlTop.add(pnlTim, BorderLayout.SOUTH); add(pnlTop, BorderLayout.NORTH);
        pnlForm.setPreferredSize(new Dimension(350,0));pnlForm.setBorder(BorderFactory.createTitledBorder("Thêm dịch vụ mới"));
        JPanel pnlGroup=new JPanel(new BorderLayout(0,4));
        JPanel pnlFields = new JPanel(new BorderLayout(0,4));
        JPanel pnlInputs=new JPanel(new GridLayout(0,1,0,4));
        pnlInputs.add(BillingUi.field("Mã dịch vụ",txtMaDichVu));
        pnlInputs.add(BillingUi.field("Tên dịch vụ",txtTenDichVu));
        pnlInputs.add(BillingUi.field("Đơn giá (VND)",txtDonGia));pnlFields.add(pnlInputs,BorderLayout.CENTER);
        JPanel pnlFlags=new JPanel(new GridLayout(1,2));pnlFlags.add(chkLaCanLamSang);pnlFlags.add(chkDangApDung);pnlFields.add(pnlFlags,BorderLayout.SOUTH);
        txtMaDichVu.setToolTipText("1–10 ký tự chữ/số không dấu. Mã được khóa khi sửa.");txtTenDichVu.setToolTipText("Tối đa 100 ký tự");BillingUi.moneyField(txtDonGia);
        pnlGroup.add(pnlFields, BorderLayout.NORTH);
        txaMoTa.setLineWrap(true); txaMoTa.setWrapStyleWord(true);
        JScrollPane scrMoTa = new JScrollPane(txaMoTa);
        scrMoTa.setBorder(BorderFactory.createTitledBorder("Mô tả"));txaMoTa.setToolTipText("Tối đa 300 ký tự");
        scrMoTa.setPreferredSize(new Dimension(320,120));scrMoTa.setMinimumSize(new Dimension(100,60));
        pnlGroup.add(scrMoTa, BorderLayout.CENTER);
        JPanel pnlActions = new JPanel(new BorderLayout(0,4));JPanel pnlSave=new JPanel(new GridLayout(1,0,6,0));
        if(mode!=EditorMode.PRICES)pnlSave.add(btnThem);
        pnlSave.add(btnXoaTrang);pnlSave.add(btnLuu);pnlActions.add(pnlSave,BorderLayout.NORTH);
        JPanel pnlState=new JPanel(new CardLayout());pnlState.add(btnNgungApDung,"stop");pnlState.add(btnApDungLai,"start");pnlState.add(new JPanel(),"new");pnlActions.add(pnlState,BorderLayout.SOUTH);
        BillingUi.primary(btnLuu);
        btnThem.setVisible(mode!=EditorMode.PRICES);
        pnlGroup.add(pnlActions, BorderLayout.SOUTH);pnlForm.add(BillingUi.topAligned(pnlGroup),BorderLayout.CENTER);
        JScrollPane scrDanhSach = BillingUi.scroll(tblDichVu);
        scrDanhSach.setMinimumSize(new Dimension(200,80));
        JPanel pnlBody = new JPanel(new BorderLayout(12, 0));
        pnlBody.add(scrDanhSach, BorderLayout.CENTER); pnlBody.add(pnlForm, BorderLayout.EAST); add(pnlBody, BorderLayout.CENTER);
        JPanel pnlBottom = new JPanel(new GridLayout(0, 1, 0, 6));
        lblTrangThai.setToolTipText("Ngừng áp dụng để giữ lịch sử. Đổi giá hiện hành không đổi giá trên hóa đơn đã lập.");
        pnlBottom.add(lblTrangThai); add(pnlBottom, BorderLayout.SOUTH);
        controller = new DichVuController(this, session, new DichVuService(session, connections));
        clearForm();
        BillingUi.style(this);
        BillingUi.columns(tblDichVu,110,270,170,145,170);
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
        mode==EditorMode.SERVICES?(selected==null?java.math.BigDecimal.ZERO:selected.donGia()):BillingUi.moneyInput(txtDonGia.getText()), txaMoTa.getText(), chkLaCanLamSang.isSelected(), chkDangApDung.isSelected());
    }
    public void showRows(List<DichVu> values) {
        rows = List.of(); tableModel.setRowCount(0); rows = List.copyOf(values);
        for (DichVu value : rows) tableModel.addRow(new Object[]{value.maDichVu(), value.tenDichVu(), value.donGia(),
                value.laCanLamSang() ? "Có" : "Không", value.dangApDung() ? "Đang áp dụng" : "Ngừng áp dụng"});
        clearForm();
        BillingUi.tableState(tblDichVu,values.isEmpty()?(keyword().isBlank()&&activeFilter()==null?"Nhập thông tin dịch vụ bên phải rồi bấm Lưu dịch vụ mới.":"Không tìm thấy dịch vụ phù hợp."):"");
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
        txtTenDichVu.setText(value.tenDichVu()); txtDonGia.setText(BillingUi.moneyText(value.donGia()));
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
        CardLayout states=(CardLayout)btnNgungApDung.getParent().getLayout();states.show(btnNgungApDung.getParent(),selected==null?"new":!selected.dangApDung()?"start":"stop");
        btnNgungApDung.getParent().setVisible(selected!=null);
        pnlForm.setBorder(BorderFactory.createTitledBorder(selected==null?"Thêm mới dịch vụ":"Chỉnh sửa · "+selected.maDichVu()));
        btnLuu.setText(selected==null?"Lưu mới":"Cập nhật");
        if(busy)BillingUi.tableState(tblDichVu,"Đang tải dịch vụ…");
    }
    public void status(String text) { lblTrangThai.setText(text);lblTrangThai.setToolTipText(text);if(tableModel.getRowCount()==0){BillingUi.tableState(tblDichVu,text.startsWith("Chưa có dịch vụ")?"Nhập thông tin dịch vụ bên phải rồi lưu.":text);if(text.startsWith("Chưa có dịch vụ"))lblTrangThai.setText("Đang thêm mới dịch vụ.");else if(text.startsWith("Không tìm"))lblTrangThai.setText("Điều chỉnh bộ lọc để tìm lại.");} }
    public void expired() { showRows(List.of()); setBusy(true); status("Phiên đã đăng xuất."); }
}
