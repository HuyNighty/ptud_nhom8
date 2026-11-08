package com.quanlynhakhoa.nghiepvu.phieukham.controller;

import com.quanlynhakhoa.nghiepvu.phieukham.entity.BacSi;
import com.quanlynhakhoa.nghiepvu.phieukham.entity.PhieuKham;
import com.quanlynhakhoa.nghiepvu.phieukham.entity.PhieuKhamListItem;
import com.quanlynhakhoa.nghiepvu.phieukham.service.PhieuKhamService;
import com.quanlynhakhoa.nghiepvu.phieukham.view.PhieuKhamPanel;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionListener;

public final class PhieuKhamController {
    private final PhieuKhamPanel view;
    private final PhieuKhamService service;

    public PhieuKhamController(PhieuKhamPanel view, PhieuKhamService service) {
        this.view = java.util.Objects.requireNonNull(view, "view");
        this.service = java.util.Objects.requireNonNull(service, "service");
        ganSuKien();
        taiBacSiVaDanhSach();
    }

    private void ganSuKien() {
        view.getBtnLoc().addActionListener(event -> taiDanhSach());
        view.getBtnXoaLoc().addActionListener(event -> {
            view.xoaBoLoc();
            taiDanhSach();
        });
        view.getBtnTaiLai().addActionListener(event -> { view.xoaBoLoc(); taiBacSiVaDanhSach(); });
        view.getBtnLuuNhap().addActionListener(event -> luuNhap());
        view.getBtnKetLuan().addActionListener(event -> ketLuan());

        ListSelectionListener selectionListener = event -> {
            if (!event.getValueIsAdjusting()) taiChiTietDangChon();
        };
        view.getTablePhieuKham().getSelectionModel().addListSelectionListener(selectionListener);
    }

    private void taiBacSiVaDanhSach() {
        view.setDangTai(true, "Đang tải danh sách bác sĩ...");
        new SwingWorker<List<BacSi>, Void>() {
            @Override
            protected List<BacSi> doInBackground() throws Exception {
                return service.danhSachBacSi();
            }

            @Override
            protected void done() {
                try {
                    view.datDanhSachBacSi(get());
                    taiDanhSach();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    ketThucTai("Đã dừng tải dữ liệu.");
                } catch (ExecutionException | CancellationException ex) {
                    ketThucTai(thongBao(ex));
                }
            }
        }.execute();
    }

    private void taiDanhSach() {
        final java.time.LocalDate date;
        try {
            date = view.layNgayLoc();
        } catch (IllegalArgumentException invalid) {
            view.hienLoi(invalid.getMessage());
            return;
        }

        final String doctorFilter = view.layMaBacSiLoc();
        final String statusFilter = view.layTrangThaiLoc();
        final String keyword = view.layTuKhoa();
        view.setDangTai(true, "Đang tải phiếu khám...");
        new SwingWorker<List<PhieuKhamListItem>, Void>() {
            @Override
            protected List<PhieuKhamListItem> doInBackground() throws Exception {
                return service.timKiem(date, doctorFilter, statusFilter, keyword);
            }

            @Override
            protected void done() {
                try {
                    List<PhieuKhamListItem> rows = get();
                    view.hienDanhSach(rows);
                    view.setDangTai(false, "Có " + rows.size() + " phiếu khám.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    ketThucTai("Đã dừng tải dữ liệu.");
                } catch (ExecutionException | CancellationException ex) {
                    ketThucTai(thongBao(ex));
                }
            }
        }.execute();
    }

    private void taiChiTietDangChon() {
        String code = view.layMaPhieuDangChon();
        if (code.isBlank()) {
            view.xoaChiTiet();
            return;
        }
        view.setDangTaiChiTiet(true);
        new SwingWorker<PhieuKham, Void>() {
            @Override
            protected PhieuKham doInBackground() throws Exception {
                return service.chiTiet(code);
            }

            @Override
            protected void done() {
                try {
                    view.hienChiTiet(get());
                    view.setDangTaiChiTiet(false);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    view.setDangTaiChiTiet(false);
                } catch (ExecutionException | CancellationException ex) {
                    view.setDangTaiChiTiet(false);
                    view.hienLoi(thongBao(ex));
                }
            }
        }.execute();
    }

    private void luuNhap() {
        String code = view.layMaPhieuDangChon();
        if (code.isBlank()) {
            view.hienLoi("Chọn phiếu khám cần lưu.");
            return;
        }
        final String responsibleDoctorId = view.layMaBacSiPhuTrach();
        final String symptoms = view.layTrieuChung();
        final String diagnosis = view.layChanDoan();
        view.setDangTaiChiTiet(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                service.luuNhap(code, responsibleDoctorId, symptoms, diagnosis);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    view.thongBaoThanhCong("Đã lưu nháp phiếu khám " + code + ".");
                    taiChiTietDangChon();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    view.hienLoi("Đã dừng thao tác.");
                } catch (ExecutionException | CancellationException ex) {
                    view.setDangTaiChiTiet(false);
                    view.hienLoi(thongBao(ex));
                }
            }
        }.execute();
    }


    private void ketLuan() {
        String code = view.layMaPhieuDangChon();

        if (code.isBlank()) {
            view.hienLoi("Chọn phiếu khám cần kết luận.");
            return;
        }

        String doctorId = view.layMaBacSiPhuTrach();
        String diagnosis = view.layChanDoan();
        String conclusion = view.layKetLuan();

        if (!view.xacNhan("Kết luận phiếu khám " + code
                + "? Sau khi kết luận, phiếu sẽ chuyển sang trạng thái Đã kết luận.")) {
            return;
        }

        view.setDangTaiChiTiet(true);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                service.ketLuan(code, doctorId, diagnosis, conclusion);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    view.thongBaoThanhCong(
                            "Đã kết luận phiếu khám " + code + ".");

                    taiDanhSach();

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    view.setDangTaiChiTiet(false);
                    view.hienLoi("Đã dừng thao tác kết luận.");

                } catch (ExecutionException | CancellationException ex) {
                    view.setDangTaiChiTiet(false);
                    view.hienLoi(thongBao(ex));
                }
            }
        }.execute();
    }

    private void ketThucTai(String message) {
        view.setDangTai(false, message);
        view.hienLoi(message);
    }

    private static String thongBao(Exception exception) {
        Throwable cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        return message == null || message.isBlank()
                ? "Không thể thực hiện thao tác. Kiểm tra kết nối và quyền SQL Server."
                : message;
    }
}
