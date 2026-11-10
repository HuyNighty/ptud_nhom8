package com.quanlynhakhoa.enums;




public enum PhuongThucThanhToan {

        TIEN_MAT, CHUYEN_KHOAN, THE_POS;
        @Override public String toString() {
            return switch (this) { case TIEN_MAT -> "Tiền mặt"; case CHUYEN_KHOAN -> "Chuyển khoản"; case THE_POS -> "Thẻ POS"; };
        }

}
