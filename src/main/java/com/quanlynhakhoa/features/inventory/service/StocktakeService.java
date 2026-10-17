package com.quanlynhakhoa.features.inventory.service;

import com.quanlynhakhoa.features.auth.enums.Permission;
import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.service.AuthorizationService;
import com.quanlynhakhoa.features.inventory.dto.request.StocktakeCountRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StocktakeRequest;
import com.quanlynhakhoa.features.inventory.dto.response.StocktakeDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StocktakeLineResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StocktakeRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StocktakeSummaryResponse;
import com.quanlynhakhoa.features.inventory.entity.MedicalSupply;
import com.quanlynhakhoa.features.inventory.entity.Medicine;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.entity.Stocktake;
import com.quanlynhakhoa.features.inventory.entity.StocktakeLine;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.StocktakeScope;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.repository.MedicalSupplyRepository;
import com.quanlynhakhoa.features.inventory.repository.MedicineRepository;
import com.quanlynhakhoa.features.inventory.repository.StockInReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StockLotRepository;
import com.quanlynhakhoa.features.inventory.repository.StocktakeRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * Kiểm kê kho (UC28).
 *
 * <p>Vòng đời: tạo đợt → chụp tồn sổ sách của mọi lô còn tồn trong phạm vi (CHO_XU_LY = "Đang kiểm kê";
 * mỗi lúc chỉ một đợt) → nhập số thực tế từng lô ("Cập nhật", lưu nhiều lần) → "Hoàn tất" bởi người
 * KHÁC người lập: mọi lô phải đã đếm, lô lệch phải có lý do; tồn từng lô được cộng/trừ đúng phần chênh lệch
 * (thực tế − sổ sách) trên tồn hiện tại — nên nhập/xuất phát sinh trong lúc kiểm vẫn được giữ — không âm tồn.
 * Hoặc → DA_HUY kèm lý do, tồn không đổi.</p>
 */
@Service
public class StocktakeService {

    static final int MAX_QUANTITY = 1_000_000;

    private final AuthorizationService authorizationService;
    private final StocktakeRepository stocktakeRepository;
    private final StockLotRepository lotRepository;
    private final StockInReceiptRepository stockInReceiptRepository;
    private final MedicineRepository medicineRepository;
    private final MedicalSupplyRepository supplyRepository;

    public StocktakeService(AuthorizationService authorizationService,
                            StocktakeRepository stocktakeRepository,
                            StockLotRepository lotRepository,
                            StockInReceiptRepository stockInReceiptRepository,
                            MedicineRepository medicineRepository,
                            MedicalSupplyRepository supplyRepository) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.stocktakeRepository = Objects.requireNonNull(stocktakeRepository);
        this.lotRepository = Objects.requireNonNull(lotRepository);
        this.stockInReceiptRepository = Objects.requireNonNull(stockInReceiptRepository);
        this.medicineRepository = Objects.requireNonNull(medicineRepository);
        this.supplyRepository = Objects.requireNonNull(supplyRepository);
    }

    // ------------------------------------------------------------------- đọc

    @Transactional(readOnly = true)
    public StocktakeSummaryResponse summarize(Set<Role> roles) {
        return summarize(roles, LocalDate.now());
    }

    @Transactional(readOnly = true)
    StocktakeSummaryResponse summarize(Set<Role> roles, LocalDate today) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        long year = 0;
        long open = 0;
        long matched = 0;
        long mismatched = 0;
        long cancelled = 0;
        for (Stocktake s : stocktakeRepository.findAllWithLines()) {
            if (s.getNgayKiemKe().getYear() == today.getYear()) {
                year++;
            }
            switch (s.getTrangThai()) {
                case CHO_XU_LY -> open++;
                case DA_HUY -> cancelled++;
                case DA_HOAN_THANH -> {
                    if (s.getLines().stream().anyMatch(l -> l.chenhLech() != null && l.chenhLech() != 0)) {
                        mismatched++;
                    } else {
                        matched++;
                    }
                }
            }
        }
        return new StocktakeSummaryResponse(year, open, matched, mismatched, cancelled);
    }

    @Transactional(readOnly = true)
    public List<StocktakeRowResponse> list(Set<Role> roles) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        List<Stocktake> all = stocktakeRepository.findAllWithLines();
        Set<String> ids = new HashSet<>();
        Set<Integer> lotIds = new HashSet<>();
        for (Stocktake s : all) {
            ids.add(s.getMaNhanVienLap());
            s.getLines().forEach(l -> lotIds.add(l.getMaLo()));
        }
        Map<String, String> employees = employeeNames(ids);
        Map<Integer, StockLot> lots = lotsOf(lotIds);
        List<StocktakeRowResponse> rows = new ArrayList<>();
        for (Stocktake s : all) {
            Totals t = totals(s, lots);
            rows.add(new StocktakeRowResponse(s.getMaKiemKe(), s.getNgayKiemKe(), s.getPhamVi(),
                    employees.getOrDefault(s.getMaNhanVienLap(), s.getMaNhanVienLap()), t.items(),
                    s.getLines().size(), t.counted(), t.shortage(), t.surplus(), s.getTrangThai()));
        }
        return List.copyOf(rows);
    }

    @Transactional(readOnly = true)
    public StocktakeDetailResponse getDetail(Set<Role> roles, String maKiemKe) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        String ma = normalizeCode(maKiemKe);
        return toDetail(stocktakeRepository.findWithLines(ma).orElseThrow(() -> notFound(ma)));
    }

    // ------------------------------------------------------------------- ghi

    @Transactional
    public StocktakeDetailResponse create(InventoryActor actor, StocktakeRequest request) {
        return create(actor, request, LocalDateTime.now());
    }

    /** Tạo đợt và chụp tồn sổ sách của mọi lô còn tồn trong phạm vi. */
    @Transactional
    StocktakeDetailResponse create(InventoryActor actor, StocktakeRequest request, LocalDateTime now) {
        requireManager(actor);
        if (request == null || request.phamVi() == null) {
            throw new InventoryValidationException("Chưa chọn phạm vi kiểm kê.");
        }
        LocalDate ngay = validDate(request.ngayKiemKe(), now.toLocalDate());
        String note = optional(request.ghiChu(), "Ghi chú", 300);
        // Khóa dải mã trước khi kiểm tra đợt đang mở để hai người không cùng tạo đợt.
        Integer max = stocktakeRepository.lockAndFindMaxCodeNumber();
        if (stocktakeRepository.countByTrangThai(VoucherStatus.CHO_XU_LY) > 0) {
            throw new InventoryValidationException("Đang có một đợt kiểm kê chưa hoàn tất — hoàn tất hoặc hủy đợt đó "
                    + "trước khi tạo đợt mới.");
        }
        List<StockLot> lots = new ArrayList<>(lotRepository.findBySoLuongTonGreaterThan(0));
        lots.removeIf(l -> !inScope(l, request.phamVi()));
        if (lots.isEmpty()) {
            throw new InventoryValidationException("Không có lô nào còn tồn trong phạm vi \""
                    + request.phamVi().label() + "\" để kiểm kê.");
        }
        lots.sort(Comparator.comparing(StockLot::maMatHang).thenComparing(StockLot::getSoLo));
        List<StocktakeLine> snapshot = new ArrayList<>();
        lots.forEach(l -> snapshot.add(StocktakeLine.snapshot(l.getMaLo(), l.getSoLuongTon())));
        Stocktake stocktake = Stocktake.open(formatCode(max == null ? 1 : max + 1), ngay, request.phamVi(), note,
                actor.maNhanVien(), now.truncatedTo(ChronoUnit.SECONDS), snapshot);
        return toDetail(stocktakeRepository.saveAndFlush(stocktake));
    }

    /** "Sửa" đợt đang kiểm kê: ngày và ghi chú (phạm vi/lô đã chụp không đổi). Đợt đã duyệt: chỉ ghi chú. */
    @Transactional
    public StocktakeDetailResponse update(InventoryActor actor, String maKiemKe, LocalDate ngay, String ghiChu) {
        requireManager(actor);
        Stocktake s = lock(maKiemKe);
        String note = optional(ghiChu, "Ghi chú", 300);
        switch (s.getTrangThai()) {
            case CHO_XU_LY -> s.updateHeader(validDate(ngay, LocalDate.now()), note);
            case DA_HOAN_THANH -> s.updateNote(note);
            case DA_HUY -> throw new InventoryValidationException("Đợt kiểm kê đã hủy không được sửa.");
        }
        return toDetail(stocktakeRepository.saveAndFlush(s));
    }

    /** "Cập nhật": ghi số đếm thực tế; có thể lưu nhiều lần khi đang kiểm kê. */
    @Transactional
    public StocktakeDetailResponse saveCounts(InventoryActor actor, String maKiemKe, List<StocktakeCountRequest> counts) {
        requireManager(actor);
        Stocktake s = lock(maKiemKe);
        if (s.getTrangThai() != VoucherStatus.CHO_XU_LY) {
            throw new InventoryValidationException("Chỉ cập nhật số đếm khi đợt đang kiểm kê.");
        }
        Map<Integer, StocktakeLine> byLot = new HashMap<>();
        s.getLines().forEach(l -> byLot.put(l.getMaLo(), l));
        Set<Integer> seen = new HashSet<>();
        for (StocktakeCountRequest c : counts == null ? List.<StocktakeCountRequest>of() : counts) {
            if (c == null || c.maLo() == null) {
                throw new InventoryValidationException("Thiếu mã lô trong kết quả đếm.");
            }
            StocktakeLine line = byLot.get(c.maLo());
            if (line == null) {
                throw new InventoryValidationException("Lô " + c.maLo() + " không thuộc đợt " + s.getMaKiemKe() + ".");
            }
            if (!seen.add(c.maLo())) {
                throw new InventoryValidationException("Lô " + c.maLo() + " bị nhập trùng.");
            }
            if (c.soLuongThucTe() != null && (c.soLuongThucTe() < 0 || c.soLuongThucTe() > MAX_QUANTITY)) {
                throw new InventoryValidationException("Số thực tế phải từ 0 đến " + MAX_QUANTITY + ".");
            }
            s.record(line, c.soLuongThucTe(), optional(c.ghiChu(), "Lý do chênh lệch", 200));
        }
        return toDetail(stocktakeRepository.saveAndFlush(s));
    }

    /** "Hoàn tất": người khác người lập duyệt; điều chỉnh tồn theo chênh lệch của từng lô. */
    @Transactional
    public StocktakeDetailResponse approve(InventoryActor actor, String maKiemKe) {
        return approve(actor, maKiemKe, LocalDateTime.now());
    }

    @Transactional
    StocktakeDetailResponse approve(InventoryActor actor, String maKiemKe, LocalDateTime now) {
        requireManager(actor);
        Stocktake s = lock(maKiemKe);
        if (s.getTrangThai() != VoucherStatus.CHO_XU_LY) {
            throw new InventoryValidationException("Đợt " + s.getMaKiemKe() + " không còn đang kiểm kê.");
        }
        if (actor.maNhanVien().equals(s.getMaNhanVienLap())) {
            throw new InventoryValidationException("Người duyệt phải khác người lập đợt kiểm kê (UC28). "
                    + "Hãy nhờ một quản lý kho khác hoàn tất đợt này.");
        }
        long notCounted = s.getLines().stream().filter(l -> l.getSoLuongThucTe() == null).count();
        if (notCounted > 0) {
            throw new InventoryValidationException("Còn " + notCounted + " lô chưa nhập số thực tế.");
        }
        Map<Integer, StockLot> info = lotsOf(s.getLines().stream().map(StocktakeLine::getMaLo).toList());
        for (StocktakeLine l : s.getLines()) {
            if (l.chenhLech() != 0 && (l.getGhiChu() == null || l.getGhiChu().isBlank())) {
                StockLot lot = info.get(l.getMaLo());
                throw new InventoryValidationException("Lô " + (lot == null ? l.getMaLo() : lot.getSoLo())
                        + " lệch " + signed(l.chenhLech()) + " — cần ghi lý do chênh lệch trước khi hoàn tất.");
            }
        }
        // Khóa lô theo mã tăng dần — cùng thứ tự với nhập/xuất/hoàn trả để không khóa chéo.
        List<StocktakeLine> ordered = s.getLines().stream().filter(l -> l.chenhLech() != 0)
                .sorted(Comparator.comparing(StocktakeLine::getMaLo)).toList();
        for (StocktakeLine l : ordered) {
            StockLot lot = lotRepository.findForUpdate(l.getMaLo())
                    .orElseThrow(() -> new InventoryValidationException("Không tìm thấy lô " + l.getMaLo() + "."));
            if (lot.getSoLuongTon() + l.chenhLech() < 0) {
                throw new InventoryValidationException("Lô " + lot.getSoLo() + " hiện chỉ còn " + lot.getSoLuongTon()
                        + " (đã xuất thêm sau khi tạo đợt), không thể trừ " + (-l.chenhLech())
                        + ". Hãy đếm lại lô này.");
            }
            lot.adjust(l.chenhLech());
            lotRepository.saveAndFlush(lot);
        }
        s.approve(actor.maNhanVien(), now.truncatedTo(ChronoUnit.SECONDS));
        return toDetail(stocktakeRepository.saveAndFlush(s));
    }

    @Transactional
    public StocktakeDetailResponse cancel(InventoryActor actor, String maKiemKe, String lyDo) {
        requireManager(actor);
        String reason = required(lyDo, "Lý do hủy", 300);
        Stocktake s = lock(maKiemKe);
        if (s.getTrangThai() != VoucherStatus.CHO_XU_LY) {
            throw new InventoryValidationException("Chỉ hủy được đợt đang kiểm kê.");
        }
        s.cancel(actor.maNhanVien(), reason, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return toDetail(stocktakeRepository.saveAndFlush(s));
    }

    // ------------------------------------------------------------ kiểm tra

    private static boolean inScope(StockLot lot, StocktakeScope scope) {
        return switch (scope) {
            case TAT_CA -> true;
            case THUOC -> lot.getMaThuoc() != null;
            case VAT_TU -> lot.getMaVatTu() != null;
        };
    }

    private static LocalDate validDate(LocalDate ngay, LocalDate today) {
        if (ngay == null) {
            throw new InventoryValidationException("Chưa chọn ngày kiểm kê.");
        }
        if (ngay.isAfter(today)) {
            throw new InventoryValidationException("Ngày kiểm kê không được sau hôm nay.");
        }
        return ngay;
    }

    private void requireManager(InventoryActor actor) {
        Objects.requireNonNull(actor, "actor must not be null");
        authorizationService.require(actor.roles(), Permission.INVENTORY_MANAGE);
        if (actor.maNhanVien() == null || actor.maNhanVien().isBlank()) {
            throw new InventoryValidationException("Phiên đăng nhập chưa gắn với nhân viên — không thể ghi chứng từ.");
        }
    }

    private Stocktake lock(String maKiemKe) {
        String ma = normalizeCode(maKiemKe);
        stocktakeRepository.findForUpdate(ma).orElseThrow(() -> notFound(ma));
        return stocktakeRepository.findWithLines(ma).orElseThrow(() -> notFound(ma));
    }

    static String formatCode(int number) {
        String digits = String.format(Locale.ROOT, "%03d", number);
        if (digits.length() > 8) {
            throw new InventoryValidationException("Đã hết dải mã đợt kiểm kê.");
        }
        return "KK" + digits;
    }

    static String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
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

    private static InventoryValidationException notFound(String ma) {
        return new InventoryValidationException("Không tìm thấy đợt kiểm kê " + ma + ".");
    }

    // ------------------------------------------------------------- chuyển đổi

    private record Totals(int items, int counted, int shortage, int surplus) {
    }

    private static Totals totals(Stocktake s, Map<Integer, StockLot> lots) {
        Set<String> items = new HashSet<>();
        int counted = 0;
        int shortage = 0;
        int surplus = 0;
        for (StocktakeLine l : s.getLines()) {
            StockLot lot = lots.get(l.getMaLo());
            if (lot != null) {
                items.add(lot.maMatHang());
            }
            Integer diff = l.chenhLech();
            if (diff != null) {
                counted++;
                if (diff < 0) {
                    shortage -= diff;
                } else {
                    surplus += diff;
                }
            }
        }
        return new Totals(items.size(), counted, shortage, surplus);
    }

    private record ItemInfo(InventoryItemType loai, String ten, String donVi) {
    }

    private ItemInfo itemInfo(String ma, boolean medicine) {
        if (medicine) {
            Medicine m = medicineRepository.findById(ma).orElse(null);
            return new ItemInfo(InventoryItemType.THUOC, m == null ? ma : m.getTenThuoc(),
                    m == null ? "" : m.getDonViTinh());
        }
        MedicalSupply s = supplyRepository.findById(ma).orElse(null);
        return new ItemInfo(InventoryItemType.VAT_TU, s == null ? ma : s.getTenVatTu(),
                s == null ? "" : s.getDonViTinh());
    }

    private Map<Integer, StockLot> lotsOf(java.util.Collection<Integer> ids) {
        Map<Integer, StockLot> lots = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            lotRepository.findByMaLoIn(ids).forEach(l -> lots.put(l.getMaLo(), l));
        }
        return lots;
    }

    private Map<String, String> employeeNames(Set<String> ids) {
        Map<String, String> names = new HashMap<>();
        ids.remove(null);
        if (!ids.isEmpty()) {
            for (Object[] row : stockInReceiptRepository.findEmployeeNames(ids)) {
                names.put((String) row[0], (String) row[1]);
            }
        }
        return names;
    }

    private StocktakeDetailResponse toDetail(Stocktake s) {
        Set<String> ids = new HashSet<>();
        ids.add(s.getMaNhanVienLap());
        ids.add(s.getMaNhanVienDuyet());
        ids.add(s.getMaNhanVienHuy());
        Map<String, String> employees = employeeNames(ids);
        Map<Integer, StockLot> lots = lotsOf(s.getLines().stream().map(StocktakeLine::getMaLo).toList());
        Map<String, ItemInfo> infos = new HashMap<>();
        List<StocktakeLineResponse> lines = new ArrayList<>();
        for (StocktakeLine l : s.getLines()) {
            StockLot lot = lots.get(l.getMaLo());
            if (lot == null) {
                continue;
            }
            ItemInfo info = infos.computeIfAbsent(lot.maMatHang(), ma -> itemInfo(ma, lot.getMaThuoc() != null));
            lines.add(new StocktakeLineResponse(lot.getMaLo(), info.loai(), lot.maMatHang(), info.ten(), info.donVi(),
                    lot.getSoLo(), lot.getHanSuDung(), l.getSoLuongSoSach(), l.getSoLuongThucTe(), l.chenhLech(),
                    l.getGhiChu()));
        }
        Totals t = totals(s, lots);
        return new StocktakeDetailResponse(s.getMaKiemKe(), s.getNgayKiemKe(), s.getPhamVi(), s.getTrangThai(),
                s.getGhiChu(), s.getMaNhanVienLap(),
                employees.getOrDefault(s.getMaNhanVienLap(), s.getMaNhanVienLap()), s.getThoiDiemLap(),
                nameOf(employees, s.getMaNhanVienDuyet()), s.getThoiDiemDuyet(),
                nameOf(employees, s.getMaNhanVienHuy()), s.getThoiDiemHuy(), s.getLyDoHuy(),
                lines, t.items(), t.counted(), t.shortage(), t.surplus());
    }

    private static String nameOf(Map<String, String> names, String id) {
        return id == null ? null : names.getOrDefault(id, id);
    }
}
