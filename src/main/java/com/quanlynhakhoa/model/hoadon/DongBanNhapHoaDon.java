package com.quanlynhakhoa.model.hoadon;

import java.math.BigDecimal;


/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class DongBanNhapHoaDon {
    private final String serviceId;
    private final String serviceName;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final boolean medicine;

    public DongBanNhapHoaDon(String serviceId, String serviceName, int quantity, BigDecimal unitPrice, boolean medicine) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.medicine = medicine;
    }

    public String serviceId() { return serviceId; }
    public String serviceName() { return serviceName; }
    public int quantity() { return quantity; }
    public BigDecimal unitPrice() { return unitPrice; }
    public boolean medicine() { return medicine; }

        public DongBanNhapHoaDon(String serviceId, String serviceName, int quantity, BigDecimal unitPrice) {
            this(serviceId,serviceName,quantity,unitPrice,false);
        }
        public BigDecimal amount() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongBanNhapHoaDon value)) return false;
        return java.util.Objects.equals(serviceId, value.serviceId)
                && java.util.Objects.equals(serviceName, value.serviceName)
                && quantity == value.quantity
                && java.util.Objects.equals(unitPrice, value.unitPrice)
                && medicine == value.medicine;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(serviceId);
        result = 31 * result + java.util.Objects.hashCode(serviceName);
        result = 31 * result + Integer.hashCode(quantity);
        result = 31 * result + java.util.Objects.hashCode(unitPrice);
        result = 31 * result + Boolean.hashCode(medicine);
        return result;
    }

    @Override
    public String toString() {
        return "Line[" + "serviceId=" + serviceId + ", " + "serviceName=" + serviceName + ", " + "quantity=" + quantity + ", " + "unitPrice=" + unitPrice + ", " + "medicine=" + medicine + "]";
    }
}
