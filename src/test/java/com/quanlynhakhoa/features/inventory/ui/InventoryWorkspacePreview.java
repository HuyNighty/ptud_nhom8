package com.quanlynhakhoa.features.inventory.ui;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.support.InventoryTestFixture;
import java.awt.Dimension;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * DEV-ONLY: xem nhanh giao diện kho KHÔNG cần SQL Server. Dữ liệu nằm trong bộ nhớ,
 * bắt đầu rỗng và mất khi đóng cửa sổ. Muốn dùng DB thật thì chạy {@link InventoryDevLauncher}.
 */
public final class InventoryWorkspacePreview {
    private InventoryWorkspacePreview() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            InventoryTestFixture fx = new InventoryTestFixture();
            fx.receipts.addEmployee("NV9999", "Người dùng");
            InventoryActor actor = new InventoryActor(Set.of(Role.SYSTEM_ADMIN), "NV9999", "Người dùng");
            InventoryWorkspacePanel pnlWorkspace = new InventoryWorkspacePanel(
                    fx.overviewService, fx.itemService, fx.stockInService, fx.stockOutService, () -> actor);

            JFrame winPreview = new JFrame("PREVIEW — Kho N08 (dữ liệu trong bộ nhớ)");
            winPreview.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            winPreview.setContentPane(pnlWorkspace);
            winPreview.setMinimumSize(new Dimension(1200, 760));
            winPreview.setSize(1500, 960);
            winPreview.setLocationRelativeTo(null);
            winPreview.setVisible(true);
        });
    }
}
