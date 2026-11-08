package com.quanlynhakhoa.nghiepvu.kho.workflow.view;

import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import com.quanlynhakhoa.nghiepvu.kho.supplyrequest.view.SupplyRequestPanel;
import com.quanlynhakhoa.nghiepvu.kho.workflow.service.InventoryWorkflowService;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Nhập, xuất và ghi nhận chênh lệch tồn kho; mọi ghi dữ liệu đi qua service. */
public final class InventoryWorkflowPanel extends JPanel {
    private final JButton save = new JButton("Lưu");
    private final JButton refresh = new JButton("Làm mới");
    private final JButton approve = new JButton("Duyệt");
    private final JButton reject = new JButton("Từ chối");
    private final JTabbedPane tabs = new JTabbedPane();
    private final ReceiptTab receipt = new ReceiptTab();
    private final IssueTab issue = new IssueTab();
    private final StocktakeTab stocktake = new StocktakeTab();

    public InventoryWorkflowPanel(DesktopSession session) {
        setLayout(new BorderLayout());
        tabs.addTab("Nhập kho", receipt);
        tabs.addTab("Xuất kho", issue);
        tabs.addTab("Kiểm kê", stocktake);
        tabs.addTab("Yêu cầu cấp phát", new SupplyRequestPanel(session, true));
        add(tabs, BorderLayout.CENTER);
        new InventoryWorkflowController(this, new InventoryWorkflowService(session));
    }

    public JTabbedPane tabs() { return tabs; }
    public ReceiptTab receipt() { return receipt; }
    public IssueTab issue() { return issue; }
    public StocktakeTab stocktake() { return stocktake; }

    private static void row(JPanel panel, String label, java.awt.Component field) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static final class ReceiptTab extends JPanel {
        final JTextField maPhieu = new JTextField(), maLo = new JTextField(), soLo = new JTextField(), maNcc = new JTextField();
        final JComboBox<String> loai = new JComboBox<>(new String[]{"THUOC", "VATTU"});
        final JTextField maHang = new JTextField(), ngaySX = new JTextField(), hanSD = new JTextField();
        final JTextField soLuong = new JTextField(), donGia = new JTextField(), ghiChu = new JTextField();
        final JButton save = new JButton("Tạo phiếu nhập"), refresh = new JButton("Tải lại");
        final DefaultTableModel data = model("Mã phiếu", "Ngày nhập", "Nhà cung cấp", "Trạng thái", "Mã lô", "Số lượng", "Đơn giá");

        ReceiptTab() {
            setLayout(new BorderLayout(10, 10));
            JPanel form = new JPanel(new GridLayout(0, 4, 7, 7));
            row(form, "Mã phiếu (≤10)", maPhieu);
            row(form, "Mã lô mới (≤10)", maLo);
            row(form, "Số lô nhà sản xuất", soLo);
            row(form, "Mã nhà cung cấp", maNcc);
            row(form, "Loại hàng", loai);
            row(form, "Mã thuốc/vật tư", maHang);
            row(form, "Ngày sản xuất yyyy-MM-dd", ngaySX);
            row(form, "Hạn sử dụng yyyy-MM-dd", hanSD);
            row(form, "Số lượng", soLuong);
            row(form, "Đơn giá nhập", donGia);
            row(form, "Ghi chú", ghiChu);
            form.add(save);
            form.add(refresh);
            add(form, BorderLayout.NORTH);
            add(new JScrollPane(new JTable(data)), BorderLayout.CENTER);
            add(new JLabel("Một phiếu nhập ghi một lô; mã thuốc/vật tư và nhà cung cấp phải tồn tại trong danh mục."), BorderLayout.SOUTH);
        }

        public JTextField maPhieu() { return maPhieu; }
        public JTextField maLo() { return maLo; }
        public JTextField soLo() { return soLo; }
        public JTextField maNcc() { return maNcc; }
        public JComboBox<String> loai() { return loai; }
        public JTextField maHang() { return maHang; }
        public JTextField ngaySX() { return ngaySX; }
        public JTextField hanSD() { return hanSD; }
        public JTextField soLuong() { return soLuong; }
        public JTextField donGia() { return donGia; }
        public JTextField ghiChu() { return ghiChu; }
        public JButton save() { return save; }
        public JButton refresh() { return refresh; }
        public DefaultTableModel data() { return data; }
    }

    public static final class IssueTab extends JPanel {
        final JTextField maPhieu = new JTextField(), maLo = new JTextField(), boPhan = new JTextField();
        final JTextField nguoiNhan = new JTextField(), lyDo = new JTextField(), soLuong = new JTextField();
        final JButton save = new JButton("Tạo phiếu xuất"), refresh = new JButton("Tải lại");
        final DefaultTableModel data = model("Mã phiếu", "Ngày xuất", "Bộ phận", "Người nhận", "Nhân viên", "Trạng thái", "Mã lô", "Số lượng");

        IssueTab() {
            setLayout(new BorderLayout(10, 10));
            JPanel form = new JPanel(new GridLayout(0, 4, 7, 7));
            row(form, "Mã phiếu (≤10)", maPhieu);
            row(form, "Mã lô", maLo);
            row(form, "Bộ phận nhận", boPhan);
            row(form, "Người nhận", nguoiNhan);
            row(form, "Lý do", lyDo);
            row(form, "Số lượng", soLuong);
            form.add(save);
            form.add(refresh);
            add(form, BorderLayout.NORTH);
            add(new JScrollPane(new JTable(data)), BorderLayout.CENTER);
            add(new JLabel("Tồn lô chỉ giảm nếu đủ số lượng; mỗi phiếu xuất ghi một lô."), BorderLayout.SOUTH);
        }

        public JTextField maPhieu() { return maPhieu; }
        public JTextField maLo() { return maLo; }
        public JTextField boPhan() { return boPhan; }
        public JTextField nguoiNhan() { return nguoiNhan; }
        public JTextField lyDo() { return lyDo; }
        public JTextField soLuong() { return soLuong; }
        public JButton save() { return save; }
        public JButton refresh() { return refresh; }
        public DefaultTableModel data() { return data; }
    }

    public static final class StocktakeTab extends JPanel {
        final JButton save = new JButton("Lưu");
        final JButton refresh = new JButton("Tải lại");
        final JButton approve = new JButton("Duyệt");
        final JButton reject = new JButton("Từ chối");
        final JTextField maPhieu = new JTextField(), maLo = new JTextField(), actual = new JTextField(), reason = new JTextField();
        final DefaultTableModel data = model("Mã phiếu", "Thời gian", "Tạo bởi", "Lý do", "Trạng thái", "Mã lô", "SL hệ thống", "SL thực tế");
        final JTable table = new JTable(data);

        StocktakeTab() {
            setLayout(new BorderLayout(10, 10));
            JPanel form = new JPanel(new GridLayout(0, 4, 7, 7));
            row(form, "Mã phiếu (≤10)", maPhieu);
            row(form, "Mã lô", maLo);
            row(form, "Số lượng thực tế", actual);
            row(form, "Lý do chênh lệch", reason);
            form.add(save);
            form.add(refresh);
            form.add(approve);
            form.add(reject);
            add(form, BorderLayout.NORTH);
        }

        public JTextField maPhieu() {
            return maPhieu;
        }
        public JTextField maLo() {
            return maLo;
        }
        public JTextField actual() {
            return actual;
        }
        public JTextField reason() {
            return reason;
        }
        public JButton save() {
            return save;
        }
        public JButton refresh() {
            return refresh;
        }
        public JButton approve() {
            return approve;
        }
        public JButton reject() {
            return reject;
        }
        public String selectedId() {
            int r = table.getSelectedRow();
            return r < 0 ? null : String.valueOf(data.getValueAt(table.convertRowIndexToModel(r), 0));
        }
        public DefaultTableModel data() { return data; }
    }
}