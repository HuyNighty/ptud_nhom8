package com.quanlynhakhoa.features.inventory.service;

import com.quanlynhakhoa.features.auth.enums.Permission;
import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.service.AuthorizationService;
import com.quanlynhakhoa.features.inventory.dto.request.StockOutLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockOutRequest;
import com.quanlynhakhoa.features.inventory.dto.response.StockOutDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockOutLineResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockOutRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockOutSummaryResponse;
import com.quanlynhakhoa.features.inventory.entity.MedicalSupply;
import com.quanlynhakhoa.features.inventory.entity.Medicine;
import com.quanlynhakhoa.features.inventory.entity.StockInLine;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.entity.StockOutLine;
import com.quanlynhakhoa.features.inventory.entity.StockOutReceipt;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.StockOutReason;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.repository.MedicalSupplyRepository;
import com.quanlynhakhoa.features.inventory.repository.MedicineRepository;
import com.quanlynhakhoa.features.inventory.repository.StockInLineRepository;
import com.quanlynhakhoa.features.inventory.repository.StockInReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StockLotRepository;
import com.quanlynhakhoa.features.inventory.repository.StockOutReceiptRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
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
 * Xuất kho (UC31): gợi ý lô theo FEFO, lập phiếu, xuất kho (trừ tồn) và hủy phiếu.
 *
 * <p>Vòng đời: lập phiếu → CHO_XU_LY (chưa trừ tồn, sửa được) → "Xuất kho" = DA_HOAN_THANH: khóa từng lô,
 * kiểm tra lại tồn/hạn dùng, trừ tồn và chốt giá vốn trong cùng transaction; hoặc → DA_HUY kèm lý do.</p>
 */
@Service
public class StockOutService {

    static final int MAX_LINES = 200;
    static final int MAX_QUANTITY = 1_000_000;

    private final AuthorizationService authorizationService;
    private final StockOutReceiptRepository receiptRepository;
    private final StockLotRepository lotRepository;
    private final StockInLineRepository stockInLineRepository;
    private final StockInReceiptRepository stockInReceiptRepository;
    private final MedicineRepository medicineRepository;
    private final MedicalSupplyRepository supplyRepository;

    public StockOutService(AuthorizationService authorizationService,
                           StockOutReceiptRepository receiptRepository,
                           StockLotRepository lotRepository,
                           StockInLineRepository stockInLineRepository,
                           StockInReceiptRepository stockInReceiptRepository,
                           MedicineRepository medicineRepository,
                           MedicalSupplyRepository supplyRepository) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.receiptRepository = Objects.requireNonNull(receiptRepository);
        this.lotRepository = Objects.requireNonNull(lotRepository);
        this.stockInLineRepository = Objects.requireNonNull(stockInLineRepository);
        this.stockInReceiptRepository = Objects.requireNonNull(stockInReceiptRepository);
        this.medicineRepository = Objects.requireNonNull(medicineRepository);
        this.supplyRepository = Objects.requireNonNull(supplyRepository);
    }

    // ------------------------------------------------------------------- đọc

    @Transactional(readOnly = true)
    public StockOutSummaryResponse summarize(Set<Role> roles) {
        return summarize(roles, LocalDate.now());
    }

    @Transactional(readOnly = true)
    StockOutSummaryResponse summarize(Set<Role> roles, LocalDate today) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        List<StockOutReceipt> receipts = receiptRepository.findAllWithLines();
        YearMonth thisMonth = YearMonth.from(today);
        BigDecimal value = BigDecimal.ZERO;
        long pending = 0;
        long completed = 0;
        long cancelled = 0;
        for (StockOutReceipt r : receipts) {
            switch (r.getTrangThai()) {
                case CHO_XU_LY -> pending++;
                case DA_HUY -> cancelled++;
                case DA_HOAN_THANH -> {
                    completed++;
                    if (YearMonth.from(r.getNgayXuat()).equals(thisMonth)) {
                        value = value.add(r.tongTien());
                    }
                }
            }
        }
        return new StockOutSummaryResponse(receipts.size(), value, pending, completed, cancelled);
    }

    @Transactional(readOnly = true)
    public List<StockOutRowResponse> list(Set<Role> roles) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        List<StockOutReceipt> receipts = receiptRepository.findAllWithLines();
        Set<String> ids = new HashSet<>();
        receipts.forEach(r -> ids.add(r.getMaNhanVienLap()));
        Map<String, String> employees = employeeNames(ids);
        List<StockOutRowResponse> rows = new ArrayList<>();
        for (StockOutReceipt r : receipts) {
            rows.add(new StockOutRowResponse(r.getMaPhieuXuat(), r.getNgayXuat(), r.getLyDoXuat(), r.getNguoiNhan(),
                    employees.getOrDefault(r.getMaNhanVienLap(), r.getMaNhanVienLap()), r.tongTien(),
                    r.getTrangThai()));
        }
        return List.copyOf(rows);
    }

    @Transactional(readOnly = true)
    public StockOutDetailResponse getDetail(Set<Role> roles, String maPhieu) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        String ma = normalizeCode(maPhieu);
        return toDetail(receiptRepository.findWithLines(ma).orElseThrow(() -> notFound(ma)));
    }

    /**
     * Gợi ý lô theo FEFO (hết hạn trước — xuất trước) cho {@code soLuong} của một mặt hàng.
     * Chỉ dùng lô còn tồn và chưa hết hạn; trừ đi phần đã chọn ở các dòng khác của phiếu đang soạn.
     *
     * @param daChon số lượng đã chọn theo mã lô trên phiếu đang soạn (có thể rỗng)
     */
    @Transactional(readOnly = true)
    public List<StockOutLineResponse> suggestFefo(Set<Role> roles, InventoryItemType loai, String maMatHang,
                                                  int soLuong, Map<Integer, Integer> daChon) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        Objects.requireNonNull(loai, "loai must not be null");
        if (soLuong <= 0 || soLuong > MAX_QUANTITY) {
            throw new InventoryValidationException("Số lượng xuất phải từ 1 đến " + MAX_QUANTITY + ".");
        }
        String ma = maMatHang == null ? "" : maMatHang.trim().toUpperCase(Locale.ROOT);
        LocalDate today = LocalDate.now();
        List<StockLot> lots = new ArrayList<>(loai == InventoryItemType.THUOC
                ? lotRepository.findByMaThuocAndSoLuongTonGreaterThan(ma, 0)
                : lotRepository.findByMaVatTuAndSoLuongTonGreaterThan(ma, 0));
        lots.removeIf(l -> !InventoryRules.usable(l.getHanSuDung(), today));
        lots.sort(InventoryRules.FEFO);
        Map<Integer, Integer> reserved = daChon == null ? Map.of() : daChon;
        int remaining = soLuong;
        int available = 0;
        List<StockOutLineResponse> result = new ArrayList<>();
        ItemInfo info = itemInfo(ma, loai == InventoryItemType.THUOC);
        for (StockLot lot : lots) {
            int free = lot.getSoLuongTon() - reserved.getOrDefault(lot.getMaLo(), 0);
            if (free <= 0) {
                continue;
            }
            available += free;
            if (remaining > 0) {
                int take = Math.min(free, remaining);
                remaining -= take;
                result.add(toLine(lot, take, info, null));
            }
        }
        if (remaining > 0) {
            throw new InventoryValidationException("Không đủ tồn khả dụng cho " + ma + ": cần " + soLuong
                    + ", chỉ còn " + available + " (không tính lô đã hết hạn).");
        }
        return List.copyOf(result);
    }

    // ------------------------------------------------------------------- ghi

    @Transactional
    public StockOutDetailResponse create(InventoryActor actor, StockOutRequest request) {
        return create(actor, request, LocalDateTime.now());
    }

    @Transactional
    StockOutDetailResponse create(InventoryActor actor, StockOutRequest request, LocalDateTime now) {
        requireManager(actor);
        Validated v = validate(request, now.toLocalDate());
        Integer max = receiptRepository.lockAndFindMaxCodeNumber();
        String ma = formatCode(max == null ? 1 : max + 1);
        StockOutReceipt receipt = StockOutReceipt.draft(ma, v.ngayXuat(), v.lyDo(), v.nguoiNhan(), v.ghiChu(),
                actor.maNhanVien(), now.truncatedTo(ChronoUnit.SECONDS));
        receipt.replaceLines(v.lines());
        return toDetail(receiptRepository.saveAndFlush(receipt));
    }

    /** Sửa toàn bộ phiếu — chỉ khi còn CHO_XU_LY. */
    @Transactional
    public StockOutDetailResponse update(InventoryActor actor, String maPhieu, StockOutRequest request) {
        requireManager(actor);
        StockOutReceipt receipt = lockReceipt(maPhieu);
        if (receipt.getTrangThai() != VoucherStatus.CHO_XU_LY) {
            throw new InventoryValidationException("Chỉ sửa được phiếu đang chờ xử lý. Phiếu đã xuất kho "
                    + "hoặc đã hủy chỉ được sửa ghi chú.");
        }
        Validated v = validate(request, LocalDate.now());
        receipt.updateHeader(v.ngayXuat(), v.lyDo(), v.nguoiNhan(), v.ghiChu());
        receipt.replaceLines(v.lines());
        return toDetail(receiptRepository.saveAndFlush(receipt));
    }

    @Transactional
    public StockOutDetailResponse updateNote(InventoryActor actor, String maPhieu, String ghiChu) {
        requireManager(actor);
        StockOutReceipt receipt = lockReceipt(maPhieu);
        if (receipt.getTrangThai() == VoucherStatus.DA_HUY) {
            throw new InventoryValidationException("Phiếu đã hủy không được sửa.");
        }
        receipt.updateNote(optional(ghiChu, "Ghi chú", 300));
        return toDetail(receiptRepository.saveAndFlush(receipt));
    }

    /** "Xuất kho": khóa từng lô, kiểm tra tồn/hạn dùng, trừ tồn, chốt giá vốn, chuyển DA_HOAN_THANH. */
    @Transactional
    public StockOutDetailResponse complete(InventoryActor actor, String maPhieu) {
        return complete(actor, maPhieu, LocalDateTime.now());
    }

    @Transactional
    StockOutDetailResponse complete(InventoryActor actor, String maPhieu, LocalDateTime now) {
        requireManager(actor);
        StockOutReceipt receipt = lockReceipt(maPhieu);
        if (receipt.getTrangThai() != VoucherStatus.CHO_XU_LY) {
            throw new InventoryValidationException("Phiếu " + receipt.getMaPhieuXuat() + " không còn ở trạng thái "
                    + "chờ xử lý (" + receipt.getTrangThai().label() + ").");
        }
        if (receipt.getLines().isEmpty()) {
            throw new InventoryValidationException("Phiếu chưa có dòng hàng nào.");
        }
        LocalDate today = now.toLocalDate();
        // Khóa lô theo mã tăng dần để các phiếu xuất cùng lô không khóa chéo nhau.
        List<StockOutLine> ordered = receipt.getLines().stream()
                .sorted(Comparator.comparing(StockOutLine::getMaLo)).toList();
        for (StockOutLine line : ordered) {
            StockLot lot = lotRepository.findForUpdate(line.getMaLo())
                    .orElseThrow(() -> new InventoryValidationException("Không tìm thấy lô " + line.getMaLo() + "."));
            if (!InventoryRules.usable(lot.getHanSuDung(), today)) {
                throw new InventoryValidationException("Lô " + lot.getSoLo() + " (" + lot.maMatHang()
                        + ") đã hết hạn — không được xuất. Hãy sửa phiếu để chọn lô khác hoặc lập phiếu hủy.");
            }
            if (line.getSoLuong() > lot.getSoLuongTon()) {
                throw new InventoryValidationException("Lô " + lot.getSoLo() + " (" + lot.maMatHang() + ") chỉ còn "
                        + lot.getSoLuongTon() + ", không đủ " + line.getSoLuong() + ". Hãy sửa phiếu để gợi ý lại lô.");
            }
            lot.issue(line.getSoLuong());
            lotRepository.saveAndFlush(lot);
            BigDecimal cost = stockInLineRepository.findFirstByMaLoOrderByMaChiTietDesc(lot.getMaLo())
                    .map(StockInLine::getDonGiaNhap).orElse(BigDecimal.ZERO);
            line.fixUnitCost(cost);
        }
        receipt.markCompleted(actor.maNhanVien(), now.truncatedTo(ChronoUnit.SECONDS));
        return toDetail(receiptRepository.saveAndFlush(receipt));
    }

    @Transactional
    public StockOutDetailResponse cancel(InventoryActor actor, String maPhieu, String lyDo) {
        requireManager(actor);
        String reason = required(lyDo, "Lý do hủy", 300);
        StockOutReceipt receipt = lockReceipt(maPhieu);
        if (receipt.getTrangThai() != VoucherStatus.CHO_XU_LY) {
            throw new InventoryValidationException("Chỉ hủy được phiếu đang chờ xử lý. Phiếu đã xuất kho cần "
                    + "lập phiếu hoàn trả để giữ lịch sử tồn.");
        }
        receipt.cancel(actor.maNhanVien(), reason, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return toDetail(receiptRepository.saveAndFlush(receipt));
    }

    // ------------------------------------------------------------ kiểm tra

    record Validated(LocalDate ngayXuat, StockOutReason lyDo, String nguoiNhan, String ghiChu,
                     List<StockOutLine> lines) {
    }

    private Validated validate(StockOutRequest request, LocalDate today) {
        if (request == null) {
            throw new InventoryValidationException("Thiếu dữ liệu phiếu xuất.");
        }
        LocalDate ngay = request.ngayXuat();
        if (ngay == null) {
            throw new InventoryValidationException("Chưa chọn ngày xuất.");
        }
        if (ngay.isAfter(today)) {
            throw new InventoryValidationException("Ngày xuất không được sau hôm nay.");
        }
        if (request.lyDo() == null) {
            throw new InventoryValidationException("Chưa chọn lý do xuất.");
        }
        String nguoiNhan = required(request.nguoiNhan(), "Người nhận", 100);
        List<StockOutLineRequest> rows = request.dong() == null ? List.of() : request.dong();
        if (rows.isEmpty()) {
            throw new InventoryValidationException("Phiếu xuất cần ít nhất một dòng hàng.");
        }
        if (rows.size() > MAX_LINES) {
            throw new InventoryValidationException("Một phiếu tối đa " + MAX_LINES + " dòng hàng.");
        }
        Set<Integer> seen = new HashSet<>();
        List<StockOutLine> lines = new ArrayList<>();
        int index = 0;
        for (StockOutLineRequest row : rows) {
            index++;
            String where = "Dòng " + index + ": ";
            if (row == null || row.maLo() == null) {
                throw new InventoryValidationException(where + "chưa chọn lô.");
            }
            if (!seen.add(row.maLo())) {
                throw new InventoryValidationException(where + "lô bị chọn trùng — gộp số lượng vào một dòng.");
            }
            if (row.soLuong() <= 0 || row.soLuong() > MAX_QUANTITY) {
                throw new InventoryValidationException(where + "số lượng phải từ 1 đến " + MAX_QUANTITY + ".");
            }
            StockLot lot = lotRepository.findById(row.maLo())
                    .orElseThrow(() -> new InventoryValidationException(where + "không tìm thấy lô " + row.maLo() + "."));
            if (!InventoryRules.usable(lot.getHanSuDung(), today)) {
                throw new InventoryValidationException(where + "lô " + lot.getSoLo() + " đã hết hạn — không được xuất.");
            }
            if (row.soLuong() > lot.getSoLuongTon()) {
                throw new InventoryValidationException(where + "lô " + lot.getSoLo() + " chỉ còn "
                        + lot.getSoLuongTon() + ".");
            }
            lines.add(StockOutLine.of(row.maLo(), row.soLuong()));
        }
        return new Validated(ngay, request.lyDo(), nguoiNhan, optional(request.ghiChu(), "Ghi chú", 300), lines);
    }

    private void requireManager(InventoryActor actor) {
        Objects.requireNonNull(actor, "actor must not be null");
        authorizationService.require(actor.roles(), Permission.INVENTORY_MANAGE);
        if (actor.maNhanVien() == null || actor.maNhanVien().isBlank()) {
            throw new InventoryValidationException("Phiên đăng nhập chưa gắn với nhân viên — không thể ghi chứng từ.");
        }
    }

    private StockOutReceipt lockReceipt(String maPhieu) {
        String ma = normalizeCode(maPhieu);
        receiptRepository.findForUpdate(ma).orElseThrow(() -> notFound(ma));
        return receiptRepository.findWithLines(ma).orElseThrow(() -> notFound(ma));
    }

    static String formatCode(int number) {
        String digits = String.format(Locale.ROOT, "%03d", number);
        if (digits.length() > 8) {
            throw new InventoryValidationException("Đã hết dải mã phiếu xuất.");
        }
        return "PX" + digits;
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
        return new InventoryValidationException("Không tìm thấy phiếu xuất " + ma + ".");
    }

    // ------------------------------------------------------------- chuyển đổi

    private record ItemInfo(InventoryItemType loai, String ten, String donVi, boolean kiemSoat) {
    }

    private ItemInfo itemInfo(String ma, boolean medicine) {
        if (medicine) {
            Medicine m = medicineRepository.findById(ma).orElse(null);
            return new ItemInfo(InventoryItemType.THUOC, m == null ? ma : m.getTenThuoc(),
                    m == null ? "" : m.getDonViTinh(), m != null && m.isKiemSoatDacBiet());
        }
        MedicalSupply s = supplyRepository.findById(ma).orElse(null);
        return new ItemInfo(InventoryItemType.VAT_TU, s == null ? ma : s.getTenVatTu(),
                s == null ? "" : s.getDonViTinh(), false);
    }

    private static StockOutLineResponse toLine(StockLot lot, int soLuong, ItemInfo info, BigDecimal donGia) {
        BigDecimal thanhTien = donGia == null ? null : donGia.multiply(BigDecimal.valueOf(soLuong));
        return new StockOutLineResponse(lot.getMaLo(), info.loai(), lot.maMatHang(), info.ten(), info.donVi(),
                lot.getSoLo(), lot.getHanSuDung(), soLuong, lot.getSoLuongTon(), info.kiemSoat(), donGia, thanhTien);
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

    private StockOutDetailResponse toDetail(StockOutReceipt r) {
        Set<String> ids = new HashSet<>();
        ids.add(r.getMaNhanVienLap());
        ids.add(r.getMaNhanVienXuatKho());
        ids.add(r.getMaNhanVienHuy());
        Map<String, String> employees = employeeNames(ids);
        List<Integer> lotIds = r.getLines().stream().map(StockOutLine::getMaLo).toList();
        Map<Integer, StockLot> lots = new HashMap<>();
        if (!lotIds.isEmpty()) {
            lotRepository.findByMaLoIn(lotIds).forEach(l -> lots.put(l.getMaLo(), l));
        }
        Map<String, ItemInfo> infos = new HashMap<>();
        List<StockOutLineResponse> lines = new ArrayList<>();
        for (StockOutLine line : r.getLines()) {
            StockLot lot = lots.get(line.getMaLo());
            if (lot == null) {
                continue;
            }
            ItemInfo info = infos.computeIfAbsent(lot.maMatHang(), ma -> itemInfo(ma, lot.getMaThuoc() != null));
            lines.add(toLine(lot, line.getSoLuong(), info, line.getDonGiaXuat()));
        }
        return new StockOutDetailResponse(r.getMaPhieuXuat(), r.getNgayXuat(), r.getLyDoXuat(), r.getNguoiNhan(),
                r.getTrangThai(), r.getGhiChu(),
                employees.getOrDefault(r.getMaNhanVienLap(), r.getMaNhanVienLap()), r.getThoiDiemLap(),
                nameOf(employees, r.getMaNhanVienXuatKho()), r.getThoiDiemXuatKho(),
                nameOf(employees, r.getMaNhanVienHuy()), r.getThoiDiemHuy(), r.getLyDoHuy(),
                lines, r.tongTien());
    }

    private static String nameOf(Map<String, String> names, String id) {
        return id == null ? null : names.getOrDefault(id, id);
    }
}
