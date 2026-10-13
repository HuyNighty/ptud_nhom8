package vn.n08.nhakhoa.util;

import java.util.List;

/**
 * Cảnh báo cần người dùng xác nhận (VD: hồ sơ nghi trùng, thuốc trùng dị ứng).
 * Giao diện hiển thị danh sách cảnh báo, nếu người dùng đồng ý thì gọi lại Service với cờ xác nhận.
 */
public class CanhBaoException extends BusinessException {
    private final List<String> canhBao;

    public CanhBaoException(String tieuDe, List<String> canhBao) {
        super(tieuDe);
        this.canhBao = List.copyOf(canhBao);
    }

    public List<String> canhBao() {
        return canhBao;
    }
}
