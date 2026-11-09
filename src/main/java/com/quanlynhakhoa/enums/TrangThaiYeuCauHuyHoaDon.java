package com.quanlynhakhoa.enums;




public enum TrangThaiYeuCauHuyHoaDon {

        CHO_DUYET("Chờ duyệt"),DA_DUYET("Đã duyệt"),TU_CHOI("Từ chối");
        private final String nhan;TrangThaiYeuCauHuyHoaDon(String nhan){this.nhan=nhan;}
        @Override public String toString(){return nhan;}

}
