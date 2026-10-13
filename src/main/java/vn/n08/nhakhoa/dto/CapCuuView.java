package vn.n08.nhakhoa.dto;

import vn.n08.nhakhoa.model.DanhMucCapCuu;
import vn.n08.nhakhoa.model.Labeled;
import vn.n08.nhakhoa.model.Tone;

/** Mục hộp cấp cứu kèm tên thuốc/vật tư liên kết và tình trạng tính theo ngưỡng cấu hình. */
public record CapCuuView(DanhMucCapCuu muc, String tenLienKet, TinhTrang tinhTrang) {
    public enum TinhTrang implements Labeled {
        DU("Đủ", Tone.GREEN), THIEU("Thiếu số lượng", Tone.RED), SAP_HET_HAN("Sắp hết hạn", Tone.AMBER),
        HET_HAN("Hết hạn", Tone.RED), NGUNG("Ngừng áp dụng", Tone.GRAY);
        private final String label;
        private final Tone tone;
        TinhTrang(String l, Tone t) { label = l; tone = t; }
        @Override public String label() { return label; }
        @Override public Tone tone() { return tone; }
        @Override public String toString() { return label; }
    }
}
