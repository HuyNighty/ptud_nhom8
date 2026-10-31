package com.quanlynhakhoa.billing.ui;

import com.quanlynhakhoa.billing.model.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Review UI contains no persistence or pricing logic. Draft table is never editable. */
public final class TaoHoaDonDialog extends JDialog {
    public final JTextField txtBenhNhan = new JTextField(18);
    public final JButton btnTimBenhNhan = new JButton("Tìm bệnh nhân"), btnTaiDichVu = new JButton("Tải dịch vụ tính phí"), btnLuuHoaDon = new JButton("Lưu hóa đơn");
    public final JComboBox<BillingChoice> cmbBenhNhan = new JComboBox<>(), cmbPhieuKham = new JComboBox<>();
    public final JLabel lblTongTien = new JLabel("Tổng tiền: —"), lblTrangThai = new JLabel("Tìm bệnh nhân để bắt đầu");
    public final JButton btnHuy=new JButton("Hủy");
    private final DefaultTableModel tableModel=BillingUi.model("Mã dịch vụ","Tên dịch vụ","Số lượng","Đơn giá áp dụng","Thành tiền");
    private final JTable tblChiTietHoaDon=BillingUi.table(tableModel);
    public TaoHoaDonDialog(Window owner) {
        super(owner,"Tạo hóa đơn",ModalityType.APPLICATION_MODAL);
        JPanel pnlRoot=new JPanel(new BorderLayout(10,10));pnlRoot.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JPanel pnlSearch=new JPanel(new BorderLayout(8,0));pnlSearch.add(txtBenhNhan);pnlSearch.add(btnTimBenhNhan,BorderLayout.EAST);
        JPanel pnlFields=new JPanel(new BorderLayout(0,10));
        pnlFields.add(BillingUi.field("Mã, tên hoặc số điện thoại bệnh nhân",pnlSearch),BorderLayout.NORTH);
        JPanel pnlSelection=BillingUi.row(BillingUi.field("Bệnh nhân",cmbBenhNhan),BillingUi.field("Lượt khám",cmbPhieuKham));
        pnlFields.add(pnlSelection,BorderLayout.CENTER);pnlFields.add(BillingUi.actions(btnTaiDichVu),BorderLayout.SOUTH);
        pnlRoot.add(pnlFields,BorderLayout.NORTH);pnlRoot.add(BillingUi.scroll(tblChiTietHoaDon),BorderLayout.CENTER);
        JPanel pnlBottom=new JPanel(new BorderLayout(8,8));pnlBottom.add(lblTrangThai,BorderLayout.NORTH);pnlBottom.add(BillingUi.card(lblTongTien),BorderLayout.CENTER);pnlBottom.add(BillingUi.actions(btnHuy,BillingUi.primary(btnLuuHoaDon)),BorderLayout.EAST);pnlRoot.add(pnlBottom,BorderLayout.SOUTH);
        btnHuy.addActionListener(new java.awt.event.ActionListener(){public void actionPerformed(java.awt.event.ActionEvent event){dispose();}});
        BillingUi.style(pnlRoot);setContentPane(pnlRoot);setSize(980,600);setMinimumSize(new Dimension(760,480));setLocationRelativeTo(owner);setDefaultCloseOperation(DISPOSE_ON_CLOSE);btnLuuHoaDon.setEnabled(false);
        btnLuuHoaDon.setToolTipText("Chọn bệnh nhân, lượt khám và tải dịch vụ trước khi lưu.");
        BillingUi.tableState(tblChiTietHoaDon,"Dịch vụ tính phí sẽ hiển thị tại đây.");
        busy(false,false);
    }
    public void showDraft(InvoiceDraft draft) {
        tableModel.setRowCount(0);
        if(draft!=null)for(InvoiceDraft.Line line:draft.lines())tableModel.addRow(new Object[]{line.serviceId(),line.serviceName(),line.quantity(),line.unitPrice(),line.amount()});
        lblTongTien.setText(draft==null?"Tổng tiền: —":"Tổng tiền: "+BillingUi.vnd(draft.total()));
    }
    public void guide(String text){lblTrangThai.setText(text);lblTrangThai.setToolTipText(text);if(tableModel.getRowCount()==0)BillingUi.tableState(tblChiTietHoaDon,"Dịch vụ tính phí sẽ hiển thị tại đây.");}
    public void busy(boolean busy, boolean hasDraft) {
        for(Component control:new Component[]{txtBenhNhan,btnTimBenhNhan,btnHuy})control.setEnabled(!busy);
        cmbBenhNhan.setEnabled(!busy&&cmbBenhNhan.getItemCount()>0);cmbPhieuKham.setEnabled(!busy&&cmbPhieuKham.getItemCount()>0);btnTaiDichVu.setEnabled(!busy&&cmbPhieuKham.getSelectedItem()!=null);
        btnLuuHoaDon.setEnabled(!busy&&hasDraft);
        if(busy)BillingUi.tableState(tblChiTietHoaDon,"Đang kiểm tra dữ liệu…");
        setDefaultCloseOperation(busy?DO_NOTHING_ON_CLOSE:DISPOSE_ON_CLOSE);
    }
}
