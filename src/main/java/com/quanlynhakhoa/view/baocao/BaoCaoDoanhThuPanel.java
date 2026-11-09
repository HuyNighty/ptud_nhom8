package com.quanlynhakhoa.view.baocao;

import com.quanlynhakhoa.security.RolePermissionPolicy;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.service.baocao.BaoCaoDoanhThuService;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.model.baocao.DongBaoCaoDoanhThu;
import com.quanlynhakhoa.model.baocao.BaoCaoDoanhThu;
import com.quanlynhakhoa.config.tinhnang.RefreshableFeature;
import com.quanlynhakhoa.controller.hoadon.SessionWorker;
import com.quanlynhakhoa.view.hoadon.GiaoDienHoaDon;
import com.quanlynhakhoa.view.benhnhan.ChonNgayDialog;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** Native Swing report screen following UC029 and the existing six-menu shell. */
public final class BaoCaoDoanhThuPanel extends JPanel implements RefreshableFeature {
    private static final Color NAVY = new Color(29,61,102), TEAL = new Color(26,132,126);
    private static final Color BG = new Color(245,248,252), MUTED = new Color(102,116,136), LINE = new Color(220,230,240);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private final DesktopSession session;
    private final BaoCaoDoanhThuService service;
    private final JTextField tuNgay = new JTextField(10), denNgay = new JTextField(10);
    private final JComboBox<String> kyBaoCao = new JComboBox<>(new String[]{"Tháng này", "Hôm nay", "7 ngày gần nhất", "Tháng trước", "Tùy chọn"});
    private final JComboBox<String> cachNhom = new JComboBox<>(new String[]{"Theo ngày", "Theo tháng"});
    private final JButton lichTuNgay = new JButton("Lịch"), lichDenNgay = new JButton("Lịch");
    private final JButton view = new JButton("Xem báo cáo");
    private final JLabel doanhThu = new JLabel("—"), daThu = new JLabel("—"), congNo = new JLabel("—"), hoaDon = new JLabel("—");
    private final JLabel trangThai = new JLabel(" "), khoangNgay = new JLabel(" ");
    private final DefaultTableModel model = GiaoDienHoaDon.model("Ngày", "Số hóa đơn", "Doanh thu", "Tiền đã thu");
    private final JTable table = GiaoDienHoaDon.table(model);
    private final Chart chart = new Chart();
    private boolean busy, updatingPeriod;

    public BaoCaoDoanhThuPanel(DesktopSession session) { this(session, new BaoCaoDoanhThuService(session), true); }
    public BaoCaoDoanhThuPanel(DesktopSession session, BaoCaoDoanhThuService service, boolean autoLoad) {
        new AuthorizationService(new RolePermissionPolicy()).require(session, Quyen.REVENUE_REPORT_READ);
        this.session = session; this.service = service;
        setLayout(new BorderLayout(0,16)); setBackground(BG); setBorder(new EmptyBorder(24,26,24,26));
        JPanel top = transparent(new BorderLayout(0,16));
        JPanel heading = transparent(new BorderLayout());
        JLabel title = new JLabel("Thống kê doanh thu"); title.setFont(new Font("SansSerif",Font.BOLD,23)); title.setForeground(NAVY);
        heading.add(title,BorderLayout.WEST); top.add(heading,BorderLayout.NORTH);
        JPanel filters = new JPanel(new GridBagLayout()); filters.setBackground(Color.WHITE); filters.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),new EmptyBorder(12,12,12,12)));
        addFilter(filters,0,"Kỳ báo cáo",kyBaoCao,1);
        addFilter(filters,1,"Từ ngày",dateField(tuNgay,lichTuNgay),1);
        addFilter(filters,2,"Đến ngày",dateField(denNgay,lichDenNgay),1);
        addFilter(filters,3,"Hiển thị",cachNhom,.6);
        addFilter(filters,4," ",view,0);
        top.add(filters,BorderLayout.CENTER);
        JPanel cards = transparent(new GridLayout(1,4,12,0));
        cards.add(card("DOANH THU",doanhThu,"Hóa đơn còn hiệu lực",NAVY));
        cards.add(card("TIỀN ĐÃ THU",daThu,"Theo ngày thu tiền",TEAL));
        cards.add(card("CÔNG NỢ CUỐI KỲ",congNo,"Đến hết ngày kết thúc",new Color(150,93,30)));
        cards.add(card("HÓA ĐƠN TRONG KỲ",hoaDon,"Không tính hóa đơn đã hủy",NAVY));
        top.add(cards,BorderLayout.SOUTH); add(top,BorderLayout.NORTH);

        JPanel plot = new JPanel(new BorderLayout(0,8)); plot.setBackground(Color.WHITE); plot.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),new EmptyBorder(12,14,8,14)));
        JPanel plotHeading = transparent(new BorderLayout()); JLabel plotTitle = new JLabel("Doanh thu và tiền đã thu"); plotTitle.setFont(new Font("SansSerif",Font.BOLD,15)); plotTitle.setForeground(NAVY);
        plotHeading.add(plotTitle,BorderLayout.WEST); khoangNgay.setForeground(MUTED); plotHeading.add(khoangNgay,BorderLayout.EAST); plot.add(plotHeading,BorderLayout.NORTH);plot.add(chart,BorderLayout.CENTER);
        JPanel legend = transparent(new FlowLayout(FlowLayout.LEFT,16,0));legend.add(legend("Doanh thu",NAVY));legend.add(legend("Tiền đã thu",TEAL));plot.add(legend,BorderLayout.SOUTH);
        JPanel details = new JPanel(new BorderLayout(0,8));details.setOpaque(false);
        JLabel tableTitle = new JLabel("Chi tiết theo thời gian");tableTitle.setFont(new Font("SansSerif",Font.BOLD,15));tableTitle.setForeground(NAVY);details.add(tableTitle,BorderLayout.NORTH);
        table.setRowHeight(32);table.getTableHeader().setFont(new Font("SansSerif",Font.BOLD,13));table.getTableHeader().setPreferredSize(new Dimension(0,32));
        GiaoDienHoaDon.columns(table,160,160,240,240);details.add(GiaoDienHoaDon.scroll(table),BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,plot,details);split.setResizeWeight(.55);split.setDividerSize(8);split.setBorder(null);split.setOpaque(false);
        plot.setMinimumSize(new Dimension(200,160));details.setMinimumSize(new Dimension(200,140)); add(split,BorderLayout.CENTER);
        JPanel bottom = transparent(new BorderLayout(0,5));
        trangThai.setForeground(MUTED);bottom.add(trangThai,BorderLayout.SOUTH);add(bottom,BorderLayout.SOUTH);
        for(JComponent input:new JComponent[]{tuNgay,denNgay,kyBaoCao,cachNhom,lichTuNgay,lichDenNgay,view}){
            Dimension size=input.getPreferredSize();input.setPreferredSize(new Dimension(size.width,34));
        }
        tuNgay.setEditable(false);denNgay.setEditable(false);tuNgay.setHorizontalAlignment(JTextField.CENTER);denNgay.setHorizontalAlignment(JTextField.CENTER);
        kyBaoCao.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){if(!updatingPeriod)setPreset();}});
        lichTuNgay.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){chooseDate(tuNgay);}});
        lichDenNgay.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){chooseDate(denNgay);}});
        view.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){refreshData();}});
        setPreset();setBusy(false);if(autoLoad)SwingUtilities.invokeLater(new Runnable(){public void run(){refreshData();}});
    }
    private static JPanel transparent(LayoutManager layout){JPanel p=new JPanel(layout);p.setOpaque(false);return p;}
    private static JPanel dateField(JTextField value,JButton calendar){JPanel p=transparent(new BorderLayout(6,0));p.add(value,BorderLayout.CENTER);p.add(calendar,BorderLayout.EAST);return p;}
    private static void addFilter(JPanel panel,int x,String title,JComponent input,double weight){
        JPanel field=transparent(new BorderLayout(0,6));JLabel label=new JLabel(title);label.setForeground(MUTED);label.setLabelFor(input);field.add(label,BorderLayout.NORTH);field.add(input,BorderLayout.CENTER);
        GridBagConstraints c=new GridBagConstraints();c.gridx=x;c.gridy=0;c.weightx=weight;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(0,x==0?0:12,0,0);panel.add(field,c);
    }
    private static JPanel card(String title,JLabel amount,String hint,Color color){
        JPanel p=new JPanel(new BorderLayout(0,8));p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),new EmptyBorder(14,16,14,16)));
        JLabel name=new JLabel(title);name.setFont(new Font("SansSerif",Font.BOLD,12));name.setForeground(MUTED);p.add(name,BorderLayout.NORTH);
        amount.setFont(new Font("SansSerif",Font.BOLD,23));amount.setForeground(color);p.add(amount,BorderLayout.CENTER);
        JLabel caption=new JLabel(hint);caption.setFont(new Font("SansSerif",Font.PLAIN,12));caption.setForeground(MUTED);p.add(caption,BorderLayout.SOUTH);return p;
    }
    private static JLabel legend(String text,Color color){JLabel label=new JLabel("■  "+text);label.setForeground(color);return label;}
    private void setPreset(){
        if(kyBaoCao.getSelectedIndex()==4)return;LocalDate today=LocalDate.now(),start=today.withDayOfMonth(1),end=today;
        if(kyBaoCao.getSelectedIndex()==1)start=today;
        if(kyBaoCao.getSelectedIndex()==2)start=today.minusDays(6);
        if(kyBaoCao.getSelectedIndex()==3){start=today.minusMonths(1).withDayOfMonth(1);end=start.withDayOfMonth(start.lengthOfMonth());}
        tuNgay.setText(DATE.format(start));denNgay.setText(DATE.format(end));
    }
    private void chooseDate(JTextField field){
        LocalDate chosen=ChonNgayDialog.chon(SwingUtilities.getWindowAncestor(this),"Chọn ngày báo cáo",LocalDate.parse(field.getText(),DATE),LocalDate.of(1900,1,1),LocalDate.now());
        if(chosen!=null){field.setText(DATE.format(chosen));updatingPeriod=true;kyBaoCao.setSelectedIndex(4);updatingPeriod=false;}
    }
    private void setBusy(boolean value){busy=value;for(JComponent c:new JComponent[]{lichTuNgay,lichDenNgay,kyBaoCao,cachNhom,view})c.setEnabled(!value&&session.isActive());}
    private void clear(){model.setRowCount(0);chart.setRows(List.of(),false);doanhThu.setText("—");daThu.setText("—");congNo.setText("—");hoaDon.setText("—");khoangNgay.setText(" ");}
    @Override public void refreshData(){
        if(busy)return;if(!session.isActive()){clear();setBusy(false);trangThai.setText("Phiên đăng nhập đã kết thúc.");return;}
        final LocalDate start,end;
        try{start=LocalDate.parse(tuNgay.getText(),DATE);end=LocalDate.parse(denNgay.getText(),DATE);BaoCaoDoanhThuService.validate(start,end);}
        catch(IllegalArgumentException|java.time.DateTimeException error){trangThai.setText("Từ ngày phải trước hoặc bằng đến ngày.");return;}
        final boolean monthly=cachNhom.getSelectedIndex()==1;clear();setBusy(true);trangThai.setText("Đang tổng hợp số liệu…");
        new SessionWorker<BaoCaoDoanhThu>(session){
            protected BaoCaoDoanhThu doInBackground()throws Exception{return service.load(start,end,monthly);}
            protected void finished(){setBusy(false);}
            protected void expired(){clear();setBusy(false);trangThai.setText("Phiên đăng nhập đã kết thúc.");}
            protected void succeeded(BaoCaoDoanhThu value){showReport(value);}
            protected void failed(Throwable failure){clear();setBusy(false);trangThai.setText("Không tải được báo cáo. "+GiaoDienHoaDon.error(failure));}
        }.execute();
    }
    public void showReport(BaoCaoDoanhThu value){
        tuNgay.setText(DATE.format(value.from()));denNgay.setText(DATE.format(value.to()));cachNhom.setSelectedIndex(value.monthly()?1:0);
        doanhThu.setText(GiaoDienHoaDon.vnd(value.revenue()));daThu.setText(GiaoDienHoaDon.vnd(value.collected()));congNo.setText(GiaoDienHoaDon.vnd(value.closingDebt()));hoaDon.setText(Integer.toString(value.invoices()));
        khoangNgay.setText(DATE.format(value.from())+" – "+DATE.format(value.to()));model.setRowCount(0);
        table.getColumnModel().getColumn(0).setHeaderValue(value.monthly()?"Tháng":"Ngày");table.getTableHeader().repaint();
        DateTimeFormatter periodFormat=DateTimeFormatter.ofPattern(value.monthly()?"MM/uuuu":"dd/MM/uuuu");
        for(var row:value.rows())model.addRow(new Object[]{periodFormat.format(row.period()),row.invoices(),row.revenue(),row.collected()});
        chart.setRows(value.rows(),value.monthly());trangThai.setText(value.invoices()==0&&value.collected().signum()==0?"Không phát sinh doanh thu hoặc khoản thu trong kỳ đã chọn.":" ");setBusy(false);
    }
    private static final class Chart extends JPanel {
        private List<DongBaoCaoDoanhThu> danhSach=List.of();private boolean theoThang;
        Chart(){setBackground(Color.WHITE);setPreferredSize(new Dimension(800,240));}
        void setRows(List<DongBaoCaoDoanhThu> value,boolean monthly){this.danhSach=List.copyOf(value);this.theoThang=monthly;repaint();}
        @Override protected void paintComponent(Graphics graphics){
            super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();
            try{
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int left=78,right=getWidth()-16,top=18,bottom=getHeight()-32;
                if(right<=left||bottom<=top)return;
                double max=0,min=0;for(var row:danhSach){max=Math.max(max,Math.max(row.revenue().doubleValue(),row.collected().doubleValue()));min=Math.min(min,Math.min(row.revenue().doubleValue(),row.collected().doubleValue()));}
                if(max==0&&min==0){g.setColor(MUTED);g.drawString("Chưa có số liệu phát sinh trong kỳ",left,top+35);return;}
                double span=max-min;double baseline=top+max/span*(bottom-top);g.setFont(new Font("SansSerif",Font.PLAIN,11));
                for(int i=0;i<=4;i++){int y=top+(bottom-top)*i/4;g.setColor(LINE);g.drawLine(left,y,right,y);g.setColor(MUTED);double value=max-span*i/4;String label=Math.abs(value)>=1000000?String.format(java.util.Locale.ROOT,"%.1f tr",value/1000000):String.format(java.util.Locale.ROOT,"%.0f k",value/1000);g.drawString(label,8,y+4);}
                double group=(right-left)/(double)Math.max(1,danhSach.size()),bar=Math.max(.5,Math.min(22,group*.3));int step=Math.max(1,(int)Math.ceil(danhSach.size()/10.0));
                DateTimeFormatter format=DateTimeFormatter.ofPattern(theoThang?"MM/yy":"dd/MM");
                for(int i=0;i<danhSach.size();i++){var row=danhSach.get(i);double center=left+(i+.5)*group;
                    double h=row.revenue().doubleValue()/span*(bottom-top);g.setColor(NAVY);g.fill(new RoundRectangle2D.Double(center-bar-1,h>=0?baseline-h:baseline,bar,Math.abs(h),3,3));
                    h=row.collected().doubleValue()/span*(bottom-top);g.setColor(TEAL);g.fill(new RoundRectangle2D.Double(center+1,h>=0?baseline-h:baseline,bar,Math.abs(h),3,3));
                    if(i%step==0){String label=format.format(row.period());g.setColor(MUTED);g.drawString(label,(int)center-g.getFontMetrics().stringWidth(label)/2,bottom+20);}
                }
            }finally{g.dispose();}
        }
    }
}
