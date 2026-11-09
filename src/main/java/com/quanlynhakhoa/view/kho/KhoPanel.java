package com.quanlynhakhoa.view.kho;

import com.quanlynhakhoa.view.kho.lohang.LoHangPanel;
import com.quanlynhakhoa.view.kho.nhacungcap.NhaCungCapPanel;
import com.quanlynhakhoa.view.kho.thuoc.ThuocPanel;
import com.quanlynhakhoa.view.kho.vattu.VatTuPanel;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.BorderLayout;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/** Feature panel duy nhất đăng ký với Core cho Module.INVENTORY. */
public final class KhoPanel extends JPanel {
    private final DesktopSession phienDangNhap;
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final boolean[] daKhoiTao = new boolean[5];

    public KhoPanel(DesktopSession phienDangNhap) {
        this.phienDangNhap = Objects.requireNonNull(phienDangNhap, "phienDangNhap");
        setLayout(new BorderLayout());
        tabbedPane.addTab("Thuốc", new JPanel());
        tabbedPane.addTab("Vật tư", new JPanel());
        tabbedPane.addTab("Nhà cung cấp", new JPanel());
        tabbedPane.addTab("Lô hàng", new JPanel());
        if(new com.quanlynhakhoa.security.AuthorizationService(new com.quanlynhakhoa.security.RolePermissionPolicy()).can(phienDangNhap,com.quanlynhakhoa.enums.Quyen.INVENTORY_MANAGE))
            tabbedPane.addTab("Nhập / xuất / kiểm kê",new JPanel());
        add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.addChangeListener(event -> khoiTaoTab(tabbedPane.getSelectedIndex()));
        khoiTaoTab(0);
    }

    public void openOverviewSection(String section) {
        if("lots".equals(section))tabbedPane.setSelectedIndex(3);
        else if("supply".equals(section)||"stocktake".equals(section)){
            if(tabbedPane.getTabCount()<5)return;
            tabbedPane.setSelectedIndex(4);
            if(tabbedPane.getComponentAt(4) instanceof com.quanlynhakhoa.view.kho.quytrinh.QuyTrinhKhoPanel workflow)
                workflow.tabs().setSelectedIndex("supply".equals(section)?3:2);
        } else if("low-supply".equals(section))tabbedPane.setSelectedIndex(1);
        else if("low".equals(section))tabbedPane.setSelectedIndex(0);
    }

    private void khoiTaoTab(int chiSo) {
        if (chiSo < 0 || chiSo >= daKhoiTao.length || daKhoiTao[chiSo]) return;
        daKhoiTao[chiSo] = true;
        JPanel panel = switch (chiSo) {
            case 0 -> new ThuocPanel(phienDangNhap);
            case 1 -> new VatTuPanel(phienDangNhap);
            case 2 -> new NhaCungCapPanel(phienDangNhap);
            case 3 -> new LoHangPanel(phienDangNhap);
            case 4 -> new com.quanlynhakhoa.view.kho.quytrinh.QuyTrinhKhoPanel(phienDangNhap);
            default -> throw new IllegalArgumentException("Tab inventory không hợp lệ: " + chiSo);
        };
        com.quanlynhakhoa.hethong.giaodien.AppTheme.apply(panel);
        tabbedPane.setComponentAt(chiSo, panel);
    }
}
