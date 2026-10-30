package com.quanlynhakhoa.billing.ui;

import com.quanlynhakhoa.billing.controller.PaymentScreenController;
import com.quanlynhakhoa.billing.dao.ConnectionProvider;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.features.RefreshableFeature;
import com.quanlynhakhoa.security.*;
import java.awt.*;
import java.util.List;
import javax.swing.*;

/** Collection workspace: compact invoice picker, collection form and history, not an invoice list clone. */
public final class PaymentPanel extends JPanel implements RefreshableFeature {
    public final JTextField txtTimHoaDon=new JTextField(22);
    public final JButton btnTim=new JButton("Tìm hóa đơn còn nợ"),btnTaiLai=new JButton("Tải lại");
    public final JComboBox<BillingChoice> cmbHoaDon=new JComboBox<>();
    public final JLabel lblTrangThai=new JLabel("Tìm mã hóa đơn, mã/tên/số điện thoại bệnh nhân; chọn chứng từ để thu tiền.");
    private final JPanel pnlCollection=new JPanel(new BorderLayout());
    private final PaymentScreenController controller;
    public PaymentPanel(DesktopSession session,ConnectionProvider connections){
        new AuthorizationService(new RolePermissionPolicy()).require(session,Permission.BILLING_MANAGE);
        setLayout(new BorderLayout(12,12));setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        JLabel lblTitle=new JLabel("THANH TOÁN");lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD,22f));
        JPanel pnlSearch=new JPanel(new FlowLayout(FlowLayout.LEFT));pnlSearch.add(txtTimHoaDon);pnlSearch.add(btnTim);pnlSearch.add(btnTaiLai);
        cmbHoaDon.setPrototypeDisplayValue(new BillingChoice("HD00000000","Bệnh nhân · Còn nợ 999.999.999 ₫"));pnlSearch.add(cmbHoaDon);
        JPanel pnlTop=new JPanel(new BorderLayout(0,8));pnlTop.add(lblTitle,BorderLayout.NORTH);pnlTop.add(pnlSearch,BorderLayout.CENTER);pnlTop.add(lblTrangThai,BorderLayout.SOUTH);add(pnlTop,BorderLayout.NORTH);
        pnlCollection.add(new JLabel("Chưa chọn hóa đơn để thu tiền.",SwingConstants.CENTER));add(pnlCollection,BorderLayout.CENTER);
        controller=new PaymentScreenController(this,session,connections);
        BillingUi.style(this);
    }
    @Override public void addNotify(){super.addNotify();refreshData();}
    @Override public void refreshData(){controller.refresh();}
    public void openInvoice(String id){controller.openInvoice(id);}
    public void showChoices(List<HoaDon> invoices,String selected){
        cmbHoaDon.removeAllItems();BillingChoice chosen=null;
        for(HoaDon h:invoices){BillingChoice item=new BillingChoice(h.maHoaDon(),h.maBenhNhan()+" · "+h.tenBenhNhan()+" · Còn nợ "+BillingUi.vnd(h.conNo()));cmbHoaDon.addItem(item);if(h.maHoaDon().equals(selected))chosen=item;}
        cmbHoaDon.setSelectedItem(chosen); // Preserve current form/history even when a fully paid invoice leaves the picker.
    }
    public void showForm(PaymentForm pnlForm){pnlCollection.removeAll();pnlCollection.add(pnlForm,BorderLayout.CENTER);pnlCollection.revalidate();pnlCollection.repaint();}
    public void pickerEnabled(boolean enabled){txtTimHoaDon.setEnabled(enabled);btnTim.setEnabled(enabled);btnTaiLai.setEnabled(enabled);cmbHoaDon.setEnabled(enabled);}
    public void expired(){cmbHoaDon.removeAllItems();pnlCollection.removeAll();pickerEnabled(false);lblTrangThai.setText("Phiên đã đăng xuất.");pnlCollection.revalidate();pnlCollection.repaint();}
}
