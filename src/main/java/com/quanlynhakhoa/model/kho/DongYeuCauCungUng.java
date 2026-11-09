package com.quanlynhakhoa.model.kho;

/** Immutable snapshot; retains the original record API and value semantics. */
public final class DongYeuCauCungUng {
    private final String itemType;
    private final String itemCode;
    private final int quantity;

    public DongYeuCauCungUng(String itemType, String itemCode, int quantity) {
        this.itemType = itemType;
        this.itemCode = itemCode;
        this.quantity = quantity;
    }

    public String itemType() { return itemType; }
    public String itemCode() { return itemCode; }
    public int quantity() { return quantity; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DongYeuCauCungUng value)) return false;
        return java.util.Objects.equals(itemType, value.itemType)
                && java.util.Objects.equals(itemCode, value.itemCode)
                && quantity == value.quantity;
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(itemType);
        result = 31 * result + java.util.Objects.hashCode(itemCode);
        result = 31 * result + Integer.hashCode(quantity);
        return result;
    }

    @Override
    public String toString() {
        return "RequestLine[" + "itemType=" + itemType + ", " + "itemCode=" + itemCode + ", " + "quantity=" + quantity + "]";
    }
}
