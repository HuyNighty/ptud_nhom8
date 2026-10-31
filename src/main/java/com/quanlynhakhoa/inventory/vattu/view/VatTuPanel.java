package com.quanlynhakhoa.inventory.vattu.view;

import com.quanlynhakhoa.inventory.vattu.controller.VatTuController;
import com.quanlynhakhoa.inventory.vattu.model.VatTu;
import com.quanlynhakhoa.inventory.vattu.service.VatTuService;
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

/** View danh mục vật tư; không chứa SQL hoặc nghiệp vụ lưu dữ liệu. */
public final class VatTuPanel extends JPanel {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(97, 115, 137);
    private static final Color BORDER = new Color(220, 231, 242);
    private static final Color BACKGROUND = new Color(245, 248, 252);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JTextField txtTimKiem = new JTextField(24);
    private final JButton btnTim = new JButton("Tìm kiếm");
    private final JButton btnTaiLai = new JButton("Làm mới");
    private final JButton btnThem = new JButton("+ Thêm vật tư");
    private final JButton btnSua = new JButton("Sửa vật tư");
    private final JLabel lblTrangThai = new JLabel("Đang tải danh sách vật tư...");
    private final DefaultTableModel tableModel;
    private final JTable tblVatTu;
    private List<VatTu> dsVatTu = new ArrayList<>();
    private boolean coTheQuanLy;
    private boolean dangTai;

    public VatTuPanel(DesktopSession phien) {
        setLayout(new BorderLayout(12, 12));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(24, 26, 20, 26));

        JLabel title = new JLabel("Danh mục vật tư");
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        title.setForeground(NAVY);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createLineBorder(BORDER));
        toolbar.add(new JLabel("Tìm theo mã, tên hoặc mô tả:"));
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

        String[] columns = {"Mã vật tư", "Tên vật tư", "Đơn vị", "Mô tả", "Tồn kho", "Định mức", "Mức tồn", "Hạn gần nhất", "Đã đặt"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        tblVatTu = new JTable(tableModel);
        tblVatTu.setRowHeight(29);
        tblVatTu.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblVatTu.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) capNhatTrangThaiNut();
        });
        tblVatTu.getTableHeader().setReorderingAllowed(false);
        tblVatTu.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(tblVatTu);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        lblTrangThai.setForeground(MUTED);
        add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 5));
        south.setOpaque(false);
        south.add(lblTrangThai, BorderLayout.NORTH);
        JLabel note = new JLabel("Tồn và hạn dùng được tổng hợp từ LoHang. Không sửa tồn trực tiếp tại danh mục; xóa vật lý không được cấp quyền.", SwingConstants.LEFT);
        note.setForeground(MUTED);
        note.setFont(note.getFont().deriveFont(11f));
        south.add(note, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        VatTuService service = new VatTuService(phien);
        new VatTuController(this, service);
    }

    public void hienDanhSach(List<VatTu> danhSach) {
        dsVatTu = new ArrayList<>(danhSach);
        tableModel.setRowCount(0);
        for (VatTu vatTu : dsVatTu) {
            String mucTon = vatTu.getTongTon() <= vatTu.getDinhMucToiThieu() ? "Cần nhập" : "Đủ";
            String han = vatTu.getHanSuDungGanNhat() == null ? "—" : vatTu.getHanSuDungGanNhat().format(DATE);
            tableModel.addRow(new Object[]{
                    vatTu.getMaVatTu(), vatTu.getTenVatTu(), vatTu.getDonViTinh(),
                    vatTu.getMoTa() == null ? "—" : vatTu.getMoTa(),
                    vatTu.getTongTon(), vatTu.getDinhMucToiThieu(), mucTon, han,
                    vatTu.isDaDatHang() ? "Có" : "Không"
            });
        }
        setTrangThai("Hiển thị " + dsVatTu.size() + " vật tư (tối đa 500). Tồn kho được tính từ LoHang.", MUTED);
        capNhatTrangThaiNut();
    }

    public VatTu layVatTuDangChon() {
        int row = tblVatTu.getSelectedRow();
        if (row < 0) return null;
        int modelRow = tblVatTu.convertRowIndexToModel(row);
        return modelRow >= 0 && modelRow < dsVatTu.size() ? dsVatTu.get(modelRow) : null;
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
        if (dangTai) setTrangThai("Đang tải dữ liệu vật tư...", MUTED);
    }

    private void capNhatTrangThaiNut() {
        btnThem.setEnabled(coTheQuanLy && !dangTai);
        btnSua.setEnabled(coTheQuanLy && !dangTai && layVatTuDangChon() != null);
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
    public JTable getTblVatTu() { return tblVatTu; }
}
