package com.quanlynhakhoa.view.hoadon;

import com.quanlynhakhoa.model.hoadon.LuaChonLapHoaDon;
import com.quanlynhakhoa.model.hoadon.DongBanNhapHoaDon;
import com.quanlynhakhoa.model.hoadon.BanNhapHoaDon;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Review UI contains no persistence or pricing logic. Draft table is never editable. */
public final class TaoHoaDonDialog extends JDialog {
    public final JTextField txtBenhNhan = new JTextField(18);
    public final JButton btnTimBenhNhan = new JButton("Tìm bệnh nhân"), btnTaiDichVu = new JButton("Tải dịch vụ và thuốc"), btnLuuHoaDon = new JButton("Lưu hóa đơn");
    public final JComboBox<LuaChonLapHoaDon> cmbBenhNhan = new JComboBox<>(), cmbPhieuKham = new JComboBox<>();
    public final JLabel lblTongTien = new JLabel("Tổng tiền: —"), lblTrangThai = new JLabel("Tìm bệnh nhân để bắt đầu");
    public final JButton btnHuy=new JButton("Hủy");
    private final DefaultTableModel tableModel=GiaoDienHoaDon.model("Loại","Mã hàng","Tên dịch vụ / thuốc","Số lượng","Đơn giá áp dụng","Thành tiền");
    private final JTable tblChiTietHoaDon=GiaoDienHoaDon.table(tableModel);
    public TaoHoaDonDialog(Window owner) {
        super(owner,"Tạo hóa đơn",ModalityType.APPLICATION_MODAL);
        JPanel pnlRoot=new JPanel(new BorderLayout(10,10));pnlRoot.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JPanel pnlSearch=new JPanel(new BorderLayout(8,0));pnlSearch.add(txtBenhNhan);pnlSearch.add(btnTimBenhNhan,BorderLayout.EAST);
        JPanel pnlFields=new JPanel(new BorderLayout(0,10));
        pnlFields.add(GiaoDienHoaDon.field("Mã, tên hoặc số điện thoại bệnh nhân",pnlSearch),BorderLayout.NORTH);
        JPanel pnlSelection=GiaoDienHoaDon.row(GiaoDienHoaDon.field("Bệnh nhân",cmbBenhNhan),GiaoDienHoaDon.field("Lượt khám",cmbPhieuKham));
        pnlFields.add(pnlSelection,BorderLayout.CENTER);pnlFields.add(GiaoDienHoaDon.actions(btnTaiDichVu),BorderLayout.SOUTH);
        pnlRoot.add(pnlFields,BorderLayout.NORTH);pnlRoot.add(GiaoDienHoaDon.scroll(tblChiTietHoaDon),BorderLayout.CENTER);
        JPanel pnlBottom=new JPanel(new BorderLayout(8,8));pnlBottom.add(lblTrangThai,BorderLayout.NORTH);pnlBottom.add(GiaoDienHoaDon.card(lblTongTien),BorderLayout.CENTER);pnlBottom.add(GiaoDienHoaDon.actions(btnHuy,GiaoDienHoaDon.primary(btnLuuHoaDon)),BorderLayout.EAST);pnlRoot.add(pnlBottom,BorderLayout.SOUTH);
        btnHuy.addActionListener(new java.awt.event.ActionListener(){public void actionPerformed(java.awt.event.ActionEvent event){dispose();}});
        GiaoDienHoaDon.style(pnlRoot);setContentPane(pnlRoot);setSize(980,600);setMinimumSize(new Dimension(760,480));setLocationRelativeTo(owner);setDefaultCloseOperation(DISPOSE_ON_CLOSE);btnLuuHoaDon.setEnabled(false);
        btnLuuHoaDon.setToolTipText("Chọn bệnh nhân, lượt khám và tải dịch vụ trước khi lưu.");
        GiaoDienHoaDon.tableState(tblChiTietHoaDon,"Dịch vụ và thuốc được kê sẽ hiển thị tại đây.");
        busy(false,false);
    }
    public void showDraft(BanNhapHoaDon draft) {
        tableModel.setRowCount(0);
        if(draft!=null)for(DongBanNhapHoaDon line:draft.lines())tableModel.addRow(new Object[]{line.medicine()?"Thuốc":"Dịch vụ",line.serviceId(),line.serviceName(),line.quantity(),line.unitPrice(),line.amount()});
        lblTongTien.setText(draft==null?"Tổng tiền: —":"Tổng tiền: "+GiaoDienHoaDon.vnd(draft.total()));
    }
    public void guide(String text){lblTrangThai.setText(text);lblTrangThai.setToolTipText(text);if(tableModel.getRowCount()==0)GiaoDienHoaDon.tableState(tblChiTietHoaDon,"Dịch vụ và thuốc được kê sẽ hiển thị tại đây.");}
    public void busy(boolean busy, boolean hasDraft) {
        for(Component control:new Component[]{txtBenhNhan,btnTimBenhNhan,btnHuy})control.setEnabled(!busy);
        cmbBenhNhan.setEnabled(!busy&&cmbBenhNhan.getItemCount()>0);cmbPhieuKham.setEnabled(!busy&&cmbPhieuKham.getItemCount()>0);btnTaiDichVu.setEnabled(!busy&&cmbPhieuKham.getSelectedItem()!=null);
        btnLuuHoaDon.setEnabled(!busy&&hasDraft);
        if(busy)GiaoDienHoaDon.tableState(tblChiTietHoaDon,"Đang kiểm tra dữ liệu…");
        setDefaultCloseOperation(busy?DO_NOTHING_ON_CLOSE:DISPOSE_ON_CLOSE);
    }
}
