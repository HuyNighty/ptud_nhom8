package com.quanlynhakhoa.lichhen.view;

import com.quanlynhakhoa.benhnhan.hopdong.BenhNhanTomTat;
import com.quanlynhakhoa.benhnhan.view.BieuTuong;
import com.quanlynhakhoa.benhnhan.view.MauNut;
import com.quanlynhakhoa.lichhen.model.BacSi;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class TiepNhanDialog extends JDialog {
    private static final Color NAVY = new Color(29, 61, 102);

    private JPanel pnlNorth;
    private JLabel lblTieuDe;

    private JPanel pnlCenter;
    private JTextField txtTimBenhNhan;
    private JButton btnTimBenhNhan;
    private JComboBox<BenhNhanTomTat> cmbBenhNhan;
    private JComboBox<BacSi> cmbBacSi;
    private JTextArea txaLyDoDen;
    private JScrollPane scrLyDoDen;

    private JPanel pnlSouth;
    private JButton btnTiepNhan;
    private JButton btnHuy;

    public TiepNhanDialog(Window cuaSoCha) {
        super(cuaSoCha, ModalityType.APPLICATION_MODAL);
        setTitle("Tiếp nhận bệnh nhân không hẹn trước");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel pnlNoiDung = new JPanel(new BorderLayout(0, 12));
        pnlNoiDung.setBorder(new EmptyBorder(18, 20, 16, 20));
        setContentPane(pnlNoiDung);

        taoNorth();
        taoCenter();
        taoSouth();
        pnlNoiDung.add(pnlNorth, BorderLayout.NORTH);
        pnlNoiDung.add(pnlCenter, BorderLayout.CENTER);
        pnlNoiDung.add(pnlSouth, BorderLayout.SOUTH);

        setSize(600, 330);
        setLocationRelativeTo(cuaSoCha);
    }

    private void taoNorth() {
        pnlNorth = new JPanel(new BorderLayout());
        lblTieuDe = new JLabel("Tiếp nhận không hẹn");
        lblTieuDe.setFont(new Font("SansSerif", Font.BOLD, 19));
        lblTieuDe.setForeground(NAVY);
        pnlNorth.add(lblTieuDe, BorderLayout.WEST);
    }

    private void taoCenter() {
        pnlCenter = new JPanel(new BorderLayout());

        txtTimBenhNhan = new JTextField();
        btnTimBenhNhan = new JButton("Tìm");
        btnTimBenhNhan.setIcon(new BieuTuong(BieuTuong.TIM_KIEM));
        Box boxTimBenhNhan = Box.createHorizontalBox();
        boxTimBenhNhan.add(txtTimBenhNhan);
        boxTimBenhNhan.add(Box.createHorizontalStrut(6));
        boxTimBenhNhan.add(btnTimBenhNhan);

        cmbBenhNhan = new JComboBox<>();
        cmbBacSi = new JComboBox<>();

        txaLyDoDen = new JTextArea(3, 20);
        txaLyDoDen.setLineWrap(true);
        txaLyDoDen.setWrapStyleWord(true);
        scrLyDoDen = new JScrollPane(txaLyDoDen);

        JLabel lblNhanTimBenhNhan = new JLabel("Tìm bệnh nhân:");
        JLabel lblNhanBenhNhan = new JLabel("Bệnh nhân (*):");
        JLabel lblNhanBacSi = new JLabel("Bác sĩ (*):");
        JLabel lblNhanLyDoDen = new JLabel("Lý do đến (*):");

        Dimension doRongNhan = lblNhanTimBenhNhan.getPreferredSize();
        lblNhanBenhNhan.setPreferredSize(doRongNhan);
        lblNhanBacSi.setPreferredSize(doRongNhan);
        lblNhanLyDoDen.setPreferredSize(doRongNhan);
        lblNhanLyDoDen.setAlignmentY(TOP_ALIGNMENT);
        scrLyDoDen.setAlignmentY(TOP_ALIGNMENT);

        Box boxForm = Box.createVerticalBox();
        themDong(boxForm, lblNhanTimBenhNhan, boxTimBenhNhan);
        themDong(boxForm, lblNhanBenhNhan, cmbBenhNhan);
        themDong(boxForm, lblNhanBacSi, cmbBacSi);
        themDong(boxForm, lblNhanLyDoDen, scrLyDoDen);

        pnlCenter.add(boxForm, BorderLayout.NORTH);
    }

    private void themDong(Box boxForm, JLabel lblNhan, Component oNhap) {
        Box boxDong = Box.createHorizontalBox();
        boxDong.add(lblNhan);
        boxDong.add(Box.createHorizontalStrut(10));
        boxDong.add(oNhap);
        boxForm.add(boxDong);
        boxForm.add(Box.createVerticalStrut(10));
    }

    private void taoSouth() {
        pnlSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnTiepNhan = new JButton("Tiếp nhận");
        MauNut.toXanhLa(btnTiepNhan);
        btnHuy = new JButton("Hủy");
        MauNut.toDo(btnHuy);
        pnlSouth.add(btnHuy);
        pnlSouth.add(btnTiepNhan);
    }

    public void hienDanhSachBenhNhan(ArrayList<BenhNhanTomTat> danhSachBenhNhan) {
        cmbBenhNhan.removeAllItems();
        for (BenhNhanTomTat benhNhan : danhSachBenhNhan) {
            cmbBenhNhan.addItem(benhNhan);
        }
        if (danhSachBenhNhan.isEmpty()) {
            hienLoi("Không tìm thấy bệnh nhân. Bệnh nhân mới thì thêm ở màn hình Bệnh nhân trước.");
        }
    }

    public void hienDanhSachBacSi(ArrayList<BacSi> danhSachBacSi) {
        cmbBacSi.removeAllItems();
        for (BacSi bacSi : danhSachBacSi) {
            cmbBacSi.addItem(bacSi);
        }
        if (danhSachBacSi.isEmpty()) {
            hienLoi("Hiện không có bác sĩ nào đang trong ca làm việc.");
        }
    }

    public BenhNhanTomTat layBenhNhanDaChon() {
        return (BenhNhanTomTat) cmbBenhNhan.getSelectedItem();
    }

    public BacSi layBacSiDaChon() {
        return (BacSi) cmbBacSi.getSelectedItem();
    }

    public String layLyDoDen() {
        return txaLyDoDen.getText();
    }

    public void datDangXuLy(boolean dangXuLy) {
        boolean choPhepThaoTac = !dangXuLy;
        btnTiepNhan.setEnabled(choPhepThaoTac);
        btnHuy.setEnabled(choPhepThaoTac);
        btnTimBenhNhan.setEnabled(choPhepThaoTac);
        cmbBacSi.setEnabled(choPhepThaoTac);

        if (dangXuLy) {
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    public void hienLoi(String thongBao) {
        JOptionPane.showMessageDialog(this, thongBao, "Thông báo", JOptionPane.WARNING_MESSAGE);
    }

    public JTextField getTxtTimBenhNhan() {
        return txtTimBenhNhan;
    }

    public JButton getBtnTimBenhNhan() {
        return btnTimBenhNhan;
    }

    public JButton getBtnTiepNhan() {
        return btnTiepNhan;
    }

    public JButton getBtnHuy() {
        return btnHuy;
    }
}
