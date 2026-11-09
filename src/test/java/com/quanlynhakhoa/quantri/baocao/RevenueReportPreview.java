package com.quanlynhakhoa.quantri.baocao;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.*;
import com.quanlynhakhoa.enums.VaiTroHeThong;

/** Synthetic design preview; no database connection or writes. */
public final class RevenueReportPreview {
    public static void main(String[] args)throws Exception{
        SwingUtilities.invokeAndWait(()->{try{
            var session=RevenueReportSelfTest.session(VaiTroHeThong.CLINIC_MANAGER);
            var panel=new RevenueReportPanel(session,new RevenueReportService(session,()->{throw new AssertionError("preview cannot access database");}),false);
            LocalDate from=LocalDate.of(2026,11,1),to=LocalDate.of(2026,11,6);
            long[] revenue={2400000,3850000,1750000,4200000,3100000,2900000},paid={1900000,3100000,2100000,3450000,2700000,3350000};
            var rows=new ArrayList<RevenueReport.Row>();BigDecimal total=BigDecimal.ZERO,collected=BigDecimal.ZERO;int invoices=0;
            for(int i=0;i<6;i++){int count=5+i;invoices+=count;rows.add(new RevenueReport.Row(from.plusDays(i),count,BigDecimal.valueOf(revenue[i]),BigDecimal.valueOf(paid[i])));total=total.add(BigDecimal.valueOf(revenue[i]));collected=collected.add(BigDecimal.valueOf(paid[i]));}
            panel.showReport(new RevenueReport(from,to,false,rows,total,collected,new BigDecimal("4250000"),invoices));
            JPanel root=new JPanel(new BorderLayout());root.setBackground(new Color(245,248,252));
            JPanel menu=new JPanel(new FlowLayout(FlowLayout.LEFT,28,12));menu.setBackground(new Color(29,61,102));
            for(String text:new String[]{"Tổng quan","Tổ chức ▾","Kế hoạch ▾","Tiếp nhận ▾","Hoạt động ▾","Tổng kết ▾"}){JLabel item=new JLabel(text);item.setForeground(Color.WHITE);item.setFont(new Font("SansSerif",Font.BOLD,13));menu.add(item);}
            root.add(menu,BorderLayout.NORTH);root.add(panel,BorderLayout.CENTER);root.setSize(1280,900);RevenueReportSelfTest.layout(root);
            BufferedImage image=new BufferedImage(1280,900,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();root.printAll(g);g.dispose();
            Path output=Path.of("target/revenue-report-design.png");ImageIO.write(image,"png",output.toFile());
            System.out.println("Preview saved: "+output.toAbsolutePath()+" (synthetic data)");
        }catch(Exception error){throw new RuntimeException(error);}});
    }
}
