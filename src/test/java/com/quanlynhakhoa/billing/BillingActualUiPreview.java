package com.quanlynhakhoa.billing;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.billing.dao.*;
import com.quanlynhakhoa.billing.model.*;
import com.quanlynhakhoa.billing.service.*;
import com.quanlynhakhoa.billing.ui.*;
import com.quanlynhakhoa.dev.TeamDevConnection;
import com.quanlynhakhoa.dichvu.dao.DichVuDao;
import com.quanlynhakhoa.dichvu.model.DichVu;
import com.quanlynhakhoa.dichvu.ui.DichVuPanel;
import com.quanlynhakhoa.features.*;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.navigation.Module;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.view.MainFrame;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;

/** Read actual permitted dev data before rendering. No invented business rows or authenticated GUI session. */
public final class BillingActualUiPreview {
    private record Data(List<DichVu> services,List<HoaDon> invoices,List<YeuCauHuyHoaDon> requests,List<BillingChoice> patients,List<BillingChoice> visits,Map<String,ChiTietBilling> details){}
    private static Data read()throws Exception{
        if(!TeamDevConnection.hasLocalConfig())throw new IllegalStateException("Approved local dev configuration required");
        try(Connection c=TeamDevConnection.open()){
            c.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);c.setAutoCommit(false);
            try{
                var services=new DichVuDao().find(c,"",null);var invoiceDao=new HoaDonDao();var invoices=invoiceDao.find(c,"",null,null,null);var requests=new CancellationDao().find(c,"",null,null,null);
                var patients=new InvoiceCreateDao().patients(c,"");var visits=patients.isEmpty()?List.<BillingChoice>of():new InvoiceCreateDao().visits(c,patients.getFirst().id());
                Map<String,ChiTietBilling> details=new HashMap<>();
                for(HoaDon h:invoices)details.put(h.maHoaDon(),new ChiTietBilling(h,invoiceDao.lines(c,h.maHoaDon()),invoiceDao.payments(c,h.maHoaDon())));
                c.commit();System.out.println("Actual SQL snapshot: services="+services.size()+", invoices="+invoices.size()+", cancellation requests="+requests.size()+", patients="+patients.size()+", candidate visits for first patient="+visits.size());
                return new Data(services,invoices,requests,patients,visits,details);
            }catch(Exception e){c.rollback();throw e;}
        }
    }
    private static void layout(Container root){root.doLayout();for(Component c:root.getComponents())if(c instanceof Container nested)layout(nested);}
    private static void select(JTable table){if(table.getRowCount()==0)return;var model=(DefaultListSelectionModel)table.getSelectionModel();ListSelectionListener[] listeners=model.getListSelectionListeners();for(var listener:listeners)model.removeListSelectionListener(listener);table.setRowSelectionInterval(0,0);for(var listener:listeners)model.addListSelectionListener(listener);}
    private static JTable firstTable(Container root){for(Component c:root.getComponents()){if(c instanceof JTable table)return table;if(c instanceof Container nested){JTable table=firstTable(nested);if(table!=null)return table;}}return null;}
    private static void screenshot(JComponent root,Path path)throws Exception{
        layout(root);layout(root);var transform=GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform();
        BufferedImage bitmap=new BufferedImage((int)Math.ceil(root.getWidth()*transform.getScaleX()),(int)Math.ceil(root.getHeight()*transform.getScaleY()),BufferedImage.TYPE_INT_RGB);var graphics=bitmap.createGraphics();graphics.scale(transform.getScaleX(),transform.getScaleY());root.printAll(graphics);graphics.dispose();ImageIO.write(bitmap,"png",path.toFile());
    }
    private static void bind(Module module,JPanel panel,Data data){
        switch(module){
            case SERVICE_CATALOG -> {var view=(DichVuPanel)panel;view.showRows(data.services());if(!data.services().isEmpty())view.showForm(data.services().getFirst());view.status(data.services().isEmpty()?"Chưa có dịch vụ. Nhập thông tin và lưu.":data.services().size()+" dịch vụ từ SQL development.");}
            case BILLING -> {var view=(HoaDonPanel)panel;view.showRows(data.invoices());if(!data.invoices().isEmpty()){select(firstTable(view));view.showDetail(data.details().get(data.invoices().getFirst().maHoaDon()));}view.setBusy(false);view.status(data.invoices().isEmpty()?"Chưa có hóa đơn.":"Dữ liệu chứng từ từ SQL development.");}
            case DEBTS -> {var view=(DebtPanel)panel;view.showPatients(DebtService.byPatient(data.invoices()));select(view.tblBenhNhan);view.selectPatient();select(view.tblHoaDon);if(view.selectedInvoice()!=null)view.showDetail(data.details().get(view.selectedInvoice()));view.busy(false);view.lblTrangThai.setText("Các số tổng hợp tính theo bộ lọc ngày lập hóa đơn.");}
            case PAYMENTS -> {var view=(PaymentPanel)panel;var found=DebtService.outstanding(data.invoices());var listeners=view.cmbHoaDon.getActionListeners();for(var listener:listeners)view.cmbHoaDon.removeActionListener(listener);view.showChoices(found,null);for(var listener:listeners)view.cmbHoaDon.addActionListener(listener);view.lblTrangThai.setText(found.isEmpty()?"Chưa có hóa đơn còn phải thu.":"Chọn hóa đơn từ kết quả tìm kiếm.");}
            case INVOICE_CANCELLATIONS -> {var view=(CancellationPanel)panel;view.showRows(data.requests());if(!data.requests().isEmpty()){var y=data.requests().getFirst();select(view.tblYeuCau);if(data.details().containsKey(y.maHoaDon()))view.showDetail(new CancellationService.Detail(y,data.details().get(y.maHoaDon())));}view.lblTrangThai.setText("Lọc theo ngày gửi và trạng thái yêu cầu.");view.tableStatus(data.requests().isEmpty()?"Chưa có yêu cầu hủy hóa đơn.":"");}
            default -> throw new AssertionError(module);
        }
    }
    public static void main(String[] args)throws Exception{
        Data data=read();System.out.println("Device scale: "+GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform().getScaleX());Path output=Path.of("target/ui-refinement-real").toAbsolutePath();String tag=args.length==0?"100":args[0];output=output.resolve("dpi"+tag);Files.createDirectories(output);final Path folder=output;
        String previous=System.getProperty("user.dir");Path noConfig=folder.resolve("no-config");Files.createDirectories(noConfig);System.setProperty("user.dir",noConfig.toString());
        // Display-only principal. All UI connections are disabled; real reads above use the app SQL login only.
        try{SwingUtilities.invokeAndWait(new Runnable(){public void run(){
            ConnectionProvider disabled=new ConnectionProvider(){public Connection open()throws SQLException{throw new SQLException("Read-only snapshot: interactive SQL disabled");}};
            for(Dimension size:List.of(new Dimension(1280,720),new Dimension(1366,768),new Dimension(1920,1080))){
                DesktopSession display=DesktopSession.fromSuccessfulLogin(new AuthDecision(true,"XEM-CHI-DOC",Role.SYSTEM_ADMIN,false));Map<Module,JPanel> pages=new EnumMap<>(Module.class);FeatureRegistry.Builder registry=FeatureRegistry.builder();
                for(final Module module:List.of(Module.SERVICE_CATALOG,Module.BILLING,Module.PAYMENTS,Module.INVOICE_CANCELLATIONS,Module.DEBTS))registry.register(module,new FeaturePanelFactory(){public JPanel create(DesktopSession session){JPanel panel=switch(module){case SERVICE_CATALOG -> new DichVuPanel(session,disabled);case BILLING -> new HoaDonPanel(session,disabled);case PAYMENTS -> new PaymentPanel(session,disabled);case INVOICE_CANCELLATIONS -> new CancellationPanel(session,disabled);case DEBTS -> new DebtPanel(session,disabled);default -> throw new AssertionError(module);};pages.put(module,panel);return panel;}});
                MainFrame frame=new MainFrame(display,new Runnable(){public void run(){}},registry.build());
                try{
                    JComponent root=(JComponent)frame.getContentPane();root.setSize(size.width,size.height-32);var navigate=MainFrame.class.getDeclaredMethod("navigate",Module.class);navigate.setAccessible(true);
                    for(Module module:List.of(Module.SERVICE_CATALOG,Module.BILLING,Module.PAYMENTS,Module.INVOICE_CANCELLATIONS,Module.DEBTS)){navigate.invoke(frame,module);bind(module,pages.get(module),data);screenshot(root,folder.resolve(module.name().toLowerCase()+"-"+size.width+"x"+size.height+".png"));}
                    TaoHoaDonDialog dialog=new TaoHoaDonDialog(frame);try{JComponent content=(JComponent)dialog.getContentPane();content.setSize(956,560);for(BillingChoice patient:data.patients())dialog.cmbBenhNhan.addItem(patient);dialog.cmbBenhNhan.setSelectedIndex(-1);dialog.busy(false,false);dialog.guide(data.patients().isEmpty()?"Tìm bệnh nhân để bắt đầu":"Chọn bệnh nhân");screenshot(content,folder.resolve("create-"+size.width+"x"+size.height+".png"));}finally{dialog.dispose();}
                }catch(Exception failure){throw new RuntimeException(failure);}finally{display.logout();frame.dispose();}
            }
        }});}finally{System.setProperty("user.dir",previous);}
        System.out.println("PASS: actual-data read-only UI snapshots: "+output+"; no authenticated interactive GUI session, no generated business data or SQL writes.");
    }
}
