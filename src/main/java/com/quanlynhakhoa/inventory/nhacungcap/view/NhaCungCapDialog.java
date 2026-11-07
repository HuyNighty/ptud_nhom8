package com.quanlynhakhoa.inventory.nhacungcap.view;

import com.quanlynhakhoa.inventory.nhacungcap.model.NhaCungCap;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/** Dialog chỉ thu thập dữ liệu; validate/lưu thuộc Service và Controller. */
public final class NhaCungCapDialog extends JDialog {
    private final JTextField txtMa = new JTextField(24);
    private final JTextField txtTen = new JTextField(24);
    private final JTextField txtDienThoai = new JTextField(24);
    private final JTextField txtEmail = new JTextField(24);
    private final JTextArea txtDiaChi = new JTextArea(3, 24);
    private final JButton btnLuu = new JButton("Lưu");
    private final JButton btnHuy = new JButton("Hủy");

    public NhaCungCapDialog(Window chuSoHuu, NhaCungCap nccCanSua) {
        super(chuSoHuu, ModalityType.APPLICATION_MODAL);
        boolean dangSua = nccCanSua != null;
        setTitle(dangSua ? "Cập nhật nhà cung cấp" : "Thêm nhà cung cấp");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        txtDiaChi.setLineWrap(true);
        txtDiaChi.setWrapStyleWord(true);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(18, 20, 16, 20));
        setContentPane(root);
        JLabel title = new JLabel(dangSua ? "Cập nhật thông tin nhà cung cấp" : "Thêm nhà cung cấp mới");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 19f));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        themDong(form, 0, "Mã nhà cung cấp (*):", txtMa);
        themDong(form, 1, "Tên nhà cung cấp (*):", txtTen);
        themDong(form, 2, "Số điện thoại (*):", txtDienThoai);
        themDong(form, 3, "Email:", txtEmail);
        themDong(form, 4, "Địa chỉ:", new JScrollPane(txtDiaChi));
        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(btnHuy);
        actions.add(btnLuu);
        root.add(actions, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnLuu);

        if (dangSua) {
            txtMa.setText(nccCanSua.getMaNhaCungCap());
            txtMa.setEditable(false);
            txtTen.setText(nccCanSua.getTenNhaCungCap());
            txtDienThoai.setText(nccCanSua.getSoDienThoai());
            txtEmail.setText(nccCanSua.getEmail() == null ? "" : nccCanSua.getEmail());
            txtDiaChi.setText(nccCanSua.getDiaChi() == null ? "" : nccCanSua.getDiaChi());
        } else {
            JLabel hint = new JLabel("Mã tối đa 10 ký tự · Điện thoại tối đa 20 · Email tối đa 100");
            JPanel heading = new JPanel(new BorderLayout(0, 4));
            heading.add(title, BorderLayout.NORTH);
            heading.add(hint, BorderLayout.SOUTH);
            root.remove(title);
            root.add(heading, BorderLayout.NORTH);
        }
        setMinimumSize(new Dimension(560, 360));
        setSize(600, 420);
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

    public NhaCungCap layNhaCungCapTuForm() {
        return new NhaCungCap(txtMa.getText(), txtTen.getText(), txtDiaChi.getText(),
                txtDienThoai.getText(), txtEmail.getText(), NhaCungCap.DANG_HOP_TAC);
    }

    public void datDangLuu(boolean dangLuu) {
        btnLuu.setEnabled(!dangLuu);
        btnHuy.setEnabled(!dangLuu);
    }

    public void hienLoi(String thongBao) {
        JOptionPane.showMessageDialog(this, thongBao, "Không thể lưu nhà cung cấp", JOptionPane.WARNING_MESSAGE);
    }

    public JButton getBtnLuu() { return btnLuu; }
    public JButton getBtnHuy() { return btnHuy; }
}
