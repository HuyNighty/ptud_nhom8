package com.quanlynhakhoa.inventory;

import com.quanlynhakhoa.inventory.lohang.view.LoHangPanel;
import com.quanlynhakhoa.inventory.nhacungcap.view.NhaCungCapPanel;
import com.quanlynhakhoa.inventory.thuoc.view.ThuocPanel;
import com.quanlynhakhoa.inventory.vattu.view.VatTuPanel;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.BorderLayout;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/** Feature panel duy nhất đăng ký với Core cho Module.INVENTORY. */
public final class InventoryPanel extends JPanel {
    private final DesktopSession phienDangNhap;
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final boolean[] daKhoiTao = new boolean[4];

    public InventoryPanel(DesktopSession phienDangNhap) {
        this.phienDangNhap = Objects.requireNonNull(phienDangNhap, "phienDangNhap");
        setLayout(new BorderLayout());
        tabbedPane.addTab("Thuốc", new JPanel());
        tabbedPane.addTab("Vật tư", new JPanel());
        tabbedPane.addTab("Nhà cung cấp", new JPanel());
        tabbedPane.addTab("Lô hàng", new JPanel());
        add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.addChangeListener(event -> khoiTaoTab(tabbedPane.getSelectedIndex()));
        khoiTaoTab(0);
    }

    private void khoiTaoTab(int chiSo) {
        if (chiSo < 0 || chiSo >= daKhoiTao.length || daKhoiTao[chiSo]) return;
        daKhoiTao[chiSo] = true;
        JPanel panel = switch (chiSo) {
            case 0 -> new ThuocPanel(phienDangNhap);
            case 1 -> new VatTuPanel(phienDangNhap);
            case 2 -> new NhaCungCapPanel(phienDangNhap);
            case 3 -> new LoHangPanel(phienDangNhap);
            default -> throw new IllegalArgumentException("Tab inventory không hợp lệ: " + chiSo);
        };
        tabbedPane.setComponentAt(chiSo, panel);
    }
}