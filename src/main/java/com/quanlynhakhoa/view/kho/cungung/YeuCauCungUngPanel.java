package com.quanlynhakhoa.view.kho.cungung;

import com.quanlynhakhoa.controller.kho.cungung.YeuCauCungUngController;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.service.kho.cungung.YeuCauCungUngService;
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
public final class YeuCauCungUngPanel extends JPanel {
    private final boolean cheDoQuanLy;
    private final JTabbedPane tabs = new JTabbedPane();
    final JTextField maYeuCau = new JTextField(), lyDo = new JTextField(), maHang = new JTextField(), soLuong = new JTextField("1");
    final JComboBox<String> loaiHang = new JComboBox<>(new String[]{"THUOC", "VATTU"});
    final JButton btnThemDong = new JButton("Thêm dòng"), btnBoDong = new JButton("Bỏ dòng"), btnTao = new JButton("Gửi yêu cầu"), btnLamMoiCuaToi = new JButton("Làm mới");
    final DefaultTableModel modelBanNhap = readOnly("Loại", "Mã hàng", "Số lượng");
    final DefaultTableModel modelCuaToi = readOnly("Mã yêu cầu", "Thời gian", "Người tạo", "Trạng thái", "Lý do", "Số dòng");
    final DefaultTableModel modelQuanLy = readOnly("Mã yêu cầu", "Thời gian", "Người yêu cầu", "Trạng thái", "Lý do", "Số dòng");
    final DefaultTableModel modelChiTiet = new DefaultTableModel(new Object[]{"Dòng", "Loại", "Mã", "Tên hàng", "SL yêu cầu", "Mã lô cấp", "SL cấp"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 5;
        }
    };
    final JTable tblBanNhap = new JTable(modelBanNhap), tblCuaToi = new JTable(modelCuaToi), tblQuanLy = new JTable(modelQuanLy), tblChiTiet = new JTable(modelChiTiet);
    final JTextField maPhieu = new JTextField(), boPhan = new JTextField("Phòng khám"), nguoiNhan = new JTextField(), lyDoXuat = new JTextField(), trangThai = new JTextField();
    final JButton btnDuyet = new JButton("Duyệt"), btnTuChoi = new JButton("Từ chối"), phieuXuat = new JButton("Cấp phát + tạo phiếu xuất"), btnLamMoiQuanLy = new JButton("Làm mới");

    public YeuCauCungUngPanel(DesktopSession session) {
        this(session, false);
    }

    public YeuCauCungUngPanel(DesktopSession session, boolean managerMode) {
        this.cheDoQuanLy = managerMode;
        YeuCauCungUngService service = new YeuCauCungUngService(session);
        if (managerMode && !service.canManage()) throw new SecurityException("Màn hình xử lý yêu cầu chỉ dành cho quản lý kho.");
        setLayout(new BorderLayout());
        if (managerMode) {
            add(buildManager(), BorderLayout.CENTER);
        } else {
            tabs.addTab("Tạo yêu cầu", buildCreate());
            tabs.addTab("Theo dõi yêu cầu", buildMine());
            add(tabs, BorderLayout.CENTER);
        }
        new YeuCauCungUngController(this, service);
    }

    private JPanel buildCreate() {
        JPanel page = new JPanel(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridLayout(0, 4, 7, 7));
        addField(form, "Mã yêu cầu (≤10)", maYeuCau);
        addField(form, "Lý do", lyDo);
        addField(form, "Loại mặt hàng", loaiHang);
        addField(form, "Mã thuốc/vật tư", maHang);
        addField(form, "Số lượng", soLuong);
        form.add(btnThemDong);
        form.add(btnBoDong);
        form.add(btnTao);
        page.add(form, BorderLayout.NORTH);
        page.add(new JScrollPane(tblBanNhap), BorderLayout.CENTER);
        page.add(new JLabel("Thêm từng mặt hàng vào danh sách rồi gửi. Quản lý kho sẽ duyệt trước khi xuất tồn."), BorderLayout.SOUTH);
        return page;
    }

    private JPanel buildMine() {
        JPanel page = new JPanel(new BorderLayout(8, 8));
        page.add(btnLamMoiCuaToi, BorderLayout.NORTH);
        page.add(new JScrollPane(tblCuaToi), BorderLayout.CENTER);
        page.add(new JLabel("Chỉ hiển thị yêu cầu do tài khoản hiện tại tạo."), BorderLayout.SOUTH);
        return page;
    }

    private JPanel buildManager() {
        JPanel page = new JPanel(new BorderLayout(8, 8));
        JPanel actions = new JPanel(new GridLayout(0, 4, 7, 7));
        addField(actions, "Mã phiếu xuất (≤10)", maPhieu);
        addField(actions, "Bộ phận nhận", boPhan);
        addField(actions, "Người nhận", nguoiNhan);
        addField(actions, "Lý do xuất", lyDoXuat);
        actions.add(btnDuyet);
        actions.add(btnTuChoi);
        actions.add(phieuXuat);
        actions.add(btnLamMoiQuanLy);
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.add(new JScrollPane(tblChiTiet), BorderLayout.CENTER);
        bottom.add(actions, BorderLayout.SOUTH);
        page.add(new JScrollPane(tblQuanLy), BorderLayout.CENTER);
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
        return cheDoQuanLy;
    }

    public JTabbedPane tabs() {
        return tabs;
    }

    public String selectedMineId() {
        return selectedId(tblCuaToi);
    }

    public String selectedManagerId() {
        return selectedId(tblQuanLy);
    }

    private static String selectedId(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return String.valueOf(table.getValueAt(table.convertRowIndexToModel(row), 0));
    }

    public void showError(Throwable error) {
        String message = error.getMessage() == null ? "Không thể xử lý yêu cầu." : error.getMessage();
        JOptionPane.showMessageDialog(this, message, "Yêu cầu cấp phát", JOptionPane.ERROR_MESSAGE);
    }

    // Controller bindings; widgets remain owned by this view.
    public JButton addLine() { return btnThemDong; }
    public JButton approve() { return btnDuyet; }
    public JButton create() { return btnTao; }
    public JTextField department() { return boPhan; }
    public DefaultTableModel detailModel() { return modelChiTiet; }
    public DefaultTableModel draftModel() { return modelBanNhap; }
    public JTable draftTable() { return tblBanNhap; }
    public JButton issue() { return phieuXuat; }
    public JTextField issueReason() { return lyDoXuat; }
    public JTextField itemCode() { return maHang; }
    public JComboBox<String> itemType() { return loaiHang; }
    public DefaultTableModel managerModel() { return modelQuanLy; }
    public JTable managerTable() { return tblQuanLy; }
    public DefaultTableModel mineModel() { return modelCuaToi; }
    public JTextField quantity() { return soLuong; }
    public JTextField reason() { return lyDo; }
    public JTextField receiver() { return nguoiNhan; }
    public JButton refreshManager() { return btnLamMoiQuanLy; }
    public JButton refreshMine() { return btnLamMoiCuaToi; }
    public JButton reject() { return btnTuChoi; }
    public JButton removeLine() { return btnBoDong; }
    public JTextField requestId() { return maYeuCau; }
    public JTextField voucherId() { return maPhieu; }
}
