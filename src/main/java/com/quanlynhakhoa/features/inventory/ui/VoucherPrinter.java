package com.quanlynhakhoa.features.inventory.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * In chứng từ kho khổ A4 (phiếu nhập, phiếu xuất, …) qua hộp thoại in của hệ điều hành
 * (chọn máy in hoặc "Microsoft Print to PDF").
 */
final class VoucherPrinter implements Printable {
    /** Bề rộng thiết kế; vị trí cột được co giãn theo khổ giấy thực tế. */
    static final int BASE_WIDTH = 470;
    private static final int ROWS_FIRST_PAGE = 22;
    private static final int ROWS_OTHER_PAGES = 34;

    /** @param x vị trí bắt đầu theo {@link #BASE_WIDTH}; cột căn phải được canh theo mép phải trang */
    record Column(String header, int x, boolean rightAligned) {
    }

    record Document(String jobName, String title, String headline, List<String> infoLines,
                    List<Column> columns, List<List<String>> rows, String totalLine,
                    String leftSignature, String rightSignature) {
    }

    private final Document doc;

    VoucherPrinter(Document doc) {
        this.doc = doc;
    }

    static void print(Component parent, Document doc) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(doc.jobName());
        job.setPrintable(new VoucherPrinter(doc));
        if (!job.printDialog()) {
            return;
        }
        try {
            job.print();
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(parent, "Không in được: " + e.getMessage(), "In phiếu",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    static int pageCount(int rows) {
        if (rows <= ROWS_FIRST_PAGE) {
            return 1;
        }
        return 1 + (rows - ROWS_FIRST_PAGE + ROWS_OTHER_PAGES - 1) / ROWS_OTHER_PAGES;
    }

    @Override
    public int print(Graphics graphics, PageFormat format, int pageIndex) {
        List<List<String>> rows = doc.rows();
        int pages = pageCount(rows.size());
        if (pageIndex >= pages) {
            return NO_SUCH_PAGE;
        }
        Graphics2D g = (Graphics2D) graphics;
        g.translate(format.getImageableX(), format.getImageableY());
        int width = (int) format.getImageableWidth();
        g.setColor(Color.BLACK);
        Font normal = InventoryUiKit.font(Font.PLAIN, 10);
        Font bold = InventoryUiKit.font(Font.BOLD, 10);
        int y = 0;
        int from;
        int to;
        if (pageIndex == 0) {
            g.setFont(InventoryUiKit.font(Font.BOLD, 11));
            g.drawString("N08 DENTAL — PHÒNG KHÁM NHA KHOA", 0, y += 12);
            g.setFont(InventoryUiKit.font(Font.BOLD, 18));
            centered(g, doc.title(), width, y += 34, 0);
            g.setFont(normal);
            centered(g, doc.headline(), width, y += 16, 0);
            y += 14;
            for (String line : doc.infoLines()) {
                g.drawString(line, 0, y += 14);
            }
            y += 10;
            from = 0;
            to = Math.min(rows.size(), ROWS_FIRST_PAGE);
        } else {
            g.setFont(normal);
            g.drawString(doc.title() + " (tiếp theo) — trang " + (pageIndex + 1), 0, y += 12);
            y += 8;
            from = ROWS_FIRST_PAGE + (pageIndex - 1) * ROWS_OTHER_PAGES;
            to = Math.min(rows.size(), from + ROWS_OTHER_PAGES);
        }

        List<Column> cols = doc.columns();
        g.setStroke(new BasicStroke(0.6f));
        g.drawLine(0, y + 4, width, y + 4);
        y += 16;
        g.setFont(bold);
        for (int c = 0; c < cols.size(); c++) {
            drawCell(g, cols, c, cols.get(c).header(), width, y);
        }
        g.drawLine(0, y + 5, width, y + 5);
        g.setFont(normal);
        for (int i = from; i < to; i++) {
            y += 16;
            List<String> row = rows.get(i);
            for (int c = 0; c < cols.size(); c++) {
                drawCell(g, cols, c, c < row.size() ? row.get(c) : "", width, y);
            }
        }
        g.drawLine(0, y + 6, width, y + 6);

        if (pageIndex == pages - 1) {
            if (doc.totalLine() != null) {
                g.setFont(bold);
                g.drawString(doc.totalLine(), width - g.getFontMetrics().stringWidth(doc.totalLine()), y += 22);
            }
            g.setFont(normal);
            y += 36;
            int half = width / 2;
            centered(g, doc.leftSignature(), half, y, 0);
            centered(g, doc.rightSignature(), half, y, half);
            centered(g, "(Ký, ghi rõ họ tên)", half, y + 13, 0);
            centered(g, "(Ký, ghi rõ họ tên)", half, y + 13, half);
        }
        return PAGE_EXISTS;
    }

    private static void drawCell(Graphics g, List<Column> cols, int c, String text, int width, int y) {
        Column col = cols.get(c);
        String value = text == null ? "" : text;
        if (col.rightAligned()) {
            int right = c + 1 < cols.size() ? scale(cols.get(c + 1).x(), width) - 6 : width;
            g.drawString(value, right - g.getFontMetrics().stringWidth(value), y);
            return;
        }
        int x = scale(col.x(), width);
        int limit = (c + 1 < cols.size() ? scale(cols.get(c + 1).x(), width) : width) - x - 6;
        g.drawString(clip(g, value, limit), x, y);
    }

    static int scale(int x, int width) {
        return x * width / BASE_WIDTH;
    }

    private static String clip(Graphics g, String text, int maxWidth) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String value = text;
        while (!value.isEmpty() && fm.stringWidth(value + "…") > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "…";
    }

    private static void centered(Graphics g, String text, int width, int y, int offset) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, offset + (width - textWidth) / 2, y);
    }
}
