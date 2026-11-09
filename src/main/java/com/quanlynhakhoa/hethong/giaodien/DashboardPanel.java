package com.quanlynhakhoa.hethong.giaodien;

import com.quanlynhakhoa.model.tongquan.SoLieuNgay;
import com.quanlynhakhoa.model.tongquan.LichHenTongQuan;
import com.quanlynhakhoa.model.tongquan.DuLieuTongQuan;
import com.quanlynhakhoa.model.tongquan.CongViecTongQuan;
import com.quanlynhakhoa.model.tongquan.CanhBaoTonKho;
import com.quanlynhakhoa.hethong.tongquan.DashboardService;
import com.quanlynhakhoa.model.tongquan.TomTatTongQuan;
import com.quanlynhakhoa.hatang.phattrien.TeamDevConnection;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.awt.*;
import com.quanlynhakhoa.hethong.dieuhuong.Module;
import com.quanlynhakhoa.hethong.dieuhuong.NavigationPolicy;
import com.quanlynhakhoa.hethong.phanquyen.Permission;
import com.quanlynhakhoa.hethong.tongquan.DashboardService.*;


public final class DashboardPanel extends JPanel implements com.quanlynhakhoa.hethong.tinhnang.RefreshableFeature {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(97, 115, 137);
    private static final Color BORDER = new Color(225, 233, 241);
    private static final Color BACKGROUND = new Color(245, 248, 252);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    private final DesktopSession session;
    private final DashboardService service;
    private final JLabel[] lblValues = new JLabel[4];
    private final JLabel[] lblDescriptions = new JLabel[4];
    private final JLabel lblStatus = label("Chưa tải dữ liệu", 12, MUTED, false);
    private final JButton btnRefresh = new JButton("Làm mới");
    private boolean loading;
    private java.util.function.BiConsumer<Module,String> navigation;
    private final DefaultTableModel appointmentModel=tableModel("Giờ", "Bệnh nhân", "Bác sĩ", "Trạng thái");
    private final DefaultTableModel alertModel=tableModel("Cảnh báo", "Mặt hàng / lô", "Chi tiết");
    private final JTable appointmentTable=new JTable(appointmentModel), alertTable=new JTable(alertModel);
    private final JPanel workList=new JPanel();
    private List<LichHenTongQuan> appointmentRows=List.of();
    private List<CanhBaoTonKho> alertRows=List.of();
    private final TrendChart visitsChart=new TrendChart(false), moneyChart=new TrendChart(true);
    private final JLabel appointmentStatus=label("Đang tải…",12,MUTED,false);
    private final JLabel stockStatus=label("Đang tải…",12,MUTED,false);

    public void setNavigation(java.util.function.BiConsumer<Module,String> navigation) { this.navigation=navigation; }
    private void open(Module module,String context) {
        if(navigation!=null&&new NavigationPolicy().canOpen(session,module))navigation.accept(module,context);
    }
    @Override public void refreshData() { if(SwingUtilities.isEventDispatchThread())refresh();else SwingUtilities.invokeLater(this::refresh); }


    public DashboardPanel(DesktopSession session) {
        this(session, new DashboardService(session, TeamDevConnection::open),
                TeamDevConnection.hasLocalConfig());
    }

    public DashboardPanel(DesktopSession session, DashboardService service) {
        this(session, service, true);
    }

    private DashboardPanel(DesktopSession session, DashboardService service, boolean configured) {
        this.session = java.util.Objects.requireNonNull(session, "session");
        this.service = java.util.Objects.requireNonNull(service, "service");
        setLayout(new BorderLayout(0, 22));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(22, 24, 22, 24));
        add(introduction(), BorderLayout.NORTH);

        JPanel pnlBody = new JPanel();
        pnlBody.setOpaque(false);
        pnlBody.setLayout(new BoxLayout(pnlBody, BoxLayout.Y_AXIS));
        pnlBody.add(metrics());
        pnlBody.add(Box.createVerticalStrut(18));
        pnlBody.add(shortcuts());
        pnlBody.add(Box.createVerticalStrut(14));
        JPanel middle=new JPanel(new GridLayout(1,0,14,0));middle.setOpaque(false);
        if(service.can(Permission.APPOINTMENT_READ))middle.add(appointmentsCard());
        middle.add(workCard());
        addSection(pnlBody,middle,265);
        JPanel charts=new JPanel(new GridLayout(1,0,14,0));charts.setOpaque(false);
        if(service.can(Permission.MEDICAL_RECORD_READ))charts.add(chartCard("Lượt khám 7 ngày",visitsChart));
        if(service.can(Permission.REVENUE_REPORT_READ))charts.add(chartCard("Tiền đã thu 7 ngày · VND",moneyChart));
        if(charts.getComponentCount()>0)addSection(pnlBody,charts,220);
        if(service.can(Permission.INVENTORY_READ))addSection(pnlBody,stockCard(),250);
        pnlBody.add(footer());
        add(pnlBody, BorderLayout.CENTER);
        btnRefresh.addActionListener(event -> refresh());
        if (configured) {
            refresh();
        } else {
            btnRefresh.setEnabled(false);
            lblStatus.setText("Chưa thiết lập kết nối dữ liệu.");
        }
    }

    private JPanel introduction() {
        JPanel pnlPanel = card(new BorderLayout(16, 8));
        pnlPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(23, 24, 23, 24)));
        JPanel pnlCopy = new JPanel();
        pnlCopy.setOpaque(false);
        pnlCopy.setLayout(new BoxLayout(pnlCopy, BoxLayout.Y_AXIS));
        pnlCopy.add(label("Tổng quan phòng khám", 23, NAVY, true));
        pnlPanel.add(pnlCopy, BorderLayout.CENTER);
        JLabel lblAccount = label("Tài khoản: " + session.accountId(), 12, NAVY, true);
        lblAccount.setHorizontalAlignment(SwingConstants.RIGHT);
        pnlPanel.add(lblAccount, BorderLayout.EAST);
        return pnlPanel;
    }

    private JPanel metrics() {
        JPanel pnlRow = new JPanel(new GridLayout(1, 4, 13, 0));
        pnlRow.setOpaque(false);
        pnlRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));
        pnlRow.add(metric(0, "Bệnh nhân", "Tổng hồ sơ bệnh nhân"));
        pnlRow.add(metric(1, "Lịch hẹn hôm nay", "Không tính lịch đã hủy hoặc đổi lịch"));
        pnlRow.add(metric(2, "Đang khám", "Phiếu khám có trạng thái đang khám"));
        pnlRow.add(metric(3, "Đã thu hôm nay", "Thanh toán thành công, đơn vị VND"));
        return pnlRow;
    }

    private JPanel metric(int index, String title, String description) {
        JPanel pnlPanel = card(new BorderLayout(0, 10));
        pnlPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(17, 17, 17, 17)));
        pnlPanel.add(label(title, 13, MUTED, true), BorderLayout.NORTH);
        lblValues[index] = label("—", 26, NAVY, true);
        pnlPanel.add(lblValues[index], BorderLayout.CENTER);
        lblDescriptions[index] = label("Chưa tải", 11, MUTED, false);
        lblDescriptions[index].setToolTipText(description);
        pnlPanel.add(lblDescriptions[index], BorderLayout.SOUTH);
        return pnlPanel;
    }

    private JPanel footer() {
        JPanel pnlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        pnlPanel.setOpaque(false);
        pnlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlPanel.add(btnRefresh);
        pnlPanel.add(lblStatus);
        return pnlPanel;
    }

    private void refresh() {
        if (loading || !session.isActive()) return;
        loading = true;
        btnRefresh.setEnabled(false);
        lblStatus.setText("Đang tải dữ liệu...");
        for (int i = 0; i < lblValues.length; i++) {
            lblValues[i].setText("—");
            lblDescriptions[i].setText("Đang tải");
        }
        new SwingWorker<DuLieuTongQuan, Void>() {
            @Override protected DuLieuTongQuan doInBackground() throws Exception {
                return service.loadOverview();
            }

            @Override protected void done() {
                if (!session.isActive()) return;
                loading = false;
                btnRefresh.setEnabled(true);
                try {
                    DuLieuTongQuan overview = get();
                    TomTatTongQuan summary = overview.summary();
                    showOverview(overview);
                    showLong(0, summary.patients());
                    showLong(1, summary.appointments());
                    showLong(2, summary.inProgressExams());
                    showMoney(summary.collected());
                    lblStatus.setText(overview.warnings().isEmpty()?"Cập nhật " + java.time.LocalTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern("HH:mm")) + " · " + DATE.format(summary.date()):"Một phần dữ liệu chưa tải được. Bấm Làm mới để thử lại.");
                    lblStatus.setToolTipText(String.join(" · ",overview.warnings()));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    showLoadError();
                } catch (ExecutionException | CancellationException ex) {
                    showLoadError();
                }
            }
        }.execute();
    }

    private void showLong(int index, Long number) {
        if (number == null) {
            lblValues[index].setText("—");
            lblDescriptions[index].setText("Không có quyền xem");
        } else {
            lblValues[index].setText(NumberFormat.getIntegerInstance(VIETNAMESE).format(number));
            lblDescriptions[index].setText("");
        }
    }

    private void showMoney(BigDecimal amount) {
        if (amount == null) {
            lblValues[3].setText("—");
            lblDescriptions[3].setText("Không có quyền xem");
        } else {
            lblValues[3].setText(NumberFormat.getIntegerInstance(VIETNAMESE).format(amount));
            lblDescriptions[3].setText("VND");
        }
    }

    private void showLoadError() {
        lblStatus.setText("Không thể tải số liệu. Kiểm tra kết nối và thử lại.");
        for (JLabel lblDescription : lblDescriptions) lblDescription.setText("Không thể cập nhật");
        appointmentRows=List.of();alertRows=List.of();appointmentModel.setRowCount(0);alertModel.setRowCount(0);
        appointmentStatus.setText("Chưa tải được lịch hẹn.");stockStatus.setText("Chưa tải được cảnh báo kho.");
        workList.removeAll();workList.add(label("Chưa tải được công việc. Bấm Làm mới để thử lại.",12,MUTED,false));workList.revalidate();workList.repaint();
        visitsChart.rows=List.of();moneyChart.rows=List.of();visitsChart.repaint();moneyChart.repaint();
    }

    private static DefaultTableModel tableModel(String... columns) {
        return new DefaultTableModel(columns,0){@Override public boolean isCellEditable(int row,int col){return false;}};
    }
    private void addSection(JPanel body,JPanel section,int height) {
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setPreferredSize(new Dimension(900,height));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE,height));
        body.add(section);body.add(Box.createVerticalStrut(14));
    }
    private JPanel section(String title,String description) {
        JPanel panel=card(new BorderLayout(0,10));
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),new EmptyBorder(14,16,14,16)));
        JPanel head=new JPanel(new BorderLayout(0,4));head.setOpaque(false);
        head.add(label(title,16,NAVY,true),BorderLayout.NORTH);
        if (!description.isBlank()) head.add(label(description,11,MUTED,false),BorderLayout.SOUTH);
        panel.add(head,BorderLayout.NORTH);return panel;
    }
    private JPanel shortcuts() {
        JPanel panel=new JPanel(new BorderLayout(0,8));panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label("Thao tác nhanh",12,NAVY,true),BorderLayout.NORTH);
        JPanel buttons=new JPanel(new GridLayout(0,4,8,8));buttons.setOpaque(false);
        shortcut(buttons,"Lịch hẹn",Module.APPOINTMENTS,Permission.APPOINTMENT_MANAGE);
        shortcut(buttons,"Tiếp nhận",Module.RECEPTION,Permission.PATIENT_CHECK_IN);
        shortcut(buttons,"Hóa đơn",Module.BILLING,Permission.BILLING_MANAGE);
        shortcut(buttons,"Phiếu khám",Module.EXAMINATIONS,Permission.MEDICAL_RECORD_READ);
        shortcut(buttons,"Yêu cầu cấp phát",Module.SUPPLY_REQUESTS,Permission.SUPPLY_REQUEST);
        shortcut(buttons,"Quản lý kho",Module.INVENTORY,Permission.INVENTORY_MANAGE);
        shortcut(buttons,"Báo cáo",Module.REVENUE_REPORTS,Permission.REVENUE_REPORT_READ);
        shortcut(buttons,"Lịch của tôi",Module.MY_SCHEDULE,Permission.SCHEDULE_SELF_READ);
        panel.add(buttons);
        int height=((buttons.getComponentCount()+3)/4)*42+24;
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE,height));panel.setPreferredSize(new Dimension(900,height));
        return panel;
    }
    private void shortcut(JPanel panel,String caption,Module module,Permission permission) {
        if(!service.can(permission))return;
        JButton button=new JButton(caption);button.addActionListener(e->open(module,""));panel.add(button);
    }
    private JScrollPane tableScroll(JTable table) {
        table.setRowHeight(30);table.setFillsViewportHeight(true);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll=new JScrollPane(table);scroll.setBorder(BorderFactory.createLineBorder(BORDER));return scroll;
    }
    private JPanel appointmentsCard() {
        JPanel panel=section("Lịch hẹn hôm nay","");
        appointmentTable.setToolTipText("Tối đa 8 lịch chưa tiếp nhận, sắp theo giờ hẹn.");
        panel.add(tableScroll(appointmentTable));
        JButton button=new JButton("Mở lịch hẹn");button.addActionListener(e->openAppointment());
        appointmentTable.addMouseListener(new java.awt.event.MouseAdapter(){@Override public void mouseClicked(java.awt.event.MouseEvent e){if(e.getClickCount()==2)openAppointment();}});
        JPanel bottom=new JPanel(new BorderLayout(8,0));bottom.setOpaque(false);bottom.add(appointmentStatus);bottom.add(button,BorderLayout.EAST);panel.add(bottom,BorderLayout.SOUTH);return panel;
    }
    private void openAppointment() {
        int row=appointmentTable.getSelectedRow();
        String id=row<0?"":appointmentRows.get(appointmentTable.convertRowIndexToModel(row)).id();
        open(Module.APPOINTMENTS,id);
    }
    private JPanel workCard() {
        JPanel panel=section("Việc cần xử lý","");
        workList.setOpaque(false);workList.setLayout(new BoxLayout(workList,BoxLayout.Y_AXIS));
        workList.add(label("Đang tải công việc…",12,MUTED,false));
        panel.add(workList);return panel;
    }
    private JPanel stockCard() {
        JPanel panel=section("Cảnh báo kho","Hạn dùng: 30 ngày");
        alertTable.setToolTipText("Tồn còn hạn dưới định mức; lô hết hạn hoặc hết hạn trong 30 ngày. Tối đa 8 dòng mỗi nhóm.");
        panel.add(tableScroll(alertTable));JButton button=new JButton("Kiểm tra kho");button.addActionListener(e->{
            int row=alertTable.getSelectedRow();String context=row<0?"lots":alertRows.get(alertTable.convertRowIndexToModel(row)).context();open(Module.INVENTORY,context);
        });JPanel bottom=new JPanel(new BorderLayout(8,0));bottom.setOpaque(false);bottom.add(stockStatus);bottom.add(button,BorderLayout.EAST);panel.add(bottom,BorderLayout.SOUTH);return panel;
    }
    private JPanel chartCard(String title,TrendChart chart) {
        JPanel panel=section(title,"");panel.add(chart);return panel;
    }
    private void showOverview(DuLieuTongQuan data) {
        appointmentRows=data.appointments();appointmentModel.setRowCount(0);
        for(LichHenTongQuan row:appointmentRows)appointmentModel.addRow(new Object[]{row.time().format(DateTimeFormatter.ofPattern("HH:mm")),row.patient(),row.doctor(),"DA_XAC_NHAN".equals(row.status())?"Đã xác nhận":"Chờ xác nhận"});
        appointmentStatus.setText(data.warnings().contains("Chưa tải được lịch hẹn")?"Chưa tải được lịch hẹn.":appointmentRows.isEmpty()?"Không có lịch đang chờ hôm nay.":appointmentRows.size()+" lịch đang hiển thị.");
        workList.removeAll();
        for(CongViecTongQuan item:data.work()){
            JButton button=new JButton(item.count()+"  ·  "+item.label()+"  →");button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setAlignmentX(Component.LEFT_ALIGNMENT);button.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));button.addActionListener(e->open(item.module(),item.context()));
            workList.add(button);workList.add(Box.createVerticalStrut(8));
        }
        if(data.work().isEmpty())workList.add(label(data.warnings().isEmpty()?"Không có việc chờ xử lý.":"Chưa tải được công việc.",12,MUTED,false));
        workList.revalidate();workList.repaint();AppTheme.applyButtons(workList);
        alertRows=data.alerts();alertModel.setRowCount(0);for(CanhBaoTonKho alert:alertRows)alertModel.addRow(new Object[]{alert.kind(),alert.item(),alert.detail()});
        stockStatus.setText(data.warnings().contains("Chưa tải được cảnh báo kho")?"Chưa tải được cảnh báo kho.":alertRows.isEmpty()?"Không có cảnh báo.":alertRows.size()+" cảnh báo đang hiển thị.");
        visitsChart.rows=data.days();moneyChart.rows=data.days();visitsChart.repaint();moneyChart.repaint();
    }
    private static final class TrendChart extends JPanel {
        private final boolean money;private List<SoLieuNgay> rows=List.of();
        TrendChart(boolean money){this.money=money;setOpaque(false);}
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();
            try{
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                if(rows.isEmpty()||(money?rows.get(0).collected()==null:rows.get(0).visits()==null)){
                    g.setColor(MUTED);g.drawString("Chưa có dữ liệu để vẽ biểu đồ.",12,40);return;
                }
                int bottom=getHeight()-28,top=25,width=getWidth()-20;double max=0,min=0;
                for(SoLieuNgay row:rows){double value=money?row.collected().doubleValue():row.visits();max=Math.max(max,value);min=Math.min(min,value);}
                double range=max-min;if(range==0)range=1;int baseline=top+(int)((bottom-top)*max/range);
                double step=width/(double)rows.size();g.setFont(new Font("SansSerif",Font.PLAIN,11));
                g.setColor(BORDER);g.drawLine(10,baseline,getWidth()-10,baseline);
                for(int i=0;i<rows.size();i++){
                    SoLieuNgay row=rows.get(i);double value=money?row.collected().doubleValue():row.visits();
                    int y=top+(int)((bottom-top)*(max-value)/range),height=Math.abs(baseline-y),bar=(int)Math.min(40,step*.55),x=(int)(10+step*(i+.5));
                    g.setColor(money?new Color(38,132,119):NAVY);g.fillRoundRect(x-bar/2,Math.min(baseline,y),bar,Math.max(height,1),5,5);
                    String text=money?compactMoney(row.collected()):Long.toString(row.visits());
                    g.setColor(NAVY);g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,value<0?y+14:y-6);
                    String date=row.date().format(DateTimeFormatter.ofPattern("dd/MM"));g.setColor(MUTED);g.drawString(date,x-g.getFontMetrics().stringWidth(date)/2,bottom+18);
                }
            }finally{g.dispose();}
        }
        private static String compactMoney(BigDecimal value){
            if(value.compareTo(new BigDecimal("1000000"))>=0)return String.format(VIETNAMESE,"%.1f tr",value.doubleValue()/1000000);
            if(value.compareTo(new BigDecimal("1000"))>=0)return String.format(VIETNAMESE,"%.0f nghìn",value.doubleValue()/1000);
            return value.toPlainString();
        }
    }

    private static JPanel card(java.awt.LayoutManager layout) {
        JPanel pnlPanel = new JPanel(layout);
        pnlPanel.setBackground(Color.WHITE);
        return pnlPanel;
    }

    private static JLabel label(String text, int size, Color color, boolean bold) {
        JLabel lblLabel = new JLabel(text);
        lblLabel.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        lblLabel.setForeground(color);
        return lblLabel;
    }
}
