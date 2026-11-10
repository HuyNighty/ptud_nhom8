package com.quanlynhakhoa.model.tongquan;












import java.util.*;




/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DuLieuTongQuan {
    private final TomTatTongQuan tomTat;
    private final List<LichHenTongQuan> lichHen;
    private final List<CongViecTongQuan> congViec;
    private final List<CanhBaoTonKho> canhBao;
    private final List<SoLieuNgay> duLieuNgay;
    private final List<String> canhBaoTaiDuLieu;

    public DuLieuTongQuan(TomTatTongQuan tomTat, List<LichHenTongQuan> lichHen, List<CongViecTongQuan> congViec, List<CanhBaoTonKho> canhBao, List<SoLieuNgay> duLieuNgay, List<String> canhBaoTaiDuLieu) {
        lichHen=List.copyOf(lichHen);congViec=List.copyOf(congViec);canhBao=List.copyOf(canhBao);
        duLieuNgay=List.copyOf(duLieuNgay);canhBaoTaiDuLieu=List.copyOf(canhBaoTaiDuLieu);
        this.tomTat = tomTat;
        this.lichHen = lichHen;
        this.congViec = congViec;
        this.canhBao = canhBao;
        this.duLieuNgay = duLieuNgay;
        this.canhBaoTaiDuLieu = canhBaoTaiDuLieu;
    }

    public TomTatTongQuan summary() { return tomTat; }
    public List<LichHenTongQuan> appointments() { return lichHen; }
    public List<CongViecTongQuan> work() { return congViec; }
    public List<CanhBaoTonKho> alerts() { return canhBao; }
    public List<SoLieuNgay> days() { return duLieuNgay; }
    public List<String> warnings() { return canhBaoTaiDuLieu; }



    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DuLieuTongQuan value)) return false;
        return java.util.Objects.equals(tomTat, value.tomTat)
                && java.util.Objects.equals(lichHen, value.lichHen)
                && java.util.Objects.equals(congViec, value.congViec)
                && java.util.Objects.equals(canhBao, value.canhBao)
                && java.util.Objects.equals(duLieuNgay, value.duLieuNgay)
                && java.util.Objects.equals(canhBaoTaiDuLieu, value.canhBaoTaiDuLieu);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.tomTat);
        result = 31 * result + java.util.Objects.hashCode(this.lichHen);
        result = 31 * result + java.util.Objects.hashCode(this.congViec);
        result = 31 * result + java.util.Objects.hashCode(this.canhBao);
        result = 31 * result + java.util.Objects.hashCode(this.duLieuNgay);
        result = 31 * result + java.util.Objects.hashCode(this.canhBaoTaiDuLieu);
        return result;
    }

    @Override
    public String toString() {
        return "Overview[" + "summary=" + tomTat + ", " + "appointments=" + lichHen + ", " + "work=" + congViec + ", " + "alerts=" + canhBao + ", " + "days=" + duLieuNgay + ", " + "warnings=" + canhBaoTaiDuLieu + "]";
    }
}
