package com.quanlynhakhoa.features.inventory.ui;

import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.service.AuthorizationService;
import com.quanlynhakhoa.features.auth.service.RolePermissionPolicy;
import com.quanlynhakhoa.features.inventory.service.InventoryOverviewService;
import java.awt.Dimension;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * DEV-ONLY: xem nhanh InventoryOverviewPanel mà không cần Spring/SQL Server/Login.
 * Nằm trong src/test nên không vào bản build. Role SYSTEM_ADMIN ở đây chỉ để preview;
 * khi chạy thật, Role phải lấy từ phiên đăng nhập do MainFrame cung cấp.
 */
public final class InventoryOverviewPanelPreview {
    private InventoryOverviewPanelPreview() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            InventoryOverviewService service =
                    new InventoryOverviewService(new AuthorizationService(new RolePermissionPolicy()));
            JFrame frame = new JFrame("Preview — Quản lý kho / Tổng quan");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(new InventoryOverviewPanel(service, () -> Set.of(Role.SYSTEM_ADMIN)));
            frame.setMinimumSize(new Dimension(1100, 700));
            frame.setSize(1500, 980);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
