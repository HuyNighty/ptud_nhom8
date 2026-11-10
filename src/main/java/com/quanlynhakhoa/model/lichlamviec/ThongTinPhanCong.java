package com.quanlynhakhoa.model.lichlamviec;

import com.quanlynhakhoa.entity.CaLamViec;
import java.time.LocalDate;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class ThongTinPhanCong {
    private final String id;
    private final String nhanVienId;
    private final String tenNhanVien;
    private final CaLamViec ca;
    private final LocalDate ngay;
    private final String nhiemVu;
    private final String trangThai;
    private final String lyDoHuy;

    public ThongTinPhanCong(String id, String nhanVienId, String tenNhanVien, CaLamViec ca, LocalDate ngay, String nhiemVu, String trangThai, String lyDoHuy) {
        this.id = id;
        this.nhanVienId = nhanVienId;
        this.tenNhanVien = tenNhanVien;
        this.ca = ca;
        this.ngay = ngay;
        this.nhiemVu = nhiemVu;
        this.trangThai = trangThai;
        this.lyDoHuy = lyDoHuy;
    }

    public String id() { return id; }
    public String nhanVienId() { return nhanVienId; }
    public String tenNhanVien() { return tenNhanVien; }
    public CaLamViec ca() { return ca; }
    public LocalDate ngay() { return ngay; }
    public String nhiemVu() { return nhiemVu; }
    public String trangThai() { return trangThai; }
    public String lyDoHuy() { return lyDoHuy; }

    public String trangThaiHienThi() { return switch (trangThai) {
        case "CHO_HIEU_LUC" -> "Chờ hiệu lực";
        case "DANG_THUC_HIEN" -> "Đang thực hiện";
        case "HOAN_THANH" -> "Hoàn thành";
        case "DA_HUY" -> "Đã hủy";
        default -> trangThai;
    }; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinPhanCong value)) return false;
        return java.util.Objects.equals(id, value.id)
                && java.util.Objects.equals(nhanVienId, value.nhanVienId)
                && java.util.Objects.equals(tenNhanVien, value.tenNhanVien)
                && java.util.Objects.equals(ca, value.ca)
                && java.util.Objects.equals(ngay, value.ngay)
                && java.util.Objects.equals(nhiemVu, value.nhiemVu)
                && java.util.Objects.equals(trangThai, value.trangThai)
                && java.util.Objects.equals(lyDoHuy, value.lyDoHuy);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(id);
        result = 31 * result + java.util.Objects.hashCode(nhanVienId);
        result = 31 * result + java.util.Objects.hashCode(tenNhanVien);
        result = 31 * result + java.util.Objects.hashCode(ca);
        result = 31 * result + java.util.Objects.hashCode(ngay);
        result = 31 * result + java.util.Objects.hashCode(nhiemVu);
        result = 31 * result + java.util.Objects.hashCode(trangThai);
        result = 31 * result + java.util.Objects.hashCode(lyDoHuy);
        return result;
    }

    @Override
    public String toString() {
        return "PhanCong[" + "id=" + id + ", " + "nhanVienId=" + nhanVienId + ", " + "tenNhanVien=" + tenNhanVien + ", " + "ca=" + ca + ", " + "ngay=" + ngay + ", " + "nhiemVu=" + nhiemVu + ", " + "trangThai=" + trangThai + ", " + "lyDoHuy=" + lyDoHuy + "]";
    }
}
