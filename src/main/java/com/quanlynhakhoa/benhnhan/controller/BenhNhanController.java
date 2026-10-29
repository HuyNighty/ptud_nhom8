package com.quanlynhakhoa.benhnhan.controller;

import com.quanlynhakhoa.benhnhan.dao.BenhNhanDao;
import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.benhnhan.model.GioiTinh;
import com.quanlynhakhoa.benhnhan.service.BenhNhanService;
import com.quanlynhakhoa.benhnhan.view.BenhNhanPanel;
import com.quanlynhakhoa.security.AccessDeniedException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

/**
 * Điều phối màn hình Bệnh nhân:
 * Panel (sự kiện) → Controller → Service (chạy nền bằng SwingWorker) → Controller → Panel (hiển thị).
 * Controller không viết SQL và không tự kiểm tra quyền (Service làm việc đó).
 */
public final class BenhNhanController {
    private final BenhNhanService benhNhanService;
    private BenhNhanPanel panel;
    private boolean dangTai;

    public BenhNhanController(BenhNhanService benhNhanService) {
        this.benhNhanService = Objects.requireNonNull(benhNhanService, "benhNhanService");
    }

    /** Panel gọi một lần ngay sau khi dựng xong giao diện. */
    public void ganPanel(BenhNhanPanel panel) {
        this.panel = Objects.requireNonNull(panel, "panel");
    }

    public BenhNhanService service() {
        return benhNhanService;
    }

    /** Nút Lọc / Tải lại / Enter trong ô tìm kiếm. */
    public void taiDanhSach(String tuKhoa, GioiTinh gioiTinh) {
        if (dangTai) return;
        dangTai = true;
        panel.hienDangTai(true, "Đang tải danh sách bệnh nhân…");
        new SwingWorker<BenhNhanDao.KetQuaTimKiem, Void>() {
            @Override
            protected BenhNhanDao.KetQuaTimKiem doInBackground() throws Exception {
                return benhNhanService.timKiem(tuKhoa, gioiTinh);
            }

            @Override
            protected void done() {
                dangTai = false;
                panel.hienDangTai(false, null);
                try {
                    panel.hienDanhSach(get());
                } catch (Exception loi) {
                    panel.hienLoi(thongDiepLoi(loi));
                }
            }
        }.execute();
    }

    /** Chọn một dòng trong bảng → đọc lại chi tiết từ database. */
    public void chonBenhNhan(String maBenhNhan) {
        if (maBenhNhan == null) {
            panel.hienChiTiet(null);
            return;
        }
        new SwingWorker<Optional<BenhNhan>, Void>() {
            @Override
            protected Optional<BenhNhan> doInBackground() throws Exception {
                return benhNhanService.xemChiTiet(maBenhNhan);
            }

            @Override
            protected void done() {
                try {
                    Optional<BenhNhan> ketQua = get();
                    if (ketQua.isEmpty()) {
                        panel.hienLoi("Không tìm thấy bệnh nhân " + maBenhNhan + ".");
                    }
                    panel.hienChiTiet(ketQua.orElse(null));
                } catch (Exception loi) {
                    panel.hienLoi(thongDiepLoi(loi));
                }
            }
        }.execute();
    }

    /** Đổi lỗi kỹ thuật thành câu tiếng Việt; không hiển thị câu SQL hay mật khẩu. */
    public static String thongDiepLoi(Throwable loi) {
        Throwable goc = loi;
        if (goc instanceof ExecutionException && goc.getCause() != null) goc = goc.getCause();
        if (goc instanceof AccessDeniedException) {
            return "Tài khoản không có quyền thực hiện thao tác này.";
        }
        if (goc instanceof IllegalArgumentException || goc instanceof IllegalStateException) {
            return goc.getMessage();
        }
        if (goc instanceof SQLException sql) {
            return "Lỗi cơ sở dữ liệu (SQLState " + sql.getSQLState()
                    + ", mã " + sql.getErrorCode() + "). Kiểm tra kết nối rồi thử lại.";
        }
        if (goc instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        return "Thao tác không thành công. Vui lòng thử lại.";
    }
}
