package com.quanlynhakhoa.view.hoadon;

import com.quanlynhakhoa.security.RolePermissionPolicy;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.model.hoadon.ThongTinHoaDon;
import com.quanlynhakhoa.model.hoadon.LuaChonLapHoaDon;
import com.quanlynhakhoa.controller.hoadon.ManHinhThanhToanController;
import com.quanlynhakhoa.config.ConnectionProvider;
import com.quanlynhakhoa.config.tinhnang.RefreshableFeature;
import java.awt.*;
import java.util.List;
import javax.swing.*;

/** Collection workspace: compact invoice picker, collection form and history, not an invoice list clone. */
public final class ThanhToanPanel extends JPanel implements RefreshableFeature {
    public final JTextField txtTimHoaDon=new JTextField(22);
    public final JButton btnTim=new JButton("Tìm hóa đơn"),btnTaiLai=new JButton("Làm mới");
    public final JComboBox<LuaChonLapHoaDon> cmbHoaDon=new JComboBox<>();
    public final JLabel lblTrangThai=new JLabel("Tìm mã hóa đơn, mã/tên/số điện thoại bệnh nhân; chọn chứng từ để thu tiền.");
    private final JPanel pnlThuTien=new JPanel(new BorderLayout());
    private final ManHinhThanhToanController controller;
    public ThanhToanPanel(DesktopSession session,ConnectionProvider connections){
        new AuthorizationService(new RolePermissionPolicy()).require(session,Quyen.BILLING_MANAGE);
        setLayout(new BorderLayout(12,12));setBorder(BorderFactory.createEmptyBorder(24,26,24,26));
        JLabel lblTitle=new JLabel("Thanh toán");lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD,22f));
        cmbHoaDon.setPrototypeDisplayValue(new LuaChonLapHoaDon("HD00000000","Bệnh nhân · Còn nợ 999.999.999 ₫"));
        JPanel pnlSearch=GiaoDienHoaDon.filters(GiaoDienHoaDon.field("Mã hóa đơn / bệnh nhân / SĐT",txtTimHoaDon),GiaoDienHoaDon.field(" ",GiaoDienHoaDon.actions(btnTim,btnTaiLai)),GiaoDienHoaDon.field("Chọn hóa đơn còn nợ",cmbHoaDon));
        JPanel pnlTop=new JPanel(new BorderLayout(0,8));pnlTop.add(lblTitle,BorderLayout.NORTH);pnlTop.add(pnlSearch,BorderLayout.CENTER);pnlTop.add(lblTrangThai,BorderLayout.SOUTH);add(pnlTop,BorderLayout.NORTH);
        pnlThuTien.add(new ThanhToanForm());add(pnlThuTien,BorderLayout.CENTER);
        lblTrangThai.addPropertyChangeListener("text", new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent event) {
                lblTrangThai.setVisible(!lblTrangThai.getText().isBlank());
            }
        });
        controller=new ManHinhThanhToanController(this,session,connections);
        GiaoDienHoaDon.style(this);
    }
    @Override public void addNotify(){super.addNotify();refreshData();}
    @Override public void refreshData(){controller.refresh();}
    public void openInvoice(String id){controller.openInvoice(id);}
    public void showChoices(List<ThongTinHoaDon> invoices,String selected){
        cmbHoaDon.removeAllItems();LuaChonLapHoaDon chosen=null;
        for(ThongTinHoaDon h:invoices){LuaChonLapHoaDon item=new LuaChonLapHoaDon(h.maHoaDon(),h.maBenhNhan()+" · "+h.tenBenhNhan()+" · Còn nợ "+GiaoDienHoaDon.vnd(h.conNo()));cmbHoaDon.addItem(item);if(h.maHoaDon().equals(selected))chosen=item;}
        cmbHoaDon.setSelectedItem(chosen); // Preserve current form/history even when a fully paid invoice leaves the picker.
    }
    public void showForm(ThanhToanForm pnlForm){com.quanlynhakhoa.view.hethong.AppTheme.applyButtons(pnlForm);pnlThuTien.removeAll();pnlThuTien.add(pnlForm,BorderLayout.CENTER);pnlThuTien.revalidate();pnlThuTien.repaint();}
    public void pickerEnabled(boolean enabled){txtTimHoaDon.setEnabled(enabled);btnTim.setEnabled(enabled);btnTaiLai.setEnabled(enabled);cmbHoaDon.setEnabled(enabled);}
    public void expired(){cmbHoaDon.removeAllItems();pnlThuTien.removeAll();pickerEnabled(false);lblTrangThai.setText("Phiên đã đăng xuất.");pnlThuTien.revalidate();pnlThuTien.repaint();}
}
