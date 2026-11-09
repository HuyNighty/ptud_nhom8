package com.quanlynhakhoa.view.hoadon;

import com.quanlynhakhoa.hethong.phanquyen.AccessDeniedException;
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

/** Shared presentation for service catalog, invoices, debt and payment screens. */
public final class GiaoDienHoaDon {
    /** Keep nonvisual runs usable while preserving normal desktop result dialogs. */
    public static void result(java.awt.Component owner, String message) {
        result(owner, message, "Kết quả", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    public static void result(java.awt.Component owner, String message, String title, int type) {
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            javax.swing.JOptionPane.showMessageDialog(owner, message, title, type);
        }
    }

    public static final Color NAVY = new Color(29,61,102), BACKGROUND = new Color(245,248,252);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final Logger LOG = createLogger();
    private GiaoDienHoaDon() { }
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
        markHeadings(root);styleChildren(root);root.setOpaque(true);root.setBackground(BACKGROUND);
        com.quanlynhakhoa.hethong.giaodien.AppTheme.apply(root);
    }
    private static void markHeadings(Component component){
        if(component instanceof JLabel label&&label.getFont()!=null&&label.getFont().getSize()>=20&&!Boolean.TRUE.equals(label.getClientProperty("billing.value")))label.putClientProperty("billing.heading",true);
        if(component instanceof Container container)for(Component child:container.getComponents())markHeadings(child);
    }
    private static void styleChildren(Component component){
        if(component instanceof JComponent control){
            String key=control.getUIClassID().replace("UI", "");
            Font font=UIManager.getFont(key+".font");
            if(font!=null)control.setFont(font);
            Color foreground=UIManager.getColor(key+".foreground");
            if(foreground!=null)control.setForeground(foreground);
        }
        if(component instanceof JLabel label){
            if(Boolean.TRUE.equals(label.getClientProperty("billing.heading"))){
                label.setFont(new Font("SansSerif",Font.BOLD,23));label.setForeground(NAVY);
            }
        }
        if(component instanceof JPanel panel){
            boolean surface=panel instanceof FilterBar||panel.getBorder() instanceof javax.swing.border.TitledBorder||Boolean.TRUE.equals(panel.getClientProperty("billing.surface"));
            panel.setOpaque(surface);panel.setBackground(surface?Color.WHITE:BACKGROUND);
        }
        if(component instanceof JCheckBox check)check.setOpaque(false);
        if(component instanceof JButton button&&button.getText()!=null&&!button.getText().isBlank()){
            // Match unstyled JButtons in LichLamViecPanel; the current L&F owns every state.
            JButton defaults=new JButton();
            button.updateUI();button.setFont(defaults.getFont());
            button.setBackground(defaults.getBackground());button.setForeground(defaults.getForeground());
            button.setBorder(defaults.getBorder());button.setMargin(defaults.getMargin());
            button.setOpaque(defaults.isOpaque());button.setContentAreaFilled(defaults.isContentAreaFilled());
            button.setFocusPainted(defaults.isFocusPainted());button.setBorderPainted(defaults.isBorderPainted());
            button.setRolloverEnabled(defaults.isRolloverEnabled());button.setCursor(defaults.getCursor());
            button.setPreferredSize(null);button.setMinimumSize(null);
        }
        if(component instanceof javax.swing.text.JTextComponent input){
            String key=input.getUIClassID().replace("UI", "");
            input.setBackground(UIManager.getColor(key+".background"));
            input.setDisabledTextColor(UIManager.getColor(key+".inactiveForeground"));
            input.setBorder(UIManager.getBorder(key+".border"));
        }
        if(component instanceof JTextField||component instanceof JComboBox<?>){
            component.setPreferredSize(null);component.setMinimumSize(null);
        }
        if(component instanceof JTable table){
            JTable defaults=new JTable();
            table.setRowHeight(32);table.setBackground(defaults.getBackground());
            table.setForeground(defaults.getForeground());table.setGridColor(defaults.getGridColor());
            table.setShowGrid(defaults.getShowHorizontalLines());
            table.setSelectionBackground(defaults.getSelectionBackground());table.setSelectionForeground(defaults.getSelectionForeground());
            table.getTableHeader().setFont(defaults.getTableHeader().getFont());
            table.getTableHeader().setBackground(defaults.getTableHeader().getBackground());
            table.getTableHeader().setForeground(defaults.getTableHeader().getForeground());
            table.getTableHeader().setPreferredSize(null);
        }
        if(component instanceof JSplitPane split){split.setBackground(BACKGROUND);split.setBorder(UIManager.getBorder("SplitPane.border"));split.setDividerSize(8);split.setContinuousLayout(true);}
        if(component instanceof JScrollPane scroll){
            scroll.getViewport().setBackground(Color.WHITE);
            if(!(scroll.getBorder() instanceof javax.swing.border.TitledBorder))scroll.setBorder(UIManager.getBorder("ScrollPane.border"));
        }
        if(component instanceof JComponent control&&control.getBorder() instanceof javax.swing.border.TitledBorder title){
            title.setTitleFont(UIManager.getFont("TitledBorder.font"));title.setTitleColor(UIManager.getColor("TitledBorder.titleColor"));title.setBorder(UIManager.getBorder("TitledBorder.border"));
        }
        if(component instanceof Container container)for(Component child:container.getComponents())styleChildren(child);
    }
    public static JButton primary(JButton button){button.putClientProperty("billing.primary",true);return button;}
    public static JButton danger(JButton button){button.putClientProperty("billing.danger",true);return button;}
    public static JPanel field(String caption,JComponent control){
        JPanel panel=new JPanel(new BorderLayout(0,3));panel.putClientProperty("billing.surface",true);JLabel label=new JLabel(caption);label.setLabelFor(control);panel.add(label,BorderLayout.NORTH);panel.add(control,BorderLayout.CENTER);return panel;
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
        panel.setBorder(BorderFactory.createCompoundBorder(UIManager.getBorder("ScrollPane.border"),BorderFactory.createEmptyBorder(6,12,6,12)));
        final JLabel lblCaption=new JLabel(),lblValue=new JLabel();panel.putClientProperty("billing.surface",true);lblCaption.putClientProperty("billing.caption",true);lblValue.putClientProperty("billing.value",true);
        Runnable update=new Runnable(){public void run(){String text=source.getText()==null?"":source.getText();int colon=text.indexOf(':');String title=caption!=null?caption:colon<0?text:text.substring(0,colon);String amount=colon<0?"—":text.substring(colon+1).trim();lblCaption.setText(title);lblValue.setText(amount);lblValue.setToolTipText(text);lblValue.setForeground(UIManager.getColor("Label.foreground"));}};
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
            LocalDate selected=com.quanlynhakhoa.view.benhnhan.ChonNgayDialog.chon(SwingUtilities.getWindowAncestor(input),"Chọn ngày",current,LocalDate.of(1900,1,1),LocalDate.of(9999,12,31));
            if(selected!=null)input.setText(DATE.format(selected));
        }});
        input.addPropertyChangeListener("enabled",new java.beans.PropertyChangeListener(){public void propertyChange(java.beans.PropertyChangeEvent e){btnChonNgay.setEnabled(input.isEnabled());}});
        JPanel control=new JPanel(new BorderLayout(4,0));control.add(input);control.add(btnChonNgay,BorderLayout.EAST);return bounded(field(caption,control),175);
    }
    public static BigDecimal moneyInput(String text){
        String value=text==null?"":text.trim();
        if(value.indexOf('.')>=0){String[] groups=value.split("\\.",-1);if(groups[0].length()<1||groups[0].length()>3||groups[0].charAt(0)=='0')throw new IllegalArgumentException("Số tiền không hợp lệ. Ví dụ: 1.500.000 hoặc 1500000.");for(int i=1;i<groups.length;i++)if(groups[i].length()!=3)throw new IllegalArgumentException("Số tiền không hợp lệ. Ví dụ: 1.500.000 hoặc 1500000.");value=value.replace(".","");}
        try{return com.quanlynhakhoa.service.dichvu.DanhMucDichVuService.parsePrice(value);}catch(IllegalArgumentException failure){throw new IllegalArgumentException("Số tiền phải là số nguyên từ 0 đến 999.999.999.999.999.999 VND.");}
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
            @Override protected void paintComponent(Graphics g){super.paintComponent(g);if(getRowCount()==0){String hint=(String)getClientProperty("billing.empty");if(hint==null)hint="Chưa có dữ liệu.";g.setColor(UIManager.getColor("Label.foreground"));FontMetrics fm=g.getFontMetrics();Rectangle visible=getVisibleRect();g.drawString(hint,Math.max(16,(visible.width-fm.stringWidth(hint))/2),visible.y+Math.min(64,Math.max(24,visible.height/2)));}}
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
