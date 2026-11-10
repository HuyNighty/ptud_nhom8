package com.quanlynhakhoa.enums;




public enum TrangThaiYeuCauHuyHoaDon {

        CHO_DUYET("Chờ duyệt"),DA_DUYET("Đã duyệt"),TU_CHOI("Từ chối");
        private final String label;TrangThaiYeuCauHuyHoaDon(String label){this.label=label;}
        @Override public String toString(){return label;}

}
