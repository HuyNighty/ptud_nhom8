package com.quanlynhakhoa.features.inventory.repository;

import com.quanlynhakhoa.features.inventory.entity.StockInLine;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface StockInLineRepository extends Repository<StockInLine, Integer> {

    /** Dòng nhập gần nhất của lô — lấy đơn giá nhập làm giá vốn khi xuất. */
    Optional<StockInLine> findFirstByMaLoOrderByMaChiTietDesc(Integer maLo);
}
