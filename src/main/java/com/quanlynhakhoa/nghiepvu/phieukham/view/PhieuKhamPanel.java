package com.quanlynhakhoa.nghiepvu.phieukham.view;

import com.quanlynhakhoa.quantri.vaitro.model.Role;
import com.quanlynhakhoa.nghiepvu.phieukham.controller.PhieuKhamController;
import com.quanlynhakhoa.nghiepvu.phieukham.model.BacSi;
import com.quanlynhakhoa.nghiepvu.phieukham.model.PhieuKham;
import com.quanlynhakhoa.nghiepvu.phieukham.model.PhieuKhamListItem;
import com.quanlynhakhoa.nghiepvu.phieukham.service.PhieuKhamService;
import com.quanlynhakhoa.hethong.phanquyen.DesktopSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.awt.Point;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;

public final class PhieuKhamPanel extends JPanel {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color MUTED = new Color(97, 115, 137);
    private static final Color BORDER = new Color(225, 233, 241);
    private static final Color BACKGROUND = new Color(245, 248, 252);
    private static final Color SELECTED = new Color(232, 239, 248);
    private static final Color GREEN = new Color(21, 128, 61);
    private static final Color PURPLE = new Color(124, 58, 237);
    private static final Color RED = new Color(185, 28, 28);
    private static final DateTimeFormatter DATE_INPUT = DateTimeFormatter
            .ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final DesktopSession session;
    private final List<PhieuKhamListItem> rows = new ArrayList<>();

    private final JTextField txtNgayKham = new JTextField(10);
    private final JComboBox<BacSi> cmbBacSiLoc = new JComboBox<>();
    private final JComboBox<String> cmbTrangThai = new JComboBox<>(new String[]{
            "Tất cả trạng thái", "Chờ khám", "Đang khám", "Đã kết luận", "Đã hủy"});
    private final JTextField txtTimKiem = new JTextField(24);
    private final JComboBox<BacSi> cmbBacSiPhuTrach = new JComboBox<>();

    private final JButton btnChonNgay = new JButton(new CalendarIcon());
    private final JButton btnLoc = button("Lọc", true);
    private final JButton btnXoaLoc = button("Xóa lọc", false);
    private final JButton btnTaiLai = button("Tải lại", false);
    private final JButton btnLuuNhap = button("Lưu nháp", false);
    private final JButton btnKetLuan = button("Kết luận", true);
    private CanLamSangPanel pnlCanLamSang;
    private DonThuocPanel pnlDonThuoc;
    private boolean detailBusy;

    private final JLabel lblSoLuong = label("0 phiếu", 11, MUTED, false);
    private final JLabel lblTenBenhNhan = label("Chưa chọn phiếu khám", 18, NAVY, true);
    private final JLabel lblThongTinBenhNhan = label("Chọn một phiếu ở danh sách bên trái", 11, MUTED, false);
    private final JLabel lblTrangThai = label("", 12, MUTED, true);
    private final JLabel lblTrangThaiHeThong = label("Chưa tải dữ liệu.", 11, MUTED, false);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Phiếu", "Giờ", "Bệnh nhân", "Trạng thái"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable tblPhieuKham = new JTable(tableModel);

    private final JTextArea txaTrieuChung = textArea(4);
    private final JTextArea txaChanDoan = textArea(4);
    private final JTextArea txaKetLuan = textArea(4);
    private final JLabel lblTienSuBenh = label("Chưa có thông tin tiền sử bệnh.", 12, NAVY, false);
    private final JLabel lblDiUng = label("Chưa có thông tin dị ứng.", 12, NAVY, false);

    private final JLabel lblBacSiCaption = label("Bác sĩ chịu trách nhiệm:", 11, MUTED, true);
    private boolean loading;
    private PhieuKham current;

    public PhieuKhamPanel(DesktopSession session) {
        this.session = java.util.Objects.requireNonNull(session, "session");
        setLayout(new BorderLayout(0, 14));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(22, 24, 22, 24));

        add(buildIntroduction(), BorderLayout.NORTH);
        add(buildWorkspace(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        cmbBacSiPhuTrach.addActionListener(event -> {
            if (current != null) {
                String doctorId = layMaBacSiPhuTrach();

                if (pnlCanLamSang != null) {
                    pnlCanLamSang.setMaBacSiPhuTrach(doctorId);
                }

                if (pnlDonThuoc != null) {
                    pnlDonThuoc.setMaBacSiPhuTrach(doctorId);
                }
            }
        });

        if (session.role() == Role.DOCTOR) {
            cmbBacSiPhuTrach.setEnabled(false);
            cmbBacSiLoc.setEnabled(false);
        }
        btnKetLuan.setToolTipText("Kết luận phiếu sau khi đã nhập chẩn đoán, kết luận và hoàn tất các chỉ định CLS.");
        new PhieuKhamController(this, new PhieuKhamService(session));
    }

    private JPanel buildIntroduction() {
        JPanel pnlHeader = new JPanel(new BorderLayout(12, 8));
        pnlHeader.setOpaque(false);

        JPanel pnlTitle = new JPanel();
        pnlTitle.setOpaque(false);
        pnlTitle.setLayout(new BoxLayout(pnlTitle, BoxLayout.Y_AXIS));
        JLabel lblTitle = label("Phiếu khám", 23, NAVY, true);
        JLabel lblFlow = label("Chọn phiếu  →  khám và ghi nhận kết quả  →  kết luận", 12, MUTED, false);
        pnlTitle.add(lblTitle);
        pnlTitle.add(Box.createVerticalStrut(4));
        pnlTitle.add(lblFlow);
        pnlHeader.add(pnlTitle, BorderLayout.CENTER);

        pnlHeader.add(pnlTitle, BorderLayout.CENTER);

        JPanel pnlWrap = new JPanel(new BorderLayout(0, 12));
        pnlWrap.setOpaque(false);
        pnlWrap.add(pnlHeader, BorderLayout.NORTH);
        pnlWrap.add(buildFilters(), BorderLayout.SOUTH);
        return pnlWrap;
    }

    private JPanel buildFilters() {
        JPanel pnlFilter = card(new BorderLayout(10, 0));
        pnlFilter.setBorder(paddedBorder(13, 14, 13, 14));

        JPanel pnlFields = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 0));
        pnlFields.setOpaque(false);
        JPanel pnlChonNgay = new JPanel(new BorderLayout(4, 0));
        pnlChonNgay.setOpaque(false);
        txtNgayKham.setColumns(9);
        pnlChonNgay.add(txtNgayKham, BorderLayout.CENTER);

        btnChonNgay.setToolTipText("Chọn ngày khám từ lịch");
        btnChonNgay.setPreferredSize(new Dimension(32, 28));
        btnChonNgay.setMargin(new Insets(2, 3, 2, 3));
        btnChonNgay.setFocusPainted(false);
        btnChonNgay.setBackground(Color.WHITE);
        btnChonNgay.setOpaque(true);
        btnChonNgay.setBorder(BorderFactory.createLineBorder(BORDER));
        btnChonNgay.addActionListener(event -> hienThiLichChonNgay());
        pnlChonNgay.add(btnChonNgay, BorderLayout.EAST);

        pnlFields.add(field("Ngày khám", pnlChonNgay));
        pnlFields.add(field("Bác sĩ", cmbBacSiLoc));
        pnlFields.add(field("Trạng thái", cmbTrangThai));

        JPanel pnlSearch = new JPanel(new BorderLayout(6, 0));
        pnlSearch.setOpaque(false);
        pnlSearch.add(label("Tìm kiếm", 10, MUTED, true), BorderLayout.NORTH);
        txtTimKiem.setToolTipText("Tìm theo mã phiếu, tên hoặc số điện thoại bệnh nhân");
        pnlSearch.add(txtTimKiem, BorderLayout.CENTER);
        pnlFields.add(pnlSearch);

        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        pnlButtons.setOpaque(false);
        pnlButtons.add(btnLoc);
        pnlButtons.add(btnXoaLoc);

        pnlFilter.add(pnlFields, BorderLayout.CENTER);
        pnlFilter.add(pnlButtons, BorderLayout.EAST);
        txtNgayKham.setText(DATE_INPUT.format(LocalDate.now()));
        return pnlFilter;
    }

    private LocalDate layNgayMacDinh() {
        try {
            return LocalDate.parse(
                    txtNgayKham.getText().trim(),
                    DATE_INPUT
            );
        } catch (RuntimeException ex) {
            return LocalDate.now();
        }
    }

    private void hienThiLichChonNgay() {

        final LocalDate ngayMacDinh = layNgayMacDinh();
        YearMonth thangMacDinh = YearMonth.from(ngayMacDinh);
        JDialog dlgLich = new JDialog(
                SwingUtilities.getWindowAncestor(this)
        );

        dlgLich.setUndecorated(true);
        dlgLich.setModal(false);
        dlgLich.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel pnlLich = new JPanel(new BorderLayout(0, 10));
        pnlLich.setBackground(Color.WHITE);
        pnlLich.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JPanel pnlTieuDe = new JPanel(new BorderLayout());
        pnlTieuDe.setOpaque(false);

        JLabel lblTieuDe = label(
                "CHỌN NGÀY KHÁM",
                13,
                NAVY,
                true
        );

        JButton btnDongLich = new JButton("×");
        btnDongLich.setFont(new Font("SansSerif", Font.BOLD, 19));
        btnDongLich.setForeground(MUTED);
        btnDongLich.setBackground(Color.WHITE);
        btnDongLich.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        btnDongLich.setFocusPainted(false);
        btnDongLich.addActionListener(event -> dlgLich.dispose());

        pnlTieuDe.add(lblTieuDe, BorderLayout.WEST);
        pnlTieuDe.add(btnDongLich, BorderLayout.EAST);

        String[] tenThang = {
                "Tháng 1", "Tháng 2", "Tháng 3",
                "Tháng 4", "Tháng 5", "Tháng 6",
                "Tháng 7", "Tháng 8", "Tháng 9",
                "Tháng 10", "Tháng 11", "Tháng 12"
        };

        JComboBox<String> cmbThang = new JComboBox<>(tenThang);

        cmbThang.setSelectedIndex(
                thangMacDinh.getMonthValue() - 1
        );

        cmbThang.setPreferredSize(new Dimension(115, 28));

        JSpinner spnNam = new JSpinner(
                new SpinnerNumberModel(
                        thangMacDinh.getYear(),
                        1900,
                        2100,
                        1
                )
        );
        JSpinner.NumberEditor edtNam =
                new JSpinner.NumberEditor(spnNam, "0");

        edtNam.getFormat().setGroupingUsed(false);
        edtNam.getTextField().setColumns(4);

        spnNam.setEditor(edtNam);
        spnNam.setPreferredSize(new Dimension(75, 28));

        JPanel pnlChonThangNam = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 8, 0)
        );

        pnlChonThangNam.setOpaque(false);
        pnlChonThangNam.add(cmbThang);
        pnlChonThangNam.add(spnNam);

        JPanel pnlHeader = new JPanel(
                new BorderLayout(0, 8)
        );

        pnlHeader.setOpaque(false);
        pnlHeader.add(pnlTieuDe, BorderLayout.NORTH);
        pnlHeader.add(pnlChonThangNam, BorderLayout.SOUTH);

        JPanel pnlNgayTrongTuan = new JPanel(
                new GridLayout(1, 7, 4, 4)
        );

        pnlNgayTrongTuan.setOpaque(false);

        String[] tenNgay = {
                "T2", "T3", "T4", "T5", "T6", "T7", "CN"
        };

        for (String ten : tenNgay) {

            JLabel lblNgay = label(
                    ten,
                    11,
                    MUTED,
                    true
            );

            lblNgay.setHorizontalAlignment(
                    SwingConstants.CENTER
            );

            pnlNgayTrongTuan.add(lblNgay);
        }
        JPanel pnlCacNgay = new JPanel(
                new GridLayout(6, 7, 4, 4)
        );

        pnlCacNgay.setBackground(Color.WHITE);

        pnlCacNgay.setPreferredSize(
                new Dimension(270, 192)
        );

        JPanel pnlNoiDung = new JPanel(
                new BorderLayout(0, 7)
        );

        pnlNoiDung.setOpaque(false);

        pnlNoiDung.add(
                pnlNgayTrongTuan,
                BorderLayout.NORTH
        );

        pnlNoiDung.add(
                pnlCacNgay,
                BorderLayout.CENTER
        );

        Runnable capNhatLich = () -> {
            int thang = cmbThang.getSelectedIndex() + 1;
            int nam = ((Number) spnNam.getValue()).intValue();
            YearMonth namThang = YearMonth.of(nam, thang);
            LocalDate ngayDauThang = namThang.atDay(1);
            int viTriBatDau = ngayDauThang.getDayOfWeek().getValue() - 1;
            pnlCacNgay.removeAll();
            for (int i = 0; i < 42; i++) {
                int ngayTrongThang = i - viTriBatDau + 1;
                if (ngayTrongThang < 1
                        || ngayTrongThang > namThang.lengthOfMonth()) {

                    pnlCacNgay.add(new JLabel(""));
                    continue;
                }

                LocalDate ngay = namThang.atDay(ngayTrongThang);

                JButton btnNgay = new JButton(
                        String.valueOf(ngayTrongThang)
                );

                btnNgay.setFont(
                        new Font("SansSerif", Font.PLAIN, 12)
                );

                btnNgay.setFocusPainted(false);
                btnNgay.setMargin(new Insets(4, 2, 4, 2));
                btnNgay.setOpaque(true);
                btnNgay.setBackground(Color.WHITE);
                btnNgay.setForeground(NAVY);

                btnNgay.setBorder(
                        BorderFactory.createLineBorder(BORDER)
                );

                if (ngay.equals(ngayMacDinh)) {

                    btnNgay.setBackground(SELECTED);

                    btnNgay.setFont(
                            new Font("SansSerif", Font.BOLD, 12)
                    );
                    btnNgay.setBorder(
                            BorderFactory.createLineBorder(NAVY)
                    );
                } else if (ngay.equals(LocalDate.now())) {
                    btnNgay.setForeground(GREEN);
                    btnNgay.setFont(
                            new Font("SansSerif", Font.BOLD, 12)
                    );
                }
                btnNgay.addActionListener(event -> {
                    txtNgayKham.setText(
                            DATE_INPUT.format(ngay)
                    );
                    dlgLich.dispose();
                });
                pnlCacNgay.add(btnNgay);
            }
            pnlCacNgay.revalidate();
            pnlCacNgay.repaint();
        };

        cmbThang.addActionListener(event -> {
            capNhatLich.run();
        });

        spnNam.addChangeListener(event -> {
            capNhatLich.run();
        });

        capNhatLich.run();
        pnlLich.add(pnlHeader, BorderLayout.NORTH);
        pnlLich.add(pnlNoiDung, BorderLayout.CENTER);

        dlgLich.setContentPane(pnlLich);
        dlgLich.pack();
        dlgLich.setResizable(false);

        Point viTri = btnChonNgay.getLocationOnScreen();

        dlgLich.setLocation(
                viTri.x,
                viTri.y + btnChonNgay.getHeight()
        );

        dlgLich.setVisible(true);
    }

    private JSplitPane buildWorkspace() {
        JPanel pnlList = buildListCard();
        JPanel pnlDetail = buildDetailCard();
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlList, pnlDetail);
        split.setResizeWeight(0.38);
        split.setDividerLocation(430);
        split.setDividerSize(8);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setContinuousLayout(true);
        split.setOpaque(false);
        return split;
    }

    private JPanel buildListCard() {
        JPanel pnlCard = card(new BorderLayout(0, 10));
        pnlCard.setBorder(paddedBorder(13, 13, 13, 13));
        JPanel pnlTitle = new JPanel(new BorderLayout());
        pnlTitle.setOpaque(false);

        pnlTitle.add(
                label("Danh sách phiếu khám", 15, NAVY, true),
                BorderLayout.WEST
        );

        JPanel pnlTitleRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlTitleRight.setOpaque(false);

        pnlTitleRight.add(lblSoLuong);
        pnlTitleRight.add(btnTaiLai);

        pnlTitle.add(pnlTitleRight, BorderLayout.EAST);

        pnlCard.add(pnlTitle, BorderLayout.NORTH);

        tblPhieuKham.setRowHeight(31);
        tblPhieuKham.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblPhieuKham.setFillsViewportHeight(true);
        tblPhieuKham.setShowVerticalLines(false);
        tblPhieuKham.setShowHorizontalLines(true);
        tblPhieuKham.setGridColor(BORDER);
        tblPhieuKham.setSelectionBackground(SELECTED);
        tblPhieuKham.setSelectionForeground(NAVY);
        tblPhieuKham.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        tblPhieuKham.getTableHeader().setReorderingAllowed(false);
        tblPhieuKham.getTableHeader().setBackground(new Color(239, 244, 249));
        tblPhieuKham.getTableHeader().setForeground(NAVY);
        tblPhieuKham.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        tblPhieuKham.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tblPhieuKham.getColumnModel().getColumn(0).setPreferredWidth(82);
        tblPhieuKham.getColumnModel().getColumn(1).setPreferredWidth(48);
        tblPhieuKham.getColumnModel().getColumn(2).setPreferredWidth(135);
        tblPhieuKham.getColumnModel().getColumn(3).setPreferredWidth(90);
        tblPhieuKham.getColumnModel().getColumn(3).setCellRenderer(new StatusRenderer());

        JScrollPane scrDanhSach = new JScrollPane(tblPhieuKham);
        scrDanhSach.setBorder(BorderFactory.createLineBorder(BORDER));
        pnlCard.add(scrDanhSach, BorderLayout.CENTER);
        return pnlCard;
    }

    private JPanel buildDetailCard() {
        JPanel pnlCard = card(new BorderLayout(0, 8));
        pnlCard.setBorder(paddedBorder(14, 15, 12, 15));
        pnlCard.add(buildDetailHeader(), BorderLayout.NORTH);

        JTabbedPane tabChiTiet = new JTabbedPane();
        tabChiTiet.setFont(new Font("SansSerif", Font.BOLD, 12));
        tabChiTiet.addTab("Khám", buildExaminationTab());
        tabChiTiet.addTab("Tiền sử", buildHistoryTab());
        pnlCanLamSang = new CanLamSangPanel(session);
        tabChiTiet.addTab("Cận lâm sàng", pnlCanLamSang);
        pnlDonThuoc = new DonThuocPanel(session);
        tabChiTiet.addTab("Đơn thuốc", pnlDonThuoc);
        pnlCard.add(tabChiTiet, BorderLayout.CENTER);
        pnlCard.add(buildDetailActions(), BorderLayout.SOUTH);
        return pnlCard;
    }

    private JPanel buildDetailHeader() {
        JPanel pnlHeader = new JPanel(new BorderLayout(8, 8));
        pnlHeader.setOpaque(false);
        JPanel pnlInfo = new JPanel();
        pnlInfo.setOpaque(false);
        pnlInfo.setLayout(new BoxLayout(pnlInfo, BoxLayout.Y_AXIS));
        pnlInfo.add(lblTenBenhNhan);
        pnlInfo.add(Box.createVerticalStrut(4));
        pnlInfo.add(lblThongTinBenhNhan);
        pnlHeader.add(pnlInfo, BorderLayout.CENTER);

        JPanel pnlDoctor = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlDoctor.setOpaque(false);
        pnlDoctor.add(lblBacSiCaption);
        cmbBacSiPhuTrach.setPreferredSize(new Dimension(210, 28));
        pnlDoctor.add(cmbBacSiPhuTrach);
        pnlDoctor.add(lblTrangThai);
        pnlHeader.add(pnlDoctor, BorderLayout.SOUTH);

        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(0, 0, 10, 0)));
        return pnlHeader;
    }

    private JPanel buildExaminationTab() {
        JPanel pnlTab = new JPanel(new BorderLayout());
        pnlTab.setBackground(Color.WHITE);

        JPanel pnlForm = new JPanel();
        pnlForm.setBackground(Color.WHITE);
        pnlForm.setBorder(new EmptyBorder(13, 8, 8, 8));
        pnlForm.setLayout(new BoxLayout(pnlForm, BoxLayout.Y_AXIS));

        pnlForm.add(formField("Triệu chứng", txaTrieuChung));
        pnlForm.add(Box.createVerticalStrut(10));
        pnlForm.add(formField("Chẩn đoán *", txaChanDoan));
        pnlForm.add(Box.createVerticalStrut(10));
        pnlForm.add(formField("Kết luận / hướng điều trị", txaKetLuan));
        pnlForm.add(Box.createVerticalStrut(8));

        JLabel lblHint = label(
                "Chẩn đoán và kết luận là thông tin bắt buộc trước khi kết luận phiếu.",
                11, MUTED, false
        );
        lblHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlForm.add(lblHint);

        // FlowLayout giữ kích thước ưu tiên của pnlForm, không kéo giãn theo chiều rộng tab.
        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlLeft.setBackground(Color.WHITE);
        pnlLeft.add(pnlForm);

        pnlTab.add(pnlLeft, BorderLayout.CENTER);
        return pnlTab;
    }

    private JPanel buildHistoryTab() {
        JPanel pnlHistory = new JPanel();
        pnlHistory.setBackground(Color.WHITE);
        pnlHistory.setBorder(new EmptyBorder(15, 8, 8, 8));
        pnlHistory.setLayout(new BoxLayout(pnlHistory, BoxLayout.Y_AXIS));
        pnlHistory.add(label("DỊ ỨNG", 11, MUTED, true));
        pnlHistory.add(Box.createVerticalStrut(6));
        pnlHistory.add(wrapInfo(lblDiUng, new Color(255, 246, 246), new Color(185, 28, 28)));
        pnlHistory.add(Box.createVerticalStrut(18));
        pnlHistory.add(label("TIỀN SỬ BỆNH", 11, MUTED, true));
        pnlHistory.add(Box.createVerticalStrut(6));
        pnlHistory.add(wrapInfo(lblTienSuBenh, BACKGROUND, NAVY));
        pnlHistory.add(Box.createVerticalGlue());
        return pnlHistory;
    }

    private JPanel buildPendingTab(String message) {
        JPanel pnlPending = new JPanel(new BorderLayout());
        pnlPending.setBackground(Color.WHITE);
        pnlPending.setBorder(new EmptyBorder(18, 12, 12, 12));
        JTextArea txaThongBao = new JTextArea(message);
        txaThongBao.setEditable(false);
        txaThongBao.setFocusable(false);
        txaThongBao.setLineWrap(true);
        txaThongBao.setWrapStyleWord(true);
        txaThongBao.setOpaque(false);
        txaThongBao.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txaThongBao.setForeground(MUTED);
        pnlPending.add(txaThongBao, BorderLayout.NORTH);
        return pnlPending;
    }

    private JPanel buildDetailActions() {
        JPanel pnlActions = new JPanel(new BorderLayout(8, 0));
        pnlActions.setOpaque(false);
        pnlActions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
                new EmptyBorder(10, 0, 0, 0)));

        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        pnlButtons.setOpaque(false);
        pnlButtons.add(btnLuuNhap);
        pnlButtons.add(btnKetLuan);
        pnlActions.add(pnlButtons, BorderLayout.EAST);
        return pnlActions;
    }

    private JPanel buildFooter() {
        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlFooter.setOpaque(false);
        pnlFooter.add(lblTrangThaiHeThong);
        return pnlFooter;
    }

    private JPanel field(String title, Component input) {
        JPanel pnlField = new JPanel(new BorderLayout(0, 4));
        pnlField.setOpaque(false);
        pnlField.add(label(title, 10, MUTED, true), BorderLayout.NORTH);
        pnlField.add(input, BorderLayout.CENTER);
        return pnlField;
    }

    private JPanel formField(String title, JTextArea txaNoiDung) {
        JPanel pnlField = new JPanel(new BorderLayout(0, 5));
        pnlField.setOpaque(false);
        pnlField.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnlField.add(label(title, 12, NAVY, true), BorderLayout.NORTH);

        JScrollPane scrNoiDung = new JScrollPane(
                txaNoiDung,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scrNoiDung.setBorder(BorderFactory.createLineBorder(BORDER));

        Dimension scrollSize = new Dimension(500, 85);
        scrNoiDung.setPreferredSize(scrollSize);
        scrNoiDung.setMinimumSize(scrollSize);
        scrNoiDung.setMaximumSize(scrollSize);

        pnlField.add(scrNoiDung, BorderLayout.CENTER);

        Dimension fieldSize = new Dimension(500, 112);
        pnlField.setPreferredSize(fieldSize);
        pnlField.setMinimumSize(fieldSize);
        pnlField.setMaximumSize(fieldSize);

        return pnlField;
    }

    private JPanel wrapInfo(JLabel lblGiaTri, Color background, Color foreground) {
        JPanel pnlWrap = new JPanel(new BorderLayout());
        pnlWrap.setBackground(background);
        pnlWrap.setBorder(new EmptyBorder(11, 12, 11, 12));
        lblGiaTri.setForeground(foreground);
        pnlWrap.add(lblGiaTri, BorderLayout.CENTER);
        pnlWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        return pnlWrap;
    }

    private static JPanel card(java.awt.LayoutManager layout) {
        JPanel pnlCard = new JPanel(layout);
        pnlCard.setBackground(Color.WHITE);
        pnlCard.setBorder(BorderFactory.createLineBorder(BORDER));
        return pnlCard;
    }

    private static EmptyBorder paddedBorder(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

    private static JLabel label(String text, int size, Color color, boolean bold) {
        JLabel lblGiaTri = new JLabel(text);
        lblGiaTri.setFont(new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, size));
        lblGiaTri.setForeground(color);
        return lblGiaTri;
    }

    private static JButton button(String text, boolean primary) {
        JButton btnGiaTri = new JButton(text);
        btnGiaTri.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnGiaTri.setFocusPainted(false);
        btnGiaTri.setBorder(new EmptyBorder(8, 13, 8, 13));
        if (primary) {
            btnGiaTri.setBackground(NAVY);
            btnGiaTri.setForeground(Color.WHITE);
            btnGiaTri.setOpaque(true);
        } else {
            btnGiaTri.setBackground(Color.WHITE);
            btnGiaTri.setForeground(NAVY);
            btnGiaTri.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER), new EmptyBorder(7, 12, 7, 12)));
        }
        return btnGiaTri;
    }

    private static JTextArea textArea(int rows) {
        JTextArea txaNoiDung = new JTextArea(rows, 25);
        txaNoiDung.setFont(new Font("SansSerif", Font.PLAIN, 12));
        txaNoiDung.setLineWrap(true);
        txaNoiDung.setWrapStyleWord(true);
        txaNoiDung.setBorder(new EmptyBorder(7, 8, 7, 8));
        txaNoiDung.setForeground(NAVY);
        return txaNoiDung;
    }

    public void datDanhSachBacSi(List<BacSi> doctors) {
        cmbBacSiLoc.removeAllItems();
        cmbBacSiLoc.addItem(new BacSi("", "Tất cả bác sĩ"));
        cmbBacSiPhuTrach.removeAllItems();
        cmbBacSiPhuTrach.addItem(new BacSi("", "— Chọn bác sĩ —"));
        for (BacSi doctor : doctors) {
            cmbBacSiLoc.addItem(doctor);
            cmbBacSiPhuTrach.addItem(doctor);
        }
        if (session.role() == Role.DOCTOR) {
            cmbBacSiLoc.setSelectedIndex(doctors.isEmpty() ? 0 : 1);
            cmbBacSiPhuTrach.setSelectedIndex(doctors.isEmpty() ? 0 : 1);
        }
    }

    public void hienDanhSach(List<PhieuKhamListItem> values) {
        rows.clear();
        rows.addAll(values);
        tableModel.setRowCount(0);
        for (PhieuKhamListItem row : rows) {
            LocalDateTime received = row.thoiGianTiepNhan();
            tableModel.addRow(new Object[]{
                    row.maPhieuKham(),
                    received == null ? "—" : TIME.format(received),
                    row.tenBenhNhan(),
                    tenTrangThai(row.trangThai())
            });
        }
        lblSoLuong.setText(rows.size() + " phiếu");
        xoaChiTiet();
        if (!rows.isEmpty()) tblPhieuKham.setRowSelectionInterval(0, 0);
    }

    public void hienChiTiet(PhieuKham value) {
        current = value;
        if (value == null) {
            xoaChiTiet();
            return;
        }
        lblTenBenhNhan.setText(value.maPhieuKham() + "  ·  " + value.tenBenhNhan());
        String received = value.thoiGianTiepNhan() == null
                ? "—" : DATE_TIME.format(value.thoiGianTiepNhan());
        lblThongTinBenhNhan.setText(value.maBenhNhan() + "  ·  "
                + safe(value.soDienThoai()) + "  ·  " + received
                + "  ·  HS: " + safe(value.maHoSo()));
        lblTrangThai.setText(tenTrangThai(value.trangThai()));
        lblTrangThai.setForeground(mauTrangThai(value.trangThai()));
        txaTrieuChung.setText(safe(value.trieuChung()));
        txaChanDoan.setText(safe(value.chanDoan()));
        txaKetLuan.setText(safe(value.ketLuan()));
        lblTienSuBenh.setText(blankAsMessage(value.tienSuBenh(), "Chưa ghi nhận tiền sử bệnh."));
        lblDiUng.setText(blankAsMessage(value.diUng(), "Chưa ghi nhận dị ứng."));
        chonBacSiPhuTrach(value.maNhanVien());
        if (pnlCanLamSang != null) pnlCanLamSang.setContext(value, layMaBacSiPhuTrach());
        if (pnlDonThuoc != null) {
            pnlDonThuoc.setContext(value, layMaBacSiPhuTrach());
        }
        capNhatTrangThaiNut(value.trangThai());
    }

    public void xoaChiTiet() {
        current = null;
        lblTenBenhNhan.setText("Chưa chọn phiếu khám");
        lblThongTinBenhNhan.setText("Chọn một phiếu ở danh sách bên trái");
        lblTrangThai.setText("");
        txaTrieuChung.setText("");
        txaChanDoan.setText("");
        txaKetLuan.setText("");
        lblTienSuBenh.setText("Chưa có thông tin tiền sử bệnh.");
        lblDiUng.setText("Chưa có thông tin dị ứng.");
        if (pnlCanLamSang != null) pnlCanLamSang.setContext(null, "");
        if (pnlDonThuoc != null) {
            pnlDonThuoc.setContext(null, "");
        }
        capNhatTrangThaiNut("");
    }

    private void capNhatTrangThaiNut(String status) {
        boolean editable = "CHO_KHAM".equals(status)
                || "DANG_KHAM".equals(status);

        boolean canEditNow = editable
                && !loading
                && !detailBusy
                && current != null;

        btnLuuNhap.setEnabled(canEditNow);
        btnKetLuan.setEnabled(canEditNow);

        txaTrieuChung.setEditable(canEditNow);
        txaChanDoan.setEditable(canEditNow);
        txaKetLuan.setEditable(canEditNow);
    }

    private void chonBacSiPhuTrach(String maBacSi) {
        for (int i = 0; i < cmbBacSiPhuTrach.getItemCount(); i++) {
            BacSi doctor = cmbBacSiPhuTrach.getItemAt(i);
            if (doctor != null && doctor.maNhanVien().equals(maBacSi)) {
                cmbBacSiPhuTrach.setSelectedIndex(i);
                return;
            }
        }
        if (maBacSi != null && !maBacSi.isBlank() && session.role() == Role.SYSTEM_ADMIN) {
            cmbBacSiPhuTrach.addItem(new BacSi(maBacSi, "Bác sĩ đang phụ trách"));
            cmbBacSiPhuTrach.setSelectedIndex(cmbBacSiPhuTrach.getItemCount() - 1);
        }
    }

    public void xoaBoLoc() {
        txtNgayKham.setText(DATE_INPUT.format(LocalDate.now()));
        if (cmbBacSiLoc.getItemCount() > 0) cmbBacSiLoc.setSelectedIndex(0);
        cmbTrangThai.setSelectedIndex(0);
        txtTimKiem.setText("");
    }

    public LocalDate layNgayLoc() {
        String value = txtNgayKham.getText() == null ? "" : txtNgayKham.getText().trim();
        try {
            return LocalDate.parse(value, DATE_INPUT);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Ngày khám phải có dạng dd/MM/yyyy và là ngày hợp lệ.");
        }
    }

    public String layMaBacSiLoc() {
        BacSi doctor = (BacSi) cmbBacSiLoc.getSelectedItem();
        return doctor == null ? "" : doctor.maNhanVien();
    }

    public String layTrangThaiLoc() {
        return switch ((String) cmbTrangThai.getSelectedItem()) {
            case "Chờ khám" -> "CHO_KHAM";
            case "Đang khám" -> "DANG_KHAM";
            case "Đã kết luận" -> "DA_KET_LUAN";
            case "Đã hủy" -> "DA_HUY";
            default -> "";
        };
    }

    public String layTuKhoa() { return txtTimKiem.getText() == null ? "" : txtTimKiem.getText().trim(); }

    public String layMaPhieuDangChon() {
        int selected = tblPhieuKham.getSelectedRow();
        if (selected < 0) return "";
        int modelRow = tblPhieuKham.convertRowIndexToModel(selected);
        return modelRow >= 0 && modelRow < rows.size() ? rows.get(modelRow).maPhieuKham() : "";
    }

    public String layMaBacSiPhuTrach() {
        BacSi doctor = (BacSi) cmbBacSiPhuTrach.getSelectedItem();
        return doctor == null ? "" : doctor.maNhanVien();
    }

    public String layTrieuChung() { return txaTrieuChung.getText(); }
    public String layChanDoan() { return txaChanDoan.getText(); }

    public void setDangTai(boolean busy, String message) {
        loading = busy;
        btnLoc.setEnabled(!busy);
        btnXoaLoc.setEnabled(!busy);
        btnTaiLai.setEnabled(!busy);
        txtNgayKham.setEnabled(!busy);
        btnChonNgay.setEnabled(!busy);
        cmbBacSiLoc.setEnabled(!busy && session.role() != Role.DOCTOR);
        cmbTrangThai.setEnabled(!busy);
        txtTimKiem.setEnabled(!busy);
        lblTrangThaiHeThong.setText(message);
        if (current != null) capNhatTrangThaiNut(current.trangThai());
    }

    public void setDangTaiChiTiet(boolean busy) {
        detailBusy = busy;

        if (current == null) {
            btnLuuNhap.setEnabled(false);
            btnKetLuan.setEnabled(false);
        } else {
            capNhatTrangThaiNut(current.trangThai());
        }
    }

    public boolean xacNhan(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Xác nhận",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    public void thongBaoThanhCong(String message) {
        lblTrangThaiHeThong.setText(message);
    }

    public void hienLoi(String message) {
        lblTrangThaiHeThong.setText(message);
        JOptionPane.showMessageDialog(this, message, "Thông báo", JOptionPane.WARNING_MESSAGE);
    }

    public JButton getBtnLoc() { return btnLoc; }
    public JButton getBtnXoaLoc() { return btnXoaLoc; }
    public JButton getBtnTaiLai() { return btnTaiLai; }
    public JButton getBtnLuuNhap() { return btnLuuNhap; }
    public JTable getTablePhieuKham() { return tblPhieuKham; }
    public JButton getBtnKetLuan() {
        return btnKetLuan;
    }

    public String layKetLuan() {
        return txaKetLuan.getText();
    }

    private static String tenTrangThai(String status) {
        return switch (status == null ? "" : status) {
            case "CHO_KHAM" -> "Chờ khám";
            case "DANG_KHAM" -> "Đang khám";
            case "DA_KET_LUAN" -> "Đã kết luận";
            case "DA_HUY" -> "Đã hủy";
            default -> "Không xác định";
        };
    }

    private static Color mauTrangThai(String status) {
        return switch (status == null ? "" : status) {
            case "DA_KET_LUAN" -> GREEN;
            case "DANG_KHAM" -> PURPLE;
            case "DA_HUY" -> RED;
            default -> NAVY;
        };
    }

    private static String blankAsMessage(String value, String message) {
        return value == null || value.isBlank() ? message : value;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static final class CalendarIcon implements Icon {
        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(NAVY);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x + 2, y + 3, 14, 13, 2, 2);
            g2.drawLine(x + 2, y + 7, x + 16, y + 7);
            g2.drawLine(x + 6, y + 1, x + 6, y + 5);
            g2.drawLine(x + 12, y + 1, x + 12, y + 5);

            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(x + 5, y + 10, x + 5, y + 10);
            g2.drawLine(x + 9, y + 10, x + 9, y + 10);
            g2.drawLine(x + 13, y + 10, x + 13, y + 10);
            g2.drawLine(x + 5, y + 13, x + 5, y + 13);
            g2.drawLine(x + 9, y + 13, x + 9, y + 13);
            g2.drawLine(x + 13, y + 13, x + 13, y + 13);
            g2.dispose();
        }
    }

    private static final class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            setFont(new Font("SansSerif", Font.BOLD, 11));
            if (!isSelected) {
                String status = value == null ? "" : value.toString();
                setForeground(switch (status) {
                    case "Đã kết luận" -> GREEN;
                    case "Đang khám" -> PURPLE;
                    case "Đã hủy" -> RED;
                    default -> NAVY;
                });
            }
            return component;
        }
    }
}
