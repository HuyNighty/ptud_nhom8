package com.quanlynhakhoa.enums;




public enum TrangThaiThanhToan {

        THANH_CONG, DA_HUY, DA_HOAN_TIEN;
        @Override public String toString() { return switch(this){case THANH_CONG -> "Thành công";case DA_HUY -> "Hủy ghi nhận";case DA_HOAN_TIEN -> "Đã hoàn tiền";}; }

}
