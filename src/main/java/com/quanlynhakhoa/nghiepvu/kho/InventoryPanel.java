package com.quanlynhakhoa.nghiepvu.kho;

import com.quanlynhakhoa.nghiepvu.kho.lohang.view.LoHangPanel;
import com.quanlynhakhoa.nghiepvu.kho.nhacungcap.view.NhaCungCapPanel;
import com.quanlynhakhoa.nghiepvu.kho.thuoc.view.ThuocPanel;
import com.quanlynhakhoa.nghiepvu.kho.vattu.view.VatTuPanel;
import com.quanlynhakhoa.nghiepvu.kho.phieunhap.view.PhieuNhapPanel;
import com.quanlynhakhoa.nghiepvu.kho.phieuxuat.view.PhieuXuatPanel;
import com.quanlynhakhoa.nghiepvu.kho.kiemke.view.KiemKePanel;
import com.quanlynhakhoa.nghiepvu.kho.hopcapcuu.view.HopCapCuuPanel;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import java.awt.BorderLayout;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public final class InventoryPanel extends JPanel {
    private final DesktopSession phienDangNhap;
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final boolean[] daKhoiTao = new boolean[8]; // Tăng lên 8 tab

    public InventoryPanel(DesktopSession phienDangNhap) {
        this.phienDangNhap = Objects.requireNonNull(phienDangNhap, "phienDangNhap");
        setLayout(new BorderLayout());

        tabbedPane.addTab("Thuốc", new JPanel());
        tabbedPane.addTab("Vật tư", new JPanel());
        tabbedPane.addTab("Nhà cung cấp", new JPanel());
        tabbedPane.addTab("Lô hàng", new JPanel());
        tabbedPane.addTab("Nhập kho", new JPanel());
        tabbedPane.addTab("Xuất kho", new JPanel());
        tabbedPane.addTab("Kiểm kê", new JPanel());
        tabbedPane.addTab("Hộp cấp cứu", new JPanel());

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
            case 4 -> new PhieuNhapPanel(phienDangNhap);
            case 5 -> new PhieuXuatPanel(phienDangNhap);
            case 6 -> new KiemKePanel(phienDangNhap);
            case 7 -> new HopCapCuuPanel(phienDangNhap);
            default -> throw new IllegalArgumentException("Tab inventory không hợp lệ: " + chiSo);
        };
        tabbedPane.setComponentAt(chiSo, panel);
    }
}