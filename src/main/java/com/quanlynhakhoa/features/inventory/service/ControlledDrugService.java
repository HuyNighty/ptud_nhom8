package com.quanlynhakhoa.features.inventory.service;

import com.quanlynhakhoa.features.auth.enums.Permission;
import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.service.AuthorizationService;
import com.quanlynhakhoa.features.inventory.dto.response.ControlledDrugDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.ControlledDrugRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.ControlledDrugSummaryResponse;
import com.quanlynhakhoa.features.inventory.dto.response.ControlledLedgerEntry;
import com.quanlynhakhoa.features.inventory.dto.response.ControlledLotResponse;
import com.quanlynhakhoa.features.inventory.entity.Medicine;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.entity.StockOutReceipt;
import com.quanlynhakhoa.features.inventory.enums.ControlledDrugGroup;
import com.quanlynhakhoa.features.inventory.enums.ExpiryLevel;
import com.quanlynhakhoa.features.inventory.enums.NoticeSeverity;
import com.quanlynhakhoa.features.inventory.enums.StockLevel;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.repository.DisposalRepository;
import com.quanlynhakhoa.features.inventory.repository.MedicineRepository;
import com.quanlynhakhoa.features.inventory.repository.StockInReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StockLotRepository;
import com.quanlynhakhoa.features.inventory.repository.StockOutReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StockReturnReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StocktakeRepository;
import com.quanlynhakhoa.features.inventory.repository.SupplierRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thuốc kiểm soát đặc biệt (UC33): danh mục thuốc KSĐB kèm tồn/hạn/trạng thái, phân loại nhóm kiểm soát và
 * sổ theo dõi (nhật ký sử dụng) dựng từ các chứng từ đã ghi sổ — không có bảng riêng nên không thể lệch với tồn.
 * Xuất kho thuốc KSĐB dùng phiếu xuất thường, nhưng {@link StockOutService} bắt buộc phê duyệt kép.
 */
@Service
public class ControlledDrugService {

    private final AuthorizationService authorizationService;
    private final MedicineRepository medicineRepository;
    private final StockLotRepository lotRepository;
    private final StockInReceiptRepository stockInRepository;
    private final SupplierRepository supplierRepository;
    private final StockOutReceiptRepository stockOutRepository;
    private final StockReturnReceiptRepository returnRepository;
    private final StocktakeRepository stocktakeRepository;
    private final DisposalRepository disposalRepository;

    public ControlledDrugService(AuthorizationService authorizationService,
                                 MedicineRepository medicineRepository,
                                 StockLotRepository lotRepository,
                                 StockInReceiptRepository stockInRepository,
                                 SupplierRepository supplierRepository,
                                 StockOutReceiptRepository stockOutRepository,
                                 StockReturnReceiptRepository returnRepository,
                                 StocktakeRepository stocktakeRepository,
                                 DisposalRepository disposalRepository) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.medicineRepository = Objects.requireNonNull(medicineRepository);
        this.lotRepository = Objects.requireNonNull(lotRepository);
        this.stockInRepository = Objects.requireNonNull(stockInRepository);
        this.supplierRepository = Objects.requireNonNull(supplierRepository);
        this.stockOutRepository = Objects.requireNonNull(stockOutRepository);
        this.returnRepository = Objects.requireNonNull(returnRepository);
        this.stocktakeRepository = Objects.requireNonNull(stocktakeRepository);
        this.disposalRepository = Objects.requireNonNull(disposalRepository);
    }

    // ------------------------------------------------------------- danh mục

    @Transactional(readOnly = true)
    public List<ControlledDrugRowResponse> list(Set<Role> roles) {
        return list(roles, LocalDate.now());
    }

    @Transactional(readOnly = true)
    List<ControlledDrugRowResponse> list(Set<Role> roles, LocalDate today) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        Map<String, List<StockLot>> lotsByDrug = lotsByDrug();
        List<ControlledDrugRowResponse> rows = new ArrayList<>();
        for (Medicine m : medicineRepository.findByKiemSoatDacBietTrueOrderByMaThuoc()) {
            rows.add(toRow(m, lotsByDrug.getOrDefault(m.getMaThuoc(), List.of()), today));
        }
        return List.copyOf(rows);
    }

    @Transactional(readOnly = true)
    public ControlledDrugSummaryResponse summarize(Set<Role> roles) {
        return summarize(roles, LocalDate.now());
    }

    @Transactional(readOnly = true)
    ControlledDrugSummaryResponse summarize(Set<Role> roles, LocalDate today) {
        List<ControlledDrugRowResponse> rows = list(roles, today);
        int stock = rows.stream().mapToInt(ControlledDrugRowResponse::tonKho).sum();
        int expiring = (int) rows.stream().filter(ControlledDrugRowResponse::canHan).count();
        Set<String> controlled = new HashSet<>();
        rows.forEach(r -> controlled.add(r.maThuoc()));
        Map<Integer, StockLot> lots = allLots();
        int pending = 0;
        for (StockOutReceipt r : stockOutRepository.findAllWithLines()) {
            if (r.getTrangThai() == VoucherStatus.CHO_XU_LY && r.getLines().stream()
                    .anyMatch(l -> controlled.contains(drugOf(lots.get(l.getMaLo()))))) {
                pending++;
            }
        }
        return new ControlledDrugSummaryResponse(rows.size(), stock, expiring, pending);
    }

    @Transactional(readOnly = true)
    public ControlledDrugDetailResponse getDetail(Set<Role> roles, String maThuoc) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        Medicine m = controlled(maThuoc);
        LocalDate today = LocalDate.now();
        List<StockLot> lots = new ArrayList<>(lotsByDrug().getOrDefault(m.getMaThuoc(), List.of()));
        lots.sort(InventoryRules.FEFO);
        List<ControlledLotResponse> lotRows = lots.stream().map(l -> new ControlledLotResponse(l.getMaLo(), l.getSoLo(),
                l.getHanSuDung(), l.getSoLuongTon(), InventoryRules.classifyExpiry(l.getHanSuDung(), today))).toList();
        return new ControlledDrugDetailResponse(toRow(m, lots, today), lotRows);
    }

    /** Phân loại nhóm kiểm soát (null = bỏ phân loại). */
    @Transactional
    public ControlledDrugDetailResponse classify(InventoryActor actor, String maThuoc, ControlledDrugGroup group) {
        Objects.requireNonNull(actor, "actor must not be null");
        authorizationService.require(actor.roles(), Permission.INVENTORY_MANAGE);
        Medicine m = controlled(maThuoc);
        m.classify(group);
        medicineRepository.saveAndFlush(m);
        return getDetail(actor.roles(), m.getMaThuoc());
    }

    // ------------------------------------------------------------ sổ theo dõi

    /**
     * Nhật ký sử dụng thuốc KSĐB theo thời gian (cũ → mới), có cột tồn sau mỗi biến động.
     *
     * @param maThuoc null = mọi thuốc kiểm soát đặc biệt
     */
    @Transactional(readOnly = true)
    public List<ControlledLedgerEntry> ledger(Set<Role> roles, String maThuoc) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        Map<String, Medicine> drugs = new HashMap<>();
        for (Medicine m : medicineRepository.findByKiemSoatDacBietTrueOrderByMaThuoc()) {
            drugs.put(m.getMaThuoc(), m);
        }
        String only = maThuoc == null || maThuoc.isBlank() ? null : maThuoc.trim().toUpperCase(Locale.ROOT);
        if (only != null && !drugs.containsKey(only)) {
            throw new InventoryValidationException("Thuốc " + only + " không thuộc danh mục kiểm soát đặc biệt.");
        }
        List<StockLedger.Movement> raw = new StockLedger(stockInRepository, supplierRepository, stockOutRepository,
                returnRepository, stocktakeRepository, disposalRepository, lotRepository).collect(drugs::containsKey);
        Set<String> employeeIds = new HashSet<>();
        raw.forEach(m -> employeeIds.add(m.employee()));
        Map<String, String> names = employeeNames(employeeIds);
        Map<String, Integer> balance = new HashMap<>();
        List<ControlledLedgerEntry> result = new ArrayList<>();
        for (StockLedger.Movement e : raw) {
            int after = balance.merge(e.item(), e.delta(), Integer::sum);
            if (only == null || only.equals(e.item())) {
                Medicine m = drugs.get(e.item());
                result.add(new ControlledLedgerEntry(e.time(), e.code(), e.type(), e.item(),
                        m.getTenThuoc() + (m.getHamLuong() == null ? "" : " " + m.getHamLuong()), e.lot(),
                        Math.max(e.delta(), 0), Math.max(-e.delta(), 0), after, e.party(),
                        e.employee() == null ? "—" : names.getOrDefault(e.employee(), e.employee())));
            }
        }
        return List.copyOf(result);
    }


    // ----------------------------------------------------------- trạng thái

    /** Ghép trạng thái tồn ("Hết hàng"/"Thiếu"/"Sắp hết") và hạn ("Hết hạn"/"Cận hạn") theo Figma. */
    static ControlledDrugRowResponse toRow(Medicine m, List<StockLot> lots, LocalDate today) {
        int total = 0;
        LocalDate nearest = null;
        boolean expired = false;
        boolean expiringSoon = false;
        for (StockLot l : lots) {
            if (l.getSoLuongTon() <= 0) {
                continue;
            }
            total += l.getSoLuongTon();
            if (l.getHanSuDung() != null && (nearest == null || l.getHanSuDung().isBefore(nearest))) {
                nearest = l.getHanSuDung();
            }
            ExpiryLevel level = InventoryRules.classifyExpiry(l.getHanSuDung(), today);
            expired |= level == ExpiryLevel.DA_HET_HAN;
            expiringSoon |= level == ExpiryLevel.CON_TOI_DA_7_NGAY || level == ExpiryLevel.CON_TOI_DA_30_NGAY;
        }
        int usable = InventoryRules.stockByItem(lots, today)
                .getOrDefault(m.getMaThuoc(), InventoryRules.StockSnapshot.EMPTY).tonKho();
        StockLevel stock = InventoryRules.classifyStock(usable, m.getDinhMucToiThieu());
        List<String> parts = new ArrayList<>();
        NoticeSeverity severity = NoticeSeverity.INFO;
        switch (stock) {
            case HET_HANG -> {
                parts.add("Hết hàng");
                severity = NoticeSeverity.CRITICAL;
            }
            case THIEU -> {
                parts.add("Thiếu");
                severity = NoticeSeverity.CRITICAL;
            }
            case SAP_HET -> {
                parts.add("Sắp hết");
                severity = NoticeSeverity.WARNING;
            }
            case DU -> { }
        }
        if (expired) {
            parts.add("Hết hạn");
            severity = NoticeSeverity.CRITICAL;
        } else if (expiringSoon) {
            parts.add("Cận hạn");
            severity = NoticeSeverity.CRITICAL;
        }
        String label = parts.isEmpty() ? "An toàn" : String.join("/", parts);
        return new ControlledDrugRowResponse(m.getMaThuoc(), m.getTenThuoc(), m.getHamLuong(), m.getHoatChat(),
                m.getNhomKiemSoat(), m.getDonViTinh(), m.getDinhMucToiThieu(), total, nearest, label, severity,
                stock != StockLevel.DU, expired || expiringSoon);
    }

    // -------------------------------------------------------------- tiện ích

    private Medicine controlled(String maThuoc) {
        String ma = maThuoc == null ? "" : maThuoc.trim().toUpperCase(Locale.ROOT);
        Medicine m = medicineRepository.findById(ma)
                .orElseThrow(() -> new InventoryValidationException("Không tìm thấy thuốc " + ma + "."));
        if (!m.isKiemSoatDacBiet()) {
            throw new InventoryValidationException("Thuốc " + ma + " không thuộc danh mục kiểm soát đặc biệt.");
        }
        return m;
    }

    private Map<String, List<StockLot>> lotsByDrug() {
        Map<String, List<StockLot>> result = new HashMap<>();
        for (StockLot l : lotRepository.findBySoLuongTonGreaterThan(0)) {
            if (l.getMaThuoc() != null) {
                result.computeIfAbsent(l.getMaThuoc(), k -> new ArrayList<>()).add(l);
            }
        }
        return result;
    }

    /** Mọi lô được nhắc tới trong chứng từ (kể cả lô đã hết tồn). */
    private Map<Integer, StockLot> allLots() {
        Set<Integer> ids = new HashSet<>();
        stockOutRepository.findAllWithLines().forEach(r -> r.getLines().forEach(l -> ids.add(l.getMaLo())));
        returnRepository.findAllWithLines().forEach(r -> r.getLines().forEach(l -> ids.add(l.getMaLo())));
        stocktakeRepository.findAllWithLines().forEach(r -> r.getLines().forEach(l -> ids.add(l.getMaLo())));
        disposalRepository.findAllWithLines().forEach(r -> r.getLines().forEach(l -> ids.add(l.getMaLo())));
        Map<Integer, StockLot> lots = new HashMap<>();
        if (!ids.isEmpty()) {
            lotRepository.findByMaLoIn(ids).forEach(l -> lots.put(l.getMaLo(), l));
        }
        return lots;
    }

    private static String drugOf(StockLot lot) {
        return lot == null ? null : lot.getMaThuoc();
    }

    private Map<String, String> employeeNames(Set<String> ids) {
        Map<String, String> names = new HashMap<>();
        ids.remove(null);
        if (!ids.isEmpty()) {
            for (Object[] row : stockInRepository.findEmployeeNames(ids)) {
                names.put((String) row[0], (String) row[1]);
            }
        }
        return names;
    }
}
