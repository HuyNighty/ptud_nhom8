package vn.n08.nhakhoa.controller;

import vn.n08.nhakhoa.util.AppLog;
import vn.n08.nhakhoa.util.BusinessException;
import vn.n08.nhakhoa.util.DataException;

/** Chuyển lỗi thành thông điệp tiếng Việt cho người dùng; lỗi ngoài dự kiến được ghi vào log kỹ thuật. */
public final class ErrorHandler {
    private ErrorHandler() {}

    public static String message(Throwable t) {
        Throwable e = unwrap(t);
        if (e instanceof BusinessException || e instanceof DataException) return e.getMessage();
        if (e instanceof IllegalStateException && e.getMessage() != null && e.getMessage().contains("cấu hình")) return e.getMessage();
        AppLog.error("Lỗi không mong đợi", e);
        return "Lỗi không mong đợi: " + e + ". Chi tiết đã ghi vào logs/n08-nhakhoa.log.";
    }

    public static Throwable unwrap(Throwable t) {
        Throwable e = t;
        while ((e instanceof java.util.concurrent.ExecutionException || e instanceof java.lang.reflect.InvocationTargetException) && e.getCause() != null) {
            e = e.getCause();
        }
        if (e instanceof DataException) AppLog.error(e.getMessage(), e.getCause());
        return e;
    }
}
