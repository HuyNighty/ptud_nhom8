package com.quanlynhakhoa.model.phieukham;

import com.quanlynhakhoa.entity.PhieuKham;

import java.time.LocalDateTime;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class ThongTinPhieuKham {
    private final PhieuKham phieuKham;
    private final String tenBenhNhan;
    private final String soDienThoai;
    private final String tenBacSi;
    private final String tienSuBenh;
    private final String diUng;

    public ThongTinPhieuKham(String maPhieuKham, String maBenhNhan, String tenBenhNhan, String soDienThoai, String maHoSo, String maLichHen, LocalDateTime thoiGianTiepNhan, String maNhanVien, String tenBacSi, String trieuChung, String chanDoan, String ketLuan, String trangThai, String tienSuBenh, String diUng) {
        this.phieuKham = new PhieuKham(maPhieuKham, maBenhNhan, maHoSo, maLichHen, thoiGianTiepNhan, maNhanVien, trieuChung, chanDoan, ketLuan, trangThai);

        this.tenBenhNhan = tenBenhNhan;
        this.soDienThoai = soDienThoai;

        this.tenBacSi = tenBacSi;

        this.tienSuBenh = tienSuBenh;
        this.diUng = diUng;
    }

    public String maPhieuKham() { return phieuKham.getMaPhieuKham(); }
    public String maBenhNhan() { return phieuKham.getMaBenhNhan(); }
    public String tenBenhNhan() { return tenBenhNhan; }
    public String soDienThoai() { return soDienThoai; }
    public String maHoSo() { return phieuKham.getMaHoSo(); }
    public String maLichHen() { return phieuKham.getMaLichHen(); }
    public LocalDateTime thoiGianTiepNhan() { return phieuKham.getThoiGianTiepNhan(); }
    public String maNhanVien() { return phieuKham.getMaNhanVien(); }
    public String tenBacSi() { return tenBacSi; }
    public String trieuChung() { return phieuKham.getTrieuChung(); }
    public String chanDoan() { return phieuKham.getChanDoan(); }
    public String ketLuan() { return phieuKham.getKetLuan(); }
    public String trangThai() { return phieuKham.getTrangThai(); }
    public String tienSuBenh() { return tienSuBenh; }
    public String diUng() { return diUng; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ThongTinPhieuKham value)) return false;
        return java.util.Objects.equals(phieuKham.getMaPhieuKham(), value.phieuKham.getMaPhieuKham())
                && java.util.Objects.equals(phieuKham.getMaBenhNhan(), value.phieuKham.getMaBenhNhan())
                && java.util.Objects.equals(tenBenhNhan, value.tenBenhNhan)
                && java.util.Objects.equals(soDienThoai, value.soDienThoai)
                && java.util.Objects.equals(phieuKham.getMaHoSo(), value.phieuKham.getMaHoSo())
                && java.util.Objects.equals(phieuKham.getMaLichHen(), value.phieuKham.getMaLichHen())
                && java.util.Objects.equals(phieuKham.getThoiGianTiepNhan(), value.phieuKham.getThoiGianTiepNhan())
                && java.util.Objects.equals(phieuKham.getMaNhanVien(), value.phieuKham.getMaNhanVien())
                && java.util.Objects.equals(tenBacSi, value.tenBacSi)
                && java.util.Objects.equals(phieuKham.getTrieuChung(), value.phieuKham.getTrieuChung())
                && java.util.Objects.equals(phieuKham.getChanDoan(), value.phieuKham.getChanDoan())
                && java.util.Objects.equals(phieuKham.getKetLuan(), value.phieuKham.getKetLuan())
                && java.util.Objects.equals(phieuKham.getTrangThai(), value.phieuKham.getTrangThai())
                && java.util.Objects.equals(tienSuBenh, value.tienSuBenh)
                && java.util.Objects.equals(diUng, value.diUng);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(phieuKham.getMaPhieuKham());
        result = 31 * result + java.util.Objects.hashCode(phieuKham.getMaBenhNhan());
        result = 31 * result + java.util.Objects.hashCode(tenBenhNhan);
        result = 31 * result + java.util.Objects.hashCode(soDienThoai);
        result = 31 * result + java.util.Objects.hashCode(phieuKham.getMaHoSo());
        result = 31 * result + java.util.Objects.hashCode(phieuKham.getMaLichHen());
        result = 31 * result + java.util.Objects.hashCode(phieuKham.getThoiGianTiepNhan());
        result = 31 * result + java.util.Objects.hashCode(phieuKham.getMaNhanVien());
        result = 31 * result + java.util.Objects.hashCode(tenBacSi);
        result = 31 * result + java.util.Objects.hashCode(phieuKham.getTrieuChung());
        result = 31 * result + java.util.Objects.hashCode(phieuKham.getChanDoan());
        result = 31 * result + java.util.Objects.hashCode(phieuKham.getKetLuan());
        result = 31 * result + java.util.Objects.hashCode(phieuKham.getTrangThai());
        result = 31 * result + java.util.Objects.hashCode(tienSuBenh);
        result = 31 * result + java.util.Objects.hashCode(diUng);
        return result;
    }

    @Override
    public String toString() {
        return "PhieuKham[" + "maPhieuKham=" + phieuKham.getMaPhieuKham() + ", " + "maBenhNhan=" + phieuKham.getMaBenhNhan() + ", " + "tenBenhNhan=" + tenBenhNhan + ", " + "soDienThoai=" + soDienThoai + ", " + "maHoSo=" + phieuKham.getMaHoSo() + ", " + "maLichHen=" + phieuKham.getMaLichHen() + ", " + "thoiGianTiepNhan=" + phieuKham.getThoiGianTiepNhan() + ", " + "maNhanVien=" + phieuKham.getMaNhanVien() + ", " + "tenBacSi=" + tenBacSi + ", " + "trieuChung=" + phieuKham.getTrieuChung() + ", " + "chanDoan=" + phieuKham.getChanDoan() + ", " + "ketLuan=" + phieuKham.getKetLuan() + ", " + "trangThai=" + phieuKham.getTrangThai() + ", " + "tienSuBenh=" + tienSuBenh + ", " + "diUng=" + diUng + "]";
    }

    public PhieuKham getPhieuKham() { return phieuKham; }
}
