package com.quanlynhakhoa.benhnhan.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
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

// Hộp thoại LỊCH để chọn ngày (không cần thư viện ngoài).
// Cách dùng: LocalDate ngay = ChonNgayDialog.chon(cuaSoCha, ngayDangCo);  → null nếu bấm Hủy
//
//  ┌──────────── pnlNorth ────────────┐
//  │  ‹   Tháng 05      ›    [ 1998 ] │
//  ├──────────── pnlCenter ───────────┤
//  │ T2 T3 T4 T5 T6 T7 CN             │
//  │  1  2  3  4  5  6  7 ...         │
//  ├──────────── pnlSouth ────────────┤
//  │                            Hủy   │
//  └──────────────────────────────────┘
public class ChonNgayDialog extends JDialog implements ActionListener, ChangeListener {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color XANH_CHON = new Color(43, 110, 190);

    private YearMonth thangDangXem;      // tháng đang hiện trên lịch
    private LocalDate ngayDaChon;        // ngày người dùng bấm (kết quả)
    private LocalDate ngayBanDau;        // ngày đang có trên form (để tô màu)

    private JPanel pnlNorth;
    private JButton btnThangTruoc;
    private JLabel lblThang;
    private JButton btnThangSau;
    private JSpinner spnNam;

    private JPanel pnlCenter;            // lưới 7 cột chứa các nút ngày

    private JPanel pnlSouth;
    private JButton btnHuy;

    // Mở lịch và chờ người dùng chọn. Trả về ngày đã chọn, hoặc null nếu bấm Hủy / đóng.
    public static LocalDate chon(Window cuaSoCha, LocalDate ngayDangCo) {
        ChonNgayDialog winChonNgay = new ChonNgayDialog(cuaSoCha, ngayDangCo);
        winChonNgay.setVisible(true);   // modal: chờ tới khi đóng
        return winChonNgay.ngayDaChon;
    }

    private ChonNgayDialog(Window cuaSoCha, LocalDate ngayDangCo) {
        super(cuaSoCha, "Chọn ngày sinh", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ngayBanDau = ngayDangCo;
        // Chưa có ngày → mở sẵn ở tháng hiện tại của 30 năm trước (người dùng đổi năm bằng spnNam)
        LocalDate ngayMoLich;
        if (ngayDangCo != null) {
            ngayMoLich = ngayDangCo;
        } else {
            ngayMoLich = LocalDate.now().minusYears(30);
        }
        thangDangXem = YearMonth.from(ngayMoLich);

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
        btnThangTruoc.setPreferredSize(new java.awt.Dimension(46, 28));   // 2 nút rộng bằng nhau
        btnThangSau.setPreferredSize(new java.awt.Dimension(46, 28));
        lblThang = new JLabel("", SwingConstants.CENTER);
        lblThang.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblThang.setForeground(NAVY);

        int namNay = LocalDate.now().getYear();
        spnNam = new JSpinner(new SpinnerNumberModel(thangDangXem.getYear(), namNay - 120, namNay, 1));
        spnNam.setEditor(new JSpinner.NumberEditor(spnNam, "#"));   // hiện 1998, không hiện 1,998

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
        btnHuy.addActionListener(this);
        pnlSouth.add(btnHuy);
    }

    // Vẽ lại lưới ngày của tháng đang xem
    private void veLaiLich() {
        lblThang.setText("Tháng " + String.format("%02d", thangDangXem.getMonthValue()));
        pnlCenter.removeAll();

        // Dòng tiêu đề thứ
        String[] cacThuTrongTuan = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        for (String tenThu : cacThuTrongTuan) {
            JLabel lblThu = new JLabel(tenThu, SwingConstants.CENTER);
            lblThu.setForeground(Color.GRAY);
            pnlCenter.add(lblThu);
        }

        // Ô trống trước ngày 1 (ví dụ ngày 1 là thứ Tư → 2 ô trống cho T2, T3)
        int soOTrong = thangDangXem.atDay(1).getDayOfWeek().getValue() - 1;
        for (int soThuTuO = 0; soThuTuO < soOTrong; soThuTuO++) {
            pnlCenter.add(new JLabel(""));
        }

        // Các nút ngày 1 → cuối tháng
        LocalDate homNay = LocalDate.now();
        for (int ngay = 1; ngay <= thangDangXem.lengthOfMonth(); ngay++) {
            LocalDate ngayTrongThang = thangDangXem.atDay(ngay);
            JButton btnNgay = new JButton(String.valueOf(ngay));
            btnNgay.setMargin(new java.awt.Insets(2, 2, 2, 2));
            btnNgay.setActionCommand(String.valueOf(ngay));   // để biết bấm ngày mấy
            btnNgay.setEnabled(!ngayTrongThang.isAfter(homNay));           // không cho chọn ngày tương lai
            if (ngayTrongThang.equals(ngayBanDau)) {                       // tô màu ngày đang có trên form
                btnNgay.setBackground(XANH_CHON);
                btnNgay.setForeground(Color.WHITE);
                btnNgay.setOpaque(true);
            }
            btnNgay.addActionListener(this);
            pnlCenter.add(btnNgay);
        }

        // Không cho đi tới tháng sau tháng hiện tại
        btnThangSau.setEnabled(thangDangXem.isBefore(YearMonth.from(homNay)));

        pnlCenter.revalidate();
        pnlCenter.repaint();
    }

    // Bấm ‹ › Hủy hoặc 1 ngày
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
            // Còn lại là nút ngày
            int ngay = Integer.parseInt(suKien.getActionCommand());
            ngayDaChon = thangDangXem.atDay(ngay);
            dispose();
        }
    }

    // Đổi năm trên spnNam
    @Override
    public void stateChanged(ChangeEvent suKien) {
        int nam = (Integer) spnNam.getValue();
        if (nam != thangDangXem.getYear()) {
            YearMonth thangMoi = YearMonth.of(nam, thangDangXem.getMonthValue());
            if (thangMoi.isAfter(YearMonth.now())) {
                thangMoi = YearMonth.now();     // năm nay nhưng tháng chưa tới → về tháng hiện tại
            }
            thangDangXem = thangMoi;
            veLaiLich();
        }
    }

    private void doiThang(YearMonth thangMoi) {
        int namNay = LocalDate.now().getYear();
        if (thangMoi.getYear() < namNay - 120 || thangMoi.isAfter(YearMonth.now())) {
            return;   // ngoài phạm vi cho phép
        }
        thangDangXem = thangMoi;
        spnNam.setValue(thangMoi.getYear());   // đồng bộ ô năm (stateChanged tự bỏ qua vì cùng năm)
        veLaiLich();
    }
}
