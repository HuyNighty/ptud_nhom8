package com.quanlynhakhoa.features.inventory.service;

import com.quanlynhakhoa.features.auth.enums.Permission;
import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.service.AuthorizationService;
import com.quanlynhakhoa.features.inventory.dto.request.StockMonitorAdjustRequest;
import com.quanlynhakhoa.features.inventory.dto.response.StockHistoryResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockLotViewResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockMonitorRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockMonitorSummaryResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockMovementResponse;
import com.quanlynhakhoa.features.inventory.entity.MedicalSupply;
import com.quanlynhakhoa.features.inventory.entity.Medicine;
import com.quanlynhakhoa.features.inventory.entity.StockInLine;
import com.quanlynhakhoa.features.inventory.entity.StockInReceipt;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.StockLevel;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.repository.DisposalRepository;
import com.quanlynhakhoa.features.inventory.repository.MedicalSupplyRepository;
import com.quanlynhakhoa.features.inventory.repository.MedicineRepository;
import com.quanlynhakhoa.features.inventory.repository.StockInReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StockLotRepository;
import com.quanlynhakhoa.features.inventory.repository.StockOutReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StockReturnReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StocktakeRepository;
import com.quanlynhakhoa.features.inventory.repository.SupplierRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
 * Theo dõi tồn kho (UC36): tồn từng mặt hàng so với định mức, giá trị kho, vị trí lưu trữ, điều chỉnh nhanh
 * thông tin theo dõi và lịch sử biến động.
 *
 * <p>Mức tồn so sánh tồn khả dụng (bỏ lô đã hết hạn) với định mức, cùng quy tắc với màn Tổng quan.
 * Giá trị tồn = tồn thực tế từng lô × giá nhập gần nhất của lô đó.</p>
 */
@Service
public class StockMonitorService {

    static final int MAX_MINIMUM = 1_000_000;
    static final int MAX_LOCATION_LENGTH = 50;

    private final AuthorizationService authorizationService;
    private final MedicineRepository medicineRepository;
    private final MedicalSupplyRepository supplyRepository;
    private final StockLotRepository lotRepository;
    private final StockInReceiptRepository stockInRepository;
    private final SupplierRepository supplierRepository;
    private final StockLedger ledger;

    public StockMonitorService(AuthorizationService authorizationService,
                               MedicineRepository medicineRepository,
                               MedicalSupplyRepository supplyRepository,
                               StockLotRepository lotRepository,
                               StockInReceiptRepository stockInRepository,
                               SupplierRepository supplierRepository,
                               StockOutReceiptRepository stockOutRepository,
                               StockReturnReceiptRepository returnRepository,
                               StocktakeRepository stocktakeRepository,
                               DisposalRepository disposalRepository) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.medicineRepository = Objects.requireNonNull(medicineRepository);
        this.supplyRepository = Objects.requireNonNull(supplyRepository);
        this.lotRepository = Objects.requireNonNull(lotRepository);
        this.stockInRepository = Objects.requireNonNull(stockInRepository);
        this.supplierRepository = Objects.requireNonNull(supplierRepository);
        this.ledger = new StockLedger(stockInRepository, supplierRepository, Objects.requireNonNull(stockOutRepository),
                Objects.requireNonNull(returnRepository), Objects.requireNonNull(stocktakeRepository),
                Objects.requireNonNull(disposalRepository), lotRepository);
    }

    // ------------------------------------------------------------------- đọc

    @Transactional(readOnly = true)
    public List<StockMonitorRowResponse> list(Set<Role> roles) {
        return list(roles, LocalDate.now());
    }

    @Transactional(readOnly = true)
    List<StockMonitorRowResponse> list(Set<Role> roles, LocalDate today) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        Map<String, List<StockLot>> lots = lotsByItem();
        Map<Integer, BigDecimal> prices = latestPrices();
        List<StockMonitorRowResponse> rows = new ArrayList<>();
        for (Medicine m : medicineRepository.search("")) {
            rows.add(toRow(InventoryItemType.THUOC, m.getMaThuoc(), medicineName(m), m.getDonViTinh(),
                    m.getDinhMucToiThieu(), m.getViTriLuuTru(), m.isDaDatHang(),
                    lots.getOrDefault(m.getMaThuoc(), List.of()), prices, today));
        }
        for (MedicalSupply s : supplyRepository.search("")) {
            rows.add(toRow(InventoryItemType.VAT_TU, s.getMaVatTu(), s.getTenVatTu(), s.getDonViTinh(),
                    s.getDinhMucToiThieu(), s.getViTriLuuTru(), s.isDaDatHang(),
                    lots.getOrDefault(s.getMaVatTu(), List.of()), prices, today));
        }
        return List.copyOf(rows);
    }

    @Transactional(readOnly = true)
    public StockMonitorSummaryResponse summarize(Set<Role> roles) {
        return summarize(list(roles));
    }

    /** Thống kê từ danh sách đã tải (tránh đọc lại khi màn hình đã có dữ liệu). */
    public static StockMonitorSummaryResponse summarize(List<StockMonitorRowResponse> rows) {
        long quantity = 0;
        BigDecimal value = BigDecimal.ZERO;
        int below = 0;
        int low = 0;
        int ok = 0;
        for (StockMonitorRowResponse r : rows) {
            quantity += r.tonThucTe();
            value = value.add(r.giaTri());
            switch (r.mucTon()) {
                case HET_HANG, THIEU -> below++;
                case SAP_HET -> low++;
                case DU -> ok++;
            }
        }
        return new StockMonitorSummaryResponse(rows.size(), quantity, value, below, low, ok);
    }

    /** Các lô đang còn và mọi biến động tồn đã ghi sổ của một mặt hàng. */
    @Transactional(readOnly = true)
    public StockHistoryResponse history(Set<Role> roles, InventoryItemType loai, String ma) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        Objects.requireNonNull(loai, "loai must not be null");
        String code = normalize(ma);
        StockMonitorRowResponse row = list(roles).stream()
                .filter(r -> r.loai() == loai && r.ma().equals(code)).findFirst()
                .orElseThrow(() -> notFound(loai, code));
        Map<Integer, BigDecimal> prices = latestPrices();
        Map<String, String> supplierNames = new HashMap<>();
        List<StockLot> current = new ArrayList<>(loai == InventoryItemType.THUOC
                ? lotRepository.findByMaThuocAndSoLuongTonGreaterThan(code, 0)
                : lotRepository.findByMaVatTuAndSoLuongTonGreaterThan(code, 0));
        current.sort(InventoryRules.FEFO);
        List<StockLotViewResponse> lots = new ArrayList<>();
        for (StockLot lot : current) {
            String supplier = supplierNames.computeIfAbsent(lot.getMaNhaCungCap(), id -> supplierRepository.findById(id)
                    .map(s -> s.getTenNhaCungCap()).orElse(id));
            lots.add(new StockLotViewResponse(lot.getMaLo(), lot.getSoLo(), lot.getHanSuDung(), lot.getSoLuongTon(),
                    prices.get(lot.getMaLo()), supplier));
        }
        List<StockLedger.Movement> raw = ledger.collect(code::equals);
        Set<String> ids = new HashSet<>();
        raw.forEach(m -> ids.add(m.employee()));
        Map<String, String> names = employeeNames(ids);
        int balance = 0;
        List<StockMovementResponse> movements = new ArrayList<>();
        for (StockLedger.Movement m : raw) {
            balance += m.delta();
            movements.add(new StockMovementResponse(m.time(), m.code(), m.type(), m.lot(), Math.max(m.delta(), 0),
                    Math.max(-m.delta(), 0), balance, m.party(),
                    m.employee() == null ? "—" : names.getOrDefault(m.employee(), m.employee())));
        }
        return new StockHistoryResponse(row, List.copyOf(lots), List.copyOf(movements));
    }

    // ------------------------------------------------------------------- ghi

    /** Điều chỉnh nhanh định mức, vị trí lưu trữ và cờ đã đặt hàng; số tồn không đổi. */
    @Transactional
    public StockMonitorRowResponse adjust(InventoryActor actor, InventoryItemType loai, String ma,
                                          StockMonitorAdjustRequest request) {
        Objects.requireNonNull(actor, "actor must not be null");
        authorizationService.require(actor.roles(), Permission.INVENTORY_MANAGE);
        Objects.requireNonNull(loai, "loai must not be null");
        if (request == null) {
            throw new InventoryValidationException("Thiếu dữ liệu điều chỉnh.");
        }
        if (request.dinhMucToiThieu() < 0 || request.dinhMucToiThieu() > MAX_MINIMUM) {
            throw new InventoryValidationException("Định mức tối thiểu phải từ 0 đến " + MAX_MINIMUM + ".");
        }
        String viTri = request.viTriLuuTru() == null || request.viTriLuuTru().isBlank()
                ? null : request.viTriLuuTru().strip();
        if (viTri != null && viTri.length() > MAX_LOCATION_LENGTH) {
            throw new InventoryValidationException("Vị trí lưu trữ tối đa " + MAX_LOCATION_LENGTH + " ký tự.");
        }
        String code = normalize(ma);
        if (loai == InventoryItemType.THUOC) {
            Medicine m = medicineRepository.findById(code).orElseThrow(() -> notFound(loai, code));
            m.updateMonitoring(request.dinhMucToiThieu(), viTri, request.daDatHang());
            medicineRepository.saveAndFlush(m);
        } else {
            MedicalSupply s = supplyRepository.findById(code).orElseThrow(() -> notFound(loai, code));
            s.updateMonitoring(request.dinhMucToiThieu(), viTri, request.daDatHang());
            supplyRepository.saveAndFlush(s);
        }
        return list(actor.roles()).stream().filter(r -> r.loai() == loai && r.ma().equals(code)).findFirst()
                .orElseThrow(() -> notFound(loai, code));
    }

    // ------------------------------------------------------------- chuyển đổi

    private static StockMonitorRowResponse toRow(InventoryItemType loai, String ma, String ten, String donVi,
                                                 int dinhMuc, String viTri, boolean daDatHang, List<StockLot> lots,
                                                 Map<Integer, BigDecimal> prices, LocalDate today) {
        int physical = 0;
        BigDecimal value = BigDecimal.ZERO;
        for (StockLot lot : lots) {
            physical += lot.getSoLuongTon();
            value = value.add(prices.getOrDefault(lot.getMaLo(), BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(lot.getSoLuongTon())));
        }
        InventoryRules.StockSnapshot usable = InventoryRules.stockByItem(lots, today)
                .getOrDefault(ma, InventoryRules.StockSnapshot.EMPTY);
        StockLevel level = InventoryRules.classifyStock(usable.tonKho(), dinhMuc);
        return new StockMonitorRowResponse(loai, ma, ten, donVi, physical, usable.tonKho(), dinhMuc, viTri, level,
                daDatHang, usable.hanGanNhat(), value);
    }

    private static String medicineName(Medicine m) {
        return m.getTenThuoc() + (m.getHamLuong() == null || m.getHamLuong().isBlank() ? "" : " " + m.getHamLuong());
    }

    private Map<String, List<StockLot>> lotsByItem() {
        Map<String, List<StockLot>> result = new HashMap<>();
        for (StockLot lot : lotRepository.findBySoLuongTonGreaterThan(0)) {
            result.computeIfAbsent(lot.maMatHang(), k -> new ArrayList<>()).add(lot);
        }
        return result;
    }

    /** Giá nhập của dòng phiếu nhập (đã nhập kho) mới nhất gắn với từng lô. */
    private Map<Integer, BigDecimal> latestPrices() {
        Map<Integer, StockInLine> latest = new HashMap<>();
        for (StockInReceipt r : stockInRepository.findAllWithLines()) {
            if (r.getTrangThai() != VoucherStatus.DA_HOAN_THANH) {
                continue;
            }
            for (StockInLine l : r.getLines()) {
                if (l.getMaLo() != null) {
                    latest.merge(l.getMaLo(), l, (a, b) -> Comparator.comparing(StockInLine::getMaChiTiet,
                            Comparator.nullsFirst(Comparator.naturalOrder())).compare(a, b) >= 0 ? a : b);
                }
            }
        }
        Map<Integer, BigDecimal> prices = new HashMap<>();
        latest.forEach((lot, line) -> prices.put(lot, line.getDonGiaNhap()));
        return prices;
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

    private static String normalize(String ma) {
        return ma == null ? "" : ma.trim().toUpperCase(Locale.ROOT);
    }

    private static InventoryValidationException notFound(InventoryItemType loai, String ma) {
        return new InventoryValidationException("Không tìm thấy " + (loai == InventoryItemType.THUOC ? "thuốc " : "vật tư ")
                + ma + ".");
    }
}
