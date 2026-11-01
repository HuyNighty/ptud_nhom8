package com.quanlynhakhoa.quantri.lichlamviec.model;
import java.time.LocalTime;
public record CaLamViec(String id, String ten, LocalTime batDau, LocalTime ketThuc) {
    @Override public String toString() { return ten + " (" + batDau + "–" + ketThuc + ")"; }
}
