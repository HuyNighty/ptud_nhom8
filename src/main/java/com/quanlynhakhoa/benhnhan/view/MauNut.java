package com.quanlynhakhoa.benhnhan.view;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JButton;

public class MauNut {
    private static final Color XANH_DUONG = new Color(43, 110, 190);
    private static final Color XANH_DUONG_NHAT = new Color(185, 208, 235);
    private static final Color XANH_LA = new Color(40, 140, 80);
    private static final Color XANH_LA_NHAT = new Color(185, 225, 200);
    private static final Color CAM = new Color(215, 125, 20);
    private static final Color CAM_NHAT = new Color(245, 215, 175);
    private static final Color DO = new Color(200, 45, 45);
    private static final Color DO_NHAT = new Color(240, 190, 190);

    private MauNut() {
    }

    public static void toXanhDuong(JButton nut) {
        toMau(nut, XANH_DUONG, XANH_DUONG_NHAT);
    }

    public static void toXanhLa(JButton nut) {
        toMau(nut, XANH_LA, XANH_LA_NHAT);
    }

    public static void toCam(JButton nut) {
        toMau(nut, CAM, CAM_NHAT);
    }

    public static void toDo(JButton nut) {
        toMau(nut, DO, DO_NHAT);
    }

    private static void toMau(JButton nut, Color mauDam, Color mauNhat) {
        nut.setOpaque(true);
        nut.setForeground(Color.WHITE);
        capNhatMau(nut, mauDam, mauNhat);

        nut.addPropertyChangeListener("enabled", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent suKien) {
                capNhatMau(nut, mauDam, mauNhat);
            }
        });
    }

    private static void capNhatMau(JButton nut, Color mauDam, Color mauNhat) {
        if (nut.isEnabled()) {
            nut.setBackground(mauDam);
        } else {
            nut.setBackground(mauNhat);
        }
    }
}
