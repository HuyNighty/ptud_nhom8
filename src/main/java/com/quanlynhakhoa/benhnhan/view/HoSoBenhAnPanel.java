package com.quanlynhakhoa.benhnhan.view;

import com.quanlynhakhoa.benhnhan.controller.HoSoBenhAnController;
import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.benhnhan.model.HoSoBenhAn;
import com.quanlynhakhoa.benhnhan.model.LuotKham;
import com.quanlynhakhoa.benhnhan.service.BenhNhanService;
import com.quanlynhakhoa.benhnhan.service.HoSoBenhAnService;
import com.quanlynhakhoa.security.DesktopSession;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class HoSoBenhAnPanel extends JPanel {
    private static final Color NAVY = new Color(29, 61, 102);
    private static final Color XAM = new Color(97, 115, 137);
    private static final Color VIEN = new Color(220, 231, 242);
    private static final Color NEN = new Color(245, 248, 252);
    private static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DINH_DANG_NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private JPanel pnlNorth;
    private JLabel lblTieuDe;
    private JTextField txtTimKiem;
    private JButton btnTimKiem;
    private JButton btnLamMoi;

    private JPanel pnlWest;
    private DefaultTableModel modelBenhNhan;
    private JTable tblBenhNhan;
    private JScrollPane scrBenhNhan;
    private ArrayList<BenhNhan> danhSachBenhNhan = new ArrayList<>();

    private JPanel pnlCenter;
    private JLabel lblTenBenhNhan;
    private JLabel lblThongTinHoSo;
    private JTextArea txaTienSuBenh;
    private JTextArea txaDiUng;
    private DefaultTableModel modelLuotKham;
    private JTable tblLuotKham;
    private JScrollPane scrLuotKham;
    private ArrayList<LuotKham> danhSachLuotKham = new ArrayList<>();

    private JPanel pnlEast;
    private JLabel lblMaPhieu;
    private JLabel lblNgayKham;
    private JLabel lblBacSi;
    private JLabel lblTrangThaiKham;
    private JTextArea txaTrieuChung;
    private JTextArea txaChanDoan;
    private JTextArea txaKetLuan;

    public HoSoBenhAnPanel(DesktopSession phienDangNhap) {
        setLayout(new BorderLayout(14, 14));
        setBackground(NEN);
        setBorder(new EmptyBorder(24, 26, 24, 26));

        taoNorth();
        taoWest();
        taoCenter();
        taoEast();

        add(pnlNorth, BorderLayout.NORTH);
        add(pnlWest, BorderLayout.WEST);
        add(pnlCenter, BorderLayout.CENTER);
        add(pnlEast, BorderLayout.EAST);

        hienHoSo(null, null);
        hienChiTietLuotKham(null);

        new HoSoBenhAnController(this, new BenhNhanService(phienDangNhap), new HoSoBenhAnService(phienDangNhap));
    }

    private void taoNorth() {
        pnlNorth = new JPanel(new BorderLayout(0, 10));
        pnlNorth.setOpaque(false);

        lblTieuDe = new JLabel("Hồ sơ bệnh án");
        lblTieuDe.setFont(new Font("SansSerif", Font.BOLD, 23));
        lblTieuDe.setForeground(NAVY);

        txtTimKiem = new JTextField(24);
        btnTimKiem = new JButton("Tìm kiếm");
        btnTimKiem.setIcon(new BieuTuong(BieuTuong.TIM_KIEM));
        btnLamMoi = new JButton("Làm mới");
        btnLamMoi.setIcon(new BieuTuong(BieuTuong.LAM_MOI));

        JPanel pnlNorthSouth = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 8));
        pnlNorthSouth.setBackground(Color.WHITE);
        pnlNorthSouth.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlNorthSouth.add(new JLabel("Tìm:"));
        pnlNorthSouth.add(txtTimKiem);
        pnlNorthSouth.add(btnTimKiem);
        pnlNorthSouth.add(btnLamMoi);

        pnlNorth.add(lblTieuDe, BorderLayout.NORTH);
        pnlNorth.add(pnlNorthSouth, BorderLayout.SOUTH);
    }

    private void taoWest() {
        pnlWest = new JPanel(new BorderLayout(0, 6));
        pnlWest.setOpaque(false);
        pnlWest.setPreferredSize(new Dimension(300, 0));

        String[] tenCotBenhNhan = {"Mã BN", "Họ tên", "SĐT"};
        modelBenhNhan = new DefaultTableModel(tenCotBenhNhan, 0) {
            @Override
            public boolean isCellEditable(int dong, int cot) {
                return false;
            }
        };
        tblBenhNhan = taoBang(modelBenhNhan);
        tblBenhNhan.getColumnModel().getColumn(0).setPreferredWidth(70);
        tblBenhNhan.getColumnModel().getColumn(1).setPreferredWidth(130);
        scrBenhNhan = new JScrollPane(tblBenhNhan);
        scrBenhNhan.setBorder(BorderFactory.createLineBorder(VIEN));

        pnlWest.add(taoTieuDeKhung("Bệnh nhân"), BorderLayout.NORTH);
        pnlWest.add(scrBenhNhan, BorderLayout.CENTER);
    }

    private void taoCenter() {
        pnlCenter = new JPanel(new BorderLayout(0, 6));
        pnlCenter.setOpaque(false);

        lblTenBenhNhan = new JLabel();
        lblTenBenhNhan.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblTenBenhNhan.setForeground(NAVY);
        lblThongTinHoSo = new JLabel();
        lblThongTinHoSo.setForeground(XAM);
        txaTienSuBenh = taoOChuChiXem();
        txaDiUng = taoOChuChiXem();

        Box boxHoSo = Box.createVerticalBox();
        boxHoSo.add(dongTrai(lblTenBenhNhan));
        boxHoSo.add(Box.createVerticalStrut(2));
        boxHoSo.add(dongTrai(lblThongTinHoSo));
        boxHoSo.add(Box.createVerticalStrut(8));
        themDongNhieuChu(boxHoSo, "Tiền sử bệnh:", txaTienSuBenh);
        themDongNhieuChu(boxHoSo, "Dị ứng:", txaDiUng);

        String[] tenCotLuotKham = {"Mã phiếu", "Ngày khám", "Bác sĩ", "Chẩn đoán", "Trạng thái"};
        modelLuotKham = new DefaultTableModel(tenCotLuotKham, 0) {
            @Override
            public boolean isCellEditable(int dong, int cot) {
                return false;
            }
        };
        tblLuotKham = taoBang(modelLuotKham);
        tblLuotKham.getColumnModel().getColumn(0).setPreferredWidth(80);
        tblLuotKham.getColumnModel().getColumn(1).setPreferredWidth(115);
        tblLuotKham.getColumnModel().getColumn(3).setPreferredWidth(170);
        scrLuotKham = new JScrollPane(tblLuotKham);
        scrLuotKham.setBorder(BorderFactory.createLineBorder(VIEN));

        JPanel pnlCenterCenter = new JPanel(new BorderLayout(0, 6));
        pnlCenterCenter.setOpaque(false);
        pnlCenterCenter.add(taoTieuDeKhung("Lịch sử khám"), BorderLayout.NORTH);
        pnlCenterCenter.add(scrLuotKham, BorderLayout.CENTER);

        JPanel pnlCenterNorth = new JPanel(new BorderLayout());
        pnlCenterNorth.setBackground(Color.WHITE);
        pnlCenterNorth.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VIEN), new EmptyBorder(10, 12, 4, 12)));
        pnlCenterNorth.add(boxHoSo, BorderLayout.CENTER);

        pnlCenter.add(pnlCenterNorth, BorderLayout.NORTH);
        pnlCenter.add(pnlCenterCenter, BorderLayout.CENTER);
    }

    private void taoEast() {
        pnlEast = new JPanel(new BorderLayout());
        pnlEast.setBackground(Color.WHITE);
        pnlEast.setBorder(BorderFactory.createLineBorder(VIEN));
        pnlEast.setPreferredSize(new Dimension(320, 0));

        lblMaPhieu = taoGiaTri();
        lblNgayKham = taoGiaTri();
        lblBacSi = taoGiaTri();
        lblTrangThaiKham = taoGiaTri();
        txaTrieuChung = taoOChuChiXem();
        txaChanDoan = taoOChuChiXem();
        txaKetLuan = taoOChuChiXem();

        Box boxChiTiet = Box.createVerticalBox();
        boxChiTiet.setBorder(new EmptyBorder(14, 14, 14, 14));
        JLabel lblTieuDeChiTiet = new JLabel("Chi tiết lần khám");
        lblTieuDeChiTiet.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblTieuDeChiTiet.setForeground(NAVY);
        boxChiTiet.add(dongTrai(lblTieuDeChiTiet));
        boxChiTiet.add(Box.createVerticalStrut(12));
        themDong(boxChiTiet, "Mã phiếu:", lblMaPhieu);
        themDong(boxChiTiet, "Ngày khám:", lblNgayKham);
        themDong(boxChiTiet, "Bác sĩ:", lblBacSi);
        themDong(boxChiTiet, "Trạng thái:", lblTrangThaiKham);
        themDongNhieuChu(boxChiTiet, "Triệu chứng:", txaTrieuChung);
        themDongNhieuChu(boxChiTiet, "Chẩn đoán:", txaChanDoan);
        themDongNhieuChu(boxChiTiet, "Kết luận:", txaKetLuan);

        pnlEast.add(boxChiTiet, BorderLayout.NORTH);
    }

    private JTable taoBang(DefaultTableModel modelCuaBang) {
        JTable tblMoi = new JTable(modelCuaBang);
        tblMoi.setRowHeight(28);
        tblMoi.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblMoi.getTableHeader().setReorderingAllowed(false);
        tblMoi.setFillsViewportHeight(true);
        return tblMoi;
    }

    private JLabel taoTieuDeKhung(String noiDung) {
        JLabel lblTieuDeKhung = new JLabel(noiDung);
        lblTieuDeKhung.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTieuDeKhung.setForeground(NAVY);
        return lblTieuDeKhung;
    }

    private JLabel taoGiaTri() {
        JLabel lblGiaTri = new JLabel("—");
        lblGiaTri.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblGiaTri.setForeground(NAVY);
        return lblGiaTri;
    }

    private JTextArea taoOChuChiXem() {
        JTextArea txaChiXem = new JTextArea("—");
        txaChiXem.setEditable(false);
        txaChiXem.setLineWrap(true);
        txaChiXem.setWrapStyleWord(true);
        txaChiXem.setOpaque(false);
        txaChiXem.setBorder(null);
        txaChiXem.setFont(new Font("SansSerif", Font.BOLD, 13));
        txaChiXem.setForeground(NAVY);
        return txaChiXem;
    }

    private Box dongTrai(JLabel lblNoiDung) {
        Box boxDong = Box.createHorizontalBox();
        boxDong.add(lblNoiDung);
        boxDong.add(Box.createHorizontalGlue());
        return boxDong;
    }

    private void themDong(Box boxCha, String noiDungNhan, JLabel lblGiaTri) {
        JLabel lblNhan = new JLabel(noiDungNhan);
        lblNhan.setForeground(XAM);
        lblNhan.setPreferredSize(new Dimension(95, lblNhan.getPreferredSize().height));
        Box boxDong = Box.createHorizontalBox();
        boxDong.add(lblNhan);
        boxDong.add(Box.createHorizontalStrut(8));
        boxDong.add(lblGiaTri);
        boxDong.add(Box.createHorizontalGlue());
        boxCha.add(boxDong);
        boxCha.add(Box.createVerticalStrut(10));
    }

    private void themDongNhieuChu(Box boxCha, String noiDungNhan, JTextArea txaGiaTri) {
        JLabel lblNhan = new JLabel(noiDungNhan);
        lblNhan.setForeground(XAM);
        lblNhan.setPreferredSize(new Dimension(95, lblNhan.getPreferredSize().height));
        lblNhan.setAlignmentY(TOP_ALIGNMENT);
        txaGiaTri.setAlignmentY(TOP_ALIGNMENT);
        Box boxDong = Box.createHorizontalBox();
        boxDong.add(lblNhan);
        boxDong.add(Box.createHorizontalStrut(8));
        boxDong.add(txaGiaTri);
        boxCha.add(boxDong);
        boxCha.add(Box.createVerticalStrut(10));
    }

    public void hienDanhSachBenhNhan(ArrayList<BenhNhan> danhSachTimDuoc) {
        danhSachBenhNhan = danhSachTimDuoc;
        modelBenhNhan.setRowCount(0);
        for (BenhNhan benhNhan : danhSachBenhNhan) {
            modelBenhNhan.addRow(new Object[]{
                    benhNhan.getMaBenhNhan(),
                    benhNhan.getHoTen(),
                    benhNhan.getSoDienThoai()
            });
        }
        hienHoSo(null, null);
    }

    public void hienHoSo(BenhNhan benhNhan, HoSoBenhAn hoSoBenhAn) {
        modelLuotKham.setRowCount(0);
        danhSachLuotKham = new ArrayList<>();
        hienChiTietLuotKham(null);
        if (benhNhan == null || hoSoBenhAn == null) {
            lblTenBenhNhan.setText("Chưa chọn bệnh nhân");
            lblThongTinHoSo.setText("");
            txaTienSuBenh.setText("—");
            txaDiUng.setText("—");
            return;
        }
        lblTenBenhNhan.setText(benhNhan.getHoTen() + " (" + benhNhan.getMaBenhNhan() + ")");
        if (hoSoBenhAn.getMaHoSo() == null) {
            lblThongTinHoSo.setText("Chưa có hồ sơ bệnh án");
        } else {
            lblThongTinHoSo.setText("Mã hồ sơ " + hoSoBenhAn.getMaHoSo()
                    + " · Lập ngày " + hoSoBenhAn.getNgayLap().format(DINH_DANG_NGAY));
        }
        txaTienSuBenh.setText(hienKhongCoNeuTrong(hoSoBenhAn.getTienSuBenh()));
        txaDiUng.setText(hienKhongCoNeuTrong(hoSoBenhAn.getDiUng()));

        danhSachLuotKham = hoSoBenhAn.getDanhSachLuotKham();
        for (LuotKham luotKham : danhSachLuotKham) {
            modelLuotKham.addRow(new Object[]{
                    luotKham.getMaPhieuKham(),
                    dinhDangNgayKham(luotKham, ""),
                    hienGachNgangNeuTrong(luotKham.getTenBacSi()),
                    hienGachNgangNeuTrong(luotKham.getChanDoan()),
                    luotKham.getTrangThaiHienThi()
            });
        }
    }

    public void hienChiTietLuotKham(LuotKham luotKham) {
        if (luotKham == null) {
            lblMaPhieu.setText("—");
            lblNgayKham.setText("—");
            lblBacSi.setText("—");
            lblTrangThaiKham.setText("—");
            txaTrieuChung.setText("—");
            txaChanDoan.setText("—");
            txaKetLuan.setText("—");
            return;
        }
        lblMaPhieu.setText(luotKham.getMaPhieuKham());
        lblNgayKham.setText(dinhDangNgayKham(luotKham, "—"));
        lblBacSi.setText(hienGachNgangNeuTrong(luotKham.getTenBacSi()));
        lblTrangThaiKham.setText(luotKham.getTrangThaiHienThi());
        txaTrieuChung.setText(hienGachNgangNeuTrong(luotKham.getTrieuChung()));
        txaChanDoan.setText(hienGachNgangNeuTrong(luotKham.getChanDoan()));
        txaKetLuan.setText(hienGachNgangNeuTrong(luotKham.getKetLuan()));
    }

    public void datDangTai(boolean dangTai) {
        btnTimKiem.setEnabled(!dangTai);
        btnLamMoi.setEnabled(!dangTai);
    }

    public void hienLoi(String thongBao) {
        JOptionPane.showMessageDialog(this, thongBao, "Thông báo lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public BenhNhan layBenhNhanDangChon() {
        int dongDangChon = tblBenhNhan.getSelectedRow();
        if (dongDangChon < 0 || dongDangChon >= danhSachBenhNhan.size()) {
            return null;
        }
        return danhSachBenhNhan.get(dongDangChon);
    }

    public LuotKham layLuotKhamDangChon() {
        int dongDangChon = tblLuotKham.getSelectedRow();
        if (dongDangChon < 0 || dongDangChon >= danhSachLuotKham.size()) {
            return null;
        }
        return danhSachLuotKham.get(dongDangChon);
    }

    private String dinhDangNgayKham(LuotKham luotKham, String thayThe) {
        if (luotKham.getThoiGianTiepNhan() == null) {
            return thayThe;
        }
        return luotKham.getThoiGianTiepNhan().format(DINH_DANG_NGAY_GIO);
    }

    private String hienGachNgangNeuTrong(String giaTri) {
        if (giaTri == null || giaTri.isBlank()) {
            return "—";
        }
        return giaTri;
    }

    private String hienKhongCoNeuTrong(String giaTri) {
        if (giaTri == null) {
            return "Không có";
        }
        return giaTri;
    }

    public JTextField getTxtTimKiem() {
        return txtTimKiem;
    }

    public JButton getBtnTimKiem() {
        return btnTimKiem;
    }

    public JButton getBtnLamMoi() {
        return btnLamMoi;
    }

    public JTable getTblBenhNhan() {
        return tblBenhNhan;
    }

    public JTable getTblLuotKham() {
        return tblLuotKham;
    }
}
