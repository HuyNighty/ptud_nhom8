package com.quanlynhakhoa.billing.ui;

import com.quanlynhakhoa.billing.model.ChiTietBilling;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import javax.swing.*;
import javax.swing.event.*;

/** Shared collection form. UI previews never replace transaction validation in PaymentService. */
public final class PaymentForm extends JPanel {
    public enum Method {
        CASH("Tiền mặt"), BANK("Chuyển khoản"), MIXED("Kết hợp");
        private final String label;
        Method(String label){this.label=label;}
        @Override public String toString(){return label;}
    }
    public final JTextField txtSoTien=new JTextField(),txtTienMat=new JTextField("0"),txtChuyenKhoan=new JTextField("0"),txtKhachDua=new JTextField(),txtMaGiaoDich=new JTextField();
    public final JComboBox<Method> cmbPhuongThuc=new JComboBox<>(Method.values());
    public final JCheckBox chkDaNhanChuyenKhoan=new JCheckBox("<html>Đã kiểm tra và nhận đủ phần chuyển khoản</html>");
    public final JButton btnThuTien=new JButton("Xác nhận thu tiền"),btnThuHet=new JButton("Thu hết còn nợ"),btnKiemTra=new JButton("Kiểm tra kết quả lần thu");
    public final JLabel lblTrangThai=new JLabel("Tìm và chọn hóa đơn còn nợ để bắt đầu thu tiền.");
    public final JLabel lblPhanBo=new JLabel("Tổng phân bổ: —"),lblNoSauThu=new JLabel("Còn nợ sau thu: —"),lblTienThua=new JLabel("Tiền thừa trả khách: —");
    private final InvoiceHistoryPanel pnlHistory=new InvoiceHistoryPanel();
    private final JPanel pnlCash=BillingUi.field("Thu bằng tiền mặt",txtTienMat),pnlBank=BillingUi.field("Thu bằng chuyển khoản",txtChuyenKhoan);
    private final JPanel pnlTender=BillingUi.field("Tiền khách đưa",txtKhachDua),pnlReference=BillingUi.field("Tham chiếu chuyển khoản",txtMaGiaoDich);
    private final JPanel pnlActions=new JPanel(new CardLayout());
    private BigDecimal outstanding,lastBank=BigDecimal.ZERO;
    private boolean busy,uncertain,mayCollect,synchronizing,collectionContextAllowed=true;
    private Runnable closeAction,stateListener;

    public PaymentForm(){
        setLayout(new BorderLayout(12,8));add(pnlHistory,BorderLayout.CENTER);
        JPanel pnlReceipt=new JPanel(new BorderLayout(0,8));
        pnlReceipt.setBorder(BorderFactory.createTitledBorder("Ghi nhận khoản thu"));
        JPanel pnlInputs=new ReceiptInputs();
        pnlInputs.add(BillingUi.row(BillingUi.field("Số tiền thu (VND)",txtSoTien),BillingUi.field("Phương thức thu",cmbPhuongThuc)));
        pnlInputs.add(BillingUi.row(pnlCash,pnlBank));pnlInputs.add(BillingUi.row(pnlTender,pnlReference));
        JScrollPane scrInputs=new JScrollPane(pnlInputs){@Override public Dimension getPreferredSize(){Dimension size=super.getPreferredSize();size.height=pnlInputs.getPreferredSize().height+getInsets().top+getInsets().bottom;return size;}};
        scrInputs.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);scrInputs.setMinimumSize(new Dimension(120,70));pnlReceipt.add(scrInputs,BorderLayout.CENTER);
        JPanel pnlReview=new JPanel(new BorderLayout(0,6));pnlReview.add(chkDaNhanChuyenKhoan,BorderLayout.NORTH);
        JPanel pnlBalances=new JPanel(new GridLayout(0,1,0,4));pnlBalances.add(lblPhanBo);pnlBalances.add(lblTienThua);pnlBalances.add(lblNoSauThu);
        pnlReview.add(pnlBalances,BorderLayout.CENTER);
        JPanel pnlCollect=BillingUi.row(btnThuHet,BillingUi.primary(btnThuTien));
        pnlActions.add(pnlCollect,"collect");pnlActions.add(BillingUi.primary(btnKiemTra),"verify");pnlReview.add(pnlActions,BorderLayout.SOUTH);
        pnlReceipt.add(pnlReview,BorderLayout.SOUTH);
        JPanel pnlRight=BillingUi.topAligned(pnlReceipt);pnlRight.setPreferredSize(new Dimension(440,0));add(pnlRight,BorderLayout.EAST);add(lblTrangThai,BorderLayout.SOUTH);
        txtSoTien.setToolTipText("Số tiền ghi nhận để thanh toán hóa đơn");txtKhachDua.setToolTipText("Tiền mặt thực nhận trước khi trả tiền thừa");
        for(JTextField input:new JTextField[]{txtSoTien,txtTienMat,txtChuyenKhoan,txtKhachDua})BillingUi.moneyField(input);
        txtSoTien.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){amountChanged();}public void removeUpdate(DocumentEvent e){amountChanged();}public void changedUpdate(DocumentEvent e){amountChanged();}});
        DocumentListener partsChanged=new DocumentListener(){public void insertUpdate(DocumentEvent e){preview();}public void removeUpdate(DocumentEvent e){preview();}public void changedUpdate(DocumentEvent e){preview();}};
        txtTienMat.getDocument().addDocumentListener(partsChanged);txtChuyenKhoan.getDocument().addDocumentListener(partsChanged);txtKhachDua.getDocument().addDocumentListener(partsChanged);
        cmbPhuongThuc.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(!synchronizing){chkDaNhanChuyenKhoan.setSelected(false);allocate();updateControls();preview();}}});
        btnThuHet.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(outstanding!=null&&mayCollect)txtSoTien.setText(BillingUi.moneyText(outstanding));}});
        busy(false,false,false);BillingUi.style(this);updateControls();
    }
    private Method method(){return (Method)cmbPhuongThuc.getSelectedItem();}
    private void amountChanged(){if(synchronizing)return;allocate();preview();}
    private void allocate(){
        if(synchronizing)return;
        synchronizing=true;
        try{
            String amount=txtSoTien.getText().isBlank()?"0":txtSoTien.getText();
            if(method()==Method.CASH){txtTienMat.setText(amount);txtChuyenKhoan.setText("0");}
            else if(method()==Method.BANK){txtTienMat.setText("0");txtChuyenKhoan.setText(amount);}
        }finally{synchronizing=false;}
    }
    private BigDecimal moneyOrZero(JTextField input){return input.getText().isBlank()?BigDecimal.ZERO:BillingUi.moneyInput(input.getText());}
    public BigDecimal cashAmount(){return method()==Method.BANK?BigDecimal.ZERO:BillingUi.moneyInput(txtTienMat.getText());}
    public BigDecimal bankAmount(){return method()==Method.CASH?BigDecimal.ZERO:BillingUi.moneyInput(txtChuyenKhoan.getText());}
    private void preview(){
        if(synchronizing)return;
        try{
            BigDecimal bank=method()==Method.CASH?BigDecimal.ZERO:moneyOrZero(txtChuyenKhoan);
            if(lastBank==null||lastBank.compareTo(bank)!=0)chkDaNhanChuyenKhoan.setSelected(false);lastBank=bank;
            chkDaNhanChuyenKhoan.setVisible(method()!=Method.CASH&&bank.signum()>0);
            if(outstanding==null){lblPhanBo.setText("Tổng phân bổ: —");lblNoSauThu.setText("Còn nợ sau thu: —");lblTienThua.setText("Tiền thừa trả khách: —");return;}
            BigDecimal amount=moneyOrZero(txtSoTien),cash=method()==Method.BANK?BigDecimal.ZERO:moneyOrZero(txtTienMat);
            BigDecimal allocated=cash.add(bank);
            lblPhanBo.setText("Tổng phân bổ: "+BillingUi.vnd(allocated)+(allocated.compareTo(amount)==0?" · Khớp":" · Chưa khớp"));
            lblNoSauThu.setText(amount.compareTo(outstanding)>0?"Số tiền thu vượt khoản còn nợ.":"Còn nợ sau thu: "+BillingUi.vnd(outstanding.subtract(amount)));
            BigDecimal tender=cash.signum()>0?moneyOrZero(txtKhachDua):BigDecimal.ZERO;
            lblTienThua.setText(cash.signum()==0?"Tiền thừa trả khách: —":tender.compareTo(cash)<0?"Tiền khách đưa chưa đủ phần tiền mặt.":"Tiền thừa trả khách: "+BillingUi.vnd(tender.subtract(cash)));
        }catch(IllegalArgumentException failure){lastBank=null;chkDaNhanChuyenKhoan.setSelected(false);lblPhanBo.setText("Nhập số tiền VND nguyên hợp lệ.");lblNoSauThu.setText("Còn nợ sau thu: —");lblTienThua.setText("Tiền thừa trả khách: —");}
        finally{revalidate();repaint();}
    }
    private void updateControls(){
        boolean enabled=!busy&&!uncertain&&mayCollect&&collectionContextAllowed;
        for(Component control:new Component[]{txtSoTien,cmbPhuongThuc,txtTienMat,txtChuyenKhoan,txtKhachDua,txtMaGiaoDich,chkDaNhanChuyenKhoan,btnThuHet})control.setEnabled(enabled);
        boolean cash=method()!=Method.BANK,bank=method()!=Method.CASH;
        pnlCash.setVisible(cash);pnlTender.setVisible(cash);pnlBank.setVisible(bank);pnlReference.setVisible(bank);lblTienThua.setVisible(cash);
        txtTienMat.setEditable(method()==Method.MIXED);txtChuyenKhoan.setEditable(method()==Method.MIXED);
        btnThuTien.setEnabled(enabled);btnKiemTra.setEnabled(!busy&&uncertain);((CardLayout)pnlActions.getLayout()).show(pnlActions,uncertain?"verify":"collect");
        if(!bank)chkDaNhanChuyenKhoan.setVisible(false);revalidate();repaint();
    }
    public void showInvoice(ChiTietBilling detail){pnlHistory.showInvoice(detail);outstanding=detail.hoaDon().conNo();preview();}
    public void loading(){outstanding=null;pnlHistory.clear();preview();lblTrangThai.setText("Đang kiểm tra hóa đơn và lịch sử thanh toán…");}
    public void onClose(Runnable callback){closeAction=callback;}
    public void onStateChange(Runnable callback){stateListener=callback;}
    public boolean finish(){if(closeAction==null)return false;closeAction.run();return true;}
    public void closeView(){if(closeAction!=null)closeAction.run();else{outstanding=null;pnlHistory.clear();clearAmounts();busy(false,false,true);lblTrangThai.setText("Phiên đã đăng xuất.");}}
    public boolean isWorking(){return busy;}
    public boolean hasUncertainResult(){return uncertain;}
    public boolean collectionContextAllowed(){return collectionContextAllowed;}
    public void setCollectionContextAllowed(boolean allowed){collectionContextAllowed=allowed;updateControls();}
    public void clearAmounts(){synchronizing=true;try{txtSoTien.setText("");txtTienMat.setText("0");txtChuyenKhoan.setText("0");txtKhachDua.setText("");txtMaGiaoDich.setText("");chkDaNhanChuyenKhoan.setSelected(false);}finally{synchronizing=false;}preview();}
    public void busy(boolean working,boolean allowed,boolean uncertainResult){busy=working;mayCollect=allowed;uncertain=uncertainResult;updateControls();if(stateListener!=null)stateListener.run();}
    private static final class ReceiptInputs extends JPanel implements Scrollable {
        ReceiptInputs(){super(new GridLayout(0,1,0,6));}
        public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
        public int getScrollableUnitIncrement(Rectangle visible,int orientation,int direction){return 32;}
        public int getScrollableBlockIncrement(Rectangle visible,int orientation,int direction){return Math.max(32,visible.height-32);}
        public boolean getScrollableTracksViewportWidth(){return true;}
        public boolean getScrollableTracksViewportHeight(){return false;}
    }
}
