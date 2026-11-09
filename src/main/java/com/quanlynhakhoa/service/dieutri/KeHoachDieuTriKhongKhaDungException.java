package com.quanlynhakhoa.service.dieutri;

/** Fixed safe message; no user data, SQL details or exception cause. */
public final class KeHoachDieuTriKhongKhaDungException extends IllegalStateException {
    public KeHoachDieuTriKhongKhaDungException() {
        super("Chức năng kế hoạch điều trị chưa sẵn sàng. Chưa thực hiện thao tác với dữ liệu.");
    }
}
