package com.quanlynhakhoa.benhnhan.controller;

import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.benhnhan.service.BenhNhanService;
import com.quanlynhakhoa.benhnhan.service.DuLieuDaThayDoiException;
import com.quanlynhakhoa.benhnhan.service.LoiNhapLieuException;
import com.quanlynhakhoa.benhnhan.view.BenhNhanDialog;
import com.quanlynhakhoa.benhnhan.view.BenhNhanPanel;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.Permission;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class BenhNhanController implements ActionListener, MouseListener {
    private static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final int CHON_QUAY_LAI = 0;
    private static final int CHON_VAN_LUU = 1;
    private static final int CHON_DUNG_HO_SO_CU = 2;

    private BenhNhanPanel view;
    private BenhNhanService benhNhanService;

    private BenhNhanDialog winBenhNhan;
    private String maDangSua;
    private BenhNhan benhNhanLucMoForm;
    private boolean dangLuu;
    private String maHoSoCuDaChon;

    private boolean dangTaiDanhSach;
    private boolean coYeuCauTaiLaiDangCho;
    private String maCanChonKhiTaiLai;

    private long soThuTuLanXemChiTiet;

    public BenhNhanController(BenhNhanPanel view, BenhNhanService benhNhanService) {
        this.view = view;
        this.benhNhanService = benhNhanService;

        view.getBtnLoc().addActionListener(this);
        view.getBtnXoaLoc().addActionListener(this);
        view.getBtnTaiLai().addActionListener(this);
        view.getTxtTimKiem().addActionListener(this);
        view.getBtnThem().addActionListener(this);
        view.getBtnSua().addActionListener(this);
        view.getTblBenhNhan().addMouseListener(this);

        boolean duocThem = benhNhanService.coQuyen(Permission.PATIENT_CREATE);
        boolean duocSua = benhNhanService.coQuyen(Permission.PATIENT_UPDATE);
        view.datQuyen(duocThem, duocSua);

        docDuLieuVaoTable(null);
    }

    @Override
    public void actionPerformed(ActionEvent suKien) {
        Object nguonSuKien = suKien.getSource();
        if (nguonSuKien.equals(view.getBtnLoc()) || nguonSuKien.equals(view.getTxtTimKiem())) {
            docDuLieuVaoTable(null);
        }
        if (nguonSuKien.equals(view.getBtnXoaLoc())) {
            view.xoaTrang();
            docDuLieuVaoTable(null);
        }
        if (nguonSuKien.equals(view.getBtnTaiLai())) {
            docDuLieuVaoTable(null);
        }
        if (nguonSuKien.equals(view.getBtnThem())) {
            moForm(null);
        }
        if (nguonSuKien.equals(view.getBtnSua()) && view.getBenhNhanDangXem() != null) {
            moForm(view.getBenhNhanDangXem());
        }
        if (winBenhNhan != null && nguonSuKien.equals(winBenhNhan.getBtnChonNgay())) {
            winBenhNhan.moLichChonNgaySinh();
        }
        if (winBenhNhan != null && nguonSuKien.equals(winBenhNhan.getBtnLuu())) {
            luu();
        }
        if (winBenhNhan != null && nguonSuKien.equals(winBenhNhan.getBtnHuy())) {
            winBenhNhan.dispose();
        }
    }

    @Override
    public void mouseClicked(MouseEvent suKien) {
        if (dangTaiDanhSach) {
            return;
        }
        int dongDangChon = view.getTblBenhNhan().getSelectedRow();
        if (dongDangChon < 0) {
            return;
        }
        String maBenhNhan = view.getTblBenhNhan().getValueAt(dongDangChon, 0).toString();
        hienChiTiet(maBenhNhan);
    }

    @Override
    public void mousePressed(MouseEvent suKien) {
    }

    @Override
    public void mouseReleased(MouseEvent suKien) {
    }

    @Override
    public void mouseEntered(MouseEvent suKien) {
    }

    @Override
    public void mouseExited(MouseEvent suKien) {
    }

    private void docDuLieuVaoTable(String maCanChon) {
        soThuTuLanXemChiTiet++;
        view.hienChiTiet(null);
        if (dangTaiDanhSach) {
            coYeuCauTaiLaiDangCho = true;
            maCanChonKhiTaiLai = maCanChon;
            return;
        }
        taiDanhSach(maCanChon);
    }

    private void taiDanhSach(String maCanChon) {
        String tuKhoa = view.getTxtTimKiem().getText();
        String gioiTinh = view.layGioiTinhDangChon();
        dangTaiDanhSach = true;
        view.datDangTai(true);

        new SwingWorker<ArrayList<BenhNhan>, Void>() {
            @Override
            protected ArrayList<BenhNhan> doInBackground() throws Exception {
                return benhNhanService.timKiem(tuKhoa, gioiTinh);
            }

            @Override
            protected void done() {
                dangTaiDanhSach = false;
                if (coYeuCauTaiLaiDangCho) {
                    coYeuCauTaiLaiDangCho = false;
                    taiDanhSach(maCanChonKhiTaiLai);
                    return;
                }
                view.datDangTai(false);
                try {
                    ArrayList<BenhNhan> danhSachBenhNhan = get();
                    soThuTuLanXemChiTiet++;
                    view.docDuLieuVaoTable(danhSachBenhNhan);
                    if (maCanChon != null) {
                        view.chonDongTheoMa(maCanChon);
                        hienChiTiet(maCanChon);
                    }
                } catch (Exception loi) {
                    view.hienLoi(thongBaoLoi(loi));
                }
            }
        }.execute();
    }

    private void hienChiTiet(String maBenhNhan) {
        soThuTuLanXemChiTiet++;
        long lanXemNay = soThuTuLanXemChiTiet;
        view.hienChiTiet(null);

        new SwingWorker<BenhNhan, Void>() {
            @Override
            protected BenhNhan doInBackground() throws Exception {
                return benhNhanService.xemChiTiet(maBenhNhan);
            }

            @Override
            protected void done() {
                if (lanXemNay != soThuTuLanXemChiTiet) {
                    return;
                }
                try {
                    view.hienChiTiet(get());
                } catch (Exception loi) {
                    view.hienLoi(thongBaoLoi(loi));
                }
            }
        }.execute();
    }

    private void moForm(BenhNhan benhNhanCanSua) {
        if (benhNhanCanSua == null) {
            maDangSua = null;
        } else {
            maDangSua = benhNhanCanSua.getMaBenhNhan();
        }
        benhNhanLucMoForm = benhNhanCanSua;
        winBenhNhan = new BenhNhanDialog(SwingUtilities.getWindowAncestor(view), benhNhanCanSua);
        winBenhNhan.getBtnChonNgay().addActionListener(this);
        winBenhNhan.getBtnLuu().addActionListener(this);
        winBenhNhan.getBtnHuy().addActionListener(this);
        try {
            winBenhNhan.setVisible(true);
        } finally {
            winBenhNhan = null;
            benhNhanLucMoForm = null;
        }
    }

    private void luu() {
        if (winBenhNhan == null || dangLuu) {
            return;
        }
        dangLuu = true;
        BenhNhanDialog winDangLuu = winBenhNhan;
        String maBenhNhanDangSua = maDangSua;
        BenhNhan benhNhanLucMo = benhNhanLucMoForm;

        winDangLuu.boToDoTatCa();
        BenhNhan benhNhanTrenForm;
        try {
            benhNhanTrenForm = winDangLuu.layBenhNhanTuForm(maBenhNhanDangSua);
        } catch (RuntimeException loi) {
            dangLuu = false;
            baoLoiTrenForm(winDangLuu, loi);
            return;
        }
        winDangLuu.datDangLuu(true);

        new SwingWorker<ArrayList<BenhNhan>, Void>() {
            @Override
            protected ArrayList<BenhNhan> doInBackground() throws Exception {
                return benhNhanService.timNghiTrung(benhNhanTrenForm);
            }

            @Override
            protected void done() {
                if (!winDangLuu.isDisplayable()) {
                    dangLuu = false;
                    return;
                }
                ArrayList<BenhNhan> danhSachTrung;
                try {
                    danhSachTrung = get();
                } catch (Exception loi) {
                    ketThucLuu(winDangLuu);
                    baoLoiTrenForm(winDangLuu, loi);
                    return;
                }

                if (danhSachTrung.isEmpty()) {
                    luuTrongNen(winDangLuu, benhNhanTrenForm, maBenhNhanDangSua, benhNhanLucMo);
                    return;
                }

                boolean dangThemMoi = maBenhNhanDangSua == null;
                int luaChon = hoiKhiCoHoSoNghiTrung(winDangLuu, danhSachTrung, dangThemMoi);
                if (luaChon == CHON_VAN_LUU) {
                    luuTrongNen(winDangLuu, benhNhanTrenForm, maBenhNhanDangSua, benhNhanLucMo);
                } else if (luaChon == CHON_DUNG_HO_SO_CU) {
                    ketThucLuu(winDangLuu);
                    winDangLuu.dispose();
                    chonHoSoCu(maHoSoCuDaChon);
                } else {
                    ketThucLuu(winDangLuu);
                }
            }
        }.execute();
    }

    private void luuTrongNen(BenhNhanDialog winDangLuu, BenhNhan benhNhanTrenForm,
                             String maBenhNhanDangSua, BenhNhan benhNhanLucMo) {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                if (maBenhNhanDangSua == null) {
                    return benhNhanService.themBenhNhan(benhNhanTrenForm);
                }
                benhNhanService.capNhatBenhNhan(benhNhanTrenForm, benhNhanLucMo);
                return maBenhNhanDangSua;
            }

            @Override
            protected void done() {
                String maDaLuu;
                try {
                    maDaLuu = get();
                } catch (Exception loi) {
                    ketThucLuu(winDangLuu);
                    Throwable loiGoc = layLoiGoc(loi);
                    if (loiGoc instanceof DuLieuDaThayDoiException) {
                        JOptionPane.showMessageDialog(winDangLuu, loiGoc.getMessage(),
                                "Dữ liệu vừa bị thay đổi", JOptionPane.WARNING_MESSAGE);
                        winDangLuu.dispose();
                        docDuLieuVaoTable(maBenhNhanDangSua);
                        return;
                    }
                    if (winDangLuu.isDisplayable()) {
                        baoLoiTrenForm(winDangLuu, loi);
                    }
                    return;
                }

                ketThucLuu(winDangLuu);
                String thongBao;
                if (maBenhNhanDangSua == null) {
                    thongBao = "Đã thêm bệnh nhân " + maDaLuu + ".";
                } else {
                    thongBao = "Đã cập nhật bệnh nhân " + maDaLuu + ".";
                }
                JOptionPane.showMessageDialog(winDangLuu, thongBao);
                winDangLuu.dispose();
                docDuLieuVaoTable(maDaLuu);
            }
        }.execute();
    }

    private void ketThucLuu(BenhNhanDialog winDangLuu) {
        dangLuu = false;
        if (winDangLuu.isDisplayable()) {
            winDangLuu.datDangLuu(false);
        }
    }

    private void baoLoiTrenForm(BenhNhanDialog winDangLuu, Throwable loi) {
        Throwable loiGoc = layLoiGoc(loi);
        if (loiGoc instanceof LoiNhapLieuException) {
            LoiNhapLieuException loiNhapLieu = (LoiNhapLieuException) loiGoc;
            winDangLuu.toDoO(loiNhapLieu.getTenO());
        }
        winDangLuu.hienLoi(thongBaoLoi(loi));
    }

    private void chonHoSoCu(String maBenhNhan) {
        view.xoaTrang();
        view.getTxtTimKiem().setText(maBenhNhan);
        docDuLieuVaoTable(maBenhNhan);
    }

    private int hoiKhiCoHoSoNghiTrung(BenhNhanDialog winDangLuu, ArrayList<BenhNhan> danhSachTrung,
                                      boolean dangThemMoi) {
        DefaultListModel<String> modelHoSoTrung = new DefaultListModel<>();
        for (BenhNhan benhNhanTrung : danhSachTrung) {
            modelHoSoTrung.addElement(benhNhanTrung.getMaBenhNhan()
                    + " · " + benhNhanTrung.getHoTen()
                    + " · " + benhNhanTrung.getNgaySinh().format(DINH_DANG_NGAY)
                    + " · " + benhNhanTrung.getSoDienThoai());
        }
        JList<String> lstHoSoTrung = new JList<>(modelHoSoTrung);
        lstHoSoTrung.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstHoSoTrung.setSelectedIndex(0);
        JScrollPane scrHoSoTrung = new JScrollPane(lstHoSoTrung);
        scrHoSoTrung.setPreferredSize(new Dimension(460, 110));

        JLabel lblCanhBao = new JLabel("Đã có hồ sơ trùng họ tên, ngày sinh và số điện thoại:");
        JPanel pnlCanhBao = new JPanel(new BorderLayout(0, 8));
        pnlCanhBao.add(lblCanhBao, BorderLayout.NORTH);
        pnlCanhBao.add(scrHoSoTrung, BorderLayout.CENTER);

        String[] cacNut;
        if (dangThemMoi) {
            cacNut = new String[]{"Dùng hồ sơ này", "Vẫn tạo mới", "Quay lại"};
        } else {
            cacNut = new String[]{"Vẫn lưu", "Quay lại"};
        }
        int nutDaBam = JOptionPane.showOptionDialog(winDangLuu, pnlCanhBao, "Cảnh báo hồ sơ nghi trùng",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                cacNut, cacNut[cacNut.length - 1]);

        if (dangThemMoi) {
            if (nutDaBam == 0) {
                int dongDaChon = lstHoSoTrung.getSelectedIndex();
                if (dongDaChon < 0) {
                    dongDaChon = 0;
                }
                maHoSoCuDaChon = danhSachTrung.get(dongDaChon).getMaBenhNhan();
                return CHON_DUNG_HO_SO_CU;
            }
            if (nutDaBam == 1) {
                return CHON_VAN_LUU;
            }
            return CHON_QUAY_LAI;
        }
        if (nutDaBam == 0) {
            return CHON_VAN_LUU;
        }
        return CHON_QUAY_LAI;
    }

    private Throwable layLoiGoc(Throwable loi) {
        while (loi instanceof ExecutionException && loi.getCause() != null) {
            loi = loi.getCause();
        }
        return loi;
    }

    private String thongBaoLoi(Throwable loi) {
        Throwable loiGoc = layLoiGoc(loi);
        if (loiGoc instanceof AccessDeniedException) {
            return "Tài khoản không có quyền thực hiện thao tác này.";
        }
        if (loiGoc instanceof IllegalArgumentException) {
            return loiGoc.getMessage();
        }
        if (loiGoc instanceof DuLieuDaThayDoiException) {
            return loiGoc.getMessage();
        }
        if (loiGoc instanceof SQLTimeoutException) {
            return "SQL Server phản hồi quá lâu. Vui lòng thử lại.";
        }
        if (loiGoc instanceof SQLException) {
            return "Lỗi khi làm việc với SQL Server. Kiểm tra kết nối rồi thử lại.";
        }
        return "Có lỗi xảy ra, vui lòng thử lại.";
    }
}
