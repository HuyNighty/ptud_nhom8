package com.quanlynhakhoa.features.inventory.ui;

import static com.quanlynhakhoa.features.inventory.ui.InventoryUiKit.*;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Màn hình của mục menu chưa được triển khai: nói rõ là đang phát triển, không hiển thị dữ liệu giả. */
final class InventoryPlaceholderPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    InventoryPlaceholderPanel(InventoryScreen screen) {
        super(new BorderLayout(0, 22));
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(28, 32, 32, 32));

        JLabel lblTitle = new JLabel(screen.label());
        lblTitle.setFont(font(Font.BOLD, 30));
        lblTitle.setForeground(TEXT);
        add(lblTitle, BorderLayout.NORTH);

        Card pnlNotice = new Card(null, 32);
        pnlNotice.setLayout(new BoxLayout(pnlNotice, BoxLayout.Y_AXIS));
        JLabel lblIcon = new JLabel(new GlyphIcon(screen.glyph(), 44, BORDER_STRONG));
        lblIcon.setAlignmentX(CENTER_ALIGNMENT);
        JLabel lblHeadline = new JLabel("Chức năng đang được phát triển");
        lblHeadline.setFont(font(Font.BOLD, 18));
        lblHeadline.setForeground(TEXT);
        lblHeadline.setAlignmentX(CENTER_ALIGNMENT);
        JLabel lblDescription = new JLabel("<html><div style='text-align:center;width:420px'>"
                + screen.plannedDescription() + "</div></html>");
        lblDescription.setFont(font(Font.PLAIN, 15));
        lblDescription.setForeground(TEXT_MUTED);
        lblDescription.setAlignmentX(CENTER_ALIGNMENT);
        pnlNotice.add(lblIcon);
        pnlNotice.add(Box.createVerticalStrut(14));
        pnlNotice.add(lblHeadline);
        pnlNotice.add(Box.createVerticalStrut(8));
        pnlNotice.add(lblDescription);

        JPanel pnlCenter = new JPanel(new GridBagLayout());
        pnlCenter.setOpaque(false);
        pnlCenter.add(pnlNotice);
        add(pnlCenter, BorderLayout.CENTER);
    }
}
