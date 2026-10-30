package com.quanlynhakhoa.view;

import com.quanlynhakhoa.auth.AuthDecision;
import com.quanlynhakhoa.features.FeatureBootstrap;
import com.quanlynhakhoa.model.Role;
import com.quanlynhakhoa.navigation.Module;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Native Swing construction without showing a window. Synthetic principal; SQL config deliberately unavailable. */
public final class LatestMainShellSelfTest {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static JMenuBar menu(Container root){for(Component child:root.getComponents()){if(child instanceof JMenuBar bar)return bar;if(child instanceof Container nested){JMenuBar found=menu(nested);if(found!=null)return found;}}return null;}
    private static JMenu group(JMenuBar bar,String name){for(Component c:bar.getComponents())if(c instanceof JMenu m&&m.getText().startsWith(name))return m;throw new AssertionError("Missing menu "+name);}
    private static JMenuItem item(JMenu parent,String name){for(int i=0;i<parent.getItemCount();i++){JMenuItem child=parent.getItem(i);if(child!=null&&child.getText().equals(name))return child;}throw new AssertionError("Missing item "+name);}
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container nested)layout(nested);}
    public static void main(String[] args)throws Exception {
        final Path root=Path.of("").toAbsolutePath();Path empty=root.resolve("target/no-config-shell-test");Files.createDirectories(empty);
        String previous=System.getProperty("user.dir");System.setProperty("user.dir",empty.toString());
        try{SwingUtilities.invokeAndWait(new Runnable(){public void run(){
            DesktopSession session=DesktopSession.fromSuccessfulLogin(new AuthDecision(true,"SYNTHETIC",Role.SYSTEM_ADMIN,false));
            check(!com.quanlynhakhoa.dev.TeamDevConnection.hasLocalConfig(),"never run synthetic shell test against a configured DB");
            MainFrame winMain=new MainFrame(session,new Runnable(){public void run(){}},FeatureBootstrap.createRegistry());
            try{
                JMenuBar bar=menu(winMain.getContentPane());check(bar!=null,"latest main horizontal menu retained");
                java.util.List<String> headers=new ArrayList<>();for(Component c:bar.getComponents())if(c instanceof JMenu m)headers.add(m.getText().replace("▾","").trim());else if(c instanceof JButton b)headers.add(b.getText());
                check(headers.equals(java.util.List.of("Tổng quan","Tổ chức","Kế hoạch","Tiếp nhận","Hoạt động","Tổng kết")),"exact latest-main menu structure: "+headers);
                item(group(bar,"Tổ chức"),"Dịch vụ & Bảng giá");item(group(bar,"Hoạt động"),"Hóa đơn");item(group(bar,"Hoạt động"),"Thanh toán");item(group(bar,"Hoạt động"),"Duyệt hủy hóa đơn");item(group(bar,"Tổng kết"),"Công nợ");
                check(FeatureBootstrap.createRegistry().contains(Module.PATIENTS),"latest patient implementation retained");
                for(String name:java.util.List.of("Hóa đơn","Thanh toán","Duyệt hủy hóa đơn"))item(group(bar,"Hoạt động"),name).doClick();
                item(group(bar,"Tổng kết"),"Công nợ").doClick();item(group(bar,"Tổ chức"),"Dịch vụ & Bảng giá").doClick();
                JPanel pnlContent=(JPanel)winMain.getContentPane();pnlContent.setSize(1360,800);layout(pnlContent);BufferedImage image=new BufferedImage(1360,800,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();pnlContent.printAll(graphics);graphics.dispose();
                try{ImageIO.write(image,"png",root.resolve("target/latest-shell-preview.png").toFile());}catch(java.io.IOException e){throw new RuntimeException(e);}
            }finally{session.logout();winMain.dispose();}
        }});}finally{System.setProperty("user.dir",previous);}
        System.out.println("PASS: LatestMainShellSelfTest (six menus, real feature factories and lazy navigation; synthetic/no SQL/no visible window).");
    }
}
