package vn.n08.nhakhoa.util;

/** Lỗi truy cập dữ liệu (mất kết nối, lỗi SQL ngoài dự kiến). Thông điệp đã được viết cho người dùng. */
public class DataException extends RuntimeException {
    public DataException(String message, Throwable cause) {
        super(message, cause);
    }
}
