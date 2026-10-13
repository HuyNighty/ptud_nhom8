package vn.n08.nhakhoa.util;

/** Vi phạm quy tắc nghiệp vụ hoặc dữ liệu nhập không hợp lệ. Thông điệp hiển thị trực tiếp cho người dùng. */
public class BusinessException extends RuntimeException {
    private final String field;

    public BusinessException(String message) {
        this(null, message);
    }

    /** @param field mã ô nhập liên quan (để giao diện đánh dấu lỗi), có thể null */
    public BusinessException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
