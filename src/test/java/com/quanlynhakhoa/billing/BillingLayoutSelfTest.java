package com.quanlynhakhoa.billing;

import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.service.*;
import com.quanlynhakhoa.billing.ui.*;
import com.quanlynhakhoa.dichvu.model.DichVu;
import com.quanlynhakhoa.dichvu.ui.DichVuPanel;
import com.quanlynhakhoa.features.*;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.navigation.Module;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.view.MainFrame;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;

/** Real shell/components, synthetic display data only. Screenshots are NOT real SQL acceptance. */
public final class BillingLayoutSelfTest {
    private static final Map<Module,JPanel> pages=new EnumMap<>(Module.class);
    private static final Path OUTPUT=Path.of("target/ui-review").toAbsolutePath();
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static <T>T field(Object target,String name,Class<T> type){try{var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return type.cast(f.get(target));}catch(Exception e){throw new AssertionError(e);}}
    private static void layout(Container root){root.doLayout();for(Component c:root.getComponents())if(c instanceof Container nested)layout(nested);}
    private static void select(JTable table){var model=(DefaultListSelectionModel)table.getSelectionModel();ListSelectionListener[] listeners=model.getListSelectionListeners();for(var listener:listeners)model.removeListSelectionListener(listener);table.setRowSelectionInterval(0,0);for(var listener:listeners)model.addListSelectionListener(listener);}
    private static void visibleControls(Container parent,JComponent root){
        for(Component c:parent.getComponents())if(c.isVisible()){
            if(c instanceof JButton button&&button.getText()!=null&&!button.getText().isBlank()){Rectangle r=SwingUtilities.convertRectangle(button.getParent(),button.getBounds(),root);check(r.x>=0&&r.y>=0&&r.x+r.width<=root.getWidth()&&r.y+r.height<=root.getHeight(),"Button clipped: "+button.getText()+" "+r+" in "+root.getSize());check(button.getHeight()>=32,"Button too short: "+button.getText());Rectangle clip=button.getBounds();Container ancestor=button.getParent();while(ancestor!=root&&!(ancestor instanceof JViewport)){check(clip.y>=0&&clip.y+clip.height<=ancestor.getHeight()&&clip.x>=0&&clip.x+clip.width<=ancestor.getWidth(),"Button clipped by parent: "+button.getText()+" "+clip+" parent="+ancestor.getSize());clip=SwingUtilities.convertRectangle(ancestor,clip,ancestor.getParent());ancestor=ancestor.getParent();}}
            if(c instanceof JTable table)check(table.getRowHeight()>=30,"Readable table rows");
            if(c instanceof JLabel label&&label.getText()!=null&&label.getText().startsWith("Còn nợ sau thu:")){
                check(label.getHeight()>=17,"Remaining debt preview must stay visible");
                Rectangle area=label.getBounds();Container ancestor=label.getParent();
                while(ancestor!=root){
                    check(area.y>=0&&area.y+area.height<=ancestor.getHeight(),"Remaining debt clipped by "+ancestor.getLayout()+" "+area+" parent="+ancestor.getSize());
                    area=SwingUtilities.convertRectangle(ancestor,area,ancestor.getParent());ancestor=ancestor.getParent();
                }
            }
            if(c instanceof Container nested)visibleControls(nested,root);
        }
    }
    private static void screenshot(JComponent root,String name)throws Exception{layout(root);if(!Boolean.getBoolean("billing.syntheticScreenshots"))return;BufferedImage image=new BufferedImage(root.getWidth(),root.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();root.printAll(g);g.dispose();ImageIO.write(image,"png",OUTPUT.resolve(name+".png").toFile());}
    private static void navigate(MainFrame frame,Module module)throws Exception{var method=MainFrame.class.getDeclaredMethod("navigate",Module.class);method.setAccessible(true);method.invoke(frame,module);}
    private static FeaturePanelFactory factory(final Module module,final com.quanlynhakhoa.billing.dao.ConnectionProvider jdbc){return new FeaturePanelFactory(){public JPanel create(DesktopSession session){JPanel result=switch(module){case SERVICE_CATALOG -> new DichVuPanel(session,jdbc);case BILLING -> new HoaDonPanel(session,jdbc);case DEBTS -> new DebtPanel(session,jdbc);case PAYMENTS -> new PaymentPanel(session,jdbc);case INVOICE_CANCELLATIONS -> new CancellationPanel(session,jdbc);default -> throw new AssertionError(module);};pages.put(module,result);return result;}};}
    private static HoaDon invoice(){return new HoaDon("TESTHD01","TESTNV","TESTBN","[KIỂM THỬ] Nguyễn An","TESTPK",LocalDateTime.of(2026,10,31,9,15),TrangThaiHoaDon.THANH_TOAN_MOT_PHAN,new BigDecimal("1500000"),new BigDecimal("500000"),BigDecimal.ZERO);}
    private static ChiTietBilling detail(){return new ChiTietBilling(invoice(),List.of(new ChiTietHoaDon(1,"TESTHD01","TESTDV",null,null,1,new BigDecimal("1500000"),"[KIỂM THỬ] Điều trị răng")),List.of(new ThanhToan("TESTTT01","TESTHD01",LocalDateTime.of(2026,10,31,10,0),new BigDecimal("500000"),ThanhToan.PhuongThuc.TIEN_MAT,null,new BigDecimal("500000"),ThanhToan.TrangThai.THANH_CONG,"TESTNV",null,null,null)));}
    private static void empty(Module module,JPanel panel){switch(module){case SERVICE_CATALOG -> ((DichVuPanel)panel).showRows(List.of());case BILLING -> ((HoaDonPanel)panel).showRows(List.of());case DEBTS -> ((DebtPanel)panel).showPatients(List.of());case PAYMENTS -> {var view=(PaymentPanel)panel;view.showChoices(List.of(),null);view.showForm(new PaymentForm());view.pickerEnabled(true);}case INVOICE_CANCELLATIONS -> ((CancellationPanel)panel).showRows(List.of());default -> throw new AssertionError(module);}}
    private static void seed(Module module,JPanel panel){
        switch(module){
            case SERVICE_CATALOG -> {var view=(DichVuPanel)panel;var item=new DichVu("TESTDV","[KIỂM THỬ] Điều trị răng",new BigDecimal("1500000"),"Giá hiện hành; hóa đơn cũ giữ giá đã chốt.",false,true);view.showRows(List.of(item));view.showForm(item);}
            case BILLING -> {var view=(HoaDonPanel)panel;view.showRows(List.of(invoice()));select(field(view,"tblHoaDon",JTable.class));view.showDetail(detail());view.setBusy(false);check(field(view,"btnThanhToan",JButton.class).isEnabled(),"Pay enabled on eligible selected invoice");}
            case DEBTS -> {var view=(DebtPanel)panel;view.showPatients(DebtService.byPatient(List.of(invoice())));select(view.tblBenhNhan);view.selectPatient();select(view.tblHoaDon);view.showDetail(detail());view.busy(false);check(view.btnThuNo.isEnabled()&&view.btnXemChiTiet.isEnabled(),"Debt actions bound to selected invoice");}
            case PAYMENTS -> {var view=(PaymentPanel)panel;var form=new PaymentForm();form.cmbPhuongThuc.setSelectedItem(PaymentForm.Method.MIXED);form.showInvoice(detail());form.busy(false,true,false);form.txtSoTien.setText("800.000");form.txtTienMat.setText("300.000");form.txtChuyenKhoan.setText("500.000");form.txtKhachDua.setText("300.000");form.txtMaGiaoDich.setText("TEST-REF");var listeners=view.cmbHoaDon.getActionListeners();for(var listener:listeners)view.cmbHoaDon.removeActionListener(listener);view.showChoices(List.of(invoice()),"TESTHD01");for(var listener:listeners)view.cmbHoaDon.addActionListener(listener);view.showForm(form);view.pickerEnabled(true);view.lblTrangThai.setText("Đang xem hóa đơn TESTHD01 · dữ liệu tổng hợp kiểm tra bố cục.");form.lblTrangThai.setText("Kiểm tra khoản thu và xác nhận đã nhận tiền thực tế trước khi lưu.");check(form.lblPhanBo.getText().contains("Khớp")&&form.lblNoSauThu.getText().contains("200.000"),"Allocation and remaining-debt preview exact");}
            case INVOICE_CANCELLATIONS -> {var view=(CancellationPanel)panel;var request=new YeuCauHuyHoaDon("TESTYC01","TESTHD01","TESTBN","[KIỂM THỬ] Nguyễn An",invoice().tongTien(),invoice().daThu(),"TESTACC",LocalDateTime.of(2026,10,31,11,0),"[KIỂM THỬ] Cần kiểm tra lại chứng từ",YeuCauHuyHoaDon.TrangThai.CHO_DUYET,null,null,null);view.showRows(List.of(request));select(view.tblYeuCau);view.showDetail(new CancellationService.Detail(request,detail()));view.buttons(false,true);view.lblTrangThai.setText("Hóa đơn đã có khoản thu: chặn duyệt hủy khi chưa có quy trình hoàn tiền.");}
            default -> throw new AssertionError(module);
        }
    }
    public static void main(String[] args)throws Exception{
        Files.createDirectories(OUTPUT);Path noConfig=OUTPUT.resolve("no-config");Files.createDirectories(noConfig);String previous=System.getProperty("user.dir");System.setProperty("user.dir",noConfig.toString());
        try{SwingUtilities.invokeAndWait(new Runnable(){public void run(){
            DesktopSession session=ServiceBillingSelfTest.session(Role.SYSTEM_ADMIN);MainFrame frame=null;
            try{
                check(!com.quanlynhakhoa.dev.TeamDevConnection.hasLocalConfig(),"Synthetic principal must never use real SQL config");
                check(BillingUi.parseDate("31/10/2026").equals(LocalDate.of(2026,10,31)),"VN date parsing");try{BillingUi.parseDate("31/02/2026");throw new AssertionError("Impossible date accepted");}catch(IllegalArgumentException expected){checks++;}
                check(BillingUi.moneyInput("1.500.000").equals(new BigDecimal("1500000")),"Grouped money exact");try{BillingUi.moneyInput("15.00");throw new AssertionError("Malformed grouping accepted");}catch(IllegalArgumentException expected){checks++;}
                String error=BillingUi.error(new SQLException("secret SQL parameter must not be logged","S0005",229));check(!error.contains("S0005")&&!error.contains("229")&&error.contains("quyền"),"Human SQL error");
                FeatureRegistry.Builder registry=FeatureRegistry.builder();com.quanlynhakhoa.billing.dao.ConnectionProvider jdbc=new com.quanlynhakhoa.billing.dao.ConnectionProvider(){public java.sql.Connection open()throws SQLException{throw new SQLException("Synthetic display only","08001");}};
                for(Module module:List.of(Module.SERVICE_CATALOG,Module.BILLING,Module.PAYMENTS,Module.INVOICE_CANCELLATIONS,Module.DEBTS))registry.register(module,factory(module,jdbc));
                frame=new MainFrame(session,new Runnable(){public void run(){}},registry.build());
                Dimension maximum=Toolkit.getDefaultToolkit().getScreenSize();
                for(Dimension size:List.of(new Dimension(1050,720),new Dimension(1280,720),new Dimension(1366,768),new Dimension(1920,1080))){
                    JComponent root=(JComponent)frame.getContentPane();root.setSize(size.width,size.height-32);
                    for(Module module:List.of(Module.SERVICE_CATALOG,Module.BILLING,Module.PAYMENTS,Module.INVOICE_CANCELLATIONS,Module.DEBTS)){
                        navigate(frame,module);layout(root);JPanel panel=pages.get(module);check(!(panel.getParent() instanceof JViewport),"No whole-page scroll wrapper: "+module);
                        empty(module,panel);layout(root);visibleControls(panel,root);screenshot(root,module.name().toLowerCase()+"-empty-"+size.width+"x"+size.height);
                        seed(module,panel);layout(root);visibleControls(panel,root);screenshot(root,module.name().toLowerCase()+"-data-"+size.width+"x"+size.height);
                        if(module==Module.INVOICE_CANCELLATIONS){var view=(CancellationPanel)panel;view.showRows(List.of());view.loadFailed(error);layout(root);visibleControls(view,root);screenshot(root,"cancellation-permission-error-"+size.width+"x"+size.height);}
                    }
                    TaoHoaDonDialog dialog=new TaoHoaDonDialog(frame);try{JComponent content=(JComponent)dialog.getContentPane();content.setSize(Math.min(980,size.width-80),Math.min(570,size.height-100));dialog.busy(false,false);layout(content);visibleControls(content,content);screenshot(content,"create-empty-"+size.width+"x"+size.height);dialog.cmbBenhNhan.addItem(new BillingChoice("TESTBN","[KIỂM THỬ] Nguyễn An"));dialog.cmbPhieuKham.addItem(new BillingChoice("TESTPK","31/10/2026 09:15 · Đã kết luận"));dialog.showDraft(new InvoiceDraft("TESTBN","TESTPK","synthetic",List.of(new InvoiceDraft.Line("TESTDV","[KIỂM THỬ] Điều trị răng",1,new BigDecimal("1500000")))));dialog.busy(false,true);screenshot(content,"create-data-"+size.width+"x"+size.height);}finally{dialog.dispose();}
                }
            }catch(Exception failure){throw new RuntimeException(failure);}finally{session.logout();if(frame!=null)frame.dispose();}
        }});}finally{System.setProperty("user.dir",previous);}
        System.out.println("PASS: BillingLayoutSelfTest "+checks+" checks; real shell at 1280x720 and screen size; logic fixtures only; synthetic screenshot output disabled by default (NO real SQL/visible windows).");
    }
}
