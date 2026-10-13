package vn.n08.nhakhoa.view.component;

import vn.n08.nhakhoa.model.Labeled;
import vn.n08.nhakhoa.model.Tone;

/** Nhãn hiển thị tính trên giao diện (VD: "Quá hạn 3 ngày") — hiện dạng badge trong DataTable. */
public record Tag(String label, Tone tone) implements Labeled, Comparable<Tag> {
    @Override public String toString() { return label; }
    @Override public int compareTo(Tag o) { return label.compareTo(o.label); }
}
