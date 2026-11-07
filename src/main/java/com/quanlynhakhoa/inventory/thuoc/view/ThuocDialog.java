package com.quanlynhakhoa.inventory.thuoc.view;

import com.quanlynhakhoa.inventory.thuoc.model.Thuoc;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

/** Dialog chỉ thu thập dữ liệu; validate và lưu do Service/Controller thực hiện. */
public final class ThuocDialog extends JDialog {
    private final JTextField txtMa = new JTextField(24);
    private final JTextField txtTen = new JTextField(24);
    private final JTextField txtDonVi = new JTextField(24);
    private final JTextField txtHamLuong = new JTextField(24);
    private final JTextField txtHoatChat = new JTextField(24);
    private final JSpinner spDinhMuc = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    private final JCheckBox chkKiemSoat = new JCheckBox("Thuốc kiểm soát đặc biệt");
    private final JCheckBox chkDaDatHang = new JCheckBox("Đã đặt hàng");
    private final JButton btnLuu = new JButton("Lưu");
    private final JButton btnHuy = new JButton("Hủy");
    private final boolean dangSua;

    public ThuocDialog(Window chuSoHuu, Thuoc thuocCanSua) {
        super(chuSoHuu, ModalityType.APPLICATION_MODAL);
        dangSua = thuocCanSua != null;
        setTitle(dangSua ? "Cập nhật thuốc" : "Thêm thuốc");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(18, 20, 16, 20));
        setContentPane(root);

        JLabel title = new JLabel(dangSua ? "Cập nhật thông tin thuốc" : "Thêm thuốc mới");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 19f));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        int row = 0;
        themDong(form, row++, "Mã thuốc (*):", txtMa);
        themDong(form, row++, "Tên thuốc (*):", txtTen);
        themDong(form, row++, "Đơn vị tính (*):", txtDonVi);
        themDong(form, row++, "Hàm lượng:", txtHamLuong);
        themDong(form, row++, "Hoạt chất:", txtHoatChat);
        themDong(form, row++, "Định mức tối thiểu:", spDinhMuc);
        themDong(form, row++, "", chkKiemSoat);
        themDong(form, row, "", chkDaDatHang);
        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(btnHuy);
        actions.add(btnLuu);
        root.add(actions, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnLuu);

        if (dangSua) {
            doDuLieuLenForm(thuocCanSua);
            txtMa.setEditable(false);
        } else {
            JLabel hint = new JLabel("Mã thuốc do người dùng nhập (tối đa 10 ký tự).");
            hint.setBorder(new EmptyBorder(4, 0, 0, 0));
            // Ghi chú nằm cùng vùng tiêu đề để form không lẫn logic nghiệp vụ.
            JPanel heading = new JPanel(new BorderLayout(0, 4));
            heading.add(title, BorderLayout.NORTH);
            heading.add(hint, BorderLayout.SOUTH);
            root.remove(title);
            root.add(heading, BorderLayout.NORTH);
        }

        setMinimumSize(new Dimension(560, 390));
        setSize(600, 430);
        setLocationRelativeTo(chuSoHuu);
    }

    private void themDong(JPanel form, int row, String nhan, Component control) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.LINE_START;
        left.insets = new Insets(5, 0, 5, 12);
        form.add(new JLabel(nhan), left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(5, 0, 5, 0);
        form.add(control, right);
    }

    private void doDuLieuLenForm(Thuoc thuoc) {
        txtMa.setText(thuoc.getMaThuoc());
        txtTen.setText(thuoc.getTenThuoc());
        txtDonVi.setText(thuoc.getDonViTinh());
        txtHamLuong.setText(thuoc.getHamLuong() == null ? "" : thuoc.getHamLuong());
        txtHoatChat.setText(thuoc.getHoatChat() == null ? "" : thuoc.getHoatChat());
        spDinhMuc.setValue(thuoc.getDinhMucToiThieu());
        chkKiemSoat.setSelected(thuoc.isKiemSoatDacBiet());
        chkDaDatHang.setSelected(thuoc.isDaDatHang());
    }

    public Thuoc layThuocTuForm() {
        return new Thuoc(
                txtMa.getText(),
                txtTen.getText(),
                txtDonVi.getText(),
                chkKiemSoat.isSelected(),
                txtHamLuong.getText(),
                txtHoatChat.getText(),
                ((Number) spDinhMuc.getValue()).intValue(),
                chkDaDatHang.isSelected());
    }

    public void datDangLuu(boolean dangLuu) {
        btnLuu.setEnabled(!dangLuu);
        btnHuy.setEnabled(!dangLuu);
    }

    public void hienLoi(String thongBao) {
        JOptionPane.showMessageDialog(this, thongBao, "Không thể lưu thuốc", JOptionPane.WARNING_MESSAGE);
    }

    public JButton getBtnLuu() { return btnLuu; }
    public JButton getBtnHuy() { return btnHuy; }
}
