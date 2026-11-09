package com.quanlynhakhoa.nghiepvu.dichvu.ui;

import com.quanlynhakhoa.enums.CheDoSuaDanhMucDichVu;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.dao.hoadon.ConnectionProvider;
import com.quanlynhakhoa.view.hoadon.GiaoDienHoaDon;
import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.nghiepvu.dichvu.controller.ServiceCatalogController;
import com.quanlynhakhoa.entity.DichVuNhaKhoa;
import com.quanlynhakhoa.nghiepvu.dichvu.service.ServiceCatalogService;
import com.quanlynhakhoa.hethong.phanquyen.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

public final class ServiceCatalogPanel extends JPanel implements com.quanlynhakhoa.hethong.tinhnang.RefreshableFeature {

    private final CheDoSuaDanhMucDichVu mode;
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
    private final DefaultTableModel tableModel = GiaoDienHoaDon.model("Mã dịch vụ", "Tên dịch vụ", "Giá hiện hành", "Cận lâm sàng", "Trạng thái");
    private final JTable tblDichVu = GiaoDienHoaDon.table(tableModel);
    private final ServiceCatalogController controller;
    private List<DichVuNhaKhoa> rows = List.of();
    private DichVuNhaKhoa selected;
    private boolean loaded;

    public ServiceCatalogPanel(DesktopSession session) { this(session, TeamDevConnection::open); }
    public ServiceCatalogPanel(DesktopSession session, ConnectionProvider connections) {
        this(session,connections,CheDoSuaDanhMucDichVu.COMBINED);
    }
    public ServiceCatalogPanel(DesktopSession session, ConnectionProvider connections,CheDoSuaDanhMucDichVu mode) {
        this.mode=mode;
        new AuthorizationService(new RolePermissionPolicy()).require(session, Quyen.SERVICE_CATALOG_MANAGE);
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(24, 26, 24, 26));
        JPanel pnlTop = new JPanel(new BorderLayout(0, 8));
        JLabel lblTitle = new JLabel(mode==CheDoSuaDanhMucDichVu.COMBINED?"Quản lí dịch vụ":mode==CheDoSuaDanhMucDichVu.SERVICES?"Dịch vụ":"Bảng giá dịch vụ");
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD, 22f));
        pnlTop.add(lblTitle, BorderLayout.NORTH);
        JPanel pnlTim = GiaoDienHoaDon.filters(GiaoDienHoaDon.field("Mã hoặc tên dịch vụ",txtTuKhoa),GiaoDienHoaDon.field("Trạng thái sử dụng",cmbTrangThai),GiaoDienHoaDon.field(" ",GiaoDienHoaDon.actions(btnTim,btnTaiLai)));
        pnlTop.add(pnlTim, BorderLayout.SOUTH); add(pnlTop, BorderLayout.NORTH);
        pnlForm.setPreferredSize(new Dimension(350,0));pnlForm.setBorder(BorderFactory.createTitledBorder("Thêm dịch vụ mới"));
        JPanel pnlGroup=new JPanel(new BorderLayout(0,4));
        JPanel pnlFields = new JPanel(new BorderLayout(0,4));
        JPanel pnlInputs=new JPanel(new GridLayout(0,1,0,4));
        pnlInputs.add(GiaoDienHoaDon.field("Mã dịch vụ",txtMaDichVu));
        pnlInputs.add(GiaoDienHoaDon.field("Tên dịch vụ",txtTenDichVu));
        pnlInputs.add(GiaoDienHoaDon.field("Đơn giá (VND)",txtDonGia));pnlFields.add(pnlInputs,BorderLayout.CENTER);
        JPanel pnlFlags=new JPanel(new GridLayout(1,2));pnlFlags.add(chkLaCanLamSang);pnlFlags.add(chkDangApDung);pnlFields.add(pnlFlags,BorderLayout.SOUTH);
        txtMaDichVu.setToolTipText("1–10 ký tự chữ/số không dấu. Mã được khóa khi sửa.");txtTenDichVu.setToolTipText("Tối đa 100 ký tự");GiaoDienHoaDon.moneyField(txtDonGia);
        pnlGroup.add(pnlFields, BorderLayout.NORTH);
        txaMoTa.setLineWrap(true); txaMoTa.setWrapStyleWord(true);
        JScrollPane scrMoTa = new JScrollPane(txaMoTa);
        scrMoTa.setBorder(BorderFactory.createTitledBorder("Mô tả"));txaMoTa.setToolTipText("Tối đa 300 ký tự");
        scrMoTa.setPreferredSize(new Dimension(320,120));scrMoTa.setMinimumSize(new Dimension(100,60));
        pnlGroup.add(scrMoTa, BorderLayout.CENTER);
        JPanel pnlActions = new JPanel(new BorderLayout(0,4));JPanel pnlSave=new JPanel(new GridLayout(1,0,6,0));
        if(mode!=CheDoSuaDanhMucDichVu.PRICES)pnlSave.add(btnThem);
        pnlSave.add(btnXoaTrang);pnlSave.add(btnLuu);pnlActions.add(pnlSave,BorderLayout.NORTH);
        JPanel pnlState=new JPanel(new CardLayout());pnlState.add(btnNgungApDung,"stop");pnlState.add(btnApDungLai,"start");pnlState.add(new JPanel(),"new");pnlActions.add(pnlState,BorderLayout.SOUTH);
        GiaoDienHoaDon.primary(btnLuu);GiaoDienHoaDon.danger(btnNgungApDung);
        btnThem.setVisible(mode!=CheDoSuaDanhMucDichVu.PRICES);
        pnlGroup.add(pnlActions, BorderLayout.SOUTH);pnlForm.add(GiaoDienHoaDon.topAligned(pnlGroup),BorderLayout.CENTER);
        JScrollPane scrDanhSach = GiaoDienHoaDon.scroll(tblDichVu);
        scrDanhSach.setMinimumSize(new Dimension(200,80));
        JPanel pnlBody = new JPanel(new BorderLayout(12, 0));
        pnlBody.add(scrDanhSach, BorderLayout.CENTER); pnlBody.add(pnlForm, BorderLayout.EAST); add(pnlBody, BorderLayout.CENTER);
        JPanel pnlBottom = new JPanel(new GridLayout(0, 1, 0, 6));
        lblTrangThai.setToolTipText("Ngừng áp dụng để giữ lịch sử. Đổi giá hiện hành không đổi giá trên hóa đơn đã lập.");
        pnlBottom.add(lblTrangThai); add(pnlBottom, BorderLayout.SOUTH);
        controller = new ServiceCatalogController(this, session, new ServiceCatalogService(session, connections));
        clearForm();
        GiaoDienHoaDon.style(this);
        GiaoDienHoaDon.columns(tblDichVu,110,270,170,145,170);
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
    public CheDoSuaDanhMucDichVu editorMode(){return mode;}
    @Override public void refreshData(){controller.load(false);}
    public void onSave(ActionListener listener) { btnLuu.addActionListener(listener); }
    public void onDeactivate(ActionListener listener) { btnNgungApDung.addActionListener(listener); }
    public void onActivate(ActionListener listener) { btnApDungLai.addActionListener(listener); }
    public void onSelect(ListSelectionListener listener) { tblDichVu.getSelectionModel().addListSelectionListener(listener); }
    public String keyword() { return txtTuKhoa.getText(); }
    public void clearFilters() {
        txtTuKhoa.setText("");
        cmbTrangThai.setSelectedIndex(0);
    }
    public void selectService(String id) {
        if (id == null) return;
        for (int index = 0; index < rows.size(); index++) {
            if (id.equals(rows.get(index).maDichVu())) {
                int viewIndex = tblDichVu.convertRowIndexToView(index);
                tblDichVu.setRowSelectionInterval(viewIndex, viewIndex);
                tblDichVu.scrollRectToVisible(tblDichVu.getCellRect(viewIndex, 0, true));
                showForm(rows.get(index));
                return;
            }
        }
    }
    public Boolean activeFilter() { return cmbTrangThai.getSelectedIndex() == 0 ? null : cmbTrangThai.getSelectedIndex() == 1; }
    public boolean creating() { return selected == null; }
    public DichVuNhaKhoa selected() { return selected; }
    public DichVuNhaKhoa input() {
        return new DichVuNhaKhoa(selected == null ? txtMaDichVu.getText() : selected.maDichVu(), txtTenDichVu.getText(),
        mode==CheDoSuaDanhMucDichVu.SERVICES?(selected==null?java.math.BigDecimal.ZERO:selected.donGia()):GiaoDienHoaDon.moneyInput(txtDonGia.getText()), txaMoTa.getText(), chkLaCanLamSang.isSelected(), chkDangApDung.isSelected());
    }
    public void showRows(List<DichVuNhaKhoa> values) {
        rows = List.of(); tableModel.setRowCount(0); rows = List.copyOf(values);
        for (DichVuNhaKhoa value : rows) tableModel.addRow(new Object[]{value.maDichVu(), value.tenDichVu(), value.donGia(),
                value.laCanLamSang() ? "Có" : "Không", value.dangApDung() ? "Đang áp dụng" : "Ngừng áp dụng"});
        clearForm();
        GiaoDienHoaDon.tableState(tblDichVu,values.isEmpty()?(keyword().isBlank()&&activeFilter()==null?"Nhập thông tin dịch vụ bên phải rồi bấm Lưu dịch vụ mới.":"Không tìm thấy dịch vụ phù hợp."):"");
    }
    public void selectRow() {
        int index = tblDichVu.getSelectedRow();
        if (index < 0) return;
        index = tblDichVu.convertRowIndexToModel(index);
        if (index >= 0 && index < rows.size()) showForm(rows.get(index));
    }
    public void showForm(DichVuNhaKhoa value) {
        selected = value;
        txtMaDichVu.setText(value.maDichVu()); txtMaDichVu.setEditable(false);
        txtTenDichVu.setText(value.tenDichVu()); txtDonGia.setText(GiaoDienHoaDon.moneyText(value.donGia()));
        txaMoTa.setText(value.moTa()); chkLaCanLamSang.setSelected(value.laCanLamSang()); chkDangApDung.setSelected(value.dangApDung());
        setBusy(false);
    }
    public void clearForm() {
        tblDichVu.clearSelection(); selected = null;
        txtMaDichVu.setText(""); txtTenDichVu.setText(""); txtDonGia.setText(mode==CheDoSuaDanhMucDichVu.SERVICES?"0":""); txaMoTa.setText("");
        chkLaCanLamSang.setSelected(false); chkDangApDung.setSelected(true); setBusy(false);
    }
    public void setBusy(boolean busy) {
        for (Component control : new Component[]{txtTuKhoa, cmbTrangThai, btnTim, btnTaiLai, txtMaDichVu,
                txtTenDichVu, txtDonGia, txaMoTa, chkLaCanLamSang, chkDangApDung, btnThem, btnXoaTrang, btnLuu, tblDichVu}) control.setEnabled(!busy);
        txtMaDichVu.setEditable(!busy && selected == null);
        if(mode==CheDoSuaDanhMucDichVu.SERVICES)txtDonGia.setEnabled(false);
        if(mode==CheDoSuaDanhMucDichVu.PRICES){txtMaDichVu.setEditable(false);txtTenDichVu.setEnabled(false);txaMoTa.setEnabled(false);chkLaCanLamSang.setEnabled(false);btnLuu.setEnabled(!busy&&selected!=null);}
        btnNgungApDung.setEnabled(!busy && selected != null && selected.dangApDung());
        btnApDungLai.setEnabled(!busy && selected != null && !selected.dangApDung());
        CardLayout states=(CardLayout)btnNgungApDung.getParent().getLayout();states.show(btnNgungApDung.getParent(),selected==null?"new":!selected.dangApDung()?"start":"stop");
        btnNgungApDung.getParent().setVisible(selected!=null);
        pnlForm.setBorder(BorderFactory.createTitledBorder(selected==null?"Thêm mới dịch vụ":"Chỉnh sửa · "+selected.maDichVu()));
        btnLuu.setText(selected==null?"Lưu mới":"Cập nhật");
        if(busy)GiaoDienHoaDon.tableState(tblDichVu,"Đang tải dịch vụ…");
    }
    public void status(String text) { lblTrangThai.setText(text);lblTrangThai.setToolTipText(text);if(tableModel.getRowCount()==0){GiaoDienHoaDon.tableState(tblDichVu,text.startsWith("Chưa có dịch vụ")?"Nhập thông tin dịch vụ bên phải rồi lưu.":text);if(text.startsWith("Chưa có dịch vụ"))lblTrangThai.setText("Đang thêm mới dịch vụ.");else if(text.startsWith("Không tìm"))lblTrangThai.setText("Điều chỉnh bộ lọc để tìm lại.");} }
    public void expired() { showRows(List.of()); setBusy(true); status("Phiên đã đăng xuất."); }
}
