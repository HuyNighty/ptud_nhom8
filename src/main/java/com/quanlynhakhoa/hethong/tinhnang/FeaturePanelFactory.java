package com.quanlynhakhoa.hethong.tinhnang;

import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import javax.swing.JPanel;

@FunctionalInterface
public interface FeaturePanelFactory {
    JPanel create(DesktopSession session);
}
