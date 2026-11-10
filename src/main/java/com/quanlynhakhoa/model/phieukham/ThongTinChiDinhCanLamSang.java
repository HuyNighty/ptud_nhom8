package com.quanlynhakhoa.model.phieukham;

import com.quanlynhakhoa.entity.ChiDinhCanLamSang;

import java.time.LocalDateTime;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class ThongTinChiDinhCanLamSang {
    private final ChiDinhCanLamSang chiDinhCanLamSang;
    private final String tenDichVu;
    private final String tenBacSi;

    public ThongTinChiDinhCanLamSang(String maChiDinh, String maPhieuKham, String maDichVu, String tenDichVu, String yeuCau, String ketQua, String maNhanVien, String tenBacSi, LocalDateTime thoiGianChiDinh, LocalDateTime thoiGianKetQua, String trangThai) {
        this.chiDinhCanLamSang = new ChiDinhCanLamSang(maChiDinh, maPhieuKham, maDichVu, yeuCau, ketQua, maNhanVien, thoiGianChiDinh, thoiGianKetQua, trangThai);

        this.tenDichVu = tenDichVu;

        this.tenBacSi = tenBacSi;

    }

    public String maChiDinh() { return chiDinhCanLamSang.getMaChiDinh(); }
    public String maPhieuKham() { return chiDinhCanLamSang.getMaPhieuKham(); }
    public String maDichVu() { return chiDinhCanLamSang.getMaDichVu(); }
    public String tenDichVu() { return tenDichVu; }
    public String yeuCau() { return chiDinhCanLamSang.getYeuCau(); }
    public String ketQua() { return chiDinhCanLamSang.getKetQua(); }
    public String maNhanVien() { return chiDinhCanLamSang.getMaNhanVien(); }
    public String tenBacSi() { return tenBacSi; }
    public LocalDateTime thoiGianChiDinh() { return chiDinhCanLamSang.getThoiGianChiDinh(); }
    public LocalDateTime thoiGianKetQua() { return chiDinhCanLamSang.getThoiGianKetQua(); }
    public String trangThai() { return chiDinhCanLamSang.getTrangThai(); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinChiDinhCanLamSang value)) return false;
        return java.util.Objects.equals(chiDinhCanLamSang.getMaChiDinh(), value.chiDinhCanLamSang.getMaChiDinh())
                && java.util.Objects.equals(chiDinhCanLamSang.getMaPhieuKham(), value.chiDinhCanLamSang.getMaPhieuKham())
                && java.util.Objects.equals(chiDinhCanLamSang.getMaDichVu(), value.chiDinhCanLamSang.getMaDichVu())
                && java.util.Objects.equals(tenDichVu, value.tenDichVu)
                && java.util.Objects.equals(chiDinhCanLamSang.getYeuCau(), value.chiDinhCanLamSang.getYeuCau())
                && java.util.Objects.equals(chiDinhCanLamSang.getKetQua(), value.chiDinhCanLamSang.getKetQua())
                && java.util.Objects.equals(chiDinhCanLamSang.getMaNhanVien(), value.chiDinhCanLamSang.getMaNhanVien())
                && java.util.Objects.equals(tenBacSi, value.tenBacSi)
                && java.util.Objects.equals(chiDinhCanLamSang.getThoiGianChiDinh(), value.chiDinhCanLamSang.getThoiGianChiDinh())
                && java.util.Objects.equals(chiDinhCanLamSang.getThoiGianKetQua(), value.chiDinhCanLamSang.getThoiGianKetQua())
                && java.util.Objects.equals(chiDinhCanLamSang.getTrangThai(), value.chiDinhCanLamSang.getTrangThai());
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(chiDinhCanLamSang.getMaChiDinh());
        result = 31 * result + java.util.Objects.hashCode(chiDinhCanLamSang.getMaPhieuKham());
        result = 31 * result + java.util.Objects.hashCode(chiDinhCanLamSang.getMaDichVu());
        result = 31 * result + java.util.Objects.hashCode(tenDichVu);
        result = 31 * result + java.util.Objects.hashCode(chiDinhCanLamSang.getYeuCau());
        result = 31 * result + java.util.Objects.hashCode(chiDinhCanLamSang.getKetQua());
        result = 31 * result + java.util.Objects.hashCode(chiDinhCanLamSang.getMaNhanVien());
        result = 31 * result + java.util.Objects.hashCode(tenBacSi);
        result = 31 * result + java.util.Objects.hashCode(chiDinhCanLamSang.getThoiGianChiDinh());
        result = 31 * result + java.util.Objects.hashCode(chiDinhCanLamSang.getThoiGianKetQua());
        result = 31 * result + java.util.Objects.hashCode(chiDinhCanLamSang.getTrangThai());
        return result;
    }

    @Override
    public String toString() {
        return "ChiDinhCanLamSang[" + "maChiDinh=" + chiDinhCanLamSang.getMaChiDinh() + ", " + "maPhieuKham=" + chiDinhCanLamSang.getMaPhieuKham() + ", " + "maDichVu=" + chiDinhCanLamSang.getMaDichVu() + ", " + "tenDichVu=" + tenDichVu + ", " + "yeuCau=" + chiDinhCanLamSang.getYeuCau() + ", " + "ketQua=" + chiDinhCanLamSang.getKetQua() + ", " + "maNhanVien=" + chiDinhCanLamSang.getMaNhanVien() + ", " + "tenBacSi=" + tenBacSi + ", " + "thoiGianChiDinh=" + chiDinhCanLamSang.getThoiGianChiDinh() + ", " + "thoiGianKetQua=" + chiDinhCanLamSang.getThoiGianKetQua() + ", " + "trangThai=" + chiDinhCanLamSang.getTrangThai() + "]";
    }

    public ChiDinhCanLamSang getChiDinhCanLamSang() { return chiDinhCanLamSang; }
}
