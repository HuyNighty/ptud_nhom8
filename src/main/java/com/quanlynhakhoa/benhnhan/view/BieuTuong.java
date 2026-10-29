package com.quanlynhakhoa.benhnhan.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import javax.swing.Icon;

// Icon nhỏ tự vẽ bằng Graphics2D (không cần file ảnh, không cần thư viện ngoài).
// Cách dùng: btnLoc.setIcon(new BieuTuong(BieuTuong.TIM_KIEM));
public class BieuTuong implements Icon {
    public static final int TIM_KIEM = 1;   // kính lúp
    public static final int LAM_MOI = 2;    // mũi tên vòng tròn

    private static final int KICH_THUOC = 16;            // icon 16x16 pixel
    private static final Color MAU = new Color(29, 61, 102); // xanh navy giống tiêu đề

    private int loai;

    public BieuTuong(int loai) {
        this.loai = loai;
    }

    @Override
    public int getIconWidth() {
        return KICH_THUOC;
    }

    @Override
    public int getIconHeight() {
        return KICH_THUOC;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // nét mịn
        // Nút bị làm mờ thì icon cũng mờ theo
        g2.setColor(c != null && !c.isEnabled() ? Color.LIGHT_GRAY : MAU);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.translate(x, y);

        if (loai == TIM_KIEM) {
            veKinhLup(g2);
        } else if (loai == LAM_MOI) {
            veLamMoi(g2);
        }
        g2.dispose();
    }

    // Kính lúp: 1 vòng tròn + 1 cán chéo xuống góc phải
    private void veKinhLup(Graphics2D g2) {
        g2.drawOval(2, 2, 9, 9);      // mắt kính
        g2.drawLine(10, 10, 14, 14);  // cán kính
    }

    // Làm mới: 1 cung tròn gần kín + 1 đầu mũi tên ở cuối cung
    private void veLamMoi(Graphics2D g2) {
        // Cung tròn từ 60° quay ngược chiều kim đồng hồ 280°
        g2.draw(new Arc2D.Double(2.5, 2.5, 11, 11, 60, 280, Arc2D.OPEN));
        // Đầu mũi tên (tam giác) ở điểm bắt đầu cung, phía trên bên phải
        Path2D muiTen = new Path2D.Double();
        muiTen.moveTo(13.5, 1.5);
        muiTen.lineTo(13.5, 6.5);
        muiTen.lineTo(8.5, 6.5);
        muiTen.closePath();
        g2.fill(muiTen);
    }
}
