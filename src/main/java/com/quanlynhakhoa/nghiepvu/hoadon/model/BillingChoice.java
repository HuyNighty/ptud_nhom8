package com.quanlynhakhoa.nghiepvu.hoadon.model;

public record BillingChoice(String id, String label) {
    @Override public String toString() { return id + " · " + label; }
}
