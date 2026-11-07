package com.quanlynhakhoa.nghiepvu.phieukham.controller;

import com.quanlynhakhoa.nghiepvu.phieukham.model.DuLieuCanLamSang;
import com.quanlynhakhoa.nghiepvu.phieukham.service.CanLamSangService;
import com.quanlynhakhoa.nghiepvu.phieukham.view.CanLamSangPanel;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

public final class CanLamSangController {
    private final CanLamSangPanel view;
    private final CanLamSangService service;
    private int loadVersion;

    public CanLamSangController(CanLamSangPanel view, CanLamSangService service) {
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
        view.setLoading(true, "Đang tải danh mục và chỉ định cận lâm sàng...");
        new SwingWorker<DuLieuCanLamSang, Void>() {
            @Override protected DuLieuCanLamSang doInBackground() throws Exception {
                return service.taiDuLieu(visitId, doctorId);
            }
            @Override protected void done() {
                if (requestVersion != loadVersion) return;
                try {
                    view.hienDuLieu(get());
                    view.setLoading(false, "Đã tải dữ liệu cận lâm sàng.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    view.setLoading(false, "Đã dừng tải dữ liệu.");
                } catch (ExecutionException | CancellationException ex) {
                    view.setLoading(false, thongBao(ex));
                }
            }
        }.execute();
    }

    public void taoChiDinh() {
        String visitId = view.getMaPhieuKham();
        String doctorId = view.getMaBacSiPhuTrach();
        String serviceId = view.getMaDichVu();
        String request = view.getYeuCau();
        if (visitId.isBlank() || serviceId.isBlank()) {
            view.showError("Chọn phiếu khám và dịch vụ cận lâm sàng trước.");
            return;
        }
        if (!view.xacNhan("Tạo chỉ định cận lâm sàng cho phiếu " + visitId + "?")) return;
        view.setLoading(true, "Đang tạo chỉ định...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                service.taoChiDinh(visitId, doctorId, serviceId, request);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    view.clearRequestInput();
                    view.showMessage("Đã tạo chỉ định cận lâm sàng.");
                    taiDuLieu();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    view.setLoading(false, "Đã dừng thao tác.");
                } catch (ExecutionException | CancellationException ex) {
                    view.setLoading(false, thongBao(ex));
                }
            }
        }.execute();
    }

    public void luuKetQua() {
        String visitId = view.getMaPhieuKham();
        String doctorId = view.getMaBacSiPhuTrach();
        String orderId = view.getMaChiDinhDangChon();
        String result = view.getKetQua();
        if (visitId.isBlank() || orderId.isBlank()) {
            view.showError("Chọn chỉ định cần nhập kết quả.");
            return;
        }
        if (!view.xacNhan("Lưu kết quả cho chỉ định " + orderId + "?")) return;
        view.setLoading(true, "Đang lưu kết quả...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                service.luuKetQua(visitId, doctorId, orderId, result);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    view.showMessage("Đã lưu kết quả cận lâm sàng.");
                    taiDuLieu();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    view.setLoading(false, "Đã dừng thao tác.");
                } catch (ExecutionException | CancellationException ex) {
                    view.setLoading(false, thongBao(ex));
                }
            }
        }.execute();
    }

    private static String thongBao(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        return message == null || message.isBlank()
                ? "Không thể thực hiện thao tác. Kiểm tra quyền SQL Server và kết nối."
                : message;
    }
}
