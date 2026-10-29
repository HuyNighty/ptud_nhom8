package com.quanlynhakhoa.benhnhan.service;

import com.quanlynhakhoa.benhnhan.dao.BenhNhanDao;
import com.quanlynhakhoa.benhnhan.model.BenhNhan;
import com.quanlynhakhoa.benhnhan.model.GioiTinh;
import com.quanlynhakhoa.security.AuthorizationService;
import com.quanlynhakhoa.security.DesktopSession;
import com.quanlynhakhoa.security.Permission;
import com.quanlynhakhoa.security.RolePermissionPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Quy tắc nghiệp vụ + kiểm tra quyền cho Bệnh nhân.
 * Mọi hàm public đều kiểm tra quyền tại đây (ẩn nút trên giao diện KHÔNG phải là phân quyền).
 */
public final class BenhNhanService {
    public static final int DO_DAI_TU_KHOA_TOI_DA = 100;
    private static final Pattern MA_BENH_NHAN = Pattern.compile("BN[0-9]{6}");
    private static final Pattern KY_TU_NGAN_CACH_SDT = Pattern.compile("[\\s.\\-]");
    private static final Pattern CHI_CHU_SO = Pattern.compile("[0-9]+");

    private final DesktopSession phien;
    private final KetNoiCsdl ketNoiCsdl;
    private final AuthorizationService phanQuyen =
            new AuthorizationService(new RolePermissionPolicy());
    private final BenhNhanDao benhNhanDao = new BenhNhanDao();

    public BenhNhanService(DesktopSession phien, KetNoiCsdl ketNoiCsdl) {
        this.phien = Objects.requireNonNull(phien, "phien");
        this.ketNoiCsdl = Objects.requireNonNull(ketNoiCsdl, "ketNoiCsdl");
    }

    /** Người dùng hiện tại có quyền này không (dùng để làm mờ nút trên giao diện). */
    public boolean coQuyen(Permission quyen) {
        return phanQuyen.can(phien, quyen);
    }

    /** UC01: tìm theo SĐT, mã BN hoặc họ tên; lọc giới tính (null = tất cả). */
    public BenhNhanDao.KetQuaTimKiem timKiem(String tuKhoa, GioiTinh gioiTinh) throws SQLException {
        phanQuyen.require(phien, Permission.PATIENT_READ);
        String tuKhoaChuan = Objects.requireNonNullElse(tuKhoa, "").trim().replaceAll("\\s+", " ");
        if (tuKhoaChuan.length() > DO_DAI_TU_KHOA_TOI_DA) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        }
        String soDienThoai = "";
        String boNganCach = KY_TU_NGAN_CACH_SDT.matcher(tuKhoaChuan).replaceAll("");
        if (!boNganCach.isEmpty() && CHI_CHU_SO.matcher(boNganCach).matches()) {
            // Từ khóa là số → coi như tìm theo số điện thoại (cho phép nhập một phần số).
            if (boNganCach.length() > 10) {
                throw new IllegalArgumentException(
                        "Số điện thoại không hợp lệ: gồm 10 chữ số và bắt đầu bằng 0.");
            }
            soDienThoai = boNganCach;
            tuKhoaChuan = boNganCach;
        }
        try (Connection ketNoi = ketNoiCsdl.moKetNoi()) {
            phanQuyen.require(phien, Permission.PATIENT_READ);
            return benhNhanDao.timKiem(ketNoi, tuKhoaChuan, soDienThoai, gioiTinh);
        }
    }

    /** UC09: xem thông tin một bệnh nhân (đọc lại từ database). */
    public Optional<BenhNhan> xemChiTiet(String maBenhNhan) throws SQLException {
        phanQuyen.require(phien, Permission.PATIENT_READ);
        String ma = kiemTraMaBenhNhan(maBenhNhan);
        try (Connection ketNoi = ketNoiCsdl.moKetNoi()) {
            phanQuyen.require(phien, Permission.PATIENT_READ);
            return benhNhanDao.timTheoMa(ketNoi, ma);
        }
    }

    /** Mã bệnh nhân hợp lệ có dạng BN + 6 chữ số, ví dụ BN000001. */
    public static String kiemTraMaBenhNhan(String maBenhNhan) {
        String ma = Objects.requireNonNullElse(maBenhNhan, "").trim().toUpperCase(Locale.ROOT);
        if (!MA_BENH_NHAN.matcher(ma).matches()) {
            throw new IllegalArgumentException("Mã bệnh nhân không hợp lệ (dạng BN000001).");
        }
        return ma;
    }
}
