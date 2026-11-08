package com.quanlynhakhoa.nghiepvu.dieutri.service;

/** Fixed safe message; no user data, SQL details or exception cause. */
public final class TreatmentPlanUnavailableException extends IllegalStateException {
    public TreatmentPlanUnavailableException() {
        super("Chức năng kế hoạch điều trị chưa sẵn sàng. Chưa thực hiện thao tác với dữ liệu.");
    }
}
