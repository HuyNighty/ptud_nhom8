package com.quanlynhakhoa.entity;

import com.quanlynhakhoa.enums.NguonKhoaTaiKhoan;
import com.quanlynhakhoa.enums.TrangThaiTaiKhoan;
import com.quanlynhakhoa.enums.VaiTroHeThong;
import java.time.LocalDateTime;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class TaiKhoan {
    private final String ma;
    private final String maNhanVien;
    private final String tenDangNhap;
    private final String tenHienThi;
    private final String matKhauHash;
    private final VaiTroHeThong vaiTro;
    private final TrangThaiTaiKhoan trangThai;
    private final int soLanDangNhapSai;
    private final LocalDateTime thoiGianSaiDauTien;
    private final LocalDateTime thoiGianKhoa;
    private final LocalDateTime khoaDenLuc;
    private final NguonKhoaTaiKhoan nguonKhoa;
    private final String lyDoKhoa;
    private final boolean batBuocDoiMatKhau;
    private final LocalDateTime thoiGianCapNhatMatKhau;
    private final LocalDateTime thoiGianDangNhapCuoi;
    private final LocalDateTime thoiGianTao;
    private final LocalDateTime thoiGianCapNhat;
    private final LocalDateTime thoiGianVoHieuHoa;
    private final String lyDoVoHieuHoa;

    public TaiKhoan(String ma, String maNhanVien, String tenDangNhap, String tenHienThi, String matKhauHash, VaiTroHeThong vaiTro, TrangThaiTaiKhoan trangThai, int soLanDangNhapSai, LocalDateTime thoiGianSaiDauTien, LocalDateTime thoiGianKhoa, LocalDateTime khoaDenLuc, NguonKhoaTaiKhoan nguonKhoa, String lyDoKhoa, boolean batBuocDoiMatKhau, LocalDateTime thoiGianCapNhatMatKhau, LocalDateTime thoiGianDangNhapCuoi, LocalDateTime thoiGianTao, LocalDateTime thoiGianCapNhat, LocalDateTime thoiGianVoHieuHoa, String lyDoVoHieuHoa) {
        this.ma = ma;
        this.maNhanVien = maNhanVien;
        this.tenDangNhap = tenDangNhap;
        this.tenHienThi = tenHienThi;
        this.matKhauHash = matKhauHash;
        this.vaiTro = vaiTro;
        this.trangThai = trangThai;
        this.soLanDangNhapSai = soLanDangNhapSai;
        this.thoiGianSaiDauTien = thoiGianSaiDauTien;
        this.thoiGianKhoa = thoiGianKhoa;
        this.khoaDenLuc = khoaDenLuc;
        this.nguonKhoa = nguonKhoa;
        this.lyDoKhoa = lyDoKhoa;
        this.batBuocDoiMatKhau = batBuocDoiMatKhau;
        this.thoiGianCapNhatMatKhau = thoiGianCapNhatMatKhau;
        this.thoiGianDangNhapCuoi = thoiGianDangNhapCuoi;
        this.thoiGianTao = thoiGianTao;
        this.thoiGianCapNhat = thoiGianCapNhat;
        this.thoiGianVoHieuHoa = thoiGianVoHieuHoa;
        this.lyDoVoHieuHoa = lyDoVoHieuHoa;
    }

    public String id() { return ma; }
    public String employeeId() { return maNhanVien; }
    public String username() { return tenDangNhap; }
    public String displayName() { return tenHienThi; }
    public String passwordHash() { return matKhauHash; }
    public VaiTroHeThong role() { return vaiTro; }
    public TrangThaiTaiKhoan status() { return trangThai; }
    public int failedAttempts() { return soLanDangNhapSai; }
    public LocalDateTime firstFailureAt() { return thoiGianSaiDauTien; }
    public LocalDateTime lockedAt() { return thoiGianKhoa; }
    public LocalDateTime lockedUntil() { return khoaDenLuc; }
    public NguonKhoaTaiKhoan lockSource() { return nguonKhoa; }
    public String lockReason() { return lyDoKhoa; }
    public boolean forcePasswordChange() { return batBuocDoiMatKhau; }
    public LocalDateTime passwordUpdatedAt() { return thoiGianCapNhatMatKhau; }
    public LocalDateTime lastLoginAt() { return thoiGianDangNhapCuoi; }
    public LocalDateTime createdAt() { return thoiGianTao; }
    public LocalDateTime updatedAt() { return thoiGianCapNhat; }
    public LocalDateTime disabledAt() { return thoiGianVoHieuHoa; }
    public String disableReason() { return lyDoVoHieuHoa; }

    // passwordHash is intentionally never included in a UI or log response.
    // Do not log/serialize this record: record.toString() contains passwordHash.
    @Override
    public String toString() {
        return "Account[id=" + ma + ",role=" + vaiTro + ",status=" + trangThai + "]";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TaiKhoan value)) return false;
        return java.util.Objects.equals(ma, value.ma)
                && java.util.Objects.equals(maNhanVien, value.maNhanVien)
                && java.util.Objects.equals(tenDangNhap, value.tenDangNhap)
                && java.util.Objects.equals(tenHienThi, value.tenHienThi)
                && java.util.Objects.equals(matKhauHash, value.matKhauHash)
                && java.util.Objects.equals(vaiTro, value.vaiTro)
                && java.util.Objects.equals(trangThai, value.trangThai)
                && soLanDangNhapSai == value.soLanDangNhapSai
                && java.util.Objects.equals(thoiGianSaiDauTien, value.thoiGianSaiDauTien)
                && java.util.Objects.equals(thoiGianKhoa, value.thoiGianKhoa)
                && java.util.Objects.equals(khoaDenLuc, value.khoaDenLuc)
                && java.util.Objects.equals(nguonKhoa, value.nguonKhoa)
                && java.util.Objects.equals(lyDoKhoa, value.lyDoKhoa)
                && batBuocDoiMatKhau == value.batBuocDoiMatKhau
                && java.util.Objects.equals(thoiGianCapNhatMatKhau, value.thoiGianCapNhatMatKhau)
                && java.util.Objects.equals(thoiGianDangNhapCuoi, value.thoiGianDangNhapCuoi)
                && java.util.Objects.equals(thoiGianTao, value.thoiGianTao)
                && java.util.Objects.equals(thoiGianCapNhat, value.thoiGianCapNhat)
                && java.util.Objects.equals(thoiGianVoHieuHoa, value.thoiGianVoHieuHoa)
                && java.util.Objects.equals(lyDoVoHieuHoa, value.lyDoVoHieuHoa);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(ma);
        result = 31 * result + java.util.Objects.hashCode(maNhanVien);
        result = 31 * result + java.util.Objects.hashCode(tenDangNhap);
        result = 31 * result + java.util.Objects.hashCode(tenHienThi);
        result = 31 * result + java.util.Objects.hashCode(matKhauHash);
        result = 31 * result + java.util.Objects.hashCode(vaiTro);
        result = 31 * result + java.util.Objects.hashCode(trangThai);
        result = 31 * result + Integer.hashCode(soLanDangNhapSai);
        result = 31 * result + java.util.Objects.hashCode(thoiGianSaiDauTien);
        result = 31 * result + java.util.Objects.hashCode(thoiGianKhoa);
        result = 31 * result + java.util.Objects.hashCode(khoaDenLuc);
        result = 31 * result + java.util.Objects.hashCode(nguonKhoa);
        result = 31 * result + java.util.Objects.hashCode(lyDoKhoa);
        result = 31 * result + Boolean.hashCode(batBuocDoiMatKhau);
        result = 31 * result + java.util.Objects.hashCode(thoiGianCapNhatMatKhau);
        result = 31 * result + java.util.Objects.hashCode(thoiGianDangNhapCuoi);
        result = 31 * result + java.util.Objects.hashCode(thoiGianTao);
        result = 31 * result + java.util.Objects.hashCode(thoiGianCapNhat);
        result = 31 * result + java.util.Objects.hashCode(thoiGianVoHieuHoa);
        result = 31 * result + java.util.Objects.hashCode(lyDoVoHieuHoa);
        return result;
    }


    public String getMa() { return ma; }
    public String getMaNhanVien() { return maNhanVien; }
    public String getTenDangNhap() { return tenDangNhap; }
    public String getTenHienThi() { return tenHienThi; }
    public String getMatKhauHash() { return matKhauHash; }
    public VaiTroHeThong getVaiTro() { return vaiTro; }
    public TrangThaiTaiKhoan getTrangThai() { return trangThai; }
    public int getSoLanDangNhapSai() { return soLanDangNhapSai; }
    public LocalDateTime getThoiGianSaiDauTien() { return thoiGianSaiDauTien; }
    public LocalDateTime getThoiGianKhoa() { return thoiGianKhoa; }
    public LocalDateTime getKhoaDenLuc() { return khoaDenLuc; }
    public NguonKhoaTaiKhoan getNguonKhoa() { return nguonKhoa; }
    public String getLyDoKhoa() { return lyDoKhoa; }
    public boolean getBatBuocDoiMatKhau() { return batBuocDoiMatKhau; }
    public LocalDateTime getThoiGianCapNhatMatKhau() { return thoiGianCapNhatMatKhau; }
    public LocalDateTime getThoiGianDangNhapCuoi() { return thoiGianDangNhapCuoi; }
    public LocalDateTime getThoiGianTao() { return thoiGianTao; }
    public LocalDateTime getThoiGianCapNhat() { return thoiGianCapNhat; }
    public LocalDateTime getThoiGianVoHieuHoa() { return thoiGianVoHieuHoa; }
    public String getLyDoVoHieuHoa() { return lyDoVoHieuHoa; }
}
