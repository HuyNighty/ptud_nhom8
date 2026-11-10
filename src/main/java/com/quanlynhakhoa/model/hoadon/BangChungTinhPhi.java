package com.quanlynhakhoa.model.hoadon;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class BangChungTinhPhi {
    private final String kind;
    private final String sourceId;
    private final String serviceId;
    private final String name;
    private final BigDecimal price;
    private final boolean active;
    private final boolean clinicalService;

    public BangChungTinhPhi(String kind, String sourceId, String serviceId, String name, BigDecimal price, boolean active, boolean clinicalService) {
        this.kind = kind;
        this.sourceId = sourceId;
        this.serviceId = serviceId;
        this.name = name;
        this.price = price;
        this.active = active;
        this.clinicalService = clinicalService;
    }

    public String kind() { return kind; }
    public String sourceId() { return sourceId; }
    public String serviceId() { return serviceId; }
    public String name() { return name; }
    public BigDecimal price() { return price; }
    public boolean active() { return active; }
    public boolean clinicalService() { return clinicalService; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BangChungTinhPhi value)) return false;
        return java.util.Objects.equals(kind, value.kind)
                && java.util.Objects.equals(sourceId, value.sourceId)
                && java.util.Objects.equals(serviceId, value.serviceId)
                && java.util.Objects.equals(name, value.name)
                && java.util.Objects.equals(price, value.price)
                && active == value.active
                && clinicalService == value.clinicalService;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(kind);
        result = 31 * result + java.util.Objects.hashCode(sourceId);
        result = 31 * result + java.util.Objects.hashCode(serviceId);
        result = 31 * result + java.util.Objects.hashCode(name);
        result = 31 * result + java.util.Objects.hashCode(price);
        result = 31 * result + Boolean.hashCode(active);
        result = 31 * result + Boolean.hashCode(clinicalService);
        return result;
    }

    @Override
    public String toString() {
        return "Evidence[" + "kind=" + kind + ", " + "sourceId=" + sourceId + ", " + "serviceId=" + serviceId + ", " + "name=" + name + ", " + "price=" + price + ", " + "active=" + active + ", " + "clinicalService=" + clinicalService + "]";
    }
}
