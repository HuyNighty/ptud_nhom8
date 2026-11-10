package com.quanlynhakhoa.model.lichlamviec;

import com.quanlynhakhoa.entity.PhanCong;

import com.quanlynhakhoa.entity.CaLamViec;
import java.time.LocalDate;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class ThongTinPhanCong {
    private final PhanCong phanCong;
    private final String tenNhanVien;

    public ThongTinPhanCong(String ma, String nhanVienId, String tenNhanVien, CaLamViec ca, LocalDate ngay, String nhiemVu, String trangThai, String lyDoHuy) {
        this.phanCong = new PhanCong(ma, nhanVienId, ca, ngay, nhiemVu, trangThai, lyDoHuy);

        this.tenNhanVien = tenNhanVien;





    }

    public String id() { return phanCong.getMaPhanCong(); }
    public String nhanVienId() { return phanCong.getMaNhanVien(); }
    public String tenNhanVien() { return tenNhanVien; }
    public CaLamViec ca() { return phanCong.getCa(); }
    public LocalDate ngay() { return phanCong.getNgayLam(); }
    public String nhiemVu() { return phanCong.getNhiemVu(); }
    public String trangThai() { return phanCong.getTrangThai(); }
    public String lyDoHuy() { return phanCong.getLyDoHuy(); }

    public String trangThaiHienThi() { return switch (phanCong.getTrangThai()) {
        case "CHO_HIEU_LUC" -> "Chờ hiệu lực";
        case "DANG_THUC_HIEN" -> "Đang thực hiện";
        case "HOAN_THANH" -> "Hoàn thành";
        case "DA_HUY" -> "Đã hủy";
        default -> phanCong.getTrangThai();
    }; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinPhanCong value)) return false;
        return java.util.Objects.equals(phanCong.getMaPhanCong(), value.phanCong.getMaPhanCong())
                && java.util.Objects.equals(phanCong.getMaNhanVien(), value.phanCong.getMaNhanVien())
                && java.util.Objects.equals(tenNhanVien, value.tenNhanVien)
                && java.util.Objects.equals(phanCong.getCa(), value.phanCong.getCa())
                && java.util.Objects.equals(phanCong.getNgayLam(), value.phanCong.getNgayLam())
                && java.util.Objects.equals(phanCong.getNhiemVu(), value.phanCong.getNhiemVu())
                && java.util.Objects.equals(phanCong.getTrangThai(), value.phanCong.getTrangThai())
                && java.util.Objects.equals(phanCong.getLyDoHuy(), value.phanCong.getLyDoHuy());
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(phanCong.getMaPhanCong());
        result = 31 * result + java.util.Objects.hashCode(phanCong.getMaNhanVien());
        result = 31 * result + java.util.Objects.hashCode(tenNhanVien);
        result = 31 * result + java.util.Objects.hashCode(phanCong.getCa());
        result = 31 * result + java.util.Objects.hashCode(phanCong.getNgayLam());
        result = 31 * result + java.util.Objects.hashCode(phanCong.getNhiemVu());
        result = 31 * result + java.util.Objects.hashCode(phanCong.getTrangThai());
        result = 31 * result + java.util.Objects.hashCode(phanCong.getLyDoHuy());
        return result;
    }

    @Override
    public String toString() {
        return "PhanCong[" + "id=" + phanCong.getMaPhanCong() + ", " + "nhanVienId=" + phanCong.getMaNhanVien() + ", " + "tenNhanVien=" + tenNhanVien + ", " + "ca=" + phanCong.getCa() + ", " + "ngay=" + phanCong.getNgayLam() + ", " + "nhiemVu=" + phanCong.getNhiemVu() + ", " + "trangThai=" + phanCong.getTrangThai() + ", " + "lyDoHuy=" + phanCong.getLyDoHuy() + "]";
    }

    public PhanCong getPhanCong() { return phanCong; }
}
