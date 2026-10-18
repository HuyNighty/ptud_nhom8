package com.quanlynhakhoa.features.inventory.support;

import com.quanlynhakhoa.features.inventory.entity.StockInLine;
import com.quanlynhakhoa.features.inventory.repository.StockInLineRepository;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/** TEST-ONLY: đọc dòng phiếu nhập từ {@link InMemoryStockInReceiptRepository}. */
public final class InMemoryStockInLineRepository implements StockInLineRepository {
    private final InMemoryStockInReceiptRepository receipts;

    public InMemoryStockInLineRepository(InMemoryStockInReceiptRepository receipts) {
        this.receipts = receipts;
    }

    @Override
    public Optional<StockInLine> findFirstByMaLoOrderByMaChiTietDesc(Integer maLo) {
        return receipts.findAllWithLines().stream()
                .flatMap(r -> r.getLines().stream())
                .filter(l -> Objects.equals(l.getMaLo(), maLo))
                .max(Comparator.comparing(StockInLine::getMaChiTiet));
    }
}
