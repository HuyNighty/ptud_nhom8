package com.quanlynhakhoa.features.inventory.service;

import com.quanlynhakhoa.features.auth.enums.Permission;
import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.service.AuthorizationService;
import com.quanlynhakhoa.features.inventory.dto.request.EmergencyKitItemRequest;
import com.quanlynhakhoa.features.inventory.dto.request.EmergencyKitRequest;
import com.quanlynhakhoa.features.inventory.dto.request.KitCheckLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.KitCheckRequest;
import com.quanlynhakhoa.features.inventory.dto.request.KitRestockLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.KitRestockRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockOutLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockOutRequest;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitItemResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitLogLineResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitLogResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.EmergencyKitSummaryResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockOutDetailResponse;
import com.quanlynhakhoa.features.inventory.entity.EmergencyKit;
import com.quanlynhakhoa.features.inventory.entity.EmergencyKitItem;
import com.quanlynhakhoa.features.inventory.entity.EmergencyKitLog;
import com.quanlynhakhoa.features.inventory.entity.EmergencyKitLogLine;
import com.quanlynhakhoa.features.inventory.entity.MedicalSupply;
import com.quanlynhakhoa.features.inventory.entity.Medicine;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.enums.EmergencyKitStatus;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.KitCheckResult;
import com.quanlynhakhoa.features.inventory.enums.KitLogType;
import com.quanlynhakhoa.features.inventory.enums.StockOutReason;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.repository.EmergencyKitLogRepository;
import com.quanlynhakhoa.features.inventory.repository.EmergencyKitRepository;
import com.quanlynhakhoa.features.inventory.repository.MedicalSupplyRepository;
import com.quanlynhakhoa.features.inventory.repository.MedicineRepository;
import com.quanlynhakhoa.features.inventory.repository.StockInReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StockLotRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hộp thuốc cấp cứu (UC34): danh mục hộp cấu hình được, kiểm tra định kỳ, bổ sung / thay thế từ kho.
 *
 * <p>Mục đạt khi số hiện có ≥ tối thiểu và hạn dùng còn hơn ngưỡng của hộp (mặc định 30 ngày); chu kỳ kiểm tra
 * mặc định 7 ngày (THIET_KE_DU_LIEU.md, giả định 12). Bổ sung / thay thế lập phiếu xuất kho và xuất ngay trong
 * cùng transaction (FEFO, chỉ lấy lô còn hạn trên ngưỡng) nên tồn kho luôn có chứng từ. Thuốc kiểm soát đặc biệt
 * không bổ sung trực tiếp được vì cần phê duyệt kép ở màn Xuất kho (UC33).</p>
 */
@Service
public class EmergencyKitService {

    static final int DEFAULT_CHECK_CYCLE_DAYS = 7;
    static final int DEFAULT_EXPIRY_THRESHOLD_DAYS = 30;
    static final int MAX_ITEMS = 100;
    static final int MAX_QUANTITY = 100_000;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AuthorizationService authorizationService;
    private final EmergencyKitRepository kitRepository;
    private final EmergencyKitLogRepository logRepository;
    private final StockLotRepository lotRepository;
    private final MedicineRepository medicineRepository;
    private final MedicalSupplyRepository supplyRepository;
    private final StockInReceiptRepository stockInRepository;
    private final StockOutService stockOutService;

    public EmergencyKitService(AuthorizationService authorizationService,
                               EmergencyKitRepository kitRepository,
                               EmergencyKitLogRepository logRepository,
                               StockLotRepository lotRepository,
                               MedicineRepository medicineRepository,
                               MedicalSupplyRepository supplyRepository,
                               StockInReceiptRepository stockInRepository,
                               StockOutService stockOutService) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.kitRepository = Objects.requireNonNull(kitRepository);
        this.logRepository = Objects.requireNonNull(logRepository);
        this.lotRepository = Objects.requireNonNull(lotRepository);
        this.medicineRepository = Objects.requireNonNull(medicineRepository);
        this.supplyRepository = Objects.requireNonNull(supplyRepository);
        this.stockInRepository = Objects.requireNonNull(stockInRepository);
        this.stockOutService = Objects.requireNonNull(stockOutService);
    }

    // ------------------------------------------------------------------- đọc

    @Transactional(readOnly = true)
    public List<EmergencyKitRowResponse> list(Set<Role> roles) {
        return list(roles, LocalDate.now());
    }

    @Transactional(readOnly = true)
    List<EmergencyKitRowResponse> list(Set<Role> roles, LocalDate today) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        Context ctx = context(today);
        List<EmergencyKitRowResponse> rows = new ArrayList<>();
        for (EmergencyKit kit : kitRepository.findAllWithItems()) {
            rows.add(toRow(kit, ctx));
        }
        return List.copyOf(rows);
    }

    @Transactional(readOnly = true)
    public EmergencyKitSummaryResponse summarize(Set<Role> roles) {
        return summarize(roles, LocalDate.now());
    }

    @Transactional(readOnly = true)
    EmergencyKitSummaryResponse summarize(Set<Role> roles, LocalDate today) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        Context ctx = context(today);
        List<EmergencyKit> kits = kitRepository.findAllWithItems();
        Set<String> distinctItems = new HashSet<>();
        int restock = 0;
        int expiry = 0;
        int due = 0;
        List<String> expiryWarnings = new ArrayList<>();
        List<String> shortageWarnings = new ArrayList<>();
        for (EmergencyKit kit : kits) {
            kit.getItems().forEach(i -> distinctItems.add(i.maMatHang()));
            EmergencyKitStatus status = status(kit, ctx);
            if (status.needsRestock()) {
                restock++;
            }
            if (status.expiryProblem()) {
                expiry++;
            }
            if (checkDue(kit, ctx)) {
                due++;
            }
            for (EmergencyKitItem item : kit.getItems()) {
                ItemInfo info = ctx.info(item);
                KitCheckResult r = evaluate(item.getSoLuongHienCo(), item.getHanDung(), item.getSoLuongToiThieu(),
                        kit.getNguongHanDungNgay(), today);
                switch (r) {
                    case HET_HAN -> expiryWarnings.add("Hộp " + kit.getMaHop() + " có " + info.ten()
                            + " đã hết hạn từ " + DATE.format(item.getHanDung()) + ".");
                    case CAN_HAN -> expiryWarnings.add("Hộp " + kit.getMaHop() + " chứa " + info.ten()
                            + " sắp hết hạn vào ngày " + DATE.format(item.getHanDung()) + ".");
                    case THIEU -> shortageWarnings.add("Hộp " + kit.getMaHop() + " đang thiếu "
                            + (item.getSoLuongToiThieu() - item.getSoLuongHienCo()) + " "
                            + info.donVi().toLowerCase(Locale.ROOT) + " " + info.ten() + ".");
                    case DAT -> { }
                }
            }
        }
        List<String> warnings = new ArrayList<>(expiryWarnings);
        warnings.addAll(shortageWarnings);
        return new EmergencyKitSummaryResponse(kits.size(), distinctItems.size(), restock, expiry, due,
                List.copyOf(warnings));
    }

    @Transactional(readOnly = true)
    public EmergencyKitDetailResponse getDetail(Set<Role> roles, String maHop) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        String ma = normalizeCode(maHop);
        EmergencyKit kit = kitRepository.findWithItems(ma).orElseThrow(() -> notFound(ma));
        return toDetail(kit, context(LocalDate.now()));
    }

    /**
     * Nhật ký kiểm tra / bổ sung / thay thế, mới nhất trước.
     *
     * @param maHop null = mọi hộp
     */
    @Transactional(readOnly = true)
    public List<EmergencyKitLogResponse> logs(Set<Role> roles, String maHop) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        String only = maHop == null || maHop.isBlank() ? null : normalizeCode(maHop);
        Context ctx = context(LocalDate.now());
        Map<String, String> kitNames = new HashMap<>();
        kitRepository.findAllWithItems().forEach(k -> kitNames.put(k.getMaHop(), k.getTenHop()));
        List<EmergencyKitLog> logs = ctx.logs().stream()
                .filter(g -> only == null || only.equals(g.getMaHop())).toList();
        return toLogResponses(logs, kitNames, ctx);
    }

    // ------------------------------------------------------------------- ghi

    @Transactional
    public EmergencyKitDetailResponse create(InventoryActor actor, EmergencyKitRequest request) {
        return create(actor, request, LocalDateTime.now());
    }

    @Transactional
    EmergencyKitDetailResponse create(InventoryActor actor, EmergencyKitRequest request, LocalDateTime now) {
        requireManager(actor);
        ValidatedKit v = validate(request);
        Integer max = kitRepository.lockAndFindMaxCodeNumber();
        String ma = formatCode(max == null ? 1 : max + 1);
        EmergencyKit kit = EmergencyKit.create(ma, v.ten(), request.loai(), v.viTri(), request.chuKyKiemTraNgay(),
                request.nguongHanDungNgay(), v.ghiChu(), actor.maNhanVien(), now.truncatedTo(ChronoUnit.SECONDS));
        kit.replaceComposition(v.items());
        return toDetail(kitRepository.saveAndFlush(kit), context(now.toLocalDate()));
    }

    /** Sửa thông tin và thành phần hộp; mặt hàng giữ lại không mất số hiện có / hạn dùng. */
    @Transactional
    public EmergencyKitDetailResponse update(InventoryActor actor, String maHop, EmergencyKitRequest request) {
        requireManager(actor);
        EmergencyKit kit = lockKit(maHop);
        ValidatedKit v = validate(request);
        kit.updateInfo(v.ten(), request.loai(), v.viTri(), request.chuKyKiemTraNgay(), request.nguongHanDungNgay(),
                v.ghiChu());
        kit.replaceComposition(v.items());
        return toDetail(kitRepository.saveAndFlush(kit), context(LocalDate.now()));
    }

    /** Kiểm tra hộp: ghi số thực có và hạn dùng của từng mặt hàng; đạt khi mọi mặt hàng đạt. */
    @Transactional
    public EmergencyKitDetailResponse check(InventoryActor actor, String maHop, KitCheckRequest request) {
        return check(actor, maHop, request, LocalDateTime.now());
    }

    @Transactional
    EmergencyKitDetailResponse check(InventoryActor actor, String maHop, KitCheckRequest request, LocalDateTime now) {
        requireManager(actor);
        EmergencyKit kit = lockKit(maHop);
        if (kit.getItems().isEmpty()) {
            throw new InventoryValidationException("Hộp " + kit.getMaHop() + " chưa có mặt hàng nào để kiểm tra.");
        }
        LocalDate today = now.toLocalDate();
        Context ctx = context(today);
        Map<Integer, KitCheckLineRequest> byLine = new HashMap<>();
        for (KitCheckLineRequest row : request == null || request.dong() == null
                ? List.<KitCheckLineRequest>of() : request.dong()) {
            if (row == null || row.maChiTiet() == null || kit.findItem(row.maChiTiet()).isEmpty()) {
                throw new InventoryValidationException("Có dòng kiểm tra không thuộc hộp " + kit.getMaHop() + ".");
            }
            if (byLine.put(row.maChiTiet(), row) != null) {
                throw new InventoryValidationException("Một mặt hàng chỉ nhập kết quả một lần.");
            }
        }
        String ghiChu = optional(request == null ? null : request.ghiChu(), "Ghi chú", 300);
        boolean allPassed = true;
        List<EmergencyKitLogLine> lines = new ArrayList<>();
        for (EmergencyKitItem item : kit.getItems()) {
            ItemInfo info = ctx.info(item);
            KitCheckLineRequest row = byLine.get(item.getMaChiTiet());
            if (row == null || row.soLuongThucTe() == null) {
                throw new InventoryValidationException("Chưa nhập kết quả cho mục " + info.ten() + ".");
            }
            int qty = row.soLuongThucTe();
            if (qty < 0 || qty > MAX_QUANTITY) {
                throw new InventoryValidationException("Số lượng thực tế của " + info.ten() + " phải từ 0 đến "
                        + MAX_QUANTITY + ".");
            }
            if (qty > 0 && row.hanDung() == null && item.getMaThuoc() != null) {
                throw new InventoryValidationException("Nhập hạn dùng gần nhất của " + info.ten() + ".");
            }
            int before = item.getSoLuongHienCo();
            item.record(qty, row.hanDung());
            KitCheckResult result = evaluate(item.getSoLuongHienCo(), item.getHanDung(), item.getSoLuongToiThieu(),
                    kit.getNguongHanDungNgay(), today);
            allPassed &= result == KitCheckResult.DAT;
            lines.add(EmergencyKitLogLine.of(item.getMaThuoc(), item.getMaVatTu(), before, qty, item.getHanDung(),
                    result));
        }
        EmergencyKitLog log = EmergencyKitLog.check(kit.getMaHop(), allPassed, actor.maNhanVien(),
                now.truncatedTo(ChronoUnit.SECONDS), ghiChu);
        lines.forEach(log::addLine);
        kitRepository.saveAndFlush(kit);
        logRepository.saveAndFlush(log);
        return toDetail(kit, context(today));
    }

    /**
     * Bổ sung (cộng thêm vào số hiện có) hoặc thay thế (thay toàn bộ số hiện có) từ kho. Lập một phiếu xuất kho lý do
     * "Phục vụ phòng khám", chọn lô FEFO chỉ trong các lô còn hạn trên ngưỡng của hộp, rồi xuất kho ngay.
     */
    @Transactional
    public EmergencyKitDetailResponse restock(InventoryActor actor, String maHop, KitRestockRequest request) {
        return restock(actor, maHop, request, LocalDateTime.now());
    }

    @Transactional
    EmergencyKitDetailResponse restock(InventoryActor actor, String maHop, KitRestockRequest request,
                                       LocalDateTime now) {
        requireManager(actor);
        if (request == null || request.loai() == null || request.loai() == KitLogType.KIEM_TRA) {
            throw new InventoryValidationException("Chọn bổ sung hoặc thay thế.");
        }
        EmergencyKit kit = lockKit(maHop);
        LocalDate today = now.toLocalDate();
        Context ctx = context(today);
        List<KitRestockLineRequest> rows = request.dong() == null ? List.of() : request.dong();
        Map<EmergencyKitItem, Integer> wanted = new LinkedHashMap<>();
        for (KitRestockLineRequest row : rows) {
            if (row == null || row.soLuong() == 0) {
                continue;
            }
            EmergencyKitItem item = row.maChiTiet() == null ? null : kit.findItem(row.maChiTiet()).orElse(null);
            if (item == null) {
                throw new InventoryValidationException("Có dòng không thuộc hộp " + kit.getMaHop() + ".");
            }
            ItemInfo info = ctx.info(item);
            if (row.soLuong() < 0 || row.soLuong() > MAX_QUANTITY) {
                throw new InventoryValidationException("Số lượng lấy cho " + info.ten() + " phải từ 1 đến "
                        + MAX_QUANTITY + ".");
            }
            if (wanted.put(item, row.soLuong()) != null) {
                throw new InventoryValidationException(info.ten() + " bị nhập trùng — gộp vào một dòng.");
            }
            if (info.kiemSoat()) {
                throw new InventoryValidationException(info.ten() + " là thuốc kiểm soát đặc biệt — cần lập phiếu ở "
                        + "màn Xuất kho để phê duyệt kép, sau đó ghi nhận số trong hộp bằng \"Kiểm tra\".");
            }
        }
        if (wanted.isEmpty()) {
            throw new InventoryValidationException("Nhập số lượng cần lấy từ kho cho ít nhất một mặt hàng.");
        }

        LocalDate minExpiry = today.plusDays(kit.getNguongHanDungNgay());
        List<StockOutLineRequest> outLines = new ArrayList<>();
        Map<EmergencyKitItem, LocalDate> newExpiry = new HashMap<>();
        for (Map.Entry<EmergencyKitItem, Integer> e : wanted.entrySet()) {
            EmergencyKitItem item = e.getKey();
            ItemInfo info = ctx.info(item);
            int remaining = e.getValue();
            int available = 0;
            LocalDate earliest = null;
            for (StockLot lot : eligibleLots(item, minExpiry)) {
                available += lot.getSoLuongTon();
                if (remaining == 0) {
                    continue;
                }
                int take = Math.min(remaining, lot.getSoLuongTon());
                remaining -= take;
                outLines.add(new StockOutLineRequest(lot.getMaLo(), take));
                if (lot.getHanSuDung() != null && (earliest == null || lot.getHanSuDung().isBefore(earliest))) {
                    earliest = lot.getHanSuDung();
                }
            }
            if (remaining > 0) {
                throw new InventoryValidationException("Kho chỉ còn " + available + " " + info.donVi().toLowerCase(Locale.ROOT)
                        + " " + info.ten() + " có hạn dùng sau " + DATE.format(minExpiry) + ", không đủ "
                        + e.getValue() + ".");
            }
            newExpiry.put(item, earliest);
        }

        String userNote = optional(request.ghiChu(), "Ghi chú", 200);
        String action = request.loai() == KitLogType.BO_SUNG ? "Bổ sung" : "Thay thế";
        String receiver = truncate("Hộp cấp cứu " + kit.getMaHop() + " — " + kit.getViTri(), 100);
        String note = truncate(action + " hộp cấp cứu " + kit.getMaHop() + " (" + kit.getTenHop() + ")"
                + (userNote == null ? "" : ": " + userNote), 300);
        StockOutDetailResponse created = stockOutService.create(actor, new StockOutRequest(today,
                StockOutReason.PHUC_VU_PHONG_KHAM, receiver, note, outLines), now);
        stockOutService.complete(actor, created.maPhieu(), now);

        EmergencyKitLog log = EmergencyKitLog.restock(kit.getMaHop(), request.loai(), created.maPhieu(),
                actor.maNhanVien(), now.truncatedTo(ChronoUnit.SECONDS), userNote);
        for (Map.Entry<EmergencyKitItem, Integer> e : wanted.entrySet()) {
            EmergencyKitItem item = e.getKey();
            int before = item.getSoLuongHienCo();
            if (request.loai() == KitLogType.BO_SUNG) {
                item.add(e.getValue(), newExpiry.get(item));
            } else {
                item.replace(e.getValue(), newExpiry.get(item));
            }
            log.addLine(EmergencyKitLogLine.of(item.getMaThuoc(), item.getMaVatTu(), before, item.getSoLuongHienCo(),
                    item.getHanDung(), null));
        }
        kitRepository.saveAndFlush(kit);
        logRepository.saveAndFlush(log);
        return toDetail(kit, context(today));
    }

    // ------------------------------------------------------------ quy tắc

    /** Đánh giá một mặt hàng: hạn dùng trước (hết hạn / còn không quá ngưỡng), rồi số lượng. */
    static KitCheckResult evaluate(int hienCo, LocalDate hanDung, int toiThieu, int nguongNgay, LocalDate today) {
        if (hienCo > 0 && hanDung != null) {
            if (hanDung.isBefore(today)) {
                return KitCheckResult.HET_HAN;
            }
            if (!hanDung.isAfter(today.plusDays(nguongNgay))) {
                return KitCheckResult.CAN_HAN;
            }
        }
        return hienCo < toiThieu ? KitCheckResult.THIEU : KitCheckResult.DAT;
    }

    private EmergencyKitStatus status(EmergencyKit kit, Context ctx) {
        if (kit.getItems().isEmpty()) {
            return EmergencyKitStatus.CHUA_CAU_HINH;
        }
        boolean expired = false;
        boolean nearExpiry = false;
        boolean shortage = false;
        boolean coverable = true;
        for (EmergencyKitItem item : kit.getItems()) {
            KitCheckResult r = evaluate(item.getSoLuongHienCo(), item.getHanDung(), item.getSoLuongToiThieu(),
                    kit.getNguongHanDungNgay(), ctx.today());
            expired |= r == KitCheckResult.HET_HAN;
            nearExpiry |= r == KitCheckResult.CAN_HAN;
            int missing = item.getSoLuongToiThieu() - item.getSoLuongHienCo();
            if (missing > 0) {
                shortage = true;
                coverable &= ctx.usableFor(item, kit.getNguongHanDungNgay()) >= missing;
            }
        }
        if (expired) {
            return EmergencyKitStatus.HET_HAN;
        }
        if (nearExpiry) {
            return EmergencyKitStatus.CAN_HAN;
        }
        if (shortage) {
            return coverable ? EmergencyKitStatus.THIEU : EmergencyKitStatus.CHO_BO_SUNG;
        }
        return EmergencyKitStatus.AN_TOAN;
    }

    private static LocalDate nextCheck(EmergencyKit kit, Context ctx) {
        EmergencyKitLog last = ctx.lastCheck(kit.getMaHop());
        return last == null ? ctx.today() : last.getThoiDiem().toLocalDate().plusDays(kit.getChuKyKiemTraNgay());
    }

    private static boolean checkDue(EmergencyKit kit, Context ctx) {
        return !nextCheck(kit, ctx).isAfter(ctx.today());
    }

    /** Lô còn tồn, hạn dùng sau {@code minExpiry} (hoặc không có hạn), theo FEFO. */
    private List<StockLot> eligibleLots(EmergencyKitItem item, LocalDate minExpiry) {
        List<StockLot> lots = new ArrayList<>(item.getMaThuoc() != null
                ? lotRepository.findByMaThuocAndSoLuongTonGreaterThan(item.getMaThuoc(), 0)
                : lotRepository.findByMaVatTuAndSoLuongTonGreaterThan(item.getMaVatTu(), 0));
        lots.removeIf(l -> l.getHanSuDung() != null && !l.getHanSuDung().isAfter(minExpiry));
        lots.sort(InventoryRules.FEFO);
        return lots;
    }

    // ------------------------------------------------------------ kiểm tra

    private record ValidatedKit(String ten, String viTri, String ghiChu, List<EmergencyKitItem> items) {
    }

    private ValidatedKit validate(EmergencyKitRequest request) {
        if (request == null) {
            throw new InventoryValidationException("Thiếu dữ liệu hộp cấp cứu.");
        }
        String ten = required(request.ten(), "Tên hộp", 150);
        String viTri = required(request.viTri(), "Vị trí", 100);
        if (request.loai() == null) {
            throw new InventoryValidationException("Chưa chọn loại hộp.");
        }
        if (request.chuKyKiemTraNgay() < 1 || request.chuKyKiemTraNgay() > 365) {
            throw new InventoryValidationException("Chu kỳ kiểm tra phải từ 1 đến 365 ngày.");
        }
        if (request.nguongHanDungNgay() < 0 || request.nguongHanDungNgay() > 365) {
            throw new InventoryValidationException("Ngưỡng cận hạn phải từ 0 đến 365 ngày.");
        }
        List<EmergencyKitItemRequest> rows = request.matHang() == null ? List.of() : request.matHang();
        if (rows.isEmpty()) {
            throw new InventoryValidationException("Hộp cần ít nhất một mặt hàng.");
        }
        if (rows.size() > MAX_ITEMS) {
            throw new InventoryValidationException("Một hộp tối đa " + MAX_ITEMS + " mặt hàng.");
        }
        Set<String> seen = new HashSet<>();
        List<EmergencyKitItem> items = new ArrayList<>();
        int index = 0;
        for (EmergencyKitItemRequest row : rows) {
            index++;
            String where = "Dòng " + index + ": ";
            if (row == null || row.loai() == null || row.maMatHang() == null || row.maMatHang().isBlank()) {
                throw new InventoryValidationException(where + "chưa chọn mặt hàng.");
            }
            String ma = row.maMatHang().trim().toUpperCase(Locale.ROOT);
            boolean medicine = row.loai() == InventoryItemType.THUOC;
            boolean exists = medicine ? medicineRepository.existsById(ma) : supplyRepository.existsById(ma);
            if (!exists) {
                throw new InventoryValidationException(where + "không tìm thấy " + (medicine ? "thuốc " : "vật tư ")
                        + ma + ".");
            }
            if (!seen.add(row.loai() + ":" + ma)) {
                throw new InventoryValidationException(where + ma + " bị chọn trùng.");
            }
            if (row.soLuongToiThieu() <= 0 || row.soLuongToiThieu() > MAX_QUANTITY) {
                throw new InventoryValidationException(where + "số lượng tối thiểu phải từ 1 đến " + MAX_QUANTITY + ".");
            }
            items.add(EmergencyKitItem.of(medicine ? ma : null, medicine ? null : ma, row.soLuongToiThieu()));
        }
        return new ValidatedKit(ten, viTri, optional(request.ghiChu(), "Ghi chú", 300), items);
    }

    private void requireManager(InventoryActor actor) {
        Objects.requireNonNull(actor, "actor must not be null");
        authorizationService.require(actor.roles(), Permission.INVENTORY_MANAGE);
        if (actor.maNhanVien() == null || actor.maNhanVien().isBlank()) {
            throw new InventoryValidationException("Phiên đăng nhập chưa gắn với nhân viên — không thể ghi nhật ký.");
        }
    }

    private EmergencyKit lockKit(String maHop) {
        String ma = normalizeCode(maHop);
        kitRepository.findForUpdate(ma).orElseThrow(() -> notFound(ma));
        return kitRepository.findWithItems(ma).orElseThrow(() -> notFound(ma));
    }

    static String formatCode(int number) {
        String digits = String.format(Locale.ROOT, "%02d", number);
        if (digits.length() > 5) {
            throw new InventoryValidationException("Đã hết dải mã hộp cấp cứu.");
        }
        return "HTCC-" + digits;
    }

    private static String normalizeCode(String ma) {
        return ma == null ? "" : ma.trim().toUpperCase(Locale.ROOT);
    }

    private static String required(String value, String field, int max) {
        String v = value == null ? "" : value.strip();
        if (v.isEmpty()) {
            throw new InventoryValidationException(field + " không được để trống.");
        }
        if (v.length() > max) {
            throw new InventoryValidationException(field + " tối đa " + max + " ký tự.");
        }
        return v;
    }

    private static String optional(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.strip();
        if (v.length() > max) {
            throw new InventoryValidationException(field + " tối đa " + max + " ký tự.");
        }
        return v;
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private static InventoryValidationException notFound(String ma) {
        return new InventoryValidationException("Không tìm thấy hộp cấp cứu " + ma + ".");
    }

    // ------------------------------------------------------------- chuyển đổi

    private record ItemInfo(InventoryItemType loai, String ten, String donVi, boolean kiemSoat) {
    }

    /** Dữ liệu đọc một lần cho cả màn: tên mặt hàng, tồn khả dụng theo lô, nhật ký. */
    private final class Context {
        private final LocalDate today;
        private final Map<String, Medicine> medicines = new HashMap<>();
        private final Map<String, MedicalSupply> supplies = new HashMap<>();
        private final Map<String, List<StockLot>> lotsByItem = new HashMap<>();
        private final List<EmergencyKitLog> logs;
        private final Map<String, EmergencyKitLog> lastChecks = new HashMap<>();

        Context(LocalDate today) {
            this.today = today;
            medicineRepository.search("").forEach(m -> medicines.put(m.getMaThuoc(), m));
            supplyRepository.search("").forEach(s -> supplies.put(s.getMaVatTu(), s));
            for (StockLot lot : lotRepository.findBySoLuongTonGreaterThan(0)) {
                lotsByItem.computeIfAbsent(lot.maMatHang(), k -> new ArrayList<>()).add(lot);
            }
            logs = logRepository.findAllWithLines();
            for (EmergencyKitLog g : logs) {
                if (g.getLoai() == KitLogType.KIEM_TRA) {
                    lastChecks.merge(g.getMaHop(), g, (a, b) -> a.getThoiDiem().isAfter(b.getThoiDiem()) ? a : b);
                }
            }
        }

        LocalDate today() {
            return today;
        }

        List<EmergencyKitLog> logs() {
            return logs;
        }

        EmergencyKitLog lastCheck(String maHop) {
            return lastChecks.get(maHop);
        }

        ItemInfo info(EmergencyKitItem item) {
            return info(item.getMaThuoc(), item.getMaVatTu());
        }

        ItemInfo info(String maThuoc, String maVatTu) {
            if (maThuoc != null) {
                Medicine m = medicines.get(maThuoc);
                if (m == null) {
                    return new ItemInfo(InventoryItemType.THUOC, maThuoc, "", false);
                }
                String ten = m.getTenThuoc() + (m.getHamLuong() == null ? "" : " " + m.getHamLuong());
                return new ItemInfo(InventoryItemType.THUOC, ten, m.getDonViTinh(), m.isKiemSoatDacBiet());
            }
            MedicalSupply s = supplies.get(maVatTu);
            return s == null ? new ItemInfo(InventoryItemType.VAT_TU, maVatTu, "", false)
                    : new ItemInfo(InventoryItemType.VAT_TU, s.getTenVatTu(), s.getDonViTinh(), false);
        }

        /** Tồn kho còn hạn sau ngưỡng của hộp — lượng tối đa bổ sung được. */
        int usableFor(EmergencyKitItem item, int nguongNgay) {
            LocalDate minExpiry = today.plusDays(nguongNgay);
            int total = 0;
            for (StockLot lot : lotsByItem.getOrDefault(item.maMatHang(), List.of())) {
                if (lot.getHanSuDung() == null || lot.getHanSuDung().isAfter(minExpiry)) {
                    total += lot.getSoLuongTon();
                }
            }
            return total;
        }
    }

    private Context context(LocalDate today) {
        return new Context(today);
    }

    private EmergencyKitRowResponse toRow(EmergencyKit kit, Context ctx) {
        int passed = 0;
        LocalDate nextExpiry = null;
        StringBuilder keywords = new StringBuilder();
        for (EmergencyKitItem item : kit.getItems()) {
            KitCheckResult r = evaluate(item.getSoLuongHienCo(), item.getHanDung(), item.getSoLuongToiThieu(),
                    kit.getNguongHanDungNgay(), ctx.today());
            if (r == KitCheckResult.DAT) {
                passed++;
            }
            if (item.getSoLuongHienCo() > 0 && item.getHanDung() != null
                    && (nextExpiry == null || item.getHanDung().isBefore(nextExpiry))) {
                nextExpiry = item.getHanDung();
            }
            keywords.append(item.maMatHang()).append(' ').append(ctx.info(item).ten()).append(' ');
        }
        EmergencyKitLog last = ctx.lastCheck(kit.getMaHop());
        return new EmergencyKitRowResponse(kit.getMaHop(), kit.getTenHop(), kit.getLoaiHop(), kit.getViTri(), passed,
                kit.getItems().size(), last == null ? null : last.getThoiDiem(), nextExpiry, nextCheck(kit, ctx),
                checkDue(kit, ctx), status(kit, ctx), keywords.toString().strip());
    }

    private EmergencyKitDetailResponse toDetail(EmergencyKit kit, Context ctx) {
        List<EmergencyKitItemResponse> items = new ArrayList<>();
        for (EmergencyKitItem item : kit.getItems()) {
            ItemInfo info = ctx.info(item);
            items.add(new EmergencyKitItemResponse(item.getMaChiTiet(), info.loai(), item.maMatHang(), info.ten(),
                    info.donVi(), info.kiemSoat(), item.getSoLuongToiThieu(), item.getSoLuongHienCo(),
                    item.getHanDung(), evaluate(item.getSoLuongHienCo(), item.getHanDung(), item.getSoLuongToiThieu(),
                    kit.getNguongHanDungNgay(), ctx.today()), ctx.usableFor(item, kit.getNguongHanDungNgay())));
        }
        List<EmergencyKitLog> kitLogs = ctx.logs().stream().filter(g -> g.getMaHop().equals(kit.getMaHop())).toList();
        List<EmergencyKitLogResponse> logs = toLogResponses(kitLogs, Map.of(kit.getMaHop(), kit.getTenHop()), ctx);
        EmergencyKitLog last = ctx.lastCheck(kit.getMaHop());
        Set<String> ids = new HashSet<>();
        ids.add(kit.getMaNhanVienTao());
        if (last != null) {
            ids.add(last.getMaNhanVien());
        }
        Map<String, String> names = employeeNames(ids);
        return new EmergencyKitDetailResponse(kit.getMaHop(), kit.getTenHop(), kit.getLoaiHop(), kit.getViTri(),
                kit.getChuKyKiemTraNgay(), kit.getNguongHanDungNgay(), kit.getGhiChu(),
                names.getOrDefault(kit.getMaNhanVienTao(), kit.getMaNhanVienTao()), kit.getThoiDiemTao(),
                status(kit, ctx), last == null ? null : last.getThoiDiem(),
                last == null ? null : names.getOrDefault(last.getMaNhanVien(), last.getMaNhanVien()),
                nextCheck(kit, ctx), checkDue(kit, ctx), List.copyOf(items), logs);
    }

    private List<EmergencyKitLogResponse> toLogResponses(List<EmergencyKitLog> logs, Map<String, String> kitNames,
                                                         Context ctx) {
        Set<String> ids = new HashSet<>();
        logs.forEach(g -> ids.add(g.getMaNhanVien()));
        Map<String, String> names = employeeNames(ids);
        List<EmergencyKitLog> sorted = new ArrayList<>(logs);
        sorted.sort(Comparator.comparing(EmergencyKitLog::getThoiDiem).reversed()
                .thenComparing(EmergencyKitLog::getMaNhatKy, Comparator.nullsLast(Comparator.reverseOrder())));
        List<EmergencyKitLogResponse> result = new ArrayList<>();
        for (EmergencyKitLog g : sorted) {
            List<EmergencyKitLogLineResponse> lines = new ArrayList<>();
            for (EmergencyKitLogLine l : g.getLines()) {
                ItemInfo info = ctx.info(l.getMaThuoc(), l.getMaVatTu());
                lines.add(new EmergencyKitLogLineResponse(l.maMatHang(), info.ten(), info.donVi(), l.getSoLuongTruoc(),
                        l.getSoLuongSau(), l.getHanDungSau(), l.getKetQua()));
            }
            result.add(new EmergencyKitLogResponse(g.getMaNhatKy(), g.getMaHop(),
                    kitNames.getOrDefault(g.getMaHop(), g.getMaHop()), g.getLoai(),
                    g.getLoai() == KitLogType.KIEM_TRA ? g.isDat() : null, g.getMaPhieuXuat(),
                    names.getOrDefault(g.getMaNhanVien(), g.getMaNhanVien()), g.getThoiDiem(), g.getGhiChu(),
                    List.copyOf(lines)));
        }
        return List.copyOf(result);
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
