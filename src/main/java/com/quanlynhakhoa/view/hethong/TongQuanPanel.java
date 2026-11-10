package com.quanlynhakhoa.view.hethong;

import com.quanlynhakhoa.model.tongquan.SoLieuNgay;
import com.quanlynhakhoa.model.tongquan.LichHenTongQuan;
import com.quanlynhakhoa.model.tongquan.DuLieuTongQuan;
import com.quanlynhakhoa.model.tongquan.CongViecTongQuan;
import com.quanlynhakhoa.model.tongquan.CanhBaoTonKho;
import com.quanlynhakhoa.service.tongquan.TongQuanService;
import com.quanlynhakhoa.model.tongquan.TomTatTongQuan;
import com.quanlynhakhoa.config.phattrien.TeamDevConnection;
import com.quanlynhakhoa.security.DesktopSession;
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
import javax.swing.border.EmptyBorder;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.awt.*;
import com.quanlynhakhoa.enums.Module;
import com.quanlynhakhoa.security.NavigationPolicy;
import com.quanlynhakhoa.enums.Quyen;
import com.quanlynhakhoa.service.tongquan.TongQuanService.*;


public final class TongQuanPanel extends JPanel implements com.quanlynhakhoa.config.tinhnang.RefreshableFeature {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(97, 115, 137);
    private static final Color BORDER = new Color(225, 233, 241);
    private static final Color BACKGROUND = new Color(245, 248, 252);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    private final DesktopSession session;
    private final TongQuanService service;
    private final JLabel[] lblGiaTri = new JLabel[4];
    private final JLabel[] lblMoTa = new JLabel[4];
    private final JLabel lblTrangThai = label("Chưa tải dữ liệu", 12, MUTED, false);
    private final JButton btnLamMoi = new JButton("Làm mới");
    private boolean loading;
    private java.util.function.BiConsumer<Module,String> navigation;
    private final DefaultTableModel modelLichHen=tableModel("Giờ", "Bệnh nhân", "Bác sĩ", "Trạng thái");
    private final DefaultTableModel modelCanhBao=tableModel("Cảnh báo", "Mặt hàng / lô", "Chi tiết");
    private final JTable tblLichHen=new JTable(modelLichHen), tblCanhBao=new JTable(modelCanhBao);
    private final JPanel danhSachCongViec=new JPanel();
    private List<LichHenTongQuan> danhSachLichHen=List.of();
    private List<CanhBaoTonKho> danhSachCanhBao=List.of();
    private final TrendChart bieuDoLuotKham=new TrendChart(false), bieuDoTien=new TrendChart(true);
    private final JLabel lblTrangThaiLichHen=label("Đang tải…",12,MUTED,false);
    private final JLabel trangThaiTonKho=label("Đang tải…",12,MUTED,false);

    public void setNavigation(java.util.function.BiConsumer<Module,String> navigation) { this.navigation=navigation; }
    private void open(Module module,String context) {
        if(navigation!=null&&new NavigationPolicy().canOpen(session,module))navigation.accept(module,context);
    }
    @Override public void refreshData() { if(SwingUtilities.isEventDispatchThread())refresh();else SwingUtilities.invokeLater(this::refresh); }


    public TongQuanPanel(DesktopSession session) {
        this(session, new TongQuanService(session, TeamDevConnection::open),
                TeamDevConnection.hasLocalConfig());
    }

    public TongQuanPanel(DesktopSession session, TongQuanService service) {
        this(session, service, true);
    }

    private TongQuanPanel(DesktopSession session, TongQuanService service, boolean configured) {
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
        if(service.can(Quyen.APPOINTMENT_READ))middle.add(appointmentsCard());
        middle.add(workCard());
        addSection(pnlBody,middle,265);
        JPanel charts=new JPanel(new GridLayout(1,0,14,0));charts.setOpaque(false);
        if(service.can(Quyen.MEDICAL_RECORD_READ))charts.add(chartCard("Lượt khám 7 ngày",bieuDoLuotKham));
        if(service.can(Quyen.REVENUE_REPORT_READ))charts.add(chartCard("Tiền đã thu 7 ngày · VND",bieuDoTien));
        if(charts.getComponentCount()>0)addSection(pnlBody,charts,220);
        if(service.can(Quyen.INVENTORY_READ))addSection(pnlBody,stockCard(),250);
        pnlBody.add(footer());
        add(pnlBody, BorderLayout.CENTER);
        btnLamMoi.addActionListener(event -> refresh());
        if (configured) {
            refresh();
        } else {
            btnLamMoi.setEnabled(false);
            lblTrangThai.setText("Chưa thiết lập kết nối dữ liệu.");
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
        lblGiaTri[index] = label("—", 26, NAVY, true);
        pnlPanel.add(lblGiaTri[index], BorderLayout.CENTER);
        lblMoTa[index] = label("Chưa tải", 11, MUTED, false);
        lblMoTa[index].setToolTipText(description);
        pnlPanel.add(lblMoTa[index], BorderLayout.SOUTH);
        return pnlPanel;
    }

    private JPanel footer() {
        JPanel pnlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        pnlPanel.setOpaque(false);
        pnlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlPanel.add(btnLamMoi);
        pnlPanel.add(lblTrangThai);
        return pnlPanel;
    }

    private void refresh() {
        if (loading || !session.isActive()) return;
        loading = true;
        btnLamMoi.setEnabled(false);
        lblTrangThai.setText("Đang tải dữ liệu...");
        for (int i = 0; i < lblGiaTri.length; i++) {
            lblGiaTri[i].setText("—");
            lblMoTa[i].setText("Đang tải");
        }
        new SwingWorker<DuLieuTongQuan, Void>() {
            @Override protected DuLieuTongQuan doInBackground() throws Exception {
                return service.loadOverview();
            }

            @Override protected void done() {
                if (!session.isActive()) return;
                loading = false;
                btnLamMoi.setEnabled(true);
                try {
                    DuLieuTongQuan overview = get();
                    TomTatTongQuan summary = overview.summary();
                    showOverview(overview);
                    showLong(0, summary.patients());
                    showLong(1, summary.appointments());
                    showLong(2, summary.inProgressExams());
                    showMoney(summary.collected());
                    lblTrangThai.setText(overview.warnings().isEmpty()?"Cập nhật " + java.time.LocalTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).format(DateTimeFormatter.ofPattern("HH:mm")) + " · " + DATE.format(summary.date()):"Một phần dữ liệu chưa tải được. Bấm Làm mới để thử lại.");
                    lblTrangThai.setToolTipText(String.join(" · ",overview.warnings()));
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
            lblGiaTri[index].setText("—");
            lblMoTa[index].setText("Không có quyền xem");
        } else {
            lblGiaTri[index].setText(NumberFormat.getIntegerInstance(VIETNAMESE).format(number));
            lblMoTa[index].setText("");
        }
    }

    private void showMoney(BigDecimal amount) {
        if (amount == null) {
            lblGiaTri[3].setText("—");
            lblMoTa[3].setText("Không có quyền xem");
        } else {
            lblGiaTri[3].setText(NumberFormat.getIntegerInstance(VIETNAMESE).format(amount));
            lblMoTa[3].setText("VND");
        }
    }

    private void showLoadError() {
        lblTrangThai.setText("Không thể tải số liệu. Kiểm tra kết nối và thử lại.");
        for (JLabel lblDescription : lblMoTa) lblDescription.setText("Không thể cập nhật");
        danhSachLichHen=List.of();danhSachCanhBao=List.of();modelLichHen.setRowCount(0);modelCanhBao.setRowCount(0);
        lblTrangThaiLichHen.setText("Chưa tải được lịch hẹn.");trangThaiTonKho.setText("Chưa tải được cảnh báo kho.");
        danhSachCongViec.removeAll();danhSachCongViec.add(label("Chưa tải được công việc. Bấm Làm mới để thử lại.",12,MUTED,false));danhSachCongViec.revalidate();danhSachCongViec.repaint();
        bieuDoLuotKham.danhSach=List.of();bieuDoTien.danhSach=List.of();bieuDoLuotKham.repaint();bieuDoTien.repaint();
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
        shortcut(buttons,"Lịch hẹn",Module.APPOINTMENTS,Quyen.APPOINTMENT_MANAGE);
        shortcut(buttons,"Tiếp nhận",Module.RECEPTION,Quyen.PATIENT_CHECK_IN);
        shortcut(buttons,"Hóa đơn",Module.BILLING,Quyen.BILLING_MANAGE);
        shortcut(buttons,"Phiếu khám",Module.EXAMINATIONS,Quyen.MEDICAL_RECORD_READ);
        shortcut(buttons,"Yêu cầu cấp phát",Module.SUPPLY_REQUESTS,Quyen.SUPPLY_REQUEST);
        shortcut(buttons,"Quản lý kho",Module.INVENTORY,Quyen.INVENTORY_MANAGE);
        shortcut(buttons,"Báo cáo",Module.REVENUE_REPORTS,Quyen.REVENUE_REPORT_READ);
        shortcut(buttons,"Lịch của tôi",Module.MY_SCHEDULE,Quyen.SCHEDULE_SELF_READ);
        panel.add(buttons);
        int height=((buttons.getComponentCount()+3)/4)*42+24;
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE,height));panel.setPreferredSize(new Dimension(900,height));
        return panel;
    }
    private void shortcut(JPanel panel,String caption,Module module,Quyen permission) {
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
        tblLichHen.setToolTipText("Tối đa 8 lịch chưa tiếp nhận, sắp theo giờ hẹn.");
        panel.add(tableScroll(tblLichHen));
        JButton button=new JButton("Mở lịch hẹn");button.addActionListener(e->openAppointment());
        tblLichHen.addMouseListener(new java.awt.event.MouseAdapter(){@Override public void mouseClicked(java.awt.event.MouseEvent e){if(e.getClickCount()==2)openAppointment();}});
        JPanel bottom=new JPanel(new BorderLayout(8,0));bottom.setOpaque(false);bottom.add(lblTrangThaiLichHen);bottom.add(button,BorderLayout.EAST);panel.add(bottom,BorderLayout.SOUTH);return panel;
    }
    private void openAppointment() {
        int row=tblLichHen.getSelectedRow();
        String id=row<0?"":danhSachLichHen.get(tblLichHen.convertRowIndexToModel(row)).id();
        open(Module.APPOINTMENTS,id);
    }
    private JPanel workCard() {
        JPanel panel=section("Việc cần xử lý","");
        danhSachCongViec.setOpaque(false);danhSachCongViec.setLayout(new BoxLayout(danhSachCongViec,BoxLayout.Y_AXIS));
        danhSachCongViec.add(label("Đang tải công việc…",12,MUTED,false));
        panel.add(danhSachCongViec);return panel;
    }
    private JPanel stockCard() {
        JPanel panel=section("Cảnh báo kho","Hạn dùng: 30 ngày");
        tblCanhBao.setToolTipText("Tồn còn hạn dưới định mức; lô hết hạn hoặc hết hạn trong 30 ngày. Tối đa 8 dòng mỗi nhóm.");
        panel.add(tableScroll(tblCanhBao));JButton button=new JButton("Kiểm tra kho");button.addActionListener(e->{
            int row=tblCanhBao.getSelectedRow();String context=row<0?"lots":danhSachCanhBao.get(tblCanhBao.convertRowIndexToModel(row)).context();open(Module.INVENTORY,context);
        });JPanel bottom=new JPanel(new BorderLayout(8,0));bottom.setOpaque(false);bottom.add(trangThaiTonKho);bottom.add(button,BorderLayout.EAST);panel.add(bottom,BorderLayout.SOUTH);return panel;
    }
    private JPanel chartCard(String title,TrendChart chart) {
        JPanel panel=section(title,"");panel.add(chart);return panel;
    }
    private void showOverview(DuLieuTongQuan data) {
        danhSachLichHen=data.appointments();modelLichHen.setRowCount(0);
        for(LichHenTongQuan row:danhSachLichHen)modelLichHen.addRow(new Object[]{row.time().format(DateTimeFormatter.ofPattern("HH:mm")),row.patient(),row.doctor(),"DA_XAC_NHAN".equals(row.status())?"Đã xác nhận":"Chờ xác nhận"});
        lblTrangThaiLichHen.setText(data.warnings().contains("Chưa tải được lịch hẹn")?"Chưa tải được lịch hẹn.":danhSachLichHen.isEmpty()?"Không có lịch đang chờ hôm nay.":danhSachLichHen.size()+" lịch đang hiển thị.");
        danhSachCongViec.removeAll();
        for(CongViecTongQuan item:data.work()){
            JButton button=new JButton(item.count()+"  ·  "+item.label()+"  →");button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setAlignmentX(Component.LEFT_ALIGNMENT);button.setMaximumSize(new Dimension(Integer.MAX_VALUE,40));button.addActionListener(e->open(item.module(),item.context()));
            danhSachCongViec.add(button);danhSachCongViec.add(Box.createVerticalStrut(8));
        }
        if(data.work().isEmpty())danhSachCongViec.add(label(data.warnings().isEmpty()?"Không có việc chờ xử lý.":"Chưa tải được công việc.",12,MUTED,false));
        danhSachCongViec.revalidate();danhSachCongViec.repaint();AppTheme.applyButtons(danhSachCongViec);
        danhSachCanhBao=data.alerts();modelCanhBao.setRowCount(0);for(CanhBaoTonKho alert:danhSachCanhBao)modelCanhBao.addRow(new Object[]{alert.kind(),alert.item(),alert.detail()});
        trangThaiTonKho.setText(data.warnings().contains("Chưa tải được cảnh báo kho")?"Chưa tải được cảnh báo kho.":danhSachCanhBao.isEmpty()?"Không có cảnh báo.":danhSachCanhBao.size()+" cảnh báo đang hiển thị.");
        bieuDoLuotKham.danhSach=data.days();bieuDoTien.danhSach=data.days();bieuDoLuotKham.repaint();bieuDoTien.repaint();
    }
    private static final class TrendChart extends JPanel {
        private final boolean coSoTien;private List<SoLieuNgay> danhSach=List.of();
        TrendChart(boolean money){this.coSoTien=money;setOpaque(false);}
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();
            try{
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                if(danhSach.isEmpty()||(coSoTien?danhSach.get(0).collected()==null:danhSach.get(0).visits()==null)){
                    g.setColor(MUTED);g.drawString("Chưa có dữ liệu để vẽ biểu đồ.",12,40);return;
                }
                int bottom=getHeight()-28,top=25,width=getWidth()-20;double max=0,min=0;
                for(SoLieuNgay row:danhSach){double value=coSoTien?row.collected().doubleValue():row.visits();max=Math.max(max,value);min=Math.min(min,value);}
                double range=max-min;if(range==0)range=1;int baseline=top+(int)((bottom-top)*max/range);
                double step=width/(double)danhSach.size();g.setFont(new Font("SansSerif",Font.PLAIN,11));
                g.setColor(BORDER);g.drawLine(10,baseline,getWidth()-10,baseline);
                for(int i=0;i<danhSach.size();i++){
                    SoLieuNgay row=danhSach.get(i);double value=coSoTien?row.collected().doubleValue():row.visits();
                    int y=top+(int)((bottom-top)*(max-value)/range),height=Math.abs(baseline-y),bar=(int)Math.min(40,step*.55),x=(int)(10+step*(i+.5));
                    g.setColor(coSoTien?new Color(38,132,119):NAVY);g.fillRoundRect(x-bar/2,Math.min(baseline,y),bar,Math.max(height,1),5,5);
                    String text=coSoTien?compactMoney(row.collected()):Long.toString(row.visits());
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
