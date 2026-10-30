package com.quanlynhakhoa.lichhen.controller;

import com.quanlynhakhoa.benhnhan.hopdong.BenhNhanTomTat;
import com.quanlynhakhoa.benhnhan.service.DuLieuDaThayDoiException;
import com.quanlynhakhoa.benhnhan.view.ChonNgayDialog;
import com.quanlynhakhoa.lichhen.model.BacSi;
import com.quanlynhakhoa.lichhen.model.LichHen;
import com.quanlynhakhoa.lichhen.service.LichHenService;
import com.quanlynhakhoa.lichhen.view.LichHenDialog;
import com.quanlynhakhoa.lichhen.view.LichHenPanel;
import com.quanlynhakhoa.security.AccessDeniedException;
import com.quanlynhakhoa.security.Permission;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

// Controller màn hình Lịch hẹn (UC004 đặt / đổi / hủy, UC005 xác nhận đến): nhận sự kiện từ View → gọi Service → đưa kết quả về View.
// Mọi việc đọc / ghi database đều chạy bằng SwingWorker (luồng nền) để app không bị đơ:
//   doInBackground() chạy ở luồng nền (gọi Service), done() chạy lại trên giao diện (cập nhật View).
public class LichHenController implements ActionListener, MouseListener {
    private LichHenPanel view;
    private LichHenService lichHenService;

    private LichHenDialog winLichHen;   // form Đặt / Đổi lịch đang mở (null nếu chưa mở)
    private boolean dangTaiDanhSach;    // đang đọc danh sách → bỏ qua bấm thêm
    private boolean dangXuLyForm;       // form đang đọc / ghi database → bỏ qua bấm thêm

    public LichHenController(LichHenPanel view, LichHenService lichHenService) {
        this.view = view;
        this.lichHenService = lichHenService;

        // Gắn sự kiện cho các nút và ô tìm kiếm (nhấn Enter)
        view.getBtnChonNgayXem().addActionListener(this);
        view.getBtnHomNay().addActionListener(this);
        view.getTxtTimKiem().addActionListener(this);
        view.getBtnTimKiem().addActionListener(this);
        view.getBtnDatLich().addActionListener(this);
        view.getBtnXacNhanDen().addActionListener(this);
        view.getBtnDoiLich().addActionListener(this);
        view.getBtnHuyLich().addActionListener(this);
        // Click 1 dòng trong bảng → hiện lịch hẹn đó ở pnlWest
        view.getTblLichHen().addMouseListener(this);

        // Không có quyền thì làm mờ nút (Service vẫn kiểm tra lại)
        boolean duocQuanLyLich = lichHenService.coQuyen(Permission.APPOINTMENT_MANAGE);
        boolean duocXacNhanDen = lichHenService.coQuyen(Permission.PATIENT_CHECK_IN);
        view.datQuyen(duocQuanLyLich, duocXacNhanDen);

        // Mở màn hình là hiện lịch hẹn hôm nay
        taiDanhSach(null);
    }

    @Override
    public void actionPerformed(ActionEvent suKien) {
        Object nguonSuKien = suKien.getSource();

        // ----- Màn hình danh sách -----
        if (nguonSuKien.equals(view.getBtnTimKiem()) || nguonSuKien.equals(view.getTxtTimKiem())) {
            taiDanhSach(null);
        }
        if (nguonSuKien.equals(view.getBtnHomNay())) {
            view.datNgayDangXem(LocalDate.now());
            taiDanhSach(null);
        }
        if (nguonSuKien.equals(view.getBtnChonNgayXem())) {
            chonNgayXem();
        }
        if (nguonSuKien.equals(view.getBtnDatLich())) {
            moForm(null);
        }
        if (nguonSuKien.equals(view.getBtnXacNhanDen())) {
            xacNhanDen();
        }
        if (nguonSuKien.equals(view.getBtnDoiLich())) {
            LichHen lichHen = layLichHenDaDatDangChon("đổi");
            if (lichHen != null) {
                moForm(lichHen);
            }
        }
        if (nguonSuKien.equals(view.getBtnHuyLich())) {
            LichHen lichHen = layLichHenDaDatDangChon("hủy");
            if (lichHen != null) {
                huyLich(lichHen);
            }
        }

        // ----- Form Đặt / Đổi lịch -----
        if (winLichHen == null) {
            return;
        }
        if (nguonSuKien.equals(winLichHen.getBtnTimBenhNhan()) || nguonSuKien.equals(winLichHen.getTxtTimBenhNhan())) {
            timBenhNhan();
        }
        if (nguonSuKien.equals(winLichHen.getCmbBacSi())) {
            winLichHen.xoaGioTrong();          // đổi bác sĩ → giờ trống cũ không còn đúng
        }
        if (nguonSuKien.equals(winLichHen.getBtnChonNgay())) {
            winLichHen.moLichChonNgayHen();
        }
        if (nguonSuKien.equals(winLichHen.getBtnXemGioTrong())) {
            xemGioTrong();
        }
        if (nguonSuKien.equals(winLichHen.getBtnLuu())) {
            luu();
        }
        if (nguonSuKien.equals(winLichHen.getBtnHuy())) {
            winLichHen.dispose();
        }
    }

    // Click 1 dòng trong bảng → hiện ở pnlWest, bật / tắt nút theo trạng thái của lịch hẹn đó
    @Override
    public void mouseClicked(MouseEvent suKien) {
        view.hienChiTiet(view.layLichHenDangChon());
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

    // Đọc lịch hẹn của ngày đang xem rồi đổ vào bảng.
    // maCanChon: mã lịch hẹn cần chọn sẵn sau khi tải xong (null = không chọn)
    private void taiDanhSach(String maCanChon) {
        if (dangTaiDanhSach) {
            return;
        }
        dangTaiDanhSach = true;
        // Lấy điều kiện lọc NGAY BÂY GIỜ (trên giao diện), luồng nền chỉ dùng 2 biến này
        LocalDate ngay = view.getNgayDangXem();
        String tuKhoa = view.getTxtTimKiem().getText();
        view.datDangTai(true);

        new SwingWorker<ArrayList<LichHen>, Void>() {
            @Override
            protected ArrayList<LichHen> doInBackground() throws Exception {
                return lichHenService.layDanhSachLichHen(ngay, tuKhoa);
            }

            @Override
            protected void done() {
                dangTaiDanhSach = false;
                view.datDangTai(false);
                try {
                    view.hienDanhSachLichHen(get());
                    if (maCanChon != null) {
                        view.chonDongTheoMa(maCanChon);
                    }
                } catch (Exception loi) {
                    view.hienLoi(thongBaoLoi(loi));
                }
            }
        }.execute();
    }

    // Bấm "Chọn..." ở ô ngày: xem được từ 5 năm trước tới 1 năm sau
    private void chonNgayXem() {
        LocalDate homNay = LocalDate.now();
        LocalDate ngayDaChon = ChonNgayDialog.chon(SwingUtilities.getWindowAncestor(view), "Chọn ngày xem lịch hẹn",
                view.getNgayDangXem(), homNay.minusYears(5), homNay.plusYears(LichHenService.SO_NAM_DAT_TRUOC_TOI_DA));
        if (ngayDaChon != null) {
            view.datNgayDangXem(ngayDaChon);
            taiDanhSach(null);
        }
    }

    // Lịch hẹn đang chọn trên bảng, phải ở trạng thái "Đã đặt" (BR03). Không hợp lệ → báo lỗi, trả về null.
    private LichHen layLichHenDaDatDangChon(String viecCanLam) {
        LichHen lichHen = view.layLichHenDangChon();
        if (lichHen == null) {
            view.hienLoi("Chọn 1 lịch hẹn trong bảng để " + viecCanLam + ".");
            return null;
        }
        if (!lichHen.laDaDat()) {
            view.hienLoi("Lịch hẹn " + lichHen.getMaLichHen() + " đang ở trạng thái \""
                    + lichHen.getTrangThaiHienThi() + "\". Chỉ lịch \"Đã đặt\" mới được " + viecCanLam + ".");
            return null;
        }
        return lichHen;
    }

    // ===================== XÁC NHẬN ĐẾN (UC005) =====================

    // Bệnh nhân có lịch hẹn hôm nay đã tới: tạo lượt khám "Chờ khám" + lịch hẹn chuyển "Đã đến"
    private void xacNhanDen() {
        LichHen lichHen = view.layLichHenDangChon();
        if (lichHen == null) {
            view.hienLoi("Chọn 1 lịch hẹn trong bảng để xác nhận đến.");
            return;
        }
        if (!lichHen.duocXacNhanDen()) {
            view.hienLoi("Chỉ xác nhận đến cho lịch hẹn \"Đã đặt\" của hôm nay.");
            return;
        }
        String cauHoi = "Xác nhận bệnh nhân " + lichHen.getTenBenhNhan() + " đã đến?\n"
                + "Hệ thống sẽ tạo lượt khám \"Chờ khám\" với bác sĩ " + lichHen.getTenBacSi() + ".";
        int luaChon = JOptionPane.showConfirmDialog(view, cauHoi, "Xác nhận đến", JOptionPane.YES_NO_OPTION);
        if (luaChon != JOptionPane.YES_OPTION) {
            return;
        }
        String maLichHen = lichHen.getMaLichHen();
        String tenBacSi = lichHen.getTenBacSi();
        view.datDangTai(true);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return lichHenService.xacNhanDen(maLichHen);
            }

            @Override
            protected void done() {
                view.datDangTai(false);
                try {
                    String maPhieuKham = get();
                    JOptionPane.showMessageDialog(view, "Đã tạo lượt khám " + maPhieuKham
                            + " (Chờ khám). Bệnh nhân vào danh sách chờ của bác sĩ " + tenBacSi + ".");
                } catch (Exception loi) {
                    view.hienLoi(thongBaoLoi(loi));
                }
                taiDanhSach(maLichHen);   // thành công hay lỗi đều tải lại để thấy trạng thái mới nhất
            }
        }.execute();
    }

    // ===================== HỦY LỊCH (UC004 A2) =====================

    private void huyLich(LichHen lichHen) {
        String cauHoi = "Hủy lịch hẹn " + lichHen.getMaLichHen() + " của " + lichHen.getTenBenhNhan()
                + "?\nNhập lý do hủy (bắt buộc):";
        String lyDoHuy = JOptionPane.showInputDialog(view, cauHoi, "Hủy lịch hẹn", JOptionPane.QUESTION_MESSAGE);
        if (lyDoHuy == null) {
            return;   // bấm Cancel
        }
        String maLichHen = lichHen.getMaLichHen();
        view.datDangTai(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                lichHenService.huyLich(maLichHen, lyDoHuy);
                return null;
            }

            @Override
            protected void done() {
                view.datDangTai(false);
                try {
                    get();
                    JOptionPane.showMessageDialog(view, "Đã hủy lịch hẹn " + maLichHen + ".");
                } catch (Exception loi) {
                    view.hienLoi(thongBaoLoi(loi));
                }
                taiDanhSach(maLichHen);   // thành công hay lỗi đều tải lại để thấy trạng thái mới nhất
            }
        }.execute();
    }

    // ===================== FORM ĐẶT / ĐỔI LỊCH =====================

    // lichHenCanDoi = null → Đặt lịch; khác null → Đổi lịch
    private void moForm(LichHen lichHenCanDoi) {
        winLichHen = new LichHenDialog(SwingUtilities.getWindowAncestor(view), lichHenCanDoi);
        winLichHen.getTxtTimBenhNhan().addActionListener(this);
        winLichHen.getBtnTimBenhNhan().addActionListener(this);
        winLichHen.getBtnChonNgay().addActionListener(this);
        winLichHen.getBtnXemGioTrong().addActionListener(this);
        winLichHen.getBtnLuu().addActionListener(this);
        winLichHen.getBtnHuy().addActionListener(this);

        // Đọc danh sách bác sĩ (luồng nền); đổi lịch thì chọn sẵn bác sĩ cũ
        String maBacSiChonSan = null;
        if (lichHenCanDoi != null) {
            maBacSiChonSan = lichHenCanDoi.getMaBacSi();
        }
        taiDanhSachBacSi(winLichHen, maBacSiChonSan);

        try {
            // Form là modal: dòng này chờ tới khi form đóng (SwingWorker vẫn chạy bình thường)
            winLichHen.setVisible(true);
        } finally {
            winLichHen = null;
            dangXuLyForm = false;
        }
    }

    private void taiDanhSachBacSi(LichHenDialog winDangMo, String maBacSiChonSan) {
        dangXuLyForm = true;
        winDangMo.datDangXuLy(true);

        new SwingWorker<ArrayList<BacSi>, Void>() {
            @Override
            protected ArrayList<BacSi> doInBackground() throws Exception {
                return lichHenService.layDanhSachBacSi();
            }

            @Override
            protected void done() {
                dangXuLyForm = false;
                if (!winDangMo.isDisplayable()) {
                    return;   // form đã đóng
                }
                winDangMo.datDangXuLy(false);
                try {
                    winDangMo.hienDanhSachBacSi(get(), maBacSiChonSan);
                } catch (Exception loi) {
                    winDangMo.hienLoi(thongBaoLoi(loi));
                }
                // Gắn sự kiện đổi bác sĩ SAU khi đổ danh sách, để việc đổ dữ liệu không xóa giờ trống
                winDangMo.getCmbBacSi().addActionListener(LichHenController.this);
            }
        }.execute();
    }

    // UC004 bước 1: tìm bệnh nhân theo mã / tên / SĐT
    private void timBenhNhan() {
        if (dangXuLyForm) {
            return;
        }
        dangXuLyForm = true;
        LichHenDialog winDangMo = winLichHen;
        String tuKhoa = winDangMo.getTxtTimBenhNhan().getText();
        winDangMo.datDangXuLy(true);

        new SwingWorker<ArrayList<BenhNhanTomTat>, Void>() {
            @Override
            protected ArrayList<BenhNhanTomTat> doInBackground() throws Exception {
                return lichHenService.timBenhNhan(tuKhoa);
            }

            @Override
            protected void done() {
                dangXuLyForm = false;
                if (!winDangMo.isDisplayable()) {
                    return;
                }
                winDangMo.datDangXuLy(false);
                try {
                    winDangMo.hienDanhSachBenhNhan(get());
                } catch (Exception loi) {
                    winDangMo.hienLoi(thongBaoLoi(loi));
                }
            }
        }.execute();
    }

    // UC004 bước 2–3: chọn bác sĩ + ngày → hiện các giờ còn trống trong ca
    private void xemGioTrong() {
        if (dangXuLyForm) {
            return;
        }
        dangXuLyForm = true;
        LichHenDialog winDangMo = winLichHen;
        String maBacSi = winDangMo.layMaBacSiDaChon();
        LocalDate ngayHen = winDangMo.layNgayHen();
        String maLichHenDangDoi = null;
        if (winDangMo.getLichHenCanDoi() != null) {
            maLichHenDangDoi = winDangMo.getLichHenCanDoi().getMaLichHen();
        }
        String maBoQua = maLichHenDangDoi;
        winDangMo.datDangXuLy(true);

        new SwingWorker<ArrayList<LocalTime>, Void>() {
            @Override
            protected ArrayList<LocalTime> doInBackground() throws Exception {
                return lichHenService.layGioTrong(maBacSi, ngayHen, maBoQua);
            }

            @Override
            protected void done() {
                dangXuLyForm = false;
                if (!winDangMo.isDisplayable()) {
                    return;
                }
                winDangMo.datDangXuLy(false);
                try {
                    winDangMo.hienGioTrong(get());
                } catch (Exception loi) {
                    winDangMo.hienLoi(thongBaoLoi(loi));
                }
            }
        }.execute();
    }

    // UC004 bước 4–6 (đặt) hoặc A1 (đổi): bấm Lưu
    private void luu() {
        if (dangXuLyForm) {
            return;
        }
        dangXuLyForm = true;
        LichHenDialog winDangMo = winLichHen;

        // Lấy dữ liệu trên form NGAY BÂY GIỜ, luồng nền chỉ dùng các biến này
        LichHen lichHenCanDoi = winDangMo.getLichHenCanDoi();
        BenhNhanTomTat benhNhan = winDangMo.layBenhNhanDaChon();
        String maBenhNhan = null;
        if (benhNhan != null) {
            maBenhNhan = benhNhan.getMaBenhNhan();
        }
        String maBenhNhanDaChon = maBenhNhan;
        String maBacSi = winDangMo.layMaBacSiDaChon();
        LocalDateTime thoiGianHen = winDangMo.layThoiGianHen();
        String lyDo = winDangMo.layLyDo();
        winDangMo.datDangXuLy(true);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                if (lichHenCanDoi == null) {
                    return lichHenService.datLich(maBenhNhanDaChon, maBacSi, thoiGianHen, lyDo);
                }
                return lichHenService.doiLich(lichHenCanDoi.getMaLichHen(), maBacSi, thoiGianHen, lyDo);
            }

            @Override
            protected void done() {
                dangXuLyForm = false;
                if (winDangMo.isDisplayable()) {
                    winDangMo.datDangXuLy(false);
                }

                String maLichHenMoi;
                try {
                    maLichHenMoi = get();
                } catch (Exception loi) {
                    winDangMo.hienLoi(thongBaoLoi(loi));
                    Throwable loiGoc = layLoiGoc(loi);
                    if (loiGoc instanceof DuLieuDaThayDoiException) {
                        // UC004 E1: khung giờ không còn trống → tải lại giờ trống; danh sách phía sau cũng tải lại
                        if (winDangMo.isDisplayable()) {
                            xemGioTrong();
                        }
                        taiDanhSach(null);
                    }
                    return;
                }

                String thongBao;
                if (lichHenCanDoi == null) {
                    thongBao = "Đã đặt lịch hẹn " + maLichHenMoi + ".";
                } else {
                    thongBao = "Đã đổi lịch " + lichHenCanDoi.getMaLichHen() + " sang lịch mới " + maLichHenMoi + ".";
                }
                JOptionPane.showMessageDialog(winDangMo, thongBao);
                winDangMo.dispose();

                // Chuyển danh sách sang ngày của lịch vừa lưu và chọn sẵn dòng đó
                view.datNgayDangXem(thoiGianHen.toLocalDate());
                taiDanhSach(maLichHenMoi);
            }
        }.execute();
    }

    // ===================== Xử lý lỗi =====================

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
            return "Lỗi khi làm việc với SQL Server. Kiểm tra kết nối và quyền của tài khoản database rồi thử lại.";
        }
        return "Có lỗi xảy ra, vui lòng thử lại.";
    }
}
