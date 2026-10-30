package com.quanlynhakhoa.benhnhan.hopdong;

import java.sql.SQLException;
import java.util.ArrayList;

// HỢP ĐỒNG DỮ LIỆU giữa phần Bệnh nhân và phần Lịch hẹn (UC004) / Tiếp đón (UC005).
//
// Phần Lịch hẹn CHỈ gọi 2 hàm dưới đây để tìm và kiểm tra bệnh nhân,
// KHÔNG tự viết câu SQL đọc bảng dbo.BenhNhan và KHÔNG ghi vào bảng đó.
// Nhờ vậy phần Bệnh nhân đổi cách lưu (thêm cột, đổi bảng) thì phần Lịch hẹn không phải sửa.
//
// Cả 2 hàm đều:
//   - kiểm tra quyền PATIENT_READ (lễ tân, bác sĩ, admin có quyền này);
//     không có quyền → ném com.quanlynhakhoa.security.AccessDeniedException
//   - đọc database → phải gọi trong SwingWorker (luồng nền), không gọi trên luồng giao diện
//
// Lớp triển khai: TraCuuBenhNhanService
public interface TraCuuBenhNhan {

    // UC004 bước 1 / UC005 bước 1: tìm bệnh nhân theo mã, họ tên hoặc số điện thoại.
    //   tuKhoa rỗng → lấy các bệnh nhân mới nhất.
    //   Trả về tối đa 200 bệnh nhân, mới nhất ở trên; không tìm thấy → danh sách rỗng (không phải null).
    //   Từ khóa dài quá 100 ký tự → IllegalArgumentException (câu báo lỗi hiện thẳng cho người dùng được).
    ArrayList<BenhNhanTomTat> timBenhNhan(String tuKhoa) throws SQLException;

    // Lấy 1 bệnh nhân theo mã. Không có bệnh nhân này → trả về null.
    //   Dùng NGAY TRƯỚC KHI LƯU lịch hẹn để kiểm tra lại bệnh nhân còn tồn tại (BR10),
    //   và khi mở lại 1 lịch hẹn cũ để hiện tên bệnh nhân.
    BenhNhanTomTat layTheoMa(String maBenhNhan) throws SQLException;
}
