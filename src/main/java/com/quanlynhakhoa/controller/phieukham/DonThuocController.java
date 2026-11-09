package com.quanlynhakhoa.controller.phieukham;

import com.quanlynhakhoa.model.phieukham.DongChiTietDonThuoc;
import com.quanlynhakhoa.model.phieukham.DuLieuDonThuoc;
import com.quanlynhakhoa.service.phieukham.DonThuocService;
import com.quanlynhakhoa.view.phieukham.DonThuocPanel;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

public final class DonThuocController {
    private final DonThuocPanel view;
    private final DonThuocService service;
    private int loadVersion;

    public DonThuocController(DonThuocPanel view, DonThuocService service) {
        this.view = java.util.Objects.requireNonNull(view, "view");
        this.service = java.util.Objects.requireNonNull(service, "service");
    }

    public void invalidateLoads() {
        loadVersion++;
    }

    public void taiDuLieu() {
        String visitId = view.getMaPhieuKham();
        String doctorId = view.getMaBacSiPhuTrach();
        if (visitId.isBlank()) return;
        int requestVersion = ++loadVersion;
        view.setLoading(true, "Đang tải danh mục và đơn thuốc...");
        new SwingWorker<DuLieuDonThuoc, Void>() {
            @Override
            protected DuLieuDonThuoc doInBackground() throws Exception {
                return service.taiDuLieu(visitId, doctorId);
            }

            @Override
            protected void done() {
                if (requestVersion != loadVersion) return;
                try {
                    view.hienDuLieu(get());
                    view.setLoading(false, "Đã tải dữ liệu đơn thuốc.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    view.setLoading(false, "Đã dừng tải dữ liệu.");
                } catch (ExecutionException | CancellationException ex) {
                    view.setLoading(false, message(ex));
                }
            }
        }.execute();
    }

    public void themThuoc() {
        try {
            DongChiTietDonThuoc item = view.taoChiTietDangNhap();
            view.themVaoDanhSach(item);
        } catch (IllegalArgumentException ex) {
            view.showError(ex.getMessage());
        }
    }

    public void luuDonThuoc() {
        String visitId = view.getMaPhieuKham();
        String doctorId = view.getMaBacSiPhuTrach();
        List<DongChiTietDonThuoc> items = view.layChiTietNhap();
        String instructions = view.getLoiDan();
        if (visitId.isBlank()) {
            view.showError("Chọn phiếu khám cần kê đơn.");
            return;
        }
        if (items.isEmpty()) {
            view.showError("Thêm ít nhất một loại thuốc trước khi lưu.");
            return;
        }
        if (!view.xacNhanCanhBao("Bạn chắc chắn muốn lưu đơn thuốc cho phiếu " + visitId + "?")) {
            return;
        }
        runSave(visitId, doctorId, items, instructions, false);
    }

    private void runSave(String visitId, String doctorId, List<DongChiTietDonThuoc> items,
                         String instructions, boolean acknowledgedWarnings) {
        view.setLoading(true, "Đang lưu đơn thuốc...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                service.luuDonThuoc(visitId, doctorId, items, instructions, acknowledgedWarnings);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    view.showMessage("Đã lưu đơn thuốc cho phiếu " + visitId + ".");
                    taiDuLieu();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    view.setLoading(false, "Đã dừng thao tác.");
                } catch (ExecutionException | CancellationException ex) {
                    Throwable cause = rootCause(ex);
                    if (cause instanceof DonThuocService.CanhBaoCanXacNhanException
                            && !acknowledgedWarnings) {
                        if (view.xacNhanCanhBao(cause.getMessage())) {
                            runSave(visitId, doctorId, items, instructions, true);
                        } else {
                            view.setLoading(false, "Đã hủy lưu đơn thuốc sau khi xem cảnh báo.");
                        }
                    } else {
                        view.setLoading(false, message(ex));
                    }
                }
            }
        }.execute();
    }

    private static Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String message(Exception exception) {
        Throwable cause = rootCause(exception);
        String text = cause.getMessage();
        return text == null || text.isBlank()
                ? "Không thể thực hiện thao tác. Kiểm tra kết nối và quyền SQL Server."
                : text;
    }
}
