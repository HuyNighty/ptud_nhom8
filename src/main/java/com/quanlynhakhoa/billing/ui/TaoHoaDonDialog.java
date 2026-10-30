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
    public final JLabel lblTongTien = new JLabel("Tổng: —"), lblTrangThai = new JLabel("Tìm bệnh nhân có thật để bắt đầu.");
    private final DefaultTableModel tableModel=BillingUi.model("Mã dịch vụ","Tên dịch vụ","Số lượng","Đơn giá áp dụng","Thành tiền");
    private final JTable tblChiTietHoaDon=BillingUi.table(tableModel);
    public TaoHoaDonDialog(Window owner) {
        super(owner,"Tạo hóa đơn",ModalityType.APPLICATION_MODAL);
        JPanel pnlRoot=new JPanel(new BorderLayout(10,10));pnlRoot.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JPanel pnlFields=new JPanel(new GridLayout(0,2,8,8));
        pnlFields.add(txtBenhNhan);pnlFields.add(btnTimBenhNhan);pnlFields.add(new JLabel("Bệnh nhân"));pnlFields.add(cmbBenhNhan);
        pnlFields.add(new JLabel("Lượt khám (cần kiểm tra điều kiện tính phí)"));pnlFields.add(cmbPhieuKham);pnlFields.add(btnTaiDichVu);pnlFields.add(lblTongTien);
        pnlRoot.add(pnlFields,BorderLayout.NORTH);pnlRoot.add(BillingUi.scroll(tblChiTietHoaDon),BorderLayout.CENTER);
        JPanel pnlBottom=new JPanel(new BorderLayout());pnlBottom.add(lblTrangThai,BorderLayout.CENTER);pnlBottom.add(btnLuuHoaDon,BorderLayout.EAST);pnlRoot.add(pnlBottom,BorderLayout.SOUTH);
        setContentPane(pnlRoot);setSize(950,520);setLocationRelativeTo(owner);setDefaultCloseOperation(DISPOSE_ON_CLOSE);btnLuuHoaDon.setEnabled(false);
    }
    public void showDraft(InvoiceDraft draft) {
        tableModel.setRowCount(0);
        if(draft!=null)for(InvoiceDraft.Line line:draft.lines())tableModel.addRow(new Object[]{line.serviceId(),line.serviceName(),line.quantity(),line.unitPrice(),line.amount()});
        lblTongTien.setText(draft==null?"Tổng: —":"Tổng: "+BillingUi.vnd(draft.total()));
    }
    public void busy(boolean busy, boolean hasDraft) {
        for(Component control:new Component[]{txtBenhNhan,btnTimBenhNhan,cmbBenhNhan,cmbPhieuKham,btnTaiDichVu})control.setEnabled(!busy);
        btnLuuHoaDon.setEnabled(!busy&&hasDraft);
        setDefaultCloseOperation(busy?DO_NOTHING_ON_CLOSE:DISPOSE_ON_CLOSE);
    }
}
