package com.quanlynhakhoa.nghiepvu.hoadon.entity;

public enum InvoiceStatus {
    CHUA_THANH_TOAN("Chưa thanh toán"), THANH_TOAN_MOT_PHAN("Thanh toán một phần"),
    DA_THANH_TOAN("Đã thanh toán"), DA_HUY("Đã hủy");
    private final String label;
    InvoiceStatus(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
