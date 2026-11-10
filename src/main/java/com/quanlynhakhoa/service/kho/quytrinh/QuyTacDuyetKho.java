package com.quanlynhakhoa.service.kho.quytrinh;

/** Pure rules shared by SQL workflow and no-DB self tests. */
public final class QuyTacDuyetKho {
    private QuyTacDuyetKho() {}

    public static void requireApprover(String status, String creator, String actor) {
        if (!"CHO_DUYET".equals(status)) throw new IllegalArgumentException("Phiếu không còn chờ duyệt.");
        if (creator == null || actor == null || creator.equals(actor)) {
            throw new IllegalArgumentException("Người lập phiếu không được tự xử lý phiếu của mình.");
        }
    }

    public static void requireQuantity(int recorded, int actual) {
        if (recorded < 0 || actual < 0 || recorded == actual) {
            throw new IllegalArgumentException("Số lượng điều chỉnh không hợp lệ.");
        }
    }
}
