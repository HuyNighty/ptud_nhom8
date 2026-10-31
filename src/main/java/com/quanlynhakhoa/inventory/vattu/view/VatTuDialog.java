package com.quanlynhakhoa.inventory.vattu.view;

import com.quanlynhakhoa.inventory.vattu.model.VatTu;
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
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

/** Dialog chỉ tạo form và chuyển dữ liệu nhập thành model. */
public final class VatTuDialog extends JDialog {
    private final JTextField txtMa = new JTextField(24);
    private final JTextField txtTen = new JTextField(24);
    private final JTextField txtDonVi = new JTextField(24);
    private final JTextArea txtMoTa = new JTextArea(4, 24);
    private final JSpinner spDinhMuc = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    private final JCheckBox chkDaDatHang = new JCheckBox("Đã đặt hàng");
    private final JButton btnLuu = new JButton("Lưu");
    private final JButton btnHuy = new JButton("Hủy");
    private final boolean dangSua;

    public VatTuDialog(Window chuSoHuu, VatTu vatTuCanSua) {
        super(chuSoHuu, ModalityType.APPLICATION_MODAL);
        dangSua = vatTuCanSua != null;
        setTitle(dangSua ? "Cập nhật vật tư" : "Thêm vật tư");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(18, 20, 16, 20));
        setContentPane(root);
        JLabel title = new JLabel(dangSua ? "Cập nhật thông tin vật tư" : "Thêm vật tư mới");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 19f));
        root.add(title, BorderLayout.NORTH);

        txtMoTa.setLineWrap(true);
        txtMoTa.setWrapStyleWord(true);
        JPanel form = new JPanel(new GridBagLayout());
        themDong(form, 0, "Mã vật tư (*):", txtMa);
        themDong(form, 1, "Tên vật tư (*):", txtTen);
        themDong(form, 2, "Đơn vị tính (*):", txtDonVi);
        themDong(form, 3, "Mô tả:", new JScrollPane(txtMoTa));
        themDong(form, 4, "Định mức tối thiểu:", spDinhMuc);
        themDong(form, 5, "", chkDaDatHang);
        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(btnHuy);
        actions.add(btnLuu);
        root.add(actions, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnLuu);

        if (dangSua) {
            doDuLieuLenForm(vatTuCanSua);
            txtMa.setEditable(false);
        } else {
            JLabel hint = new JLabel("Mã vật tư tối đa 10 ký tự; mô tả tối đa 300 ký tự.");
            JPanel heading = new JPanel(new BorderLayout(0, 4));
            heading.add(title, BorderLayout.NORTH);
            heading.add(hint, BorderLayout.SOUTH);
            root.remove(title);
            root.add(heading, BorderLayout.NORTH);
        }
        setMinimumSize(new Dimension(540, 360));
        setSize(580, 410);
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

    private void doDuLieuLenForm(VatTu vatTu) {
        txtMa.setText(vatTu.getMaVatTu());
        txtTen.setText(vatTu.getTenVatTu());
        txtDonVi.setText(vatTu.getDonViTinh());
        txtMoTa.setText(vatTu.getMoTa() == null ? "" : vatTu.getMoTa());
        spDinhMuc.setValue(vatTu.getDinhMucToiThieu());
        chkDaDatHang.setSelected(vatTu.isDaDatHang());
    }

    public VatTu layVatTuTuForm() {
        return new VatTu(txtMa.getText(), txtTen.getText(), txtDonVi.getText(), txtMoTa.getText(),
                ((Number) spDinhMuc.getValue()).intValue(), chkDaDatHang.isSelected());
    }

    public void datDangLuu(boolean dangLuu) {
        btnLuu.setEnabled(!dangLuu);
        btnHuy.setEnabled(!dangLuu);
    }

    public void hienLoi(String thongBao) {
        JOptionPane.showMessageDialog(this, thongBao, "Không thể lưu vật tư", JOptionPane.WARNING_MESSAGE);
    }

    public JButton getBtnLuu() { return btnLuu; }
    public JButton getBtnHuy() { return btnHuy; }
}
