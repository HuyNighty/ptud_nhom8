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
    private boolean dangTai;              // true = đang đọc database, bỏ qua bấm thêm lần nữa

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
        if (dangTai) {
            return;                                    // đang tải rồi, không tải chồng
        }
        dangTai = true;
        view.datDangTai(true);                         // làm mờ nút + hiện "Đang tải..."

        // Đọc điều kiện lọc TRƯỚC (trên luồng giao diện)
        String tuKhoa = view.getTxtTimKiem().getText();
        String gioiTinh = view.layGioiTinhDangChon();

        new SwingWorker<ArrayList<BenhNhan>, Void>() {
            @Override
            protected ArrayList<BenhNhan> doInBackground() throws Exception {
                return service.timKiem(tuKhoa, gioiTinh);   // chạy ở luồng nền
            }

            @Override
            protected void done() {                          // chạy lại trên luồng giao diện
                dangTai = false;
                view.datDangTai(false);
                try {
                    view.docDuLieuVaoTable(get());
                    if (maCanChon != null) {
                        view.chonDongTheoMa(maCanChon);
                        hienChiTiet(maCanChon);
                    }
                } catch (Exception ex) {
                    view.hienLoi(thongBaoLoi(ex));
                }
            }
        }.execute();
    }

    // Đọc lại 1 bệnh nhân từ database (luồng nền) rồi hiện ở khung bên phải
    private void hienChiTiet(String maBenhNhan) {
        new SwingWorker<BenhNhan, Void>() {
            @Override
            protected BenhNhan doInBackground() throws Exception {
                return service.xemChiTiet(maBenhNhan);
            }

            @Override
            protected void done() {
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
        winBenhNhan.setVisible(true);   // form modal: dòng này chờ tới khi form đóng
        winBenhNhan = null;
    }

    private void dongForm() {
        winBenhNhan.dispose();
    }

    // Bấm Lưu trên form
    private void luu() {
        try {
            // 1. Đọc dữ liệu trên form
            BenhNhan bn = winBenhNhan.layBenhNhanTuForm(maDangSua);

            // 2. Kiểm tra nghi trùng → hỏi người dùng có muốn lưu tiếp không
            ArrayList<BenhNhan> dsTrung = service.timNghiTrung(bn);
            if (!dsTrung.isEmpty() && !xacNhanLuuKhiTrung(dsTrung)) {
                return;   // người dùng chọn "Không" → giữ form để sửa lại
            }

            // 3. Lưu vào database
            String maDaLuu;
            if (maDangSua == null) {
                maDaLuu = service.themBenhNhan(bn);
                JOptionPane.showMessageDialog(winBenhNhan, "Đã thêm bệnh nhân " + maDaLuu + ".");
            } else {
                service.capNhatBenhNhan(bn);
                maDaLuu = maDangSua;
                JOptionPane.showMessageDialog(winBenhNhan, "Đã cập nhật bệnh nhân " + maDaLuu + ".");
            }

            // 4. Đóng form, tải lại bảng; tải xong sẽ tự chọn đúng bệnh nhân vừa lưu
            dongForm();
            docDuLieuVaoTable(maDaLuu);
        } catch (Exception ex) {
            winBenhNhan.hienLoi(thongBaoLoi(ex));   // hiện hộp thông báo lỗi, form vẫn mở để sửa lại
        }
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
        // Lỗi từ SwingWorker bị bọc trong ExecutionException → lấy lỗi gốc bên trong
        if (ex instanceof ExecutionException && ex.getCause() != null) {
            ex = ex.getCause();
        }
        if (ex instanceof AccessDeniedException) {
            return "Tài khoản không có quyền thực hiện thao tác này.";
        }
        if (ex instanceof IllegalArgumentException) {
            return ex.getMessage();
        }
        if (ex instanceof SQLException) {
            return "Lỗi khi làm việc với SQL Server. Kiểm tra kết nối rồi thử lại.";
        }
        return "Có lỗi xảy ra, vui lòng thử lại.";
    }
}
