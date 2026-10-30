package com.quanlynhakhoa.benhnhan.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.YearMonth;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ChonNgayDialog extends JDialog implements ActionListener, ChangeListener {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color XANH_CHON = new Color(43, 110, 190);

    private YearMonth thangDangXem;
    private LocalDate ngayDaChon;
    private LocalDate ngayBanDau;
    private LocalDate ngayNhoNhat;
    private LocalDate ngayLonNhat;

    private JPanel pnlNorth;
    private JButton btnThangTruoc;
    private JLabel lblThang;
    private JButton btnThangSau;
    private JSpinner spnNam;

    private JPanel pnlCenter;

    private JPanel pnlSouth;
    private JButton btnHuy;

    public static LocalDate chon(Window cuaSoCha, LocalDate ngayDangCo) {
        LocalDate homNay = LocalDate.now();
        LocalDate ngayMoLich = ngayDangCo;
        if (ngayMoLich == null) {
            ngayMoLich = homNay.minusYears(30);
        }
        ChonNgayDialog winChonNgay = new ChonNgayDialog(cuaSoCha, "Chọn ngày sinh", ngayDangCo, ngayMoLich,
                homNay.minusYears(120), homNay);
        winChonNgay.setVisible(true);
        return winChonNgay.ngayDaChon;
    }

    public static LocalDate chon(Window cuaSoCha, String tieuDe, LocalDate ngayDangCo,
                                 LocalDate ngayNhoNhat, LocalDate ngayLonNhat) {
        LocalDate ngayMoLich = ngayDangCo;
        if (ngayMoLich == null) {
            ngayMoLich = LocalDate.now();
        }
        if (ngayMoLich.isBefore(ngayNhoNhat)) {
            ngayMoLich = ngayNhoNhat;
        }
        if (ngayMoLich.isAfter(ngayLonNhat)) {
            ngayMoLich = ngayLonNhat;
        }
        ChonNgayDialog winChonNgay = new ChonNgayDialog(cuaSoCha, tieuDe, ngayDangCo, ngayMoLich,
                ngayNhoNhat, ngayLonNhat);
        winChonNgay.setVisible(true);
        return winChonNgay.ngayDaChon;
    }

    private ChonNgayDialog(Window cuaSoCha, String tieuDe, LocalDate ngayDangCo, LocalDate ngayMoLich,
                           LocalDate ngayNhoNhat, LocalDate ngayLonNhat) {
        super(cuaSoCha, tieuDe, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.ngayBanDau = ngayDangCo;
        this.ngayNhoNhat = ngayNhoNhat;
        this.ngayLonNhat = ngayLonNhat;
        this.thangDangXem = YearMonth.from(ngayMoLich);

        JPanel pnlNoiDung = new JPanel(new BorderLayout(0, 8));
        pnlNoiDung.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(pnlNoiDung);

        taoNorth();
        pnlCenter = new JPanel(new GridLayout(0, 7, 3, 3));
        taoSouth();
        pnlNoiDung.add(pnlNorth, BorderLayout.NORTH);
        pnlNoiDung.add(pnlCenter, BorderLayout.CENTER);
        pnlNoiDung.add(pnlSouth, BorderLayout.SOUTH);

        veLaiLich();
        setSize(360, 330);
        setLocationRelativeTo(cuaSoCha);
    }

    private void taoNorth() {
        pnlNorth = new JPanel(new BorderLayout(6, 0));
        btnThangTruoc = new JButton("‹");
        btnThangSau = new JButton("›");
        btnThangTruoc.setPreferredSize(new Dimension(46, 28));
        btnThangSau.setPreferredSize(new Dimension(46, 28));
        lblThang = new JLabel("", SwingConstants.CENTER);
        lblThang.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblThang.setForeground(NAVY);

        SpinnerNumberModel modelNam = new SpinnerNumberModel(thangDangXem.getYear(),
                ngayNhoNhat.getYear(), ngayLonNhat.getYear(), 1);
        spnNam = new JSpinner(modelNam);
        spnNam.setEditor(new JSpinner.NumberEditor(spnNam, "#"));

        JPanel pnlNorthCenter = new JPanel(new BorderLayout(4, 0));
        pnlNorthCenter.add(btnThangTruoc, BorderLayout.WEST);
        pnlNorthCenter.add(lblThang, BorderLayout.CENTER);
        pnlNorthCenter.add(btnThangSau, BorderLayout.EAST);

        pnlNorth.add(pnlNorthCenter, BorderLayout.CENTER);
        pnlNorth.add(spnNam, BorderLayout.EAST);

        btnThangTruoc.addActionListener(this);
        btnThangSau.addActionListener(this);
        spnNam.addChangeListener(this);
    }

    private void taoSouth() {
        pnlSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnHuy = new JButton("Hủy");
        MauNut.toDo(btnHuy);
        btnHuy.addActionListener(this);
        pnlSouth.add(btnHuy);
    }

    private void veLaiLich() {
        lblThang.setText("Tháng " + String.format("%02d", thangDangXem.getMonthValue()));
        pnlCenter.removeAll();

        String[] cacThuTrongTuan = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        for (String tenThu : cacThuTrongTuan) {
            JLabel lblThu = new JLabel(tenThu, SwingConstants.CENTER);
            lblThu.setForeground(Color.GRAY);
            pnlCenter.add(lblThu);
        }

        int soOTrong = thangDangXem.atDay(1).getDayOfWeek().getValue() - 1;
        for (int soThuTuO = 0; soThuTuO < soOTrong; soThuTuO++) {
            pnlCenter.add(new JLabel(""));
        }

        for (int ngay = 1; ngay <= thangDangXem.lengthOfMonth(); ngay++) {
            LocalDate ngayTrongThang = thangDangXem.atDay(ngay);
            JButton btnNgay = new JButton(String.valueOf(ngay));
            btnNgay.setMargin(new Insets(2, 2, 2, 2));
            btnNgay.setActionCommand(String.valueOf(ngay));

            boolean duocChon = !ngayTrongThang.isBefore(ngayNhoNhat) && !ngayTrongThang.isAfter(ngayLonNhat);
            btnNgay.setEnabled(duocChon);

            if (ngayTrongThang.equals(ngayBanDau)) {
                btnNgay.setBackground(XANH_CHON);
                btnNgay.setForeground(Color.WHITE);
                btnNgay.setOpaque(true);
            }
            btnNgay.addActionListener(this);
            pnlCenter.add(btnNgay);
        }

        btnThangTruoc.setEnabled(thangDangXem.isAfter(YearMonth.from(ngayNhoNhat)));
        btnThangSau.setEnabled(thangDangXem.isBefore(YearMonth.from(ngayLonNhat)));

        pnlCenter.revalidate();
        pnlCenter.repaint();
    }

    @Override
    public void actionPerformed(ActionEvent suKien) {
        Object nguonSuKien = suKien.getSource();
        if (nguonSuKien.equals(btnThangTruoc)) {
            doiThang(thangDangXem.minusMonths(1));
        } else if (nguonSuKien.equals(btnThangSau)) {
            doiThang(thangDangXem.plusMonths(1));
        } else if (nguonSuKien.equals(btnHuy)) {
            dispose();
        } else {
            int ngay = Integer.parseInt(suKien.getActionCommand());
            ngayDaChon = thangDangXem.atDay(ngay);
            dispose();
        }
    }

    @Override
    public void stateChanged(ChangeEvent suKien) {
        int nam = (Integer) spnNam.getValue();
        if (nam != thangDangXem.getYear()) {
            YearMonth thangMoi = YearMonth.of(nam, thangDangXem.getMonthValue());
            if (thangMoi.isAfter(YearMonth.from(ngayLonNhat))) {
                thangMoi = YearMonth.from(ngayLonNhat);
            }
            if (thangMoi.isBefore(YearMonth.from(ngayNhoNhat))) {
                thangMoi = YearMonth.from(ngayNhoNhat);
            }
            thangDangXem = thangMoi;
            veLaiLich();
        }
    }

    private void doiThang(YearMonth thangMoi) {
        if (thangMoi.isBefore(YearMonth.from(ngayNhoNhat)) || thangMoi.isAfter(YearMonth.from(ngayLonNhat))) {
            return;
        }
        thangDangXem = thangMoi;
        spnNam.setValue(thangMoi.getYear());
        veLaiLich();
    }
}
