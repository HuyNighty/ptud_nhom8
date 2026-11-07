package com.quanlynhakhoa.nghiepvu.kho.supplyrequest.view;

import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.nghiepvu.kho.supplyrequest.service.SupplyRequestService;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Requester mode is registered for SUPPLY_REQUEST; manager mode is embedded in inventory workflow. */
public final class SupplyRequestPanel extends JPanel {
    private final boolean managerMode;
    private final JTabbedPane tabs = new JTabbedPane();
    final JTextField requestId = new JTextField(), reason = new JTextField(), itemCode = new JTextField(), quantity = new JTextField("1");
    final JComboBox<String> itemType = new JComboBox<>(new String[]{"THUOC", "VATTU"});
    final JButton addLine = new JButton("Thêm dòng"), removeLine = new JButton("Bỏ dòng"), create = new JButton("Gửi yêu cầu"), refreshMine = new JButton("Làm mới");
    final DefaultTableModel draftModel = readOnly("Loại", "Mã hàng", "Số lượng");
    final DefaultTableModel mineModel = readOnly("Mã yêu cầu", "Thời gian", "Người tạo", "Trạng thái", "Lý do", "Số dòng");
    final DefaultTableModel managerModel = readOnly("Mã yêu cầu", "Thời gian", "Người yêu cầu", "Trạng thái", "Lý do", "Số dòng");
    final DefaultTableModel detailModel = new DefaultTableModel(new Object[]{"Dòng", "Loại", "Mã", "Tên hàng", "SL yêu cầu", "Mã lô cấp", "SL cấp"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 5;
        }
    };
    final JTable draftTable = new JTable(draftModel), mineTable = new JTable(mineModel), managerTable = new JTable(managerModel), detailTable = new JTable(detailModel);
    final JTextField voucherId = new JTextField(), department = new JTextField("Phòng khám"), receiver = new JTextField(), issueReason = new JTextField(), status = new JTextField();
    final JButton approve = new JButton("Duyệt"), reject = new JButton("Từ chối"), issue = new JButton("Cấp phát + tạo phiếu xuất"), refreshManager = new JButton("Làm mới");

    public SupplyRequestPanel(DesktopSession session) {
        this(session, false);
    }

    public SupplyRequestPanel(DesktopSession session, boolean managerMode) {
        this.managerMode = managerMode;
        SupplyRequestService service = new SupplyRequestService(session);
        if (managerMode && !service.canManage()) throw new SecurityException("Màn hình xử lý yêu cầu chỉ dành cho quản lý kho.");
        setLayout(new BorderLayout());
        if (managerMode) {
            add(buildManager(), BorderLayout.CENTER);
        } else {
            tabs.addTab("Tạo yêu cầu", buildCreate());
            tabs.addTab("Theo dõi yêu cầu", buildMine());
            add(tabs, BorderLayout.CENTER);
        }
        new SupplyRequestController(this, service);
    }

    private JPanel buildCreate() {
        JPanel page = new JPanel(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridLayout(0, 4, 7, 7));
        addField(form, "Mã yêu cầu (≤10)", requestId);
        addField(form, "Lý do", reason);
        addField(form, "Loại mặt hàng", itemType);
        addField(form, "Mã thuốc/vật tư", itemCode);
        addField(form, "Số lượng", quantity);
        form.add(addLine);
        form.add(removeLine);
        form.add(create);
        page.add(form, BorderLayout.NORTH);
        page.add(new JScrollPane(draftTable), BorderLayout.CENTER);
        page.add(new JLabel("Thêm từng mặt hàng vào danh sách rồi gửi. Quản lý kho sẽ duyệt trước khi xuất tồn."), BorderLayout.SOUTH);
        return page;
    }

    private JPanel buildMine() {
        JPanel page = new JPanel(new BorderLayout(8, 8));
        page.add(refreshMine, BorderLayout.NORTH);
        page.add(new JScrollPane(mineTable), BorderLayout.CENTER);
        page.add(new JLabel("Chỉ hiển thị yêu cầu do tài khoản hiện tại tạo."), BorderLayout.SOUTH);
        return page;
    }

    private JPanel buildManager() {
        JPanel page = new JPanel(new BorderLayout(8, 8));
        JPanel actions = new JPanel(new GridLayout(0, 4, 7, 7));
        addField(actions, "Mã phiếu xuất (≤10)", voucherId);
        addField(actions, "Bộ phận nhận", department);
        addField(actions, "Người nhận", receiver);
        addField(actions, "Lý do xuất", issueReason);
        actions.add(approve);
        actions.add(reject);
        actions.add(issue);
        actions.add(refreshManager);
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(new JScrollPane(detailTable), BorderLayout.CENTER);
        bottom.add(actions, BorderLayout.SOUTH);
        page.add(new JScrollPane(managerTable), BorderLayout.CENTER);
        page.add(bottom, BorderLayout.SOUTH);
        page.add(new JLabel("Duyệt trước khi cấp. Nhập mã lô cho từng dòng; mỗi dòng phải được cấp đủ từ một lô."), BorderLayout.NORTH);
        return page;
    }

    private static void addField(JPanel panel, String label, java.awt.Component field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    private static DefaultTableModel readOnly(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
    }

    public boolean isManagerMode() {
        return managerMode;
    }

    public JTabbedPane tabs() {
        return tabs;
    }

    String selectedMineId() {
        return selectedId(mineTable);
    }

    String selectedManagerId() {
        return selectedId(managerTable);
    }

    private static String selectedId(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return String.valueOf(table.getValueAt(table.convertRowIndexToModel(row), 0));
    }

    void showError(Throwable error) {
        String message = error.getMessage() == null ? "Không thể xử lý yêu cầu." : error.getMessage();
        JOptionPane.showMessageDialog(this, message, "Yêu cầu cấp phát", JOptionPane.ERROR_MESSAGE);
    }
}