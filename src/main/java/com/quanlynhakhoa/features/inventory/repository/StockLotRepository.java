package com.quanlynhakhoa.features.inventory.repository;

import com.quanlynhakhoa.features.inventory.entity.StockLot;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.Repository;

public interface StockLotRepository extends Repository<StockLot, Integer> {

    /** Khóa dòng lô để cộng tồn an toàn khi nhiều người nhập cùng lúc. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StockLot> findByMaThuocAndSoLo(String maThuoc, String soLo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StockLot> findByMaVatTuAndSoLo(String maVatTu, String soLo);

    List<StockLot> findBySoLuongTonGreaterThan(int soLuong);

    Optional<StockLot> findById(Integer maLo);

    List<StockLot> findByMaLoIn(java.util.Collection<Integer> maLo);

    /** Khóa dòng lô trước khi trừ tồn (xuất kho). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select l from LoHangJpaEntity l where l.maLo = :maLo")
    Optional<StockLot> findForUpdate(@org.springframework.data.repository.query.Param("maLo") Integer maLo);

    List<StockLot> findByMaThuocAndSoLuongTonGreaterThan(String maThuoc, int soLuong);

    List<StockLot> findByMaVatTuAndSoLuongTonGreaterThan(String maVatTu, int soLuong);

    StockLot saveAndFlush(StockLot lot);
}
