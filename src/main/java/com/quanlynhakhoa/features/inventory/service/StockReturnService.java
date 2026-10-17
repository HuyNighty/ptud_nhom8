package com.quanlynhakhoa.features.inventory.service;

import com.quanlynhakhoa.features.auth.enums.Permission;
import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.service.AuthorizationService;
import com.quanlynhakhoa.features.inventory.dto.request.StockReturnLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockReturnRequest;
import com.quanlynhakhoa.features.inventory.dto.response.ReturnSourceResponse;
import com.quanlynhakhoa.features.inventory.dto.response.ReturnableLineResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockReturnDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockReturnLineResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockReturnRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockReturnSummaryResponse;
import com.quanlynhakhoa.features.inventory.entity.MedicalSupply;
import com.quanlynhakhoa.features.inventory.entity.Medicine;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.entity.StockOutLine;
import com.quanlynhakhoa.features.inventory.entity.StockOutReceipt;
import com.quanlynhakhoa.features.inventory.entity.StockReturnLine;
import com.quanlynhakhoa.features.inventory.entity.StockReturnReceipt;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.ReturnReason;
import com.quanlynhakhoa.features.inventory.enums.StockOutReason;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.repository.MedicalSupplyRepository;
import com.quanlynhakhoa.features.inventory.repository.MedicineRepository;
import com.quanlynhakhoa.features.inventory.repository.StockInReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StockLotRepository;
import com.quanlynhakhoa.features.inventory.repository.StockOutReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StockReturnReceiptRepository;
import java.math.BigDecimal;
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
 * Hoàn trả kho (UC32): nhập lại hàng đã xuất, luôn gắn với một phiếu xuất gốc đã hoàn thành.
 *
 * <p>Ràng buộc: chỉ trả các lô có trên phiếu xuất gốc; tổng trả của mỗi lô (kể cả phiếu đang chờ duyệt)
 * không vượt số đã xuất. Vòng đời: lập phiếu → CHO_XU_LY ("Chờ kiểm duyệt", chưa đổi tồn) → "Duyệt" =
 * DA_HOAN_THANH: khóa phiếu xuất gốc và từng lô, kiểm tra lại rồi cộng tồn (trừ hàng lỗi); hoặc → DA_HUY.</p>
 */
@Service
public class StockReturnService {

    static final int MAX_LINES = 200;
    static final int MAX_QUANTITY = 1_000_000;

    private final AuthorizationService authorizationService;
    private final StockReturnReceiptRepository receiptRepository;
    private final StockOutReceiptRepository stockOutRepository;
    private final StockLotRepository lotRepository;
    private final StockInReceiptRepository stockInReceiptRepository;
    private final MedicineRepository medicineRepository;
    private final MedicalSupplyRepository supplyRepository;

    public StockReturnService(AuthorizationService authorizationService,
                              StockReturnReceiptRepository receiptRepository,
                              StockOutReceiptRepository stockOutRepository,
                              StockLotRepository lotRepository,
                              StockInReceiptRepository stockInReceiptRepository,
                              MedicineRepository medicineRepository,
                              MedicalSupplyRepository supplyRepository) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.receiptRepository = Objects.requireNonNull(receiptRepository);
        this.stockOutRepository = Objects.requireNonNull(stockOutRepository);
        this.lotRepository = Objects.requireNonNull(lotRepository);
        this.stockInReceiptRepository = Objects.requireNonNull(stockInReceiptRepository);
        this.medicineRepository = Objects.requireNonNull(medicineRepository);
        this.supplyRepository = Objects.requireNonNull(supplyRepository);
    }

    // ------------------------------------------------------------------- đọc

    @Transactional(readOnly = true)
    public StockReturnSummaryResponse summarize(Set<Role> roles) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        List<StockReturnReceipt> receipts = receiptRepository.findAllWithLines();
        BigDecimal value = BigDecimal.ZERO;
        long pending = 0;
        long completed = 0;
        long cancelled = 0;
        for (StockReturnReceipt r : receipts) {
            switch (r.getTrangThai()) {
                case CHO_XU_LY -> pending++;
                case DA_HUY -> cancelled++;
                case DA_HOAN_THANH -> {
                    completed++;
                    value = value.add(r.tongTien());
                }
            }
        }
        return new StockReturnSummaryResponse(receipts.size(), value, pending, completed, cancelled);
    }

    @Transactional(readOnly = true)
    public List<StockReturnRowResponse> list(Set<Role> roles) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        List<StockReturnReceipt> receipts = receiptRepository.findAllWithLines();
        Set<String> ids = new HashSet<>();
        receipts.forEach(r -> ids.add(r.getMaNhanVienDuyet()));
        Map<String, String> employees = employeeNames(ids);
        List<StockReturnRowResponse> rows = new ArrayList<>();
        for (StockReturnReceipt r : receipts) {
            rows.add(new StockReturnRowResponse(r.getMaPhieuHoanTra(), r.getNgayHoanTra(), r.getMaPhieuXuat(),
                    r.getLyDo(), r.getNguonHoanTra(), r.getNguoiHoanTra(),
                    nameOf(employees, r.getMaNhanVienDuyet()), r.tongTien(), r.getTrangThai()));
        }
        return List.copyOf(rows);
    }

    @Transactional(readOnly = true)
    public StockReturnDetailResponse getDetail(Set<Role> roles, String maPhieu) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        String ma = normalizeCode(maPhieu);
        return toDetail(receiptRepository.findWithLines(ma).orElseThrow(() -> notFound(ma)));
    }

    /** Các phiếu xuất đã hoàn thành còn lô có thể trả (không gồm phiếu xuất hủy/hao hụt). */
    @Transactional(readOnly = true)
    public List<ReturnSourceResponse> listSources(Set<Role> roles) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        Map<String, List<StockReturnReceipt>> returnsByIssue = new HashMap<>();
        for (StockReturnReceipt r : receiptRepository.findAllWithLines()) {
            returnsByIssue.computeIfAbsent(r.getMaPhieuXuat(), k -> new ArrayList<>()).add(r);
        }
        List<ReturnSourceResponse> result = new ArrayList<>();
        for (StockOutReceipt issue : stockOutRepository.findAllWithLines()) {
            if (!returnable(issue)) {
                continue;
            }
            Map<Integer, Integer> returned = returnedByLot(returnsByIssue.getOrDefault(issue.getMaPhieuXuat(),
                    List.of()), null, true);
            int remaining = 0;
            for (StockOutLine line : issue.getLines()) {
                remaining += Math.max(0, line.getSoLuong() - returned.getOrDefault(line.getMaLo(), 0));
            }
            if (remaining > 0) {
                result.add(new ReturnSourceResponse(issue.getMaPhieuXuat(), issue.getNgayXuat(), issue.getLyDoXuat(),
                        issue.getNguoiNhan(), remaining));
            }
        }
        return List.copyOf(result);
    }

    /**
     * Các lô trên phiếu xuất gốc kèm số còn được trả.
     *
     * @param boQuaPhieu mã phiếu hoàn trả đang sửa (không tính phần của chính nó); null khi lập mới
     */
    @Transactional(readOnly = true)
    public List<ReturnableLineResponse> returnableLines(Set<Role> roles, String maPhieuXuat, String boQuaPhieu) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        StockOutReceipt issue = loadIssue(maPhieuXuat);
        Map<Integer, Integer> returned = returnedByLot(
                receiptRepository.findByPhieuXuatWithLines(issue.getMaPhieuXuat()), normalizeOrNull(boQuaPhieu), true);
        Map<Integer, StockLot> lots = lotsOf(issue.getLines().stream().map(StockOutLine::getMaLo).toList());
        Map<String, ItemInfo> infos = new HashMap<>();
        List<ReturnableLineResponse> result = new ArrayList<>();
        for (StockOutLine line : issue.getLines()) {
            StockLot lot = lots.get(line.getMaLo());
            if (lot == null) {
                continue;
            }
            ItemInfo info = infos.computeIfAbsent(lot.maMatHang(), ma -> itemInfo(ma, lot.getMaThuoc() != null));
            int daTra = returned.getOrDefault(line.getMaLo(), 0);
            result.add(new ReturnableLineResponse(lot.getMaLo(), info.loai(), lot.maMatHang(), info.ten(),
                    info.donVi(), lot.getSoLo(), lot.getHanSuDung(), line.getSoLuong(), daTra,
                    Math.max(0, line.getSoLuong() - daTra), costOf(line)));
        }
        return List.copyOf(result);
    }

    // ------------------------------------------------------------------- ghi

    @Transactional
    public StockReturnDetailResponse create(InventoryActor actor, StockReturnRequest request) {
        return create(actor, request, LocalDateTime.now());
    }

    @Transactional
    StockReturnDetailResponse create(InventoryActor actor, StockReturnRequest request, LocalDateTime now) {
        requireManager(actor);
        Validated v = validate(request, now.toLocalDate(), null);
        Integer max = receiptRepository.lockAndFindMaxCodeNumber();
        String ma = formatCode(max == null ? 1 : max + 1);
        StockReturnReceipt receipt = StockReturnReceipt.draft(ma, v.maPhieuXuat(), v.ngay(), v.lyDo(), v.nguon(),
                v.nguoi(), v.ghiChu(), actor.maNhanVien(), now.truncatedTo(ChronoUnit.SECONDS));
        receipt.replaceLines(v.lines());
        return toDetail(receiptRepository.saveAndFlush(receipt));
    }

    /** Sửa phiếu đang chờ kiểm duyệt; không đổi được phiếu xuất gốc. */
    @Transactional
    public StockReturnDetailResponse update(InventoryActor actor, String maPhieu, StockReturnRequest request) {
        requireManager(actor);
        StockReturnReceipt receipt = lockReceipt(maPhieu);
        if (receipt.getTrangThai() != VoucherStatus.CHO_XU_LY) {
            throw new InventoryValidationException("Chỉ sửa được phiếu đang chờ kiểm duyệt. Phiếu đã duyệt "
                    + "chỉ được sửa ghi chú.");
        }
        if (request != null && request.maPhieuXuat() != null
                && !normalizeCode(request.maPhieuXuat()).equals(receipt.getMaPhieuXuat())) {
            throw new InventoryValidationException("Không đổi được phiếu xuất gốc — hãy hủy phiếu này và lập phiếu mới.");
        }
        StockReturnRequest fixed = request == null ? null : new StockReturnRequest(request.ngayHoanTra(),
                receipt.getMaPhieuXuat(), request.lyDo(), request.nguonHoanTra(), request.nguoiHoanTra(),
                request.ghiChu(), request.dong());
        Validated v = validate(fixed, LocalDate.now(), receipt.getMaPhieuHoanTra());
        receipt.updateHeader(v.ngay(), v.lyDo(), v.nguon(), v.nguoi(), v.ghiChu());
        receipt.replaceLines(v.lines());
        return toDetail(receiptRepository.saveAndFlush(receipt));
    }

    @Transactional
    public StockReturnDetailResponse updateNote(InventoryActor actor, String maPhieu, String ghiChu) {
        requireManager(actor);
        StockReturnReceipt receipt = lockReceipt(maPhieu);
        if (receipt.getTrangThai() == VoucherStatus.DA_HUY) {
            throw new InventoryValidationException("Phiếu đã hủy không được sửa.");
        }
        receipt.updateNote(optional(ghiChu, "Ghi chú", 300));
        return toDetail(receiptRepository.saveAndFlush(receipt));
    }

    /** "Duyệt": nhận lại hàng vào kho — cộng tồn đúng lô đã xuất (trừ hàng lỗi), chuyển DA_HOAN_THANH. */
    @Transactional
    public StockReturnDetailResponse approve(InventoryActor actor, String maPhieu) {
        return approve(actor, maPhieu, LocalDateTime.now());
    }

    @Transactional
    StockReturnDetailResponse approve(InventoryActor actor, String maPhieu, LocalDateTime now) {
        requireManager(actor);
        StockReturnReceipt receipt = lockReceipt(maPhieu);
        if (receipt.getTrangThai() != VoucherStatus.CHO_XU_LY) {
            throw new InventoryValidationException("Phiếu " + receipt.getMaPhieuHoanTra() + " không còn chờ kiểm duyệt ("
                    + receipt.getTrangThai().label() + ").");
        }
        if (receipt.getLines().isEmpty()) {
            throw new InventoryValidationException("Phiếu chưa có dòng hàng nào.");
        }
        // Khóa phiếu xuất gốc để các phiếu hoàn trả cùng gốc được duyệt lần lượt.
        stockOutRepository.findForUpdate(receipt.getMaPhieuXuat())
                .orElseThrow(() -> new InventoryValidationException("Không tìm thấy phiếu xuất gốc "
                        + receipt.getMaPhieuXuat() + "."));
        StockOutReceipt issue = loadIssue(receipt.getMaPhieuXuat());
        Map<Integer, Integer> issued = issuedByLot(issue);
        Map<Integer, Integer> approved = returnedByLot(
                receiptRepository.findByPhieuXuatWithLines(issue.getMaPhieuXuat()), receipt.getMaPhieuHoanTra(), false);
        List<StockReturnLine> ordered = receipt.getLines().stream()
                .sorted(Comparator.comparing(StockReturnLine::getMaLo)).toList();
        for (StockReturnLine line : ordered) {
            int max = issued.getOrDefault(line.getMaLo(), 0) - approved.getOrDefault(line.getMaLo(), 0);
            if (line.getSoLuong() > max) {
                throw new InventoryValidationException("Lô " + line.getMaLo() + " chỉ còn được trả " + Math.max(0, max)
                        + " theo phiếu xuất " + issue.getMaPhieuXuat() + ". Hãy sửa số lượng trả.");
            }
        }
        if (receipt.getLyDo().restock()) {
            // Khóa lô theo mã tăng dần — cùng thứ tự với nhập/xuất kho để không khóa chéo.
            for (StockReturnLine line : ordered) {
                StockLot lot = lotRepository.findForUpdate(line.getMaLo())
                        .orElseThrow(() -> new InventoryValidationException("Không tìm thấy lô " + line.getMaLo() + "."));
                lot.receive(line.getSoLuong());
                lotRepository.saveAndFlush(lot);
            }
        }
        receipt.approve(actor.maNhanVien(), now.truncatedTo(ChronoUnit.SECONDS));
        return toDetail(receiptRepository.saveAndFlush(receipt));
    }

    @Transactional
    public StockReturnDetailResponse cancel(InventoryActor actor, String maPhieu, String lyDo) {
        requireManager(actor);
        String reason = required(lyDo, "Lý do hủy", 300);
        StockReturnReceipt receipt = lockReceipt(maPhieu);
        if (receipt.getTrangThai() != VoucherStatus.CHO_XU_LY) {
            throw new InventoryValidationException("Chỉ hủy được phiếu đang chờ kiểm duyệt.");
        }
        receipt.cancel(actor.maNhanVien(), reason, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return toDetail(receiptRepository.saveAndFlush(receipt));
    }

    // ------------------------------------------------------------ kiểm tra

    record Validated(String maPhieuXuat, LocalDate ngay, ReturnReason lyDo, String nguon, String nguoi,
                     String ghiChu, List<StockReturnLine> lines) {
    }

    private Validated validate(StockReturnRequest request, LocalDate today, String boQuaPhieu) {
        if (request == null) {
            throw new InventoryValidationException("Thiếu dữ liệu phiếu hoàn trả.");
        }
        if (request.maPhieuXuat() == null || request.maPhieuXuat().isBlank()) {
            throw new InventoryValidationException("Chưa chọn phiếu xuất gốc.");
        }
        // Khóa phiếu xuất gốc: hai người lập phiếu trả cùng lúc không thể cùng vượt số đã xuất.
        String maPx = normalizeCode(request.maPhieuXuat());
        stockOutRepository.findForUpdate(maPx)
                .orElseThrow(() -> new InventoryValidationException("Không tìm thấy phiếu xuất " + maPx + "."));
        StockOutReceipt issue = loadIssue(maPx);
        LocalDate ngay = request.ngayHoanTra();
        if (ngay == null) {
            throw new InventoryValidationException("Chưa chọn ngày hoàn trả.");
        }
        if (ngay.isAfter(today)) {
            throw new InventoryValidationException("Ngày hoàn trả không được sau hôm nay.");
        }
        if (ngay.isBefore(issue.getNgayXuat())) {
            throw new InventoryValidationException("Ngày hoàn trả không được trước ngày xuất của phiếu "
                    + issue.getMaPhieuXuat() + ".");
        }
        if (request.lyDo() == null) {
            throw new InventoryValidationException("Chưa chọn lý do hoàn trả.");
        }
        String nguon = required(request.nguonHoanTra(), "Nguồn hoàn trả", 100);
        String nguoi = required(request.nguoiHoanTra(), "Người hoàn trả", 100);
        List<StockReturnLineRequest> rows = request.dong() == null ? List.of() : request.dong();
        if (rows.isEmpty()) {
            throw new InventoryValidationException("Phiếu hoàn trả cần ít nhất một dòng có số lượng trả.");
        }
        if (rows.size() > MAX_LINES) {
            throw new InventoryValidationException("Một phiếu tối đa " + MAX_LINES + " dòng hàng.");
        }
        Map<Integer, StockOutLine> issuedLines = new HashMap<>();
        issue.getLines().forEach(l -> issuedLines.put(l.getMaLo(), l));
        Map<Integer, Integer> returned = returnedByLot(receiptRepository.findByPhieuXuatWithLines(maPx), boQuaPhieu, true);
        Set<Integer> seen = new HashSet<>();
        List<StockReturnLine> lines = new ArrayList<>();
        int index = 0;
        for (StockReturnLineRequest row : rows) {
            index++;
            String where = "Dòng " + index + ": ";
            if (row == null || row.maLo() == null) {
                throw new InventoryValidationException(where + "chưa chọn lô.");
            }
            if (!seen.add(row.maLo())) {
                throw new InventoryValidationException(where + "lô bị chọn trùng.");
            }
            StockOutLine issued = issuedLines.get(row.maLo());
            if (issued == null) {
                throw new InventoryValidationException(where + "lô " + row.maLo() + " không có trên phiếu xuất " + maPx + ".");
            }
            if (row.soLuong() <= 0 || row.soLuong() > MAX_QUANTITY) {
                throw new InventoryValidationException(where + "số lượng trả phải lớn hơn 0.");
            }
            int remaining = issued.getSoLuong() - returned.getOrDefault(row.maLo(), 0);
            if (row.soLuong() > remaining) {
                throw new InventoryValidationException(where + "chỉ còn được trả " + Math.max(0, remaining)
                        + " (đã xuất " + issued.getSoLuong() + ", đã/đang trả "
                        + returned.getOrDefault(row.maLo(), 0) + ").");
            }
            lines.add(StockReturnLine.of(row.maLo(), row.soLuong(), costOf(issued)));
        }
        return new Validated(maPx, ngay, request.lyDo(), nguon, nguoi, optional(request.ghiChu(), "Ghi chú", 300),
                lines);
    }

    private StockOutReceipt loadIssue(String maPhieuXuat) {
        String ma = normalizeCode(maPhieuXuat);
        StockOutReceipt issue = stockOutRepository.findWithLines(ma)
                .orElseThrow(() -> new InventoryValidationException("Không tìm thấy phiếu xuất " + ma + "."));
        if (issue.getTrangThai() != VoucherStatus.DA_HOAN_THANH) {
            throw new InventoryValidationException("Phiếu xuất " + ma + " chưa xuất kho (" + issue.getTrangThai().label()
                    + ") — không có hàng để hoàn trả.");
        }
        if (!returnable(issue)) {
            throw new InventoryValidationException("Phiếu xuất " + ma + " là phiếu hủy / hao hụt — hàng đã hủy "
                    + "không hoàn trả về kho.");
        }
        return issue;
    }

    private static boolean returnable(StockOutReceipt issue) {
        return issue.getTrangThai() == VoucherStatus.DA_HOAN_THANH && issue.getLyDoXuat() != StockOutReason.HUY_HAO_HUT;
    }

    private static Map<Integer, Integer> issuedByLot(StockOutReceipt issue) {
        Map<Integer, Integer> issued = new HashMap<>();
        issue.getLines().forEach(l -> issued.merge(l.getMaLo(), l.getSoLuong(), Integer::sum));
        return issued;
    }

    /**
     * Số đã trả theo lô từ các phiếu hoàn trả cùng phiếu xuất gốc.
     *
     * @param boQuaPhieu bỏ qua phiếu này (phiếu đang sửa/duyệt)
     * @param tinhChoDuyet true = tính cả phiếu đang chờ duyệt (khi lập/sửa); false = chỉ phiếu đã duyệt
     */
    private static Map<Integer, Integer> returnedByLot(List<StockReturnReceipt> returns, String boQuaPhieu,
                                                       boolean tinhChoDuyet) {
        Map<Integer, Integer> result = new HashMap<>();
        for (StockReturnReceipt r : returns) {
            boolean counted = r.getTrangThai() == VoucherStatus.DA_HOAN_THANH
                    || (tinhChoDuyet && r.getTrangThai() == VoucherStatus.CHO_XU_LY);
            if (!counted || r.getMaPhieuHoanTra().equals(boQuaPhieu)) {
                continue;
            }
            r.getLines().forEach(l -> result.merge(l.getMaLo(), l.getSoLuong(), Integer::sum));
        }
        return result;
    }

    private static BigDecimal costOf(StockOutLine issued) {
        return issued.getDonGiaXuat() == null ? BigDecimal.ZERO : issued.getDonGiaXuat();
    }

    private void requireManager(InventoryActor actor) {
        Objects.requireNonNull(actor, "actor must not be null");
        authorizationService.require(actor.roles(), Permission.INVENTORY_MANAGE);
        if (actor.maNhanVien() == null || actor.maNhanVien().isBlank()) {
            throw new InventoryValidationException("Phiên đăng nhập chưa gắn với nhân viên — không thể ghi chứng từ.");
        }
    }

    private StockReturnReceipt lockReceipt(String maPhieu) {
        String ma = normalizeCode(maPhieu);
        receiptRepository.findForUpdate(ma).orElseThrow(() -> notFound(ma));
        return receiptRepository.findWithLines(ma).orElseThrow(() -> notFound(ma));
    }

    static String formatCode(int number) {
        String digits = String.format(Locale.ROOT, "%03d", number);
        if (digits.length() > 8) {
            throw new InventoryValidationException("Đã hết dải mã phiếu hoàn trả.");
        }
        return "HT" + digits;
    }

    private static String normalizeCode(String ma) {
        return ma == null ? "" : ma.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeOrNull(String ma) {
        return ma == null || ma.isBlank() ? null : normalizeCode(ma);
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
        return new InventoryValidationException("Không tìm thấy phiếu hoàn trả " + ma + ".");
    }

    // ------------------------------------------------------------- chuyển đổi

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

    private Map<Integer, StockLot> lotsOf(List<Integer> ids) {
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

    private StockReturnDetailResponse toDetail(StockReturnReceipt r) {
        Set<String> ids = new HashSet<>();
        ids.add(r.getMaNhanVienLap());
        ids.add(r.getMaNhanVienDuyet());
        ids.add(r.getMaNhanVienHuy());
        Map<String, String> employees = employeeNames(ids);
        Map<Integer, StockLot> lots = lotsOf(r.getLines().stream().map(StockReturnLine::getMaLo).toList());
        Map<String, ItemInfo> infos = new HashMap<>();
        List<StockReturnLineResponse> lines = new ArrayList<>();
        for (StockReturnLine line : r.getLines()) {
            StockLot lot = lots.get(line.getMaLo());
            if (lot == null) {
                continue;
            }
            ItemInfo info = infos.computeIfAbsent(lot.maMatHang(), ma -> itemInfo(ma, lot.getMaThuoc() != null));
            lines.add(new StockReturnLineResponse(lot.getMaLo(), info.loai(), lot.maMatHang(), info.ten(),
                    info.donVi(), lot.getSoLo(), lot.getHanSuDung(), line.getSoLuong(), line.getDonGia(),
                    line.thanhTien()));
        }
        return new StockReturnDetailResponse(r.getMaPhieuHoanTra(), r.getNgayHoanTra(), r.getMaPhieuXuat(),
                r.getLyDo(), r.getNguonHoanTra(), r.getNguoiHoanTra(), r.getTrangThai(), r.getGhiChu(),
                employees.getOrDefault(r.getMaNhanVienLap(), r.getMaNhanVienLap()), r.getThoiDiemLap(),
                nameOf(employees, r.getMaNhanVienDuyet()), r.getThoiDiemDuyet(),
                nameOf(employees, r.getMaNhanVienHuy()), r.getThoiDiemHuy(), r.getLyDoHuy(),
                lines, r.tongTien());
    }

    private static String nameOf(Map<String, String> names, String id) {
        return id == null ? null : names.getOrDefault(id, id);
    }
}
