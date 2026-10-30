package com.quanlynhakhoa.billing.ui;

import com.quanlynhakhoa.security.AccessDeniedException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Shared display helpers for Khang's two panels. */
public final class BillingUi {
    private BillingUi() { }
    public static void style(javax.swing.JPanel root){
        styleChildren(root);root.setBackground(new java.awt.Color(245,248,252));
    }
    private static void styleChildren(java.awt.Component component){
        if(component instanceof javax.swing.JPanel panel)panel.setBackground(java.awt.Color.WHITE);
        if(component instanceof javax.swing.JLabel label)label.setForeground(new java.awt.Color(28,48,75));
        if(component instanceof javax.swing.JButton button){button.setFocusPainted(false);button.setBackground(new java.awt.Color(232,240,249));button.setForeground(new java.awt.Color(29,61,102));}
        if(component instanceof javax.swing.JTable table){table.setRowHeight(28);table.getTableHeader().setBackground(new java.awt.Color(232,240,249));table.getTableHeader().setForeground(new java.awt.Color(29,61,102));}
        if(component instanceof java.awt.Container container)for(java.awt.Component child:container.getComponents())styleChildren(child);
    }
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
        JTable tblResult = new JTable(model);
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
    public static JScrollPane scroll(JTable tblTable) {
        JScrollPane scrTable = new JScrollPane(tblTable);
        scrTable.setColumnHeaderView(tblTable.getTableHeader());
        return scrTable;
    }
    public static String error(Throwable failure) {
        if (failure instanceof AccessDeniedException) return "Không có quyền hoặc phiên đã đăng xuất.";
        if (failure instanceof IllegalArgumentException) return failure.getMessage();
        if (failure instanceof SQLException sql) return "Lỗi SQL/kết nối/quyền. SQLState="
                + sql.getSQLState() + ", mã=" + sql.getErrorCode() + ". Kiểm tra cấu hình development.";
        return "Không thể hoàn tất thao tác. Kiểm tra môi trường ứng dụng.";
    }
}
