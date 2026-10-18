package com.quanlynhakhoa.features.inventory.ui;

import com.quanlynhakhoa.QuanLyNhaKhoa;
import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.inventory.service.InventoryActor;
import com.quanlynhakhoa.features.inventory.service.InventoryItemService;
import com.quanlynhakhoa.features.inventory.service.InventoryOverviewService;
import com.quanlynhakhoa.features.inventory.service.StockInService;
import com.quanlynhakhoa.features.inventory.service.StockOutService;
import com.quanlynhakhoa.features.inventory.service.StockReturnService;
import com.quanlynhakhoa.features.inventory.service.StocktakeService;
import com.quanlynhakhoa.features.inventory.service.DisposalService;
import com.quanlynhakhoa.features.inventory.service.ControlledDrugService;
import com.quanlynhakhoa.features.inventory.service.EmergencyKitService;
import com.quanlynhakhoa.features.inventory.service.StockMonitorService;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import javax.sql.DataSource;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * DEV-ONLY (src/test, không vào bản build): chạy không gian kho với Spring + SQL Server THẬT
 * khi MainFrame/Login của nhóm chưa có. Cần Docker SQL Server đang chạy và file .env.
 *
 * <p>Chưa có phiên đăng nhập nên launcher tạm dùng Role SYSTEM_ADMIN và một nhân viên
 * đang làm việc có sẵn trong dbo.NhanVien (ưu tiên chức danh KHO) để ghi người lập phiếu.
 * Bản thật lấy cả hai từ phiên đăng nhập do MainFrame cấp.</p>
 *
 * <p>Muốn đóng vai nhân viên khác (để thử phê duyệt kép: người duyệt/xuất kho khác người lập),
 * thêm Program argument {@code --nv=NV0002}.</p>
 */
public final class InventoryDevLauncher {
    private InventoryDevLauncher() {
    }

    public static void main(String[] args) throws SQLException {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(QuanLyNhaKhoa.class)
                .headless(false)
                .web(WebApplicationType.NONE)
                .properties("n08.desktop.enabled=false",
                        // Bảng dùng tên camelCase (maThuoc); chặn Spring đổi sang ma_thuoc cho tới khi application.yaml có cấu hình này.
                        "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl")
                .run(args);
        String wanted = null;
        for (String arg : args) {
            if (arg.startsWith("--nv=")) {
                wanted = arg.substring(5).trim();
            }
        }
        InventoryActor actor = findDevActor(context.getBean(DataSource.class), wanted);
        if (actor == null) {
            JOptionPane.showMessageDialog(null,
                    "dbo.NhanVien chưa có nhân viên nào đang làm việc.\n"
                            + "Phiếu kho cần ghi người lập, nên hãy thêm ít nhất một nhân viên rồi chạy lại.",
                    "DEV — Kho", JOptionPane.WARNING_MESSAGE);
            context.close();
            return;
        }
        InventoryItemService itemService = context.getBean(InventoryItemService.class);
        InventoryOverviewService overviewService = context.getBean(InventoryOverviewService.class);
        StockInService stockInService = context.getBean(StockInService.class);
        StockOutService stockOutService = context.getBean(StockOutService.class);
        StockReturnService stockReturnService = context.getBean(StockReturnService.class);
        StocktakeService stocktakeService = context.getBean(StocktakeService.class);
        DisposalService disposalService = context.getBean(DisposalService.class);
        ControlledDrugService controlledDrugService = context.getBean(ControlledDrugService.class);
        EmergencyKitService emergencyKitService = context.getBean(EmergencyKitService.class);
        StockMonitorService stockMonitorService = context.getBean(StockMonitorService.class);

        SwingUtilities.invokeLater(() -> {
            InventoryWorkspacePanel pnlWorkspace =
                    new InventoryWorkspacePanel(overviewService, itemService, stockInService, stockOutService,
                            stockReturnService, stocktakeService,
                            disposalService, controlledDrugService, emergencyKitService, stockMonitorService,
                            () -> actor);
            JFrame winDev = new JFrame("DEV — Kho (SQL Server thật · " + actor.hoTen() + " · Role tạm SYSTEM_ADMIN)");
            winDev.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            winDev.setContentPane(pnlWorkspace);
            winDev.setSize(1500, 960);
            winDev.setLocationRelativeTo(null);
            winDev.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    new Thread(context::close, "n08-dev-shutdown").start();
                }
            });
            winDev.setVisible(true);
        });
    }

    /** @param maNhanVien null = tự chọn (ưu tiên chức danh KHO) */
    private static InventoryActor findDevActor(DataSource dataSource, String maNhanVien) throws SQLException {
        String sql = "SELECT TOP 1 maNhanVien, hoTen FROM dbo.NhanVien WHERE trangThai = 'DANG_LAM_VIEC' "
                + "AND (? IS NULL OR maNhanVien = ?) "
                + "ORDER BY CASE chucDanh WHEN 'KHO' THEN 0 WHEN 'AD' THEN 1 ELSE 2 END, maNhanVien";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, maNhanVien);
            statement.setString(2, maNhanVien);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new InventoryActor(Set.of(Role.SYSTEM_ADMIN), rs.getString(1), rs.getNString(2));
            }
        }
    }
}
