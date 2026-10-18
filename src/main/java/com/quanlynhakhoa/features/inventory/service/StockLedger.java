package com.quanlynhakhoa.features.inventory.service;

import com.quanlynhakhoa.features.inventory.entity.Disposal;
import com.quanlynhakhoa.features.inventory.entity.DisposalLine;
import com.quanlynhakhoa.features.inventory.entity.StockInLine;
import com.quanlynhakhoa.features.inventory.entity.StockInReceipt;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.entity.StockOutLine;
import com.quanlynhakhoa.features.inventory.entity.StockOutReceipt;
import com.quanlynhakhoa.features.inventory.entity.StockReturnLine;
import com.quanlynhakhoa.features.inventory.entity.StockReturnReceipt;
import com.quanlynhakhoa.features.inventory.entity.Stocktake;
import com.quanlynhakhoa.features.inventory.entity.StocktakeLine;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.repository.DisposalRepository;
import com.quanlynhakhoa.features.inventory.repository.StockInReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StockLotRepository;
import com.quanlynhakhoa.features.inventory.repository.StockOutReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StockReturnReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StocktakeRepository;
import com.quanlynhakhoa.features.inventory.repository.SupplierRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Gom mọi biến động tồn đã ghi sổ (nhập kho, xuất kho, hoàn trả nhập lại, điều chỉnh kiểm kê, hủy) từ các chứng từ
 * đã hoàn thành, theo thời gian cũ → mới. Dùng chung cho sổ thuốc kiểm soát đặc biệt và lịch sử tồn kho.
 */
final class StockLedger {

    /** Một biến động; {@code delta} dương = tăng tồn. {@code employee} là mã nhân viên ghi sổ. */
    record Movement(LocalDateTime time, String code, String type, String item, String lot, int delta, String party,
                    String employee) {
    }

    private final StockInReceiptRepository stockInRepository;
    private final SupplierRepository supplierRepository;
    private final StockOutReceiptRepository stockOutRepository;
    private final StockReturnReceiptRepository returnRepository;
    private final StocktakeRepository stocktakeRepository;
    private final DisposalRepository disposalRepository;
    private final StockLotRepository lotRepository;

    StockLedger(StockInReceiptRepository stockInRepository, SupplierRepository supplierRepository,
                StockOutReceiptRepository stockOutRepository, StockReturnReceiptRepository returnRepository,
                StocktakeRepository stocktakeRepository, DisposalRepository disposalRepository,
                StockLotRepository lotRepository) {
        this.stockInRepository = stockInRepository;
        this.supplierRepository = supplierRepository;
        this.stockOutRepository = stockOutRepository;
        this.returnRepository = returnRepository;
        this.stocktakeRepository = stocktakeRepository;
        this.disposalRepository = disposalRepository;
        this.lotRepository = lotRepository;
    }

    /** Biến động của các mặt hàng có mã thỏa {@code itemFilter}, sắp theo thời gian rồi mã chứng từ. */
    List<Movement> collect(Predicate<String> itemFilter) {
        List<StockOutReceipt> outs = stockOutRepository.findAllWithLines();
        List<StockReturnReceipt> returns = returnRepository.findAllWithLines();
        List<Stocktake> stocktakes = stocktakeRepository.findAllWithLines();
        List<Disposal> disposals = disposalRepository.findAllWithLines();
        Map<Integer, StockLot> lots = lots(outs, returns, stocktakes, disposals);
        List<Movement> raw = new ArrayList<>();

        for (StockInReceipt r : stockInRepository.findAllWithLines()) {
            if (r.getTrangThai() != VoucherStatus.DA_HOAN_THANH) {
                continue;
            }
            String supplier = null;
            for (StockInLine l : r.getLines()) {
                if (!itemFilter.test(l.maMatHang())) {
                    continue;
                }
                if (supplier == null) {
                    supplier = supplierRepository.findById(r.getMaNhaCungCap())
                            .map(s -> s.getTenNhaCungCap()).orElse(r.getMaNhaCungCap());
                }
                raw.add(new Movement(r.getThoiDiemNhapKho(), r.getMaPhieuNhap(), "Nhập kho", l.maMatHang(), l.getSoLo(),
                        l.getSoLuong(), "NCC: " + supplier, r.getMaNhanVienNhapKho()));
            }
        }
        for (StockOutReceipt r : outs) {
            if (r.getTrangThai() != VoucherStatus.DA_HOAN_THANH) {
                continue;
            }
            for (StockOutLine l : r.getLines()) {
                StockLot lot = lots.get(l.getMaLo());
                if (lot != null && itemFilter.test(lot.maMatHang())) {
                    raw.add(new Movement(r.getThoiDiemXuatKho(), r.getMaPhieuXuat(), "Xuất kho", lot.maMatHang(),
                            lot.getSoLo(), -l.getSoLuong(), "Nhận: " + r.getNguoiNhan() + " ("
                            + r.getLyDoXuat().shortLabel() + ")", r.getMaNhanVienXuatKho()));
                }
            }
        }
        for (StockReturnReceipt r : returns) {
            if (r.getTrangThai() != VoucherStatus.DA_HOAN_THANH || !r.getLyDo().restock()) {
                continue;
            }
            for (StockReturnLine l : r.getLines()) {
                StockLot lot = lots.get(l.getMaLo());
                if (lot != null && itemFilter.test(lot.maMatHang())) {
                    raw.add(new Movement(r.getThoiDiemDuyet(), r.getMaPhieuHoanTra(), "Hoàn trả", lot.maMatHang(),
                            lot.getSoLo(), l.getSoLuong(), "Trả: " + r.getNguoiHoanTra() + " · gốc " + r.getMaPhieuXuat(),
                            r.getMaNhanVienDuyet()));
                }
            }
        }
        for (Stocktake s : stocktakes) {
            if (s.getTrangThai() != VoucherStatus.DA_HOAN_THANH) {
                continue;
            }
            for (StocktakeLine l : s.getLines()) {
                StockLot lot = lots.get(l.getMaLo());
                Integer diff = l.chenhLech();
                if (diff != null && diff != 0 && lot != null && itemFilter.test(lot.maMatHang())) {
                    raw.add(new Movement(s.getThoiDiemDuyet(), s.getMaKiemKe(), "Kiểm kê", lot.maMatHang(), lot.getSoLo(),
                            diff, "Lệch: " + l.getGhiChu(), s.getMaNhanVienDuyet()));
                }
            }
        }
        for (Disposal d : disposals) {
            if (d.getTrangThai() != VoucherStatus.DA_HOAN_THANH) {
                continue;
            }
            for (DisposalLine l : d.getLines()) {
                StockLot lot = lots.get(l.getMaLo());
                if (lot != null && itemFilter.test(lot.maMatHang())) {
                    raw.add(new Movement(d.getThoiDiemDuyet(), d.getMaPhieuHuy(), "Hủy", lot.maMatHang(), lot.getSoLo(),
                            -l.getSoLuong(), "Lý do: " + (d.getMoTa() == null ? d.getLyDo().label() : d.getMoTa()),
                            d.getMaNhanVienDuyet()));
                }
            }
        }
        raw.sort(Comparator.comparing(Movement::time, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(Movement::code));
        return raw;
    }

    /** Mọi lô được nhắc tới trong chứng từ (kể cả lô đã hết tồn). */
    private Map<Integer, StockLot> lots(List<StockOutReceipt> outs, List<StockReturnReceipt> returns,
                                        List<Stocktake> stocktakes, List<Disposal> disposals) {
        Set<Integer> ids = new HashSet<>();
        outs.forEach(r -> r.getLines().forEach(l -> ids.add(l.getMaLo())));
        returns.forEach(r -> r.getLines().forEach(l -> ids.add(l.getMaLo())));
        stocktakes.forEach(r -> r.getLines().forEach(l -> ids.add(l.getMaLo())));
        disposals.forEach(r -> r.getLines().forEach(l -> ids.add(l.getMaLo())));
        Map<Integer, StockLot> lots = new HashMap<>();
        if (!ids.isEmpty()) {
            lotRepository.findByMaLoIn(ids).forEach(l -> lots.put(l.getMaLo(), l));
        }
        return lots;
    }
}
