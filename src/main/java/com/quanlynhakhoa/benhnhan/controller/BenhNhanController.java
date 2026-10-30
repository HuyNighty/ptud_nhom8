package com.quanlynhakhoa.benhnhan.controller;

import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.benhnhan.service.BenhNhanService;
import com.quanlynhakhoa.benhnhan.view.BenhNhanDialog;
import com.quanlynhakhoa.benhnhan.view.BenhNhanPanel;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.Permission;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

// Controller: nhận sự kiện từ View → gọi Service → đưa kết quả về View
public class BenhNhanController implements ActionListener, MouseListener {
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private BenhNhanPanel view;
    private BenhNhanService service;
    private BenhNhanDialog winBenhNhan;   // form Thêm/Sửa đang mở (null nếu chưa mở)
    private String maDangSua;             // null = đang THÊM, khác null = đang SỬA bệnh nhân này
    private boolean dangTai;
    private RefreshRequest pendingRefresh;

    private record RefreshRequest(String tuKhoa, String gioiTinh, String maCanChon) {}
    private final BenhNhanUiState uiState = new BenhNhanUiState();

    public BenhNhanController(BenhNhanPanel view, BenhNhanService service) {
        this.view = view;
        this.service = service;

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
        view.datQuyen(service.coQuyen(Permission.PATIENT_CREATE), service.coQuyen(Permission.PATIENT_UPDATE));

        // Mở màn hình là đọc dữ liệu từ database vào bảng luôn
        docDuLieuVaoTable(null);
    }

    // Xử lý khi bấm nút hoặc nhấn Enter
    @Override
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        if (o.equals(view.getBtnLoc()) || o.equals(view.getTxtTimKiem())) {
            docDuLieuVaoTable(null);
        }
        if (o.equals(view.getBtnXoaLoc())) {
            view.xoaTrang();
            docDuLieuVaoTable(null);
        }
        if (o.equals(view.getBtnTaiLai())) {
            docDuLieuVaoTable(null);
        }
        if (o.equals(view.getBtnThem())) {
            moForm(null);                       // form trống
        }
        if (o.equals(view.getBtnSua()) && view.getBnDangXem() != null) {
            moForm(view.getBnDangXem());        // form có sẵn dữ liệu
        }
        // Các nút trên form Thêm/Sửa
        if (winBenhNhan != null && o.equals(winBenhNhan.getBtnChonNgay())) {
            winBenhNhan.moLichChonNgaySinh();
        }
        if (winBenhNhan != null && o.equals(winBenhNhan.getBtnLuu())) {
            luu();
        }
        if (winBenhNhan != null && o.equals(winBenhNhan.getBtnHuy())) {
            dongForm();
        }
    }

    // Click 1 dòng trong bảng → đọc lại bệnh nhân từ database → hiện ở khung bên phải
    @Override
    public void mouseClicked(MouseEvent e) {
        if (dangTai) return; // the displayed table belongs to the previous load
        int row = view.getTblBenhNhan().getSelectedRow();
        if (row < 0) {
            return;
        }
        String maBenhNhan = view.getTblBenhNhan().getValueAt(row, 0).toString();
        hienChiTiet(maBenhNhan);
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    // ===================== DANH SÁCH =====================

    // Lấy điều kiện lọc trên View → gọi Service → đổ kết quả vào bảng.
    // Dùng SwingWorker: đọc database ở LUỒNG NỀN (doInBackground) để app không bị đơ,
    // xong rồi mới cập nhật giao diện (done).
    // maCanChon: mã bệnh nhân cần chọn sẵn sau khi tải xong (null = không chọn)
    private void docDuLieuVaoTable(String maCanChon) {
        // Snapshot the latest request on EDT, including its requested selection.
        RefreshRequest request = new RefreshRequest(view.getTxtTimKiem().getText(),
                view.layGioiTinhDangChon(), maCanChon);
        uiState.nextDetailRequest();
        view.hienChiTiet(null);
        if (dangTai) {
            pendingRefresh = request;
            return;
        }
        taiDanhSach(request);
    }

    private void taiDanhSach(RefreshRequest request) {
        uiState.nextDetailRequest();
        dangTai = true;
        view.datDangTai(true);
        new SwingWorker<ArrayList<BenhNhan>, Void>() {
            @Override
            protected ArrayList<BenhNhan> doInBackground() throws Exception {
                return service.timKiem(request.tuKhoa(), request.gioiTinh());
            }

            @Override
            protected void done() {
                try {
                    ArrayList<BenhNhan> rows = get();
                    // A newer request supersedes both rows and selection from this load.
                    if (pendingRefresh == null) {
                        uiState.nextDetailRequest();
                        view.docDuLieuVaoTable(rows);
                        if (request.maCanChon() != null) {
                            view.chonDongTheoMa(request.maCanChon());
                            hienChiTiet(request.maCanChon());
                        }
                    }
                } catch (Exception ex) {
                    if (pendingRefresh == null) view.hienLoi(thongBaoLoi(ex));
                } finally {
                    // Drain pending work even when the active load failed.
                    dangTai = false;
                    RefreshRequest next = pendingRefresh;
                    pendingRefresh = null;
                    if (next != null) {
                        taiDanhSach(next);
                    } else {
                        view.datDangTai(false);
                    }
                }
            }
        }.execute();
    }

    // Detail requests are asynchronous; never render a response for an old selection.
    private void hienChiTiet(String maBenhNhan) {
        long request = uiState.nextDetailRequest();
        view.hienChiTiet(null);
        new SwingWorker<BenhNhan, Void>() {
            @Override
            protected BenhNhan doInBackground() throws Exception {
                return service.xemChiTiet(maBenhNhan);
            }

            @Override
            protected void done() {
                if (!uiState.isLatestDetail(request)) return;
                int row = view.getTblBenhNhan().getSelectedRow();
                if (row < 0 || !maBenhNhan.equals(
                        String.valueOf(view.getTblBenhNhan().getValueAt(row, 0)))) return;
                try {
                    view.hienChiTiet(get());
                } catch (Exception ex) {
                    view.hienLoi(thongBaoLoi(ex));
                }
            }
        }.execute();
    }

    // ===================== THÊM / SỬA =====================

    // Mở form: bnCanSua = null → Thêm; khác null → Sửa
    private void moForm(BenhNhan bnCanSua) {
        maDangSua = bnCanSua == null ? null : bnCanSua.getMaBenhNhan();
        winBenhNhan = new BenhNhanDialog(SwingUtilities.getWindowAncestor(view), bnCanSua);
        winBenhNhan.getBtnChonNgay().addActionListener(this);
        winBenhNhan.getBtnLuu().addActionListener(this);
        winBenhNhan.getBtnHuy().addActionListener(this);
        try {
            winBenhNhan.setVisible(true); // modal nested event loop still processes SwingWorker.done
        } finally {
            winBenhNhan = null;
        }
    }

    private void dongForm() {
        winBenhNhan.dispose();
    }

    // Snapshot Swing inputs on EDT, then check duplicates on a worker.
    private void luu() {
        if (winBenhNhan == null || !uiState.beginSave()) return;
        BenhNhanDialog dialog = winBenhNhan;
        final BenhNhan bn;
        final String maSua = maDangSua;
        try {
            bn = dialog.layBenhNhanTuForm(maSua);
        } catch (RuntimeException ex) {
            uiState.finishSave();
            dialog.hienLoi(thongBaoLoi(ex));
            return;
        }
        dialog.datDangLuu(true);
        new SwingWorker<ArrayList<BenhNhan>, Void>() {
            @Override protected ArrayList<BenhNhan> doInBackground() throws Exception {
                return service.timNghiTrung(bn);
            }
            @Override protected void done() {
                if (!dialog.isDisplayable()) { uiState.finishSave(); return; }
                try {
                    ArrayList<BenhNhan> dsTrung = get();
                    if (!dsTrung.isEmpty() && !xacNhanLuuKhiTrung(dsTrung)) {
                        ketThucLuu(dialog);
                        return;
                    }
                    luuTrongNen(dialog, bn, maSua);
                } catch (Exception ex) {
                    ketThucLuu(dialog);
                    dialog.hienLoi(thongBaoLoi(ex));
                }
            }
        }.execute();
    }

    // SQL write and audit execute only in doInBackground(), never on EDT.
    private void luuTrongNen(BenhNhanDialog dialog, BenhNhan bn, String maSua) {
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                if (maSua == null) return service.themBenhNhan(bn);
                service.capNhatBenhNhan(bn);
                return maSua;
            }
            @Override protected void done() {
                try {
                    String maDaLuu = get();
                    ketThucLuu(dialog);
                    JOptionPane.showMessageDialog(dialog,
                            (maSua == null ? "Đã thêm bệnh nhân " : "Đã cập nhật bệnh nhân ") + maDaLuu + ".");
                    dialog.dispose();
                    docDuLieuVaoTable(maDaLuu);
                } catch (Exception ex) {
                    ketThucLuu(dialog);
                    if (dialog.isDisplayable()) dialog.hienLoi(thongBaoLoi(ex));
                }
            }
        }.execute();
    }

    private void ketThucLuu(BenhNhanDialog dialog) {
        uiState.finishSave();
        if (dialog.isDisplayable()) dialog.datDangLuu(false);
    }

    // Hiện danh sách hồ sơ nghi trùng, trả về true nếu người dùng vẫn muốn lưu
    private boolean xacNhanLuuKhiTrung(ArrayList<BenhNhan> dsTrung) {
        StringBuilder noiDung = new StringBuilder("Có hồ sơ NGHI TRÙNG (cùng SĐT, hoặc cùng họ tên + ngày sinh):\n\n");
        for (BenhNhan t : dsTrung) {
            noiDung.append("• ").append(t.getMaBenhNhan())
                    .append(" · ").append(t.getHoTen())
                    .append(" · ").append(t.getNgaySinh().format(NGAY))
                    .append(" · ").append(t.getSoDienThoai())
                    .append("\n");
        }
        noiDung.append("\nVẫn lưu hồ sơ này?");
        int chon = JOptionPane.showConfirmDialog(winBenhNhan, noiDung.toString(),
                "Cảnh báo hồ sơ nghi trùng", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return chon == JOptionPane.YES_OPTION;
    }

    // Đổi lỗi thành câu dễ hiểu (không hiện câu SQL ra màn hình)
    private String thongBaoLoi(Throwable ex) {
        // SwingWorker exceptions may be wrapped more than once.
        while (ex instanceof ExecutionException && ex.getCause() != null) {
            ex = ex.getCause();
        }
        if (ex instanceof AccessDeniedException) {
            return "Tài khoản không có quyền thực hiện thao tác này.";
        }
        if (ex instanceof IllegalArgumentException) {
            return ex.getMessage();
        }
        if (ex instanceof SQLTimeoutException) {
            return "SQL Server phản hồi quá lâu. Vui lòng thử lại.";
        }
        if (ex instanceof SQLException) {
            return "Lỗi khi làm việc với SQL Server. Kiểm tra kết nối rồi thử lại.";
        }
        return "Có lỗi xảy ra, vui lòng thử lại.";
    }
}
