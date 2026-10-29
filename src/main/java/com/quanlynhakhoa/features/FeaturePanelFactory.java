package com.quanlynhakhoa.features;

import com.quanlynhakhoa.security.DesktopSession;
import javax.swing.JPanel;

@FunctionalInterface
public interface FeaturePanelFactory {
    JPanel create(DesktopSession session);
}
