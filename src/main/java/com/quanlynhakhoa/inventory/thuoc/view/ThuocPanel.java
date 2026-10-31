package com.quanlynhakhoa.inventory.thuoc.view;

import com.quanlynhakhoa.inventory.thuoc.controller.ThuocController;
import com.quanlynhakhoa.inventory.thuoc.model.Thuoc;
import com.quanlynhakhoa.inventory.thuoc.service.ThuocService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** View danh mục thuốc; không chứa SQL hoặc quy tắc lưu trữ. */
public final class ThuocPanel extends JPanel {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(97, 115, 137);
    private static final Color BORDER = new Color(220, 231, 242);
    private static final Color BACKGROUND = new Color(245, 248, 252);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField txtTimKiem = new JTextField(24);
    private final JButton btnTim = new JButton("Tìm kiếm");
    private final JButton btnTaiLai = new JButton("Làm mới");
    private final JButton btnThem = new JButton("+ Thêm thuốc");
    private final JButton btnSua = new JButton("Sửa thuốc");
    private final JLabel lblTrangThai = new JLabel("Đang tải danh sách thuốc...");
    private final DefaultTableModel tableModel;
    private final JTable tblThuoc;
    private List<Thuoc> dsThuoc = new ArrayList<>();
    private boolean coTheQuanLy;
    private boolean dangTai;

    public ThuocPanel(DesktopSession phien) {
        setLayout(new BorderLayout(12, 12));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 26, 20, 26));

        JLabel title = new JLabel("Danh mục thuốc");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        title.setForeground(NAVY);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createLineBorder(BORDER));
        toolbar.add(new JLabel("Tìm theo mã, tên hoặc hoạt chất:"));
        toolbar.add(txtTimKiem);
        toolbar.add(btnTim);
        toolbar.add(btnTaiLai);
        toolbar.add(btnThem);
        toolbar.add(btnSua);

        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.setOpaque(false);
        north.add(title, BorderLayout.NORTH);
        north.add(toolbar, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);

        String[] columns = {"Mã thuốc", "Tên thuốc", "Hàm lượng", "Đơn vị", "Tồn kho", "Định mức", "Mức tồn", "Hạn gần nhất", "Đã đặt"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblThuoc = new JTable(tableModel);
        tblThuoc.setRowHeight(29);
        tblThuoc.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblThuoc.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) capNhatTrangThaiNut();
        });
        tblThuoc.getTableHeader().setReorderingAllowed(false);
        tblThuoc.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(tblThuoc);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        lblTrangThai.setForeground(MUTED);
        add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 5));
        south.setOpaque(false);
        lblTrangThai.setForeground(MUTED);
        south.add(lblTrangThai, BorderLayout.NORTH);
        JLabel note = new JLabel("Tồn/hạn dùng được tổng hợp từ lô thuốc. Xóa vật lý chưa được hỗ trợ theo quyền/schema hiện tại.", SwingConstants.LEFT);
        note.setForeground(MUTED);
        note.setFont(note.getFont().deriveFont(11f));
        south.add(note, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        ThuocService service = new ThuocService(phien);
        new ThuocController(this, service);
    }

    public void hienDanhSach(List<Thuoc> danhSach) {
        dsThuoc = new ArrayList<>(danhSach);
        tableModel.setRowCount(0);
        for (Thuoc thuoc : dsThuoc) {
            String mucTon = thuoc.getTongTon() <= thuoc.getDinhMucToiThieu() ? "Cần nhập" : "Đủ";
            String han = thuoc.getHanSuDungGanNhat() == null ? "—" : thuoc.getHanSuDungGanNhat().format(DATE);
            tableModel.addRow(new Object[]{
                    thuoc.getMaThuoc(), thuoc.getTenThuoc(),
                    thuoc.getHamLuong() == null ? "—" : thuoc.getHamLuong(),
                    thuoc.getDonViTinh(), thuoc.getTongTon(), thuoc.getDinhMucToiThieu(),
                    mucTon, han, thuoc.isDaDatHang() ? "Có" : "Không"
            });
        }
        lblTrangThai.setForeground(MUTED);
        lblTrangThai.setText("Hiển thị " + dsThuoc.size() + " thuốc (tối đa 500). Tồn kho được tính từ LoHang.");
        capNhatTrangThaiNut();
    }

    public Thuoc layThuocDangChon() {
        int row = tblThuoc.getSelectedRow();
        if (row < 0) return null;
        int modelRow = tblThuoc.convertRowIndexToModel(row);
        return modelRow >= 0 && modelRow < dsThuoc.size() ? dsThuoc.get(modelRow) : null;
    }

    public void datQuyen(boolean coTheQuanLy) {
        this.coTheQuanLy = coTheQuanLy;
        capNhatTrangThaiNut();
    }

    public void datDangTai(boolean dangTai) {
        this.dangTai = dangTai;
        btnTim.setEnabled(!dangTai);
        btnTaiLai.setEnabled(!dangTai);
        capNhatTrangThaiNut();
        if (dangTai) setTrangThai("Đang tải dữ liệu thuốc...", MUTED);
    }

    private void capNhatTrangThaiNut() {
        btnThem.setEnabled(coTheQuanLy && !dangTai);
        btnSua.setEnabled(coTheQuanLy && !dangTai && layThuocDangChon() != null);
    }

    public void setTrangThai(String thongBao, Color mau) {
        lblTrangThai.setText(thongBao);
        lblTrangThai.setForeground(mau);
    }

    public void hienLoi(String thongBao) {
        setTrangThai(thongBao, new Color(176, 32, 32));
        JOptionPane.showMessageDialog(this, thongBao, "Thông báo", JOptionPane.ERROR_MESSAGE);
    }

    public String getTuKhoa() { return txtTimKiem.getText(); }
    public JButton getBtnTim() { return btnTim; }
    public JButton getBtnTaiLai() { return btnTaiLai; }
    public JButton getBtnThem() { return btnThem; }
    public JButton getBtnSua() { return btnSua; }
    public JTextField getTxtTimKiem() { return txtTimKiem; }
    public JTable getTblThuoc() { return tblThuoc; }
}
