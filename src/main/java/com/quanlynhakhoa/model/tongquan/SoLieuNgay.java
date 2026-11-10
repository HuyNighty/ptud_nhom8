package com.quanlynhakhoa.model.tongquan;






import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;




/** Snapshot bất biến, giữ API accessor và value semantics của record gốc. */
public final class SoLieuNgay {
    private final LocalDate date;
    private final Long visits;
    private final BigDecimal collected;

    public SoLieuNgay(LocalDate date, Long visits, BigDecimal collected) {
        this.date = date;
        this.visits = visits;
        this.collected = collected;
    }

    public LocalDate date() { return date; }
    public Long visits() { return visits; }
    public BigDecimal collected() { return collected; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SoLieuNgay value)) return false;
        return java.util.Objects.equals(date, value.date)
                && java.util.Objects.equals(visits, value.visits)
                && java.util.Objects.equals(collected, value.collected);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + java.util.Objects.hashCode(this.date);
        result = 31 * result + java.util.Objects.hashCode(this.visits);
        result = 31 * result + java.util.Objects.hashCode(this.collected);
        return result;
    }

    @Override
    public String toString() {
        return "Day[" + "date=" + date + ", " + "visits=" + visits + ", " + "collected=" + collected + "]";
    }
}
