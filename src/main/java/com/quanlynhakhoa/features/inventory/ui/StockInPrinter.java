package com.quanlynhakhoa.features.inventory.ui;

import com.quanlynhakhoa.features.inventory.dto.response.StockInDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockInLineResponse;
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

/** In phiếu nhập kho khổ A4 bằng hộp thoại in của hệ điều hành (chọn máy in hoặc "Microsoft Print to PDF"). */
final class StockInPrinter implements Printable {
    private static final int ROWS_FIRST_PAGE = 22;
    private static final int ROWS_OTHER_PAGES = 34;
    private final StockInDetailResponse detail;

    private StockInPrinter(StockInDetailResponse detail) {
        this.detail = detail;
    }

    static void print(Component parent, StockInDetailResponse detail) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Phieu nhap " + detail.maPhieu());
        job.setPrintable(new StockInPrinter(detail));
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

    static int pageCount(int lines) {
        if (lines <= ROWS_FIRST_PAGE) {
            return 1;
        }
        return 1 + (lines - ROWS_FIRST_PAGE + ROWS_OTHER_PAGES - 1) / ROWS_OTHER_PAGES;
    }

    @Override
    public int print(Graphics graphics, PageFormat format, int pageIndex) {
        List<StockInLineResponse> lines = detail.dong();
        if (pageIndex >= pageCount(lines.size())) {
            return NO_SUCH_PAGE;
        }
        Graphics2D g = (Graphics2D) graphics;
        g.translate(format.getImageableX(), format.getImageableY());
        int width = (int) format.getImageableWidth();
        g.setColor(Color.BLACK);
        int y = 0;
        Font normal = InventoryUiKit.font(Font.PLAIN, 10);
        Font bold = InventoryUiKit.font(Font.BOLD, 10);

        int from;
        int to;
        if (pageIndex == 0) {
            g.setFont(InventoryUiKit.font(Font.BOLD, 11));
            g.drawString("N08 DENTAL — PHÒNG KHÁM NHA KHOA", 0, y += 12);
            g.setFont(InventoryUiKit.font(Font.BOLD, 18));
            centered(g, "PHIẾU NHẬP KHO", width, y += 34);
            g.setFont(normal);
            centered(g, "Số: " + detail.maPhieu() + "   ·   Ngày nhập: " + InventoryFormat.date(detail.ngayNhap())
                    + "   ·   Trạng thái: " + detail.trangThai().label(), width, y += 16);
            y += 14;
            g.drawString("Nhà cung cấp: " + detail.tenNhaCungCap(), 0, y += 14);
            g.drawString("Người lập: " + detail.nguoiLap(), 0, y += 14);
            if (detail.nguoiNhapKho() != null) {
                g.drawString("Người nhập kho: " + detail.nguoiNhapKho() + " — "
                        + InventoryFormat.dateTime(detail.thoiDiemNhapKho()), 0, y += 14);
            }
            if (detail.ghiChu() != null) {
                g.drawString("Ghi chú: " + detail.ghiChu(), 0, y += 14);
            }
            y += 10;
            from = 0;
            to = Math.min(lines.size(), ROWS_FIRST_PAGE);
        } else {
            g.setFont(normal);
            g.drawString("Phiếu nhập " + detail.maPhieu() + " (tiếp theo) — trang " + (pageIndex + 1), 0, y += 12);
            y += 8;
            from = ROWS_FIRST_PAGE + (pageIndex - 1) * ROWS_OTHER_PAGES;
            to = Math.min(lines.size(), from + ROWS_OTHER_PAGES);
        }

        int[] cols = {0, 26, 186, 250, 330, 362};
        String[] headers = {"STT", "Mặt hàng", "Số lô", "Hạn dùng", "SL", "ĐVT", "Thành tiền"};
        g.setFont(bold);
        g.setStroke(new BasicStroke(0.6f));
        g.drawLine(0, y + 4, width, y + 4);
        y += 16;
        for (int i = 0; i < headers.length - 1; i++) {
            g.drawString(headers[i], scale(cols[i], width), y);
        }
        rightAligned(g, headers[headers.length - 1], width, y);
        g.drawLine(0, y + 5, width, y + 5);
        g.setFont(normal);
        for (int i = from; i < to; i++) {
            StockInLineResponse l = lines.get(i);
            y += 16;
            String[] values = {String.valueOf(i + 1), clip(g, l.maMatHang() + " " + l.tenMatHang(), scale(165, width)),
                    l.soLo(), InventoryFormat.date(l.hanSuDung()), String.valueOf(l.soLuong()), l.donViTinh(),
                    InventoryFormat.money(l.thanhTien())};
            for (int c = 0; c < values.length - 1; c++) {
                g.drawString(values[c], scale(cols[c], width), y);
            }
            rightAligned(g, values[values.length - 1], width, y);
        }
        g.drawLine(0, y + 6, width, y + 6);

        if (pageIndex == pageCount(lines.size()) - 1) {
            g.setFont(bold);
            String total = "Tổng tiền: " + InventoryFormat.money(detail.tongTien());
            g.drawString(total, width - g.getFontMetrics().stringWidth(total), y += 22);
            g.setFont(normal);
            y += 36;
            int half = width / 2;
            centered(g, "Người lập phiếu", half, y, 0);
            centered(g, "Thủ kho", half, y, half);
            centered(g, "(Ký, ghi rõ họ tên)", half, y + 13, 0);
            centered(g, "(Ký, ghi rõ họ tên)", half, y + 13, half);
        }
        return PAGE_EXISTS;
    }

    /** Cột thiết kế cho bề rộng 470pt, co giãn theo khổ giấy thực tế. */
    private static int scale(int x, int width) {
        return x * width / 470;
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

    private static void rightAligned(Graphics g, String text, int width, int y) {
        g.drawString(text, width - g.getFontMetrics().stringWidth(text), y);
    }

    private static void centered(Graphics g, String text, int width, int y) {
        centered(g, text, width, y, 0);
    }

    private static void centered(Graphics g, String text, int width, int y, int offset) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, offset + (width - textWidth) / 2, y);
    }
}
