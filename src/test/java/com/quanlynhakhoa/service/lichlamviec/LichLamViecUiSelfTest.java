package com.quanlynhakhoa.service.lichlamviec;
import com.quanlynhakhoa.service.lichlamviec.PhanCongService;
import com.quanlynhakhoa.view.lichlamviec.LichLamViecPanel;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import com.quanlynhakhoa.enums.Module;
import com.quanlynhakhoa.view.hethong.MainFrame;
import com.quanlynhakhoa.config.tinhnang.FeatureRegistry;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.util.concurrent.atomic.AtomicReference;
public final class LichLamViecUiSelfTest {
    private static int checks;
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    private static void edt(Runnable run)throws Exception{SwingUtilities.invokeAndWait(run);}
    private static <T>T field(Object object,String name,Class<T> type){try{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return type.cast(f.get(object));}catch(Exception e){throw new AssertionError(e);}}
    private static boolean busy(Object panel){return field(panel,"busy",Boolean.class);}
    private static void await(LichLamViecPanel panel)throws Exception{for(int i=0;i<250;i++){boolean[] done={false};edt(()->done[0]=!busy(panel));if(done[0])return;Thread.sleep(20);}throw new AssertionError("worker timeout");}
    private static JMenu findMenu(Container parent,String title){for(Component c:parent.getComponents()){if(c instanceof JMenu m&&m.getText().startsWith(title))return m;if(c instanceof Container p){JMenu found=findMenu(p,title);if(found!=null)return found;}}return null;}
    private static JMenuItem item(JMenu menu,String title){for(int i=0;i<menu.getItemCount();i++){var item=menu.getItem(i);if(item!=null&&item.getText().equals(title))return item;}return null;}
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container p)layout(p);}
    private static void screenshot(MainFrame frame,String name)throws Exception{edt(()->{try{JComponent root=(JComponent)frame.getContentPane();root.setSize(1280,760);layout(root);var image=new BufferedImage(1280,760,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();root.printAll(g);g.dispose();Path output=Path.of("target/shifts-ui");Files.createDirectories(output);ImageIO.write(image,"png",output.resolve(name).toFile());}catch(Exception e){throw new AssertionError(e);}});}
    public static void main(String[]args)throws Exception{
        var s=PhanCongSelfTest.session(VaiTroHeThong.SYSTEM_ADMIN);var db=new ShiftJdbcDouble();
        db.assignments.add(ShiftJdbcDouble.assignment("TEST1","NV1","CHO_HIEU_LUC"));db.assignments.add(ShiftJdbcDouble.assignment("TEST2","NV2","DA_HUY"));
        AtomicReference<LichLamViecPanel> management=new AtomicReference<>(),mine=new AtomicReference<>();AtomicReference<MainFrame> frame=new AtomicReference<>();
        FeatureRegistry registry=FeatureRegistry.builder()
            .register(Module.SHIFT_ASSIGNMENTS,principal->{var p=new LichLamViecPanel(principal,true,new PhanCongService(principal,db::open),false);management.set(p);return p;})
            .register(Module.MY_SCHEDULE,principal->{var p=new LichLamViecPanel(principal,false,new PhanCongService(principal,db::open),false);mine.set(p);return p;}).build();
        try{
            edt(()->{frame.set(new MainFrame(s,()->{},registry));JMenu planning=findMenu(frame.get().getContentPane(),"Kế hoạch");check(planning!=null,"planning menu");JMenuItem assignment=item(planning,"Phân công ca");check(assignment!=null&&assignment.isEnabled(),"real enabled menu");assignment.doClick();check(management.get()!=null,"menu creates real assignment panel");management.get().refreshData();});
            await(management.get());edt(()->{check(field(management.get(),"table",JTable.class).getRowCount()==2,"assignment rows including history");check(field(management.get(),"btnTao",JButton.class).isEnabled(),"create enabled after choices loaded");check(!field(management.get(),"btnHuy",JButton.class).isEnabled(),"cancel requires selection");field(management.get(),"table",JTable.class).setRowSelectionInterval(1,1);check(!field(management.get(),"btnHuy",JButton.class).isEnabled(),"cancelled assignment disabled");});
            edt(()->{check(field(management.get(),"tuKhoa",JTextField.class).getToolTipText().contains("không tìm nội dung bảng"),"search scope explained");field(management.get(),"nhanVien",JComboBox.class).setSelectedIndex(1);management.get().refreshData();});await(management.get());
            edt(()->{check(field(management.get(),"table",JTable.class).getRowCount()==1,"employee combo filters table");field(management.get(),"tuKhoa",JTextField.class).setText("no match");management.get().refreshData();});await(management.get());
            edt(()->{check(field(management.get(),"nhanVien",JComboBox.class).getSelectedItem()!=null,"active filter retained outside search results");check(field(management.get(),"table",JTable.class).getRowCount()==1,"search does not silently clear table filter");field(management.get(),"tuKhoa",JTextField.class).setText("");field(management.get(),"nhanVien",JComboBox.class).setSelectedIndex(0);management.get().refreshData();});await(management.get());
            screenshot(frame.get(),"assignment-synthetic.png");
            edt(()->{item(findMenu(frame.get().getContentPane(),"Kế hoạch"),"Lịch làm việc của tôi").doClick();check(mine.get()!=null,"real personal panel");mine.get().refreshData();});await(mine.get());
            edt(()->{check(field(mine.get(),"table",JTable.class).getRowCount()==1,"personal rows only");check(field(mine.get(),"nhanVien",JComboBox.class).getParent()==null,"no arbitrary employee selector");check(field(mine.get(),"btnTao",JButton.class).getParent()==null,"no personal write action");});screenshot(frame.get(),"my-schedule-synthetic.png");
            db.assignments.add(ShiftJdbcDouble.assignment("TEST3","NV1","DANG_THUC_HIEN"));db.assignments.add(ShiftJdbcDouble.assignment("TEST4","NV1","HOAN_THANH"));
            edt(()->management.get().refreshData());await(management.get());
            edt(()->{JTable t=field(management.get(),"table",JTable.class);check(t.getRowCount()==4,"four state rows for cancel UI");for(int row=0;row<t.getRowCount();row++){t.setRowSelectionInterval(row,row);check(field(management.get(),"btnHuy",JButton.class).isEnabled()=="Chờ hiệu lực".equals(t.getValueAt(row,6)),"cancel button follows PO matrix: "+t.getValueAt(row,6));}});
            // Exercise the async error path: a stale/forbidden UPDATE must never show success.
            edt(()->{try{Class<?> operation=Class.forName(LichLamViecPanel.class.getName()+"$Operation");Object task=java.lang.reflect.Proxy.newProxyInstance(operation.getClassLoader(),new Class<?>[]{operation},(o,m,a)->{new PhanCongService(s,db::open).cancel("TEST4","Lý do");return "TEST4";});var write=LichLamViecPanel.class.getDeclaredMethod("write",operation,String.class);write.setAccessible(true);write.invoke(management.get(),task,"Đã hủy phân công. ");}catch(Exception e){throw new AssertionError(e);}});await(management.get());
            edt(()->{String message=field(management.get(),"trangThai",JLabel.class).getText();check(message.contains("Chỉ hủy phân công chờ hiệu lực")&&!message.contains("Đã hủy phân công"),"failed conditional UPDATE cannot display success");});
            db.linkedEmployee=null;edt(()->mine.get().refreshData());await(mine.get());edt(()->{check(field(mine.get(),"table",JTable.class).getRowCount()==0,"missing association clears previous data");check(field(mine.get(),"trangThai",JLabel.class).getText().contains("chưa liên kết"),"association feedback");});
            edt(()->{field(mine.get(),"tuNgay",JTextField.class).setText("invalid");mine.get().refreshData();check(field(mine.get(),"trangThai",JLabel.class).getText().contains("không hợp lệ"),"date validation");});
            s.logout();edt(()->{management.get().refreshData();check(field(management.get(),"table",JTable.class).getRowCount()==0,"logout clears data");check(!field(management.get(),"btnTao",JButton.class).isEnabled(),"logout disables writes");});
        }finally{edt(()->{if(frame.get()!=null)frame.get().dispose();});}
        for(VaiTroHeThong role:VaiTroHeThong.values()){
            var principal=PhanCongSelfTest.session(role);boolean manager=role==VaiTroHeThong.SYSTEM_ADMIN||role==VaiTroHeThong.CLINIC_MANAGER;
            edt(()->{MainFrame shell=new MainFrame(principal,()->{},FeatureRegistry.builder().build());try{JMenu planning=findMenu(shell.getContentPane(),"Kế hoạch");check(item(planning,"Lịch làm việc của tôi")!=null,"self menu "+role);check((item(planning,"Phân công ca")!=null)==manager,"management menu "+role);}finally{principal.logout();shell.dispose();}});
        }
        System.out.println("PASS: LichLamViecUiSelfTest "+checks+" Swing checks; synthetic JDBC only, screenshots are test fixtures, not database evidence.");
    }
}
