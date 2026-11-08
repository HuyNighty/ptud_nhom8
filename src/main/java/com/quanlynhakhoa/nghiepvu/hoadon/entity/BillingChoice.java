package com.quanlynhakhoa.nghiepvu.hoadon.entity;

public record BillingChoice(String id, String label) {
    @Override public String toString() { return id + " · " + label; }
}
