package com.quanlynhakhoa.features.inventory.support;

import com.quanlynhakhoa.features.inventory.entity.StockInReceipt;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.repository.StockInReceiptRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** TEST-ONLY: StockInReceiptRepository trong bộ nhớ; tên nhân viên đăng ký qua {@link #addEmployee}. */
public final class InMemoryStockInReceiptRepository implements StockInReceiptRepository {
    private final TreeMap<String, StockInReceipt> rows = new TreeMap<>();
    private final Map<String, String> employees = new HashMap<>();
    private int lineId = 1;

    public void addEmployee(String maNhanVien, String hoTen) {
        employees.put(maNhanVien, hoTen);
    }

    @Override
    public Optional<StockInReceipt> findWithLines(String ma) {
        return Optional.ofNullable(rows.get(ma));
    }

    @Override
    public Optional<StockInReceipt> findForUpdate(String ma) {
        return Optional.ofNullable(rows.get(ma));
    }

    @Override
    public List<StockInReceipt> findAllWithLines() {
        return rows.values().stream()
                .sorted(Comparator.comparing(StockInReceipt::getNgayNhap).reversed()
                        .thenComparing(StockInReceipt::getMaPhieuNhap, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public long countByTrangThai(VoucherStatus trangThai) {
        return rows.values().stream().filter(r -> r.getTrangThai() == trangThai).count();
    }

    @Override
    public long countByThoiDiemNhapKhoBetween(LocalDateTime from, LocalDateTime to) {
        return rows.values().stream().filter(r -> r.getThoiDiemNhapKho() != null
                && !r.getThoiDiemNhapKho().isBefore(from) && !r.getThoiDiemNhapKho().isAfter(to)).count();
    }

    @Override
    public StockInReceipt saveAndFlush(StockInReceipt receipt) {
        receipt.getLines().forEach(line -> {
            if (line.getMaChiTiet() == null) {
                InMemoryStockLotRepository.setField(line, "maChiTiet", lineId++);
            }
        });
        rows.put(receipt.getMaPhieuNhap(), receipt);
        return receipt;
    }

    @Override
    public Integer lockAndFindMaxCodeNumber() {
        return rows.keySet().stream().map(k -> Integer.valueOf(k.substring(2))).max(Integer::compare).orElse(null);
    }

    @Override
    public List<Object[]> findEmployeeNames(Collection<String> ids) {
        List<Object[]> result = new ArrayList<>();
        for (String id : ids) {
            if (employees.containsKey(id)) {
                result.add(new Object[] {id, employees.get(id)});
            }
        }
        return result;
    }
}
