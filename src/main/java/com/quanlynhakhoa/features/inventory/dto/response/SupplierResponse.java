package com.quanlynhakhoa.features.inventory.dto.response;

public record SupplierResponse(String ma, String ten, String soDienThoai, String diaChi, String email,
                               boolean dangHopTac) {
    @Override
    public String toString() {
        return ten;
    }
}
