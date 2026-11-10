package com.quanlynhakhoa.view.hoadon;

import com.quanlynhakhoa.enums.PhuongThucThanhToan;
import com.quanlynhakhoa.enums.PhuongThucNhapThanhToan;
import com.quanlynhakhoa.model.hoadon.DuLieuChiTietHoaDon;
import com.quanlynhakhoa.model.hoadon.YeuCauThanhToan;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import javax.swing.*;
import javax.swing.event.*;

/** Shared collection form. UI previews never replace transaction validation in PaymentService. */
public final class ThanhToanForm extends JPanel {

    public final JTextField txtSoTien=new JTextField(),txtTienMat=new JTextField("0"),txtChuyenKhoan=new JTextField("0"),txtKhachDua=new JTextField(),txtMaGiaoDich=new JTextField();
    public final JComboBox<PhuongThucNhapThanhToan> cmbPhuongThuc=new JComboBox<>(PhuongThucNhapThanhToan.values());
    public final JCheckBox chkDaNhanChuyenKhoan=new JCheckBox("<html>Đã kiểm tra và nhận đủ tiền chuyển khoản</html>");
    public final JButton btnThuTien=new JButton("Xác nhận thu tiền"),btnThuHet=new JButton("Thu hết còn nợ"),btnKiemTra=new JButton("Kiểm tra kết quả lần thu"),btnThuLai=new JButton("Xác nhận lại lần thu này");
    public final JLabel lblTrangThai=new JLabel("Tìm và chọn hóa đơn còn nợ để bắt đầu thu tiền.");
    public final JLabel lblPhanBo=new JLabel("Tổng phân bổ: —"),lblNoSauThu=new JLabel("Còn nợ sau thu: —"),lblTienThua=new JLabel("Tiền thừa trả khách: —");
    private final LichSuHoaDonPanel pnlLichSu=new LichSuHoaDonPanel();
    private final JPanel pnlTienMat=GiaoDienHoaDon.field("Thu bằng tiền mặt",txtTienMat),pnlChuyenKhoan=GiaoDienHoaDon.field("Thu bằng chuyển khoản",txtChuyenKhoan);
    private final JPanel pnlTienKhachDua=GiaoDienHoaDon.field("Tiền khách đưa",txtKhachDua),pnlThamChieu=GiaoDienHoaDon.field("Tham chiếu chuyển khoản",txtMaGiaoDich);
    private final JPanel pnlActions=new JPanel(new CardLayout());
    private BigDecimal conNo,tienChuyenKhoanTruoc=BigDecimal.ZERO;
    private boolean duocThuLai,busy,chuaRoKetQua,duocThuTien,synchronizing,duocThuTrongNguCanh=true;
    private Runnable closeAction,stateListener;

    public ThanhToanForm(){
        setLayout(new BorderLayout(12,8));add(pnlLichSu,BorderLayout.CENTER);
        JPanel pnlReceipt=new JPanel(new BorderLayout(0,8));
        pnlReceipt.setBorder(BorderFactory.createTitledBorder("Ghi nhận khoản thu"));
        JPanel pnlInputs=new ReceiptInputs();
        pnlInputs.add(GiaoDienHoaDon.row(GiaoDienHoaDon.field("Số tiền thu (VND)",txtSoTien),GiaoDienHoaDon.field("Phương thức thu",cmbPhuongThuc)));
        pnlInputs.add(GiaoDienHoaDon.row(pnlTienKhachDua,pnlThamChieu));
        JScrollPane scrInputs=new JScrollPane(pnlInputs){@Override public Dimension getPreferredSize(){Dimension size=super.getPreferredSize();size.height=pnlInputs.getPreferredSize().height+getInsets().top+getInsets().bottom;return size;}};
        scrInputs.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);scrInputs.setMinimumSize(new Dimension(120,70));pnlReceipt.add(scrInputs,BorderLayout.CENTER);
        JPanel pnlReview=new JPanel(new BorderLayout(0,6));pnlReview.add(chkDaNhanChuyenKhoan,BorderLayout.NORTH);
        JPanel pnlBalances=new JPanel(new GridLayout(0,1,0,4));pnlBalances.add(lblTienThua);pnlBalances.add(lblNoSauThu);
        pnlReview.add(pnlBalances,BorderLayout.CENTER);
        JPanel pnlCollect=GiaoDienHoaDon.row(btnThuHet,GiaoDienHoaDon.primary(btnThuTien));
        pnlActions.add(pnlCollect,"collect");pnlActions.add(GiaoDienHoaDon.actions(btnKiemTra,GiaoDienHoaDon.primary(btnThuLai)),"verify");pnlReview.add(pnlActions,BorderLayout.SOUTH);
        pnlReceipt.add(pnlReview,BorderLayout.SOUTH);
        JPanel pnlRight=GiaoDienHoaDon.topAligned(pnlReceipt);pnlRight.setPreferredSize(new Dimension(440,0));add(pnlRight,BorderLayout.EAST);add(lblTrangThai,BorderLayout.SOUTH);
        txtSoTien.setToolTipText("Số tiền ghi nhận để thanh toán hóa đơn");txtKhachDua.setToolTipText("Tiền mặt thực nhận trước khi trả tiền thừa");
        for(JTextField input:new JTextField[]{txtSoTien,txtTienMat,txtChuyenKhoan,txtKhachDua})GiaoDienHoaDon.moneyField(input);
        txtSoTien.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){amountChanged();}public void removeUpdate(DocumentEvent e){amountChanged();}public void changedUpdate(DocumentEvent e){amountChanged();}});
        DocumentListener partsChanged=new DocumentListener(){public void insertUpdate(DocumentEvent e){preview();}public void removeUpdate(DocumentEvent e){preview();}public void changedUpdate(DocumentEvent e){preview();}};
        txtTienMat.getDocument().addDocumentListener(partsChanged);txtChuyenKhoan.getDocument().addDocumentListener(partsChanged);txtKhachDua.getDocument().addDocumentListener(partsChanged);
        txtMaGiaoDich.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){resetConfirmation();}
            public void removeUpdate(DocumentEvent e){resetConfirmation();}
            public void changedUpdate(DocumentEvent e){resetConfirmation();}
            private void resetConfirmation(){chkDaNhanChuyenKhoan.setSelected(false);}
        });
        cmbPhuongThuc.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(!synchronizing){chkDaNhanChuyenKhoan.setSelected(false);allocate();updateControls();preview();}}});
        btnThuHet.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(conNo!=null&&duocThuTien)txtSoTien.setText(GiaoDienHoaDon.moneyText(conNo));}});
        busy(false,false,false);GiaoDienHoaDon.style(this);updateControls();
    }
    private PhuongThucNhapThanhToan method(){return (PhuongThucNhapThanhToan)cmbPhuongThuc.getSelectedItem();}
    private void amountChanged(){if(synchronizing)return;allocate();preview();}
    private void allocate(){
        if(synchronizing)return;
        synchronizing=true;
        try{
            String amount=txtSoTien.getText().isBlank()?"0":txtSoTien.getText();
            if(method()==PhuongThucNhapThanhToan.CASH){txtTienMat.setText(amount);txtChuyenKhoan.setText("0");}
            else if(method()==PhuongThucNhapThanhToan.BANK){txtTienMat.setText("0");txtChuyenKhoan.setText(amount);}
        }finally{synchronizing=false;}
    }
    private BigDecimal moneyOrZero(JTextField input){return input.getText().isBlank()?BigDecimal.ZERO:GiaoDienHoaDon.moneyInput(input.getText());}
    public BigDecimal cashAmount(){return method()==PhuongThucNhapThanhToan.BANK?BigDecimal.ZERO:GiaoDienHoaDon.moneyInput(txtTienMat.getText());}
    public BigDecimal bankAmount(){return method()==PhuongThucNhapThanhToan.CASH?BigDecimal.ZERO:GiaoDienHoaDon.moneyInput(txtChuyenKhoan.getText());}
    private void preview(){
        if(synchronizing)return;
        try{
            BigDecimal bank=method()==PhuongThucNhapThanhToan.CASH?BigDecimal.ZERO:moneyOrZero(txtChuyenKhoan);
            if(tienChuyenKhoanTruoc==null||tienChuyenKhoanTruoc.compareTo(bank)!=0)chkDaNhanChuyenKhoan.setSelected(false);tienChuyenKhoanTruoc=bank;
            chkDaNhanChuyenKhoan.setVisible(method()!=PhuongThucNhapThanhToan.CASH&&bank.signum()>0);
            if(conNo==null){lblPhanBo.setText("Tổng phân bổ: —");lblNoSauThu.setText("Còn nợ sau thu: —");lblTienThua.setText("Tiền thừa trả khách: —");return;}
            BigDecimal amount=moneyOrZero(txtSoTien),cash=method()==PhuongThucNhapThanhToan.BANK?BigDecimal.ZERO:moneyOrZero(txtTienMat);
            BigDecimal allocated=cash.add(bank);
            lblPhanBo.setText("Tổng phân bổ: "+GiaoDienHoaDon.vnd(allocated)+(allocated.compareTo(amount)==0?" · Khớp":" · Chưa khớp"));
            lblNoSauThu.setText(amount.compareTo(conNo)>0?"Số tiền thu vượt khoản còn nợ.":"Còn nợ sau thu: "+GiaoDienHoaDon.vnd(conNo.subtract(amount)));
            BigDecimal tender=cash.signum()>0?moneyOrZero(txtKhachDua):BigDecimal.ZERO;
            lblTienThua.setText(cash.signum()==0?"Tiền thừa trả khách: —":tender.compareTo(cash)<0?"Tiền khách đưa chưa đủ phần tiền mặt.":"Tiền thừa trả khách: "+GiaoDienHoaDon.vnd(tender.subtract(cash)));
        }catch(IllegalArgumentException failure){tienChuyenKhoanTruoc=null;chkDaNhanChuyenKhoan.setSelected(false);lblPhanBo.setText("Nhập số tiền VND nguyên hợp lệ.");lblNoSauThu.setText("Còn nợ sau thu: —");lblTienThua.setText("Tiền thừa trả khách: —");}
        finally{revalidate();repaint();}
    }
    private void updateControls(){
        boolean enabled=!busy&&!chuaRoKetQua&&duocThuTien&&duocThuTrongNguCanh;
        for(Component control:new Component[]{txtSoTien,cmbPhuongThuc,txtTienMat,txtChuyenKhoan,txtKhachDua,txtMaGiaoDich,chkDaNhanChuyenKhoan,btnThuHet})control.setEnabled(enabled);
        boolean cash=method()!=PhuongThucNhapThanhToan.BANK,bank=method()!=PhuongThucNhapThanhToan.CASH;
        pnlTienMat.setVisible(cash);pnlTienKhachDua.setVisible(cash);pnlChuyenKhoan.setVisible(bank);pnlThamChieu.setVisible(bank);lblTienThua.setVisible(cash);
        txtTienMat.setEditable(false);txtChuyenKhoan.setEditable(false);
        btnThuTien.setEnabled(enabled);btnKiemTra.setEnabled(!busy&&chuaRoKetQua);btnThuLai.setVisible(duocThuLai);btnThuLai.setEnabled(!busy&&chuaRoKetQua&&duocThuLai);((CardLayout)pnlActions.getLayout()).show(pnlActions,chuaRoKetQua?"verify":"collect");
        if(!bank)chkDaNhanChuyenKhoan.setVisible(false);revalidate();repaint();
    }
    public void restoreRequest(YeuCauThanhToan request){
        var part=request.parts().getFirst();boolean bank=part.method()==PhuongThucThanhToan.CHUYEN_KHOAN;
        synchronizing=true;
        try{
            cmbPhuongThuc.setSelectedItem(bank?PhuongThucNhapThanhToan.BANK:PhuongThucNhapThanhToan.CASH);
            txtSoTien.setText(GiaoDienHoaDon.moneyText(request.amount()));
            txtTienMat.setText(bank?"0":GiaoDienHoaDon.moneyText(part.amount()));
            txtChuyenKhoan.setText(bank?GiaoDienHoaDon.moneyText(part.amount()):"0");
            txtKhachDua.setText(part.customerTender()==null?"":GiaoDienHoaDon.moneyText(part.customerTender()));
            txtMaGiaoDich.setText(part.reference()==null?"":part.reference());
            tienChuyenKhoanTruoc=bank?part.amount():BigDecimal.ZERO;
            chkDaNhanChuyenKhoan.setSelected(request.transferConfirmed());
        }finally{synchronizing=false;}
        updateControls();preview();
    }
    public void allowRetry(boolean allowed){duocThuLai=allowed;updateControls();}
    public void showInvoice(DuLieuChiTietHoaDon detail){pnlLichSu.showInvoice(detail);conNo=detail.hoaDon().conNo();preview();}
    public void loading(){conNo=null;pnlLichSu.clear();preview();lblTrangThai.setText("Đang kiểm tra hóa đơn và lịch sử thanh toán…");}
    public void onClose(Runnable callback){closeAction=callback;}
    public void onStateChange(Runnable callback){stateListener=callback;}
    public boolean finish(){if(closeAction==null)return false;closeAction.run();return true;}
    public void closeView(){if(closeAction!=null)closeAction.run();else{conNo=null;pnlLichSu.clear();clearAmounts();busy(false,false,true);lblTrangThai.setText("Phiên đã đăng xuất.");}}
    public boolean isWorking(){return busy;}
    public boolean hasUncertainResult(){return chuaRoKetQua;}
    public boolean collectionContextAllowed(){return duocThuTrongNguCanh;}
    public void setCollectionContextAllowed(boolean allowed){duocThuTrongNguCanh=allowed;updateControls();}
    public void clearAmounts(){synchronizing=true;try{txtSoTien.setText("");txtTienMat.setText("0");txtChuyenKhoan.setText("0");txtKhachDua.setText("");txtMaGiaoDich.setText("");chkDaNhanChuyenKhoan.setSelected(false);}finally{synchronizing=false;}preview();}
    public void busy(boolean working,boolean allowed,boolean uncertainResult){busy=working;duocThuTien=allowed;chuaRoKetQua=uncertainResult;updateControls();if(stateListener!=null)stateListener.run();}
    private static final class ReceiptInputs extends JPanel implements Scrollable {
        ReceiptInputs(){super(new GridLayout(0,1,0,6));}
        public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle visible,int orientation,int direction){return 32;}
        public int getScrollableBlockIncrement(Rectangle visible,int orientation,int direction){return Math.max(32,visible.height-32);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return false;}
    }
}
