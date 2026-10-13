package vn.n08.nhakhoa.model;

/** Giá trị có nhãn tiếng Việt và sắc thái hiển thị (dùng cho badge trong bảng). */
public interface Labeled {
    String label();
    Tone tone();
}
