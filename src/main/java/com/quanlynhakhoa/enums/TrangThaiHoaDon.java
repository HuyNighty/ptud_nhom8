package com.quanlynhakhoa.enums;



public enum TrangThaiHoaDon {

    CHUA_THANH_TOAN("Chưa thanh toán"), THANH_TOAN_MOT_PHAN("Thanh toán một phần"),
    DA_THANH_TOAN("Đã thanh toán"), DA_HUY("Đã hủy");
    private final String nhan;
    TrangThaiHoaDon(String nhan) { this.nhan = nhan; }
    @Override public String toString() { return nhan; }

}
