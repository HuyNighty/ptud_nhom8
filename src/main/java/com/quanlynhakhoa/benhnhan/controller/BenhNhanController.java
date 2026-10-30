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

// Controller màn hình Bệnh nhân (UC003): nhận sự kiện từ View → gọi Service → đưa kết quả về View.
// Mọi việc đọc / ghi database đều chạy bằng SwingWorker (luồng nền) để app không bị đơ:
//   doInBackground() chạy ở luồng nền (gọi Service), done() chạy lại trên giao diện (cập nhật View).
public class BenhNhanController implements ActionListener, MouseListener {
    private static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // 3 lựa chọn khi có hồ sơ nghi trùng (UC003 A2)
    private static final int CHON_QUAY_LAI = 0;        // quay lại form để sửa tiếp
    private static final int CHON_VAN_LUU = 1;         // "Vẫn tạo mới" (hoặc "Vẫn lưu" khi đang sửa)
    private static final int CHON_DUNG_HO_SO_CU = 2;   // "Dùng hồ sơ này": không tạo mới

    private BenhNhanPanel view;
    private BenhNhanService benhNhanService;

    // ----- Form Thêm / Sửa -----
    private BenhNhanDialog winBenhNhan;        // form Thêm/Sửa đang mở (null nếu chưa mở)
    private String maDangSua;                  // null = đang THÊM, khác null = đang SỬA bệnh nhân này
    private BenhNhan benhNhanLucMoForm;        // dữ liệu lúc mở form Sửa (để kiểm tra BR10 khi lưu)
    private boolean dangLuu;                   // đang lưu → bỏ qua bấm Lưu thêm lần nữa
    private String maHoSoCuDaChon;             // mã hồ sơ cũ lễ tân chọn ở hộp thoại nghi trùng

    // ----- Danh sách -----
    private boolean dangTaiDanhSach;           // đang đọc danh sách từ database
    private boolean coYeuCauTaiLaiDangCho;     // trong lúc đang tải lại có người bấm Tìm/Làm mới nữa
    private String maCanChonKhiTaiLai;         // mã cần chọn sẵn sau lần tải đang chờ đó

    // ----- Khung chi tiết -----
    // Mỗi lần xem chi tiết tăng số này lên 1. Kết quả trả về mà không phải lần mới nhất → bỏ qua
    // (tránh trường hợp click nhanh 2 dòng, kết quả của dòng cũ về sau đè lên dòng mới).
    private long soThuTuLanXemChiTiet;

    public BenhNhanController(BenhNhanPanel view, BenhNhanService benhNhanService) {
        this.view = view;
        this.benhNhanService = benhNhanService;

        // Gắn sự kiện cho các nút và ô tìm kiếm (nhấn Enter)
        view.getBtnLoc().addActionListener(this);
        view.getBtnXoaLoc().addActionListener(this);
        view.getBtnTaiLai().addActionListener(this);
        view.getTxtTimKiem().addActionListener(this);
        view.getBtnThem().addActionListener(this);
        view.getBtnSua().addActionListener(this);
        // Gắn sự kiện click chuột vào bảng
        view.getTblBenhNhan().addMouseListener(this);

        // Không có quyền thêm/sửa thì làm mờ nút (Service vẫn kiểm tra lại khi lưu)
        boolean duocThem = benhNhanService.coQuyen(Permission.PATIENT_CREATE);
        boolean duocSua = benhNhanService.coQuyen(Permission.PATIENT_UPDATE);
        view.datQuyen(duocThem, duocSua);

        // Mở màn hình là đọc dữ liệu từ database vào bảng luôn
        docDuLieuVaoTable(null);
    }

    // Xử lý khi bấm nút hoặc nhấn Enter
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
            moForm(null);                                   // form trống
        }
        if (nguonSuKien.equals(view.getBtnSua()) && view.getBenhNhanDangXem() != null) {
            moForm(view.getBenhNhanDangXem());              // form có sẵn dữ liệu
        }
        // Các nút trên form Thêm/Sửa
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

    // Click 1 dòng trong bảng → đọc lại bệnh nhân từ database → hiện ở khung bên phải
    @Override
    public void mouseClicked(MouseEvent suKien) {
        if (dangTaiDanhSach) {
            return;   // bảng đang hiện là danh sách cũ, chờ tải xong
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

    // ===================== DANH SÁCH =====================

    // Đọc danh sách theo điều kiện lọc đang có trên View rồi đổ vào bảng.
    // maCanChon: mã bệnh nhân cần chọn sẵn sau khi tải xong (null = không chọn)
    private void docDuLieuVaoTable(String maCanChon) {
        soThuTuLanXemChiTiet++;          // bỏ kết quả xem chi tiết đang chờ (nếu có)
        view.hienChiTiet(null);
        if (dangTaiDanhSach) {
            // Đang tải dở → ghi nhớ lại, tải xong lần này thì tải tiếp lần nữa với điều kiện mới nhất
            coYeuCauTaiLaiDangCho = true;
            maCanChonKhiTaiLai = maCanChon;
            return;
        }
        taiDanhSach(maCanChon);
    }

    private void taiDanhSach(String maCanChon) {
        // Lấy điều kiện lọc NGAY BÂY GIỜ (trên giao diện), luồng nền chỉ dùng 2 biến này
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
                    // Có yêu cầu mới hơn → bỏ kết quả này, tải lại luôn
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

    // Đọc chi tiết 1 bệnh nhân rồi hiện ở khung bên phải
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
                    return;   // đã có lần xem mới hơn → bỏ kết quả cũ
                }
                try {
                    view.hienChiTiet(get());
                } catch (Exception loi) {
                    view.hienLoi(thongBaoLoi(loi));
                }
            }
        }.execute();
    }

    // ===================== THÊM / SỬA =====================

    // Mở form: benhNhanCanSua = null → Thêm; khác null → Sửa
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
            // Form là modal: dòng này chờ tới khi form đóng (SwingWorker vẫn chạy bình thường)
            winBenhNhan.setVisible(true);
        } finally {
            winBenhNhan = null;
            benhNhanLucMoForm = null;
        }
    }

    // Bấm Lưu: đọc form → kiểm tra nghi trùng (luồng nền) → hỏi lễ tân nếu trùng → lưu (luồng nền)
    private void luu() {
        if (winBenhNhan == null || dangLuu) {
            return;
        }
        dangLuu = true;
        BenhNhanDialog winDangLuu = winBenhNhan;
        String maBenhNhanDangSua = maDangSua;
        BenhNhan benhNhanLucMo = benhNhanLucMoForm;

        winDangLuu.boToDoTatCa();     // xóa viền đỏ của lần lưu trước
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
                    return;   // form đã bị đóng
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
                    // Không tạo mới: đóng form, mở hồ sơ cũ trong danh sách
                    ketThucLuu(winDangLuu);
                    winDangLuu.dispose();
                    chonHoSoCu(maHoSoCuDaChon);
                } else {
                    ketThucLuu(winDangLuu);   // quay lại form
                }
            }
        }.execute();
    }

    // Ghi vào database ở luồng nền (không bao giờ ghi trên luồng giao diện)
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
                        // BR10 / UC003 E2: báo, tải lại dữ liệu mới nhất và không lưu
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

    // Báo lỗi trên form: lỗi nhập ở 1 ô thì tô đỏ ô đó (UC003 E1), rồi hiện hộp thông báo
    private void baoLoiTrenForm(BenhNhanDialog winDangLuu, Throwable loi) {
        Throwable loiGoc = layLoiGoc(loi);
        if (loiGoc instanceof LoiNhapLieuException) {
            LoiNhapLieuException loiNhapLieu = (LoiNhapLieuException) loiGoc;
            winDangLuu.toDoO(loiNhapLieu.getTenO());
        }
        winDangLuu.hienLoi(thongBaoLoi(loi));
    }

    // Lễ tân chọn "Dùng hồ sơ này": hiện đúng hồ sơ cũ đó trong danh sách
    private void chonHoSoCu(String maBenhNhan) {
        view.xoaTrang();
        view.getTxtTimKiem().setText(maBenhNhan);
        docDuLieuVaoTable(maBenhNhan);
    }

    // Hộp thoại cảnh báo hồ sơ nghi trùng (BR01, UC003 A2).
    //   Đang THÊM: "Dùng hồ sơ này" / "Vẫn tạo mới" / "Quay lại"
    //   Đang SỬA : "Vẫn lưu" / "Quay lại"
    private int hoiKhiCoHoSoNghiTrung(BenhNhanDialog winDangLuu, ArrayList<BenhNhan> danhSachTrung,
                                      boolean dangThemMoi) {
        // Danh sách các hồ sơ nghi trùng, chọn sẵn dòng đầu tiên
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

    // SwingWorker bọc lỗi thật bên trong ExecutionException → lấy lỗi thật ra
    private Throwable layLoiGoc(Throwable loi) {
        while (loi instanceof ExecutionException && loi.getCause() != null) {
            loi = loi.getCause();
        }
        return loi;
    }

    // Đổi lỗi thành câu dễ hiểu (không hiện câu SQL ra màn hình)
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
