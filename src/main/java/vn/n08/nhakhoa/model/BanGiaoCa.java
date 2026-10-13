package vn.n08.nhakhoa.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Bảng BanGiaoCa — biên bản giao nhận ca; người xác nhận phải khác người tạo. */
public record BanGiaoCa(
        String maBanGiao,
        LocalDateTime thoiGian,
        String noiDung,
        TrangThaiBanGiao trangThai,
        LocalDate ngay,
        String maNguoiGiao,
        String maNguoiNhan,
        String maCaGiao,
        String maCaNhan,
        boolean coChenhLech,
        String maTaiKhoanTao,
        String maTaiKhoanXacNhan,
        LocalDateTime thoiGianXacNhan,
        String ghiChuXacNhan,
        String maTaiKhoanHuy,
        LocalDateTime thoiGianHuy,
        String lyDoHuy) {}
