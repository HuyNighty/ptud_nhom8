package com.quanlynhakhoa.view.kho.quytrinh;

import com.quanlynhakhoa.controller.kho.quytrinh.QuyTrinhKhoController;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.view.kho.cungung.YeuCauCungUngPanel;
import com.quanlynhakhoa.service.kho.quytrinh.QuyTrinhKhoService;
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
public final class QuyTrinhKhoPanel extends JPanel {
    private final JTabbedPane tabs = new JTabbedPane();
    private final ReceiptTab phieuNhap = new ReceiptTab();
    private final IssueTab phieuXuat = new IssueTab();
    private final StocktakeTab kiemKe = new StocktakeTab();

    public QuyTrinhKhoPanel(DesktopSession session) {
        setLayout(new BorderLayout());
        tabs.addTab("Nhập kho", phieuNhap);
        tabs.addTab("Xuất kho", phieuXuat);
        tabs.addTab("Kiểm kê", kiemKe);
        tabs.addTab("Yêu cầu cấp phát", new YeuCauCungUngPanel(session, true));
        add(tabs, BorderLayout.CENTER);
        new QuyTrinhKhoController(this, new QuyTrinhKhoService(session));
    }

    public JTabbedPane tabs() { return tabs; }
    public ReceiptTab receipt() { return phieuNhap; }
    public IssueTab issue() { return phieuXuat; }
    public StocktakeTab stocktake() { return kiemKe; }

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
        final JButton btnLuu = new JButton("Tạo phiếu nhập"), btnLamMoi = new JButton("Làm mới");
        final DefaultTableModel duLieu = model("Mã phiếu", "Ngày nhập", "Nhà cung cấp", "Trạng thái", "Mã lô", "Số lượng", "Đơn giá");

        ReceiptTab() {
            setLayout(new BorderLayout(10, 10));
            JPanel form = new JPanel(new GridLayout(0, 4, 7, 7));
            row(form, "Mã phiếu (≤10)", maPhieu);
            row(form, "Mã lô mới (≤10)", maLo);
            row(form, "Số lô nhà sản xuất", soLo);
            row(form, "Mã nhà cung cấp", maNcc);
            row(form, "Loại hàng", loai);
            row(form, "Mã thuốc/vật tư", maHang);
            row(form, "Ngày sản xuất yyyy-MM-dd", com.quanlynhakhoa.view.hethong.DateInput.iso(ngaySX,"ngày sản xuất"));
            row(form, "Hạn sử dụng yyyy-MM-dd", com.quanlynhakhoa.view.hethong.DateInput.iso(hanSD,"hạn sử dụng"));
            row(form, "Số lượng", soLuong);
            row(form, "Đơn giá nhập", donGia);
            row(form, "Ghi chú", ghiChu);
            form.add(btnLuu);
            form.add(btnLamMoi);
            add(form, BorderLayout.NORTH);
            add(new JScrollPane(new JTable(duLieu)), BorderLayout.CENTER);
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
        public JButton save() { return btnLuu; }
        public JButton refresh() { return btnLamMoi; }
        public DefaultTableModel data() { return duLieu; }
    }

    public static final class IssueTab extends JPanel {
        final JTextField maPhieu = new JTextField(), maLo = new JTextField(), boPhan = new JTextField();
        final JTextField nguoiNhan = new JTextField(), lyDo = new JTextField(), soLuong = new JTextField();
        final JButton btnLuu = new JButton("Tạo phiếu xuất"), btnLamMoi = new JButton("Làm mới");
        final DefaultTableModel duLieu = model("Mã phiếu", "Ngày xuất", "Bộ phận", "Người nhận", "Nhân viên", "Trạng thái", "Mã lô", "Số lượng");

        IssueTab() {
            setLayout(new BorderLayout(10, 10));
            JPanel form = new JPanel(new GridLayout(0, 4, 7, 7));
            row(form, "Mã phiếu (≤10)", maPhieu);
            row(form, "Mã lô", maLo);
            row(form, "Bộ phận nhận", boPhan);
            row(form, "Người nhận", nguoiNhan);
            row(form, "Lý do", lyDo);
            row(form, "Số lượng", soLuong);
            form.add(btnLuu);
            form.add(btnLamMoi);
            add(form, BorderLayout.NORTH);
            add(new JScrollPane(new JTable(duLieu)), BorderLayout.CENTER);
            add(new JLabel("Tồn lô chỉ giảm nếu đủ số lượng; mỗi phiếu xuất ghi một lô."), BorderLayout.SOUTH);
        }

        public JTextField maPhieu() { return maPhieu; }
        public JTextField maLo() { return maLo; }
        public JTextField boPhan() { return boPhan; }
        public JTextField nguoiNhan() { return nguoiNhan; }
        public JTextField lyDo() { return lyDo; }
        public JTextField soLuong() { return soLuong; }
        public JButton save() { return btnLuu; }
        public JButton refresh() { return btnLamMoi; }
        public DefaultTableModel data() { return duLieu; }
    }

    public static final class StocktakeTab extends JPanel {
        final JTextField maPhieu = new JTextField(), maLo = new JTextField(), soLuongThucTe = new JTextField(), lyDo = new JTextField();
        final JButton btnLuu = new JButton("Gửi yêu cầu kiểm kê"), btnLamMoi = new JButton("Làm mới"), btnDuyet = new JButton("Duyệt kiểm kê"), btnTuChoi = new JButton("Từ chối");
        final DefaultTableModel duLieu = model("Mã phiếu", "Thời gian", "Tạo bởi", "Lý do", "Trạng thái", "Mã lô", "SL hệ thống", "SL thực tế");
        final JTable table = new JTable(duLieu);

        StocktakeTab() {
            setLayout(new BorderLayout(10, 10));
            JPanel form = new JPanel(new GridLayout(0, 4, 7, 7));
            row(form, "Mã phiếu (≤10)", maPhieu);
            row(form, "Mã lô", maLo);
            row(form, "Số lượng thực tế", soLuongThucTe);
            row(form, "Lý do chênh lệch", lyDo);
            form.add(btnLuu);
            form.add(btnLamMoi);
            form.add(btnDuyet);
            form.add(btnTuChoi);
            add(form, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);
            add(new JLabel("Người duyệt khác người tạo. Nếu tồn kho thay đổi sau kiểm kê thì phải kiểm kê lại."), BorderLayout.SOUTH);
        }

        public JTextField maPhieu() { return maPhieu; }
        public JTextField maLo() { return maLo; }
        public JTextField actual() { return soLuongThucTe; }
        public JTextField reason() { return lyDo; }
        public JButton save() { return btnLuu; }
        public JButton refresh() { return btnLamMoi; }
        public JButton approve() { return btnDuyet; }
        public JButton reject() { return btnTuChoi; }
        public String selectedId() {
            int r = table.getSelectedRow();
            return r < 0 ? null : String.valueOf(duLieu.getValueAt(table.convertRowIndexToModel(r), 0));
        }
        public DefaultTableModel data() { return duLieu; }
    }
}
