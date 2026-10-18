package com.quanlynhakhoa.desktop.frame;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

public final class MainFrame extends JFrame {
    public MainFrame() {
        super("N08 - Quan ly phong kham nha khoa");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(800, 500));
        setSize(1024, 640);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(new JLabel("N08 - Swing + Spring Boot are running in one JVM", SwingConstants.CENTER),
                BorderLayout.CENTER);
    }
}
