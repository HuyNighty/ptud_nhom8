package com.quanlynhakhoa.billing.ui;

import com.quanlynhakhoa.security.AccessDeniedException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.ResolverStyle;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Shared display helpers for Khang's two panels. */
public final class BillingUi {
    public static final Color NAVY = new Color(29,61,102), BACKGROUND = new Color(245,248,252);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final Logger LOG = createLogger();
    private BillingUi() { }
    private static Logger createLogger() {
        Logger logger=Logger.getLogger("dental.billing.ui");logger.setUseParentHandlers(false);
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("logs"));
            FileHandler handler=new FileHandler("logs/billing-ui-%g.log",262144,3,true);
            handler.setFormatter(new SimpleFormatter());logger.addHandler(handler);
        } catch(java.io.IOException | SecurityException failure) { logger.addHandler(new ConsoleHandler()); }
        return logger;
    }
    public static void style(javax.swing.JPanel root){
        styleChildren(root);root.setBackground(new java.awt.Color(245,248,252));
    }
    private static void styleChildren(java.awt.Component component){
        Font old=component.getFont();
        boolean emphasis=component instanceof JComponent c&&Boolean.TRUE.equals(c.getClientProperty("billing.emphasis"));
        boolean heading=old!=null&&old.getSize()>=20;
        boolean caption=component instanceof JComponent c&&Boolean.TRUE.equals(c.getClientProperty("billing.caption"));
        component.setFont(new Font(Font.SANS_SERIF,heading||emphasis?Font.BOLD:Font.PLAIN,heading?old.getSize():caption?12:14));
        if(component instanceof javax.swing.JPanel panel)panel.setBackground(java.awt.Color.WHITE);
        if(component instanceof javax.swing.JLabel label)label.setForeground(Boolean.TRUE.equals(label.getClientProperty("billing.debt"))?new Color(147,69,20):new java.awt.Color(28,48,75));
        if(component instanceof javax.swing.JButton button&&button.getText()!=null&&!button.getText().isBlank()){
            button.setFocusPainted(false);button.setMargin(new Insets(6,12,6,12));
            boolean primary=Boolean.TRUE.equals(button.getClientProperty("billing.primary"));
            button.setFont(button.getFont().deriveFont(primary?Font.BOLD:Font.PLAIN));
            boolean danger=Boolean.TRUE.equals(button.getClientProperty("billing.danger"));
            button.setBackground(primary?NAVY:danger?new Color(253,235,235):new Color(232,240,249));
            button.setForeground(primary?Color.WHITE:danger?new Color(161,38,45):NAVY);
            Dimension size=button.getPreferredSize();Insets insets=button.getInsets();int width=Math.max(size.width,button.getFontMetrics(button.getFont()).stringWidth(button.getText())+insets.left+insets.right+12);button.setPreferredSize(new Dimension(width,34));button.setMinimumSize(new Dimension(width,34));
        }
        if(component instanceof JTextField || component instanceof JComboBox<?>){Dimension size=component.getPreferredSize();component.setPreferredSize(new Dimension(size.width,34));component.setMinimumSize(new Dimension(40,34));}
        if(component instanceof javax.swing.JTable table){table.setRowHeight(32);table.getTableHeader().setFont(new Font(Font.SANS_SERIF,Font.BOLD,13));table.getTableHeader().setPreferredSize(new Dimension(0,32));table.getTableHeader().setBackground(new java.awt.Color(232,240,249));table.getTableHeader().setForeground(NAVY);table.setShowVerticalLines(false);table.setGridColor(new Color(229,235,242));table.setSelectionBackground(new Color(218,233,249));table.setSelectionForeground(NAVY);}
        if(component instanceof JSplitPane split){split.setBorder(null);split.setDividerSize(8);split.setContinuousLayout(true);}
        if(component instanceof JScrollPane scroll){scroll.getViewport().setBackground(Color.WHITE);if(!(scroll.getBorder() instanceof javax.swing.border.TitledBorder))scroll.setBorder(BorderFactory.createLineBorder(new Color(218,226,236)));}
        if(component instanceof JComponent control&&control.getBorder() instanceof javax.swing.border.TitledBorder title){title.setTitleFont(new Font(Font.SANS_SERIF,Font.PLAIN,13));title.setTitleColor(NAVY);}
        if(component instanceof java.awt.Container container)for(java.awt.Component child:container.getComponents())styleChildren(child);
    }
    public static JButton primary(JButton button){button.putClientProperty("billing.primary",true);return button;}
    public static JButton danger(JButton button){button.putClientProperty("billing.danger",true);return button;}
    public static JPanel field(String caption,JComponent control){
        JPanel panel=new JPanel(new BorderLayout(0,3));JLabel label=new JLabel(caption);label.setLabelFor(control);panel.add(label,BorderLayout.NORTH);panel.add(control,BorderLayout.CENTER);return panel;
    }
    public static JPanel row(JComponent... cells){
        JPanel panel=new JPanel(new GridBagLayout());
        for(int index=0;index<cells.length;index++){GridBagConstraints c=new GridBagConstraints();c.gridx=index;c.gridy=0;c.weightx=1;c.fill=GridBagConstraints.BOTH;c.insets=new Insets(0,index==0?0:8,0,0);panel.add(cells[index],c);}
        return panel;
    }
    public static JPanel actions(JButton... buttons){
        JPanel panel=new JPanel(new GridBagLayout());int index=0;
        for(JButton button:buttons){GridBagConstraints c=new GridBagConstraints();c.gridx=index++;c.gridy=0;c.anchor=GridBagConstraints.WEST;c.insets=new Insets(0,c.gridx==0?0:8,0,0);panel.add(button,c);}
        GridBagConstraints space=new GridBagConstraints();space.gridx=index;space.gridy=0;space.weightx=1;space.fill=GridBagConstraints.HORIZONTAL;panel.add(Box.createHorizontalGlue(),space);return panel;
    }
    public static JPanel card(final JLabel source){return card(null,source);}
    public static JPanel card(final String caption,final JLabel source){
        JPanel panel=new JPanel(new BorderLayout(0,3));
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(218,226,236)),BorderFactory.createEmptyBorder(6,12,6,12)));
        final JLabel lblCaption=new JLabel(),lblValue=new JLabel();lblCaption.putClientProperty("billing.caption",true);lblValue.setFont(new Font(Font.SANS_SERIF,Font.BOLD,22));
        Runnable update=new Runnable(){public void run(){String text=source.getText()==null?"":source.getText();int colon=text.indexOf(':');String title=caption!=null?caption:colon<0?text:text.substring(0,colon);String amount=colon<0?"—":text.substring(colon+1).trim();lblCaption.setText(title);lblValue.setText(amount);lblValue.setToolTipText(text);boolean debt=title.toLowerCase(Locale.ROOT).startsWith("còn")||title.toLowerCase(Locale.ROOT).startsWith("tổng còn");lblValue.putClientProperty("billing.debt",debt);lblValue.setForeground(debt&&!amount.equals("—")?new Color(147,69,20):NAVY);}};
        source.addPropertyChangeListener("text",new java.beans.PropertyChangeListener(){public void propertyChange(java.beans.PropertyChangeEvent event){update.run();}});update.run();panel.add(lblCaption,BorderLayout.NORTH);panel.add(lblValue,BorderLayout.CENTER);return panel;
    }
    public static JPanel boundedField(String caption,JComponent input,int width){return bounded(field(caption,input),width);}
    public static JPanel bounded(JPanel group,final int width){
        JPanel wrapper=new JPanel(new BorderLayout()){@Override public Dimension getPreferredSize(){return new Dimension(width,super.getPreferredSize().height);}};
        wrapper.putClientProperty("billing.bounded",true);wrapper.add(group);return wrapper;
    }
    public static JPanel filters(JComponent... groups){
        for(int index=0;index<groups.length;index++)if(groups[index] instanceof JPanel panel&&!Boolean.TRUE.equals(panel.getClientProperty("billing.bounded"))&&panel.getLayout() instanceof BorderLayout layout){
            Component control=layout.getLayoutComponent(BorderLayout.CENTER),caption=layout.getLayoutComponent(BorderLayout.NORTH);
            String text=caption instanceof JLabel label?label.getText():"";
            groups[index]=bounded(panel,control instanceof JTextField?320:control instanceof JComboBox<?>?text.startsWith("Chọn hóa đơn")?440:210:240);
        }
        return new FilterBar(groups);
    }
    public static JPanel topAligned(final JPanel content){
        JPanel wrapper=new JPanel();wrapper.setLayout(new BoxLayout(wrapper,BoxLayout.Y_AXIS));
        JPanel capped=new JPanel(new BorderLayout()){@Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}};
        capped.add(content);capped.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(capped);wrapper.add(Box.createVerticalGlue());return wrapper;
    }
    public static JSplitPane split(int orientation,Component first,Component second,double weight){
        first.setMinimumSize(new Dimension(100,80));second.setMinimumSize(new Dimension(100,80));
        JSplitPane split=new JSplitPane(orientation,first,second){
            private boolean initialized;
            @Override public void doLayout(){
                if(!initialized&&getWidth()>0&&getHeight()>0){
                    initialized=true;
                    if(orientation==JSplitPane.VERTICAL_SPLIT&&second instanceof JComponent detail&&detail.getClientProperty("billing.initialDetailHeight") instanceof Integer height){setDividerLocation(Math.max(80,getHeight()-Math.min(height,Math.max(80,getHeight()-108))-getDividerSize()));}
                    else setDividerLocation(weight);
                }
                super.doLayout();
            }
        };split.setResizeWeight(weight);return split;
    }
    public static LocalDate parseDate(String text){
        if(text==null||text.isBlank())return null;
        try{return LocalDate.parse(text.trim(),DATE);}catch(java.time.format.DateTimeParseException failure){throw new IllegalArgumentException("Ngày phải có dạng dd/MM/yyyy và tồn tại; để trống nếu không lọc.");}
    }
    public static JPanel dateField(final JTextField input,String caption){
        input.setColumns(8);input.setToolTipText("dd/MM/yyyy · Có thể nhập ngày hoặc chọn lịch");
        final JButton btnChonNgay=new JButton("Lịch");btnChonNgay.setToolTipText("Chọn "+caption.toLowerCase(Locale.ROOT));
        btnChonNgay.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent event){
            if(!input.isEnabled())return;LocalDate current=null;try{current=parseDate(input.getText());}catch(IllegalArgumentException ignored){ /* Let the calendar correct invalid input. */ }
            LocalDate selected=com.quanlynhakhoa.benhnhan.view.ChonNgayDialog.chon(SwingUtilities.getWindowAncestor(input),"Chọn ngày",current,LocalDate.of(1900,1,1),LocalDate.of(9999,12,31));
            if(selected!=null)input.setText(DATE.format(selected));
        }});
        input.addPropertyChangeListener("enabled",new java.beans.PropertyChangeListener(){public void propertyChange(java.beans.PropertyChangeEvent e){btnChonNgay.setEnabled(input.isEnabled());}});
        JPanel control=new JPanel(new BorderLayout(4,0));control.add(input);control.add(btnChonNgay,BorderLayout.EAST);return bounded(field(caption,control),175);
    }
    public static BigDecimal moneyInput(String text){
        String value=text==null?"":text.trim();
        if(value.indexOf('.')>=0){String[] groups=value.split("\\.",-1);if(groups[0].length()<1||groups[0].length()>3||groups[0].charAt(0)=='0')throw new IllegalArgumentException("Số tiền không hợp lệ. Ví dụ: 1.500.000 hoặc 1500000.");for(int i=1;i<groups.length;i++)if(groups[i].length()!=3)throw new IllegalArgumentException("Số tiền không hợp lệ. Ví dụ: 1.500.000 hoặc 1500000.");value=value.replace(".","");}
        try{return com.quanlynhakhoa.dichvu.service.DichVuService.parsePrice(value);}catch(IllegalArgumentException failure){throw new IllegalArgumentException("Số tiền phải là số nguyên từ 0 đến 999.999.999.999.999.999 VND.");}
    }
    public static String moneyText(BigDecimal value){return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value);}
    public static void moneyField(final JTextField input){input.setHorizontalAlignment(JTextField.RIGHT);input.setToolTipText("Số tiền VND nguyên, ví dụ 1.500.000");input.addFocusListener(new FocusAdapter(){@Override public void focusLost(FocusEvent event){if(input.getText().isBlank())return;try{input.setText(moneyText(moneyInput(input.getText())));}catch(IllegalArgumentException ignored){ /* Validation stays visible at submit. */ }}});}
    public static String vnd(BigDecimal value) {
        if (value == null) return "";
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value) + " ₫";
    }
    public static String time(LocalDateTime value) {
        return value == null ? "" : value.format(DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss"));
    }
    public static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int column) {
                if (getRowCount() == 0) return Object.class;
                Object value = getValueAt(0, column);
                return value instanceof BigDecimal ? BigDecimal.class : value instanceof Integer ? Integer.class
                        : value instanceof LocalDateTime ? LocalDateTime.class : Object.class;
            }
        };
    }
    public static JTable table(DefaultTableModel model) {
        JTable tblResult = new JTable(model){
            @Override public boolean getScrollableTracksViewportWidth(){return getParent()!=null&&getPreferredSize().width<getParent().getWidth();}
            @Override public String getToolTipText(java.awt.event.MouseEvent event){int row=rowAtPoint(event.getPoint()),column=columnAtPoint(event.getPoint());if(row<0||column<0)return null;Object value=getValueAt(row,column);return value instanceof BigDecimal money?vnd(money):value instanceof LocalDateTime date?time(date):value==null?null:value.toString();}
            @Override protected void paintComponent(Graphics g){super.paintComponent(g);if(getRowCount()==0){String hint=(String)getClientProperty("billing.empty");if(hint==null)hint="Chưa có dữ liệu.";g.setColor(new Color(65,82,103));FontMetrics fm=g.getFontMetrics();Rectangle visible=getVisibleRect();g.drawString(hint,Math.max(16,(visible.width-fm.stringWidth(hint))/2),visible.y+Math.min(64,Math.max(24,visible.height/2)));}}
        };
        tblResult.setFillsViewportHeight(true);
        tblResult.setIntercellSpacing(new Dimension(8,1));
        tblResult.setAutoCreateRowSorter(true);
        tblResult.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tblResult.setRowHeight(26);
        tblResult.setDefaultRenderer(BigDecimal.class, new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                setHorizontalAlignment(RIGHT);
                setText(value instanceof BigDecimal money ? vnd(money) : "");
            }
        });
        tblResult.setDefaultRenderer(LocalDateTime.class, new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                setText(value instanceof LocalDateTime date ? time(date) : "");
            }
        });
        tblResult.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focus,int row,int column){super.getTableCellRendererComponent(table,value,selected,focus,row,column);if(!selected&&value instanceof Enum<?> status){String state=status.name();setForeground(state.equals("CHO_DUYET")||state.equals("CHUA_THANH_TOAN")?new Color(142,91,0):state.equals("DA_HUY")||state.equals("TU_CHOI")?new Color(161,38,45):state.equals("DA_DUYET")||state.equals("DA_THANH_TOAN")||state.equals("THANH_CONG")?new Color(26,111,79):NAVY);}else if(!selected)setForeground(new Color(28,48,75));return this;}
        });
        return tblResult;
    }
    public static void columns(JTable table,int... widths){table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);for(int i=0;i<widths.length&&i<table.getColumnCount();i++)table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);}
    public static void dateColumn(JTable table,int column){table.getColumnModel().getColumn(column).setCellRenderer(new DefaultTableCellRenderer(){@Override protected void setValue(Object value){setText(value instanceof LocalDateTime date?DATE.format(date.toLocalDate()):"");}});}
    public static JScrollPane scroll(JTable tblTable) {
        JScrollPane scrTable = new JScrollPane(tblTable);
        scrTable.setColumnHeaderView(tblTable.getTableHeader());
        return scrTable;
    }
    public static String error(Throwable failure) {
        if (failure instanceof AccessDeniedException) return "Không có quyền hoặc phiên đã đăng xuất.";
        if (failure instanceof IllegalArgumentException) {String message=failure.getMessage();if(message!=null&&message.contains("ASCII"))return "Mã dịch vụ gồm 1–10 ký tự không dấu, không có khoảng trắng.";return message;}
        // Never log driver messages, SQL parameters, credentials or patient data.
        StringBuilder diagnostic=new StringBuilder(failure.getClass().getName());
        if(failure instanceof SQLException sql)diagnostic.append(" SQLState=").append(sql.getSQLState()).append(" code=").append(sql.getErrorCode());
        for(StackTraceElement frame:failure.getStackTrace())if(frame.getClassName().startsWith("com.quanlynhakhoa."))diagnostic.append("\n at ").append(frame);
        LOG.warning(diagnostic.toString());
        if(failure instanceof SQLException sql){if(sql.getErrorCode()==229)return "Chưa có quyền truy cập dữ liệu. Liên hệ quản trị viên, rồi bấm Làm mới.";return "Không thể đọc hoặc lưu dữ liệu. Kiểm tra kết nối rồi thử lại; thông tin kỹ thuật đã được ghi nhật ký.";}
        return "Không thể hoàn tất thao tác. Kiểm tra môi trường ứng dụng.";
    }
    public static void tableState(JTable table,String message){table.putClientProperty("billing.empty",message);table.repaint();}
}
