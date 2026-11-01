package com.quanlynhakhoa.nghiepvu.phieukham.model;

import java.util.List;

public record DuLieuCanLamSang(
        List<DichVuCanLamSang> dichVu,
        List<ChiDinhCanLamSang> chiDinh) {

    public DuLieuCanLamSang {
        dichVu = List.copyOf(dichVu);
        chiDinh = List.copyOf(chiDinh);
    }
}
