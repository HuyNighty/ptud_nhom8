package com.quanlynhakhoa.desktop.menu;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/**
 * Icon dạng nét vẽ bằng Java2D trên lưới 24×24 — sắc nét ở mọi kích thước và mức scale,
 * không cần file ảnh trong resources.
 */
public final class MenuIcon implements Icon {

    public enum Shape {
        GRID, BELL, USER_PLUS, CALENDAR, PATIENT_FILE,
        TOOTH, CLIPBOARD, PLAN, REPEAT,
        WALLET, RECEIPT, CANCEL_DOC, TAG,
        TEAM, ID_CARD, CALENDAR_CHECK, HANDOVER, STAR,
        BOX, PILL, TRUCK, CHECKLIST, FIRST_AID, SUPPLIER,
        GEAR, SHIELD, CLOCK, KEY, HISTORY,
        CHEVRON_DOWN, SEARCH
    }

    private final Shape shape;
    private final int size;
    private Color color;
    private float strokeWidth = 1.8f;

    public MenuIcon(Shape shape, int size, Color color) {
        this.shape = shape;
        this.size = size;
        this.color = color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public MenuIcon withStroke(float width) {
        this.strokeWidth = width;
        return this;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.scale(size / 24.0, size / 24.0);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            draw(g2);
        } finally {
            g2.dispose();
        }
    }

    private void draw(Graphics2D g) {
        switch (shape) {
            case GRID -> {
                g.draw(rr(3, 3, 7.5, 7.5, 2));
                g.draw(rr(13.5, 3, 7.5, 7.5, 2));
                g.draw(rr(3, 13.5, 7.5, 7.5, 2));
                g.draw(rr(13.5, 13.5, 7.5, 7.5, 2));
            }
            case BELL -> {
                Path2D p = new Path2D.Double();
                p.moveTo(6, 16);
                p.lineTo(6, 11);
                p.curveTo(6, 7.2, 8.6, 5, 12, 5);
                p.curveTo(15.4, 5, 18, 7.2, 18, 11);
                p.lineTo(18, 16);
                p.lineTo(20, 18);
                p.lineTo(4, 18);
                p.closePath();
                g.draw(p);
                line(g, 12, 3, 12, 5);
                g.draw(new Arc2D.Double(10, 18.5, 4, 3.5, 180, 180, Arc2D.OPEN));
            }
            case USER_PLUS -> {
                g.draw(new Ellipse2D.Double(5, 4, 8, 8));
                g.draw(new Arc2D.Double(2, 14, 14, 14, 0, 180, Arc2D.OPEN));
                line(g, 19, 8, 19, 14);
                line(g, 16, 11, 22, 11);
            }
            case CALENDAR -> {
                g.draw(rr(3, 5, 18, 16, 3));
                line(g, 3, 10, 21, 10);
                line(g, 8, 3, 8, 7);
                line(g, 16, 3, 16, 7);
            }
            case CALENDAR_CHECK -> {
                g.draw(rr(3, 5, 18, 16, 3));
                line(g, 3, 10, 21, 10);
                line(g, 8, 3, 8, 7);
                line(g, 16, 3, 16, 7);
                poly(g, false, 8.5, 15.2, 11, 17.6, 15.5, 13);
            }
            case PATIENT_FILE -> {
                Path2D p = new Path2D.Double();
                p.moveTo(3, 18.5);
                p.lineTo(3, 6);
                p.quadTo(3, 4.5, 4.5, 4.5);
                p.lineTo(9, 4.5);
                p.lineTo(11, 7);
                p.lineTo(19.5, 7);
                p.quadTo(21, 7, 21, 8.5);
                p.lineTo(21, 18.5);
                p.quadTo(21, 20, 19.5, 20);
                p.lineTo(4.5, 20);
                p.quadTo(3, 20, 3, 18.5);
                p.closePath();
                g.draw(p);
                g.draw(new Ellipse2D.Double(10, 9.5, 4, 4));
                g.draw(new Arc2D.Double(8.5, 15, 7, 6, 0, 180, Arc2D.OPEN));
            }
            case TOOTH -> {
                Path2D p = new Path2D.Double();
                p.moveTo(12, 5.5);
                p.curveTo(9, 3, 4, 3.5, 4.5, 9);
                p.curveTo(4.8, 12, 6.5, 13, 7, 16);
                p.curveTo(7.5, 19.5, 8.5, 21, 9.5, 21);
                p.curveTo(10.7, 21, 10.5, 16, 12, 16);
                p.curveTo(13.5, 16, 13.3, 21, 14.5, 21);
                p.curveTo(15.5, 21, 16.5, 19.5, 17, 16);
                p.curveTo(17.5, 13, 19.2, 12, 19.5, 9);
                p.curveTo(20, 3.5, 15, 3, 12, 5.5);
                p.closePath();
                g.draw(p);
            }
            case CLIPBOARD -> {
                g.draw(rr(5, 4, 14, 17, 2.5));
                g.draw(rr(9, 2.5, 6, 3.5, 1.5));
                line(g, 8.5, 11, 15.5, 11);
                line(g, 8.5, 15, 13.5, 15);
            }
            case CHECKLIST -> {
                g.draw(rr(5, 4, 14, 17, 2.5));
                g.draw(rr(9, 2.5, 6, 3.5, 1.5));
                poly(g, false, 8.5, 13, 11, 15.5, 15.5, 10.5);
            }
            case PLAN -> {
                poly(g, false, 3, 6, 4.6, 7.6, 7.4, 4.6);
                poly(g, false, 3, 12, 4.6, 13.6, 7.4, 10.6);
                g.draw(new Ellipse2D.Double(3.5, 17, 3, 3));
                line(g, 10.5, 6, 21, 6);
                line(g, 10.5, 12, 21, 12);
                line(g, 10.5, 18.5, 21, 18.5);
            }
            case REPEAT -> {
                g.draw(new Arc2D.Double(4, 4, 16, 16, 40, 290, Arc2D.OPEN));
                // đầu mũi tên tại góc 40°
                double ax = 12 + 8 * Math.cos(Math.toRadians(40));
                double ay = 12 - 8 * Math.sin(Math.toRadians(40));
                poly(g, false, ax - 4.2, ay - 0.6, ax, ay, ax + 0.4, ay - 4.2);
                poly(g, false, 12, 8.5, 12, 12, 14.5, 13.5);
            }
            case WALLET -> {
                g.draw(rr(3, 6.5, 18, 14, 2.5));
                poly(g, false, 5.5, 6.5, 15.5, 3, 17, 6.5);
                g.draw(rr(14, 11, 7, 5, 1.5));
            }
            case RECEIPT -> {
                poly(g, true, 6, 3, 18, 3, 18, 21, 15, 19.3, 12, 21, 9, 19.3, 6, 21);
                line(g, 9, 8, 15, 8);
                line(g, 9, 12, 15, 12);
                line(g, 9, 16, 12.5, 16);
            }
            case CANCEL_DOC -> {
                g.draw(rr(5, 3, 14, 18, 2.5));
                line(g, 9.5, 9.5, 14.5, 14.5);
                line(g, 14.5, 9.5, 9.5, 14.5);
            }
            case TAG -> {
                poly(g, true, 3, 4.5, 4.5, 3, 11.5, 3, 21, 12.5, 12.5, 21, 3, 11.5);
                g.draw(new Ellipse2D.Double(6.2, 6.2, 3, 3));
            }
            case TEAM -> {
                g.draw(new Ellipse2D.Double(4, 4.5, 7.5, 7.5));
                g.draw(new Arc2D.Double(1.5, 14.5, 12.5, 12, 0, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Double(14, 5, 6.5, 6.5, -110, 250, Arc2D.OPEN));
                g.draw(new Arc2D.Double(15, 14.5, 7.5, 12, 0, 90, Arc2D.OPEN));
            }
            case ID_CARD -> {
                g.draw(rr(2.5, 5, 19, 14, 2.5));
                g.draw(new Ellipse2D.Double(6, 8, 4.5, 4.5));
                g.draw(new Arc2D.Double(5, 13.5, 6.5, 5.5, 0, 180, Arc2D.OPEN));
                line(g, 14, 10, 18.5, 10);
                line(g, 14, 14, 17, 14);
            }
            case HANDOVER -> {
                line(g, 4, 8, 19, 8);
                poly(g, false, 15, 4, 19, 8, 15, 12);
                line(g, 20, 16, 5, 16);
                poly(g, false, 9, 12, 5, 16, 9, 20);
            }
            case STAR -> {
                Path2D p = new Path2D.Double();
                for (int i = 0; i < 10; i++) {
                    double r = (i % 2 == 0) ? 9.5 : 4.2;
                    double a = Math.toRadians(-90 + i * 36);
                    double px = 12 + r * Math.cos(a);
                    double py = 12.8 + r * Math.sin(a);
                    if (i == 0) {
                        p.moveTo(px, py);
                    } else {
                        p.lineTo(px, py);
                    }
                }
                p.closePath();
                g.draw(p);
            }
            case BOX -> {
                poly(g, true, 12, 2.5, 20.5, 7, 20.5, 17, 12, 21.5, 3.5, 17, 3.5, 7);
                poly(g, false, 3.5, 7, 12, 11.5, 20.5, 7);
                line(g, 12, 11.5, 12, 21.5);
            }
            case PILL -> {
                AffineTransform old = g.getTransform();
                g.rotate(Math.toRadians(-45), 12, 12);
                g.draw(rr(2.5, 8, 19, 8, 8));
                line(g, 12, 8, 12, 16);
                g.setTransform(old);
            }
            case TRUCK -> {
                g.draw(rr(1.5, 6, 12.5, 10, 1.5));
                poly(g, false, 14, 9, 18, 9, 21.5, 12.5, 21.5, 16, 14, 16);
                g.draw(new Ellipse2D.Double(4, 14.5, 4, 4));
                g.draw(new Ellipse2D.Double(15, 14.5, 4, 4));
            }
            case FIRST_AID -> {
                g.draw(rr(3, 6.5, 18, 14, 2.5));
                poly(g, false, 9, 6.5, 9, 4, 15, 4, 15, 6.5);
                line(g, 12, 10.5, 12, 16.5);
                line(g, 9, 13.5, 15, 13.5);
            }
            case SUPPLIER -> {
                poly(g, true, 3, 20.5, 3, 10, 8, 13, 8, 10, 13, 13, 13, 5, 21, 5, 21, 20.5);
                line(g, 16.5, 9, 17.5, 9);
                line(g, 16.5, 13, 17.5, 13);
                line(g, 6, 17, 7, 17);
                line(g, 10.5, 17, 11.5, 17);
            }
            case GEAR -> {
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    line(g, 12 + 7 * Math.cos(a), 12 + 7 * Math.sin(a),
                            12 + 9.5 * Math.cos(a), 12 + 9.5 * Math.sin(a));
                }
                g.draw(new Ellipse2D.Double(5, 5, 14, 14));
                g.draw(new Ellipse2D.Double(9, 9, 6, 6));
            }
            case SHIELD -> {
                Path2D p = new Path2D.Double();
                p.moveTo(12, 2.5);
                p.lineTo(20, 5.5);
                p.lineTo(20, 11);
                p.curveTo(20, 16, 16.5, 19.5, 12, 21.5);
                p.curveTo(7.5, 19.5, 4, 16, 4, 11);
                p.lineTo(4, 5.5);
                p.closePath();
                g.draw(p);
                poly(g, false, 8.5, 12, 11, 14.5, 15.5, 9.5);
            }
            case CLOCK -> {
                g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                poly(g, false, 12, 7, 12, 12, 15.5, 14);
            }
            case KEY -> {
                g.draw(new Ellipse2D.Double(2.5, 8, 8, 8));
                line(g, 10.5, 12, 21, 12);
                line(g, 17, 12, 17, 15.5);
                line(g, 20.5, 12, 20.5, 15);
            }
            case HISTORY -> {
                g.draw(new Arc2D.Double(3, 3, 18, 18, 160, -330, Arc2D.OPEN));
                poly(g, false, 2, 8, 3.6, 12, 6.8, 9.5);
                poly(g, false, 12, 7, 12, 12, 15.5, 14);
            }
            case CHEVRON_DOWN -> poly(g, false, 7, 9.5, 12, 14.5, 17, 9.5);
            case SEARCH -> {
                g.draw(new Ellipse2D.Double(4, 4, 12.5, 12.5));
                line(g, 14.8, 14.8, 20, 20);
            }
        }
    }

    private static RoundRectangle2D rr(double x, double y, double w, double h, double r) {
        return new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2);
    }

    private static void line(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void poly(Graphics2D g, boolean closed, double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        if (closed) {
            p.closePath();
        }
        g.draw(p);
    }
}
