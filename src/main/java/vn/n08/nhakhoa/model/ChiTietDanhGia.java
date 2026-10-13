package vn.n08.nhakhoa.model;

/** [Bổ sung] Điểm từng tiêu chí của một lần đánh giá. */
public record ChiTietDanhGia(
        String maDanhGia,
        int thuTu,
        String tieuChi,
        int diem) {}
