package com.quanlynhakhoa.features.inventory.service;

import com.quanlynhakhoa.features.auth.enums.Permission;
import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.service.AuthorizationService;
import com.quanlynhakhoa.features.inventory.dto.request.DisposalLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.DisposalRequest;
import com.quanlynhakhoa.features.inventory.dto.response.DisposalDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.DisposalLineResponse;
import com.quanlynhakhoa.features.inventory.dto.response.DisposalLotOption;
import com.quanlynhakhoa.features.inventory.dto.response.DisposalRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.DisposalSummaryResponse;
import com.quanlynhakhoa.features.inventory.entity.Disposal;
import com.quanlynhakhoa.features.inventory.entity.DisposalLine;
import com.quanlynhakhoa.features.inventory.entity.MedicalSupply;
import com.quanlynhakhoa.features.inventory.entity.Medicine;
import com.quanlynhakhoa.features.inventory.entity.StockInLine;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.enums.DisposalReason;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.repository.DisposalRepository;
import com.quanlynhakhoa.features.inventory.repository.MedicalSupplyRepository;
import com.quanlynhakhoa.features.inventory.repository.MedicineRepository;
import com.quanlynhakhoa.features.inventory.repository.StockInLineRepository;
import com.quanlynhakhoa.features.inventory.repository.StockInReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StockLotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
 * Hủy / hao hụt (UC35): ghi nhận hàng hư hỏng, hết hạn, mất mát; chỉ giảm tồn sau khi được duyệt.
 *
 * <p>Vòng đời: lập phiếu → CHO_XU_LY ("Chờ duyệt", chưa trừ tồn, sửa được; giá trị chốt theo giá nhập của lô)
 * → "Duyệt" bởi người KHÁC người lập = DA_HOAN_THANH: khóa từng lô, kiểm tra lại tồn rồi trừ tồn trong cùng
 * transaction; hoặc → DA_HUY kèm lý do, tồn không đổi.</p>
 */
@Service
public class DisposalService {

    static final int MAX_LINES = 200;
    static final int MAX_QUANTITY = 1_000_000;

    private final AuthorizationService authorizationService;
    private final DisposalRepository disposalRepository;
    private final StockLotRepository lotRepository;
    private final StockInLineRepository stockInLineRepository;
    private final StockInReceiptRepository stockInReceiptRepository;
    private final MedicineRepository medicineRepository;
    private final MedicalSupplyRepository supplyRepository;

    public DisposalService(AuthorizationService authorizationService,
                           DisposalRepository disposalRepository,
                           StockLotRepository lotRepository,
                           StockInLineRepository stockInLineRepository,
                           StockInReceiptRepository stockInReceiptRepository,
                           MedicineRepository medicineRepository,
                           MedicalSupplyRepository supplyRepository) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.disposalRepository = Objects.requireNonNull(disposalRepository);
        this.lotRepository = Objects.requireNonNull(lotRepository);
        this.stockInLineRepository = Objects.requireNonNull(stockInLineRepository);
        this.stockInReceiptRepository = Objects.requireNonNull(stockInReceiptRepository);
        this.medicineRepository = Objects.requireNonNull(medicineRepository);
        this.supplyRepository = Objects.requireNonNull(supplyRepository);
    }

    // ------------------------------------------------------------------- đọc

    @Transactional(readOnly = true)
    public DisposalSummaryResponse summarize(Set<Role> roles) {
        return summarize(roles, LocalDate.now());
    }

    @Transactional(readOnly = true)
    DisposalSummaryResponse summarize(Set<Role> roles, LocalDate today) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        List<Disposal> all = disposalRepository.findAllWithLines();
        YearMonth month = YearMonth.from(today);
        BigDecimal value = BigDecimal.ZERO;
        long pending = 0;
        long done = 0;
        long cancelled = 0;
        for (Disposal d : all) {
            switch (d.getTrangThai()) {
                case CHO_XU_LY -> pending++;
                case DA_HUY -> cancelled++;
                case DA_HOAN_THANH -> {
                    done++;
                    if (YearMonth.from(d.getNgayHuy()).equals(month)) {
                        value = value.add(d.tongTien());
                    }
                }
            }
        }
        return new DisposalSummaryResponse(all.size(), value, pending, done, cancelled);
    }

    @Transactional(readOnly = true)
    public List<DisposalRowResponse> list(Set<Role> roles) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        List<Disposal> all = disposalRepository.findAllWithLines();
        Set<String> ids = new HashSet<>();
        all.forEach(d -> {
            ids.add(d.getMaNhanVienLap());
            ids.add(d.getMaNhanVienDuyet());
        });
        Map<String, String> employees = employeeNames(ids);
        List<DisposalRowResponse> rows = new ArrayList<>();
        for (Disposal d : all) {
            rows.add(new DisposalRowResponse(d.getMaPhieuHuy(), d.getNgayHuy(), d.getLyDo(), d.getMoTa(),
                    employees.getOrDefault(d.getMaNhanVienLap(), d.getMaNhanVienLap()),
                    nameOf(employees, d.getMaNhanVienDuyet()), d.tongTien(), d.getTrangThai()));
        }
        return List.copyOf(rows);
    }

    @Transactional(readOnly = true)
    public DisposalDetailResponse getDetail(Set<Role> roles, String maPhieu) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        String ma = normalizeCode(maPhieu);
        return toDetail(disposalRepository.findWithLines(ma).orElseThrow(() -> notFound(ma)));
    }

    /** Các lô còn tồn (kể cả đã hết hạn — đối tượng chính của phiếu hủy), lô hết hạn xếp trước. */
    @Transactional(readOnly = true)
    public List<DisposalLotOption> availableLots(Set<Role> roles) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        LocalDate today = LocalDate.now();
        List<StockLot> lots = new ArrayList<>(lotRepository.findBySoLuongTonGreaterThan(0));
        lots.sort(Comparator.comparing((StockLot l) -> InventoryRules.usable(l.getHanSuDung(), today))
                .thenComparing(InventoryRules.FEFO));
        Map<String, ItemInfo> infos = new HashMap<>();
        List<DisposalLotOption> result = new ArrayList<>();
        for (StockLot lot : lots) {
            ItemInfo info = infos.computeIfAbsent(lot.maMatHang(), ma -> itemInfo(ma, lot.getMaThuoc() != null));
            result.add(new DisposalLotOption(lot.getMaLo(), info.loai(), lot.maMatHang(), info.ten(), info.donVi(),
                    lot.getSoLo(), lot.getHanSuDung(), lot.getSoLuongTon(),
                    !InventoryRules.usable(lot.getHanSuDung(), today), costOf(lot.getMaLo())));
        }
        return List.copyOf(result);
    }

    // ------------------------------------------------------------------- ghi

    @Transactional
    public DisposalDetailResponse create(InventoryActor actor, DisposalRequest request) {
        return create(actor, request, LocalDateTime.now());
    }

    @Transactional
    DisposalDetailResponse create(InventoryActor actor, DisposalRequest request, LocalDateTime now) {
        requireManager(actor);
        Validated v = validate(request, now.toLocalDate());
        Integer max = disposalRepository.lockAndFindMaxCodeNumber();
        Disposal d = Disposal.draft(formatCode(max == null ? 1 : max + 1), v.ngay(), v.lyDo(), v.moTa(),
                actor.maNhanVien(), now.truncatedTo(ChronoUnit.SECONDS));
        d.replaceLines(v.lines());
        return toDetail(disposalRepository.saveAndFlush(d));
    }

    /** Sửa phiếu đang chờ duyệt (giá trị tính lại theo giá nhập hiện tại của lô). */
    @Transactional
    public DisposalDetailResponse update(InventoryActor actor, String maPhieu, DisposalRequest request) {
        requireManager(actor);
        Disposal d = lock(maPhieu);
        if (d.getTrangThai() != VoucherStatus.CHO_XU_LY) {
            throw new InventoryValidationException("Chỉ sửa được phiếu đang chờ duyệt.");
        }
        Validated v = validate(request, LocalDate.now());
        d.updateHeader(v.ngay(), v.lyDo(), v.moTa());
        d.replaceLines(v.lines());
        return toDetail(disposalRepository.saveAndFlush(d));
    }

    /** "Duyệt": người khác người lập; khóa lô theo mã tăng dần, kiểm tra tồn rồi trừ tồn. */
    @Transactional
    public DisposalDetailResponse approve(InventoryActor actor, String maPhieu) {
        return approve(actor, maPhieu, LocalDateTime.now());
    }

    @Transactional
    DisposalDetailResponse approve(InventoryActor actor, String maPhieu, LocalDateTime now) {
        requireManager(actor);
        Disposal d = lock(maPhieu);
        if (d.getTrangThai() != VoucherStatus.CHO_XU_LY) {
            throw new InventoryValidationException("Phiếu " + d.getMaPhieuHuy() + " không còn chờ duyệt ("
                    + d.getTrangThai().label() + ").");
        }
        if (actor.maNhanVien().equals(d.getMaNhanVienLap())) {
            throw new InventoryValidationException("Người duyệt phải khác người lập phiếu hủy. "
                    + "Hãy nhờ một quản lý kho khác duyệt phiếu này.");
        }
        if (d.getLines().isEmpty()) {
            throw new InventoryValidationException("Phiếu chưa có dòng hàng nào.");
        }
        List<DisposalLine> ordered = d.getLines().stream().sorted(Comparator.comparing(DisposalLine::getMaLo)).toList();
        for (DisposalLine line : ordered) {
            StockLot lot = lotRepository.findForUpdate(line.getMaLo())
                    .orElseThrow(() -> new InventoryValidationException("Không tìm thấy lô " + line.getMaLo() + "."));
            if (line.getSoLuong() > lot.getSoLuongTon()) {
                throw new InventoryValidationException("Lô " + lot.getSoLo() + " (" + lot.maMatHang() + ") hiện chỉ còn "
                        + lot.getSoLuongTon() + ", không đủ hủy " + line.getSoLuong() + ". Hãy sửa phiếu.");
            }
            lot.issue(line.getSoLuong());
            lotRepository.saveAndFlush(lot);
        }
        d.approve(actor.maNhanVien(), now.truncatedTo(ChronoUnit.SECONDS));
        return toDetail(disposalRepository.saveAndFlush(d));
    }

    @Transactional
    public DisposalDetailResponse cancel(InventoryActor actor, String maPhieu, String lyDo) {
        requireManager(actor);
        String reason = required(lyDo, "Lý do hủy phiếu", 300);
        Disposal d = lock(maPhieu);
        if (d.getTrangThai() != VoucherStatus.CHO_XU_LY) {
            throw new InventoryValidationException("Chỉ hủy được phiếu đang chờ duyệt.");
        }
        d.cancel(actor.maNhanVien(), reason, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return toDetail(disposalRepository.saveAndFlush(d));
    }

    // ------------------------------------------------------------ kiểm tra

    record Validated(LocalDate ngay, DisposalReason lyDo, String moTa, List<DisposalLine> lines) {
    }

    private Validated validate(DisposalRequest request, LocalDate today) {
        if (request == null) {
            throw new InventoryValidationException("Thiếu dữ liệu phiếu hủy.");
        }
        if (request.ngayHuy() == null) {
            throw new InventoryValidationException("Chưa chọn ngày hủy.");
        }
        if (request.ngayHuy().isAfter(today)) {
            throw new InventoryValidationException("Ngày hủy không được sau hôm nay.");
        }
        if (request.lyDo() == null) {
            throw new InventoryValidationException("Chưa chọn lý do hủy.");
        }
        List<DisposalLineRequest> rows = request.dong() == null ? List.of() : request.dong();
        if (rows.isEmpty()) {
            throw new InventoryValidationException("Phiếu hủy cần ít nhất một lô.");
        }
        if (rows.size() > MAX_LINES) {
            throw new InventoryValidationException("Một phiếu tối đa " + MAX_LINES + " dòng.");
        }
        Set<Integer> seen = new HashSet<>();
        List<DisposalLine> lines = new ArrayList<>();
        int index = 0;
        for (DisposalLineRequest row : rows) {
            index++;
            String where = "Dòng " + index + ": ";
            if (row == null || row.maLo() == null) {
                throw new InventoryValidationException(where + "chưa chọn lô.");
            }
            if (!seen.add(row.maLo())) {
                throw new InventoryValidationException(where + "lô bị chọn trùng — gộp số lượng vào một dòng.");
            }
            if (row.soLuong() <= 0 || row.soLuong() > MAX_QUANTITY) {
                throw new InventoryValidationException(where + "số lượng hủy phải lớn hơn 0.");
            }
            StockLot lot = lotRepository.findById(row.maLo())
                    .orElseThrow(() -> new InventoryValidationException(where + "không tìm thấy lô " + row.maLo() + "."));
            if (row.soLuong() > lot.getSoLuongTon()) {
                throw new InventoryValidationException(where + "lô " + lot.getSoLo() + " chỉ còn " + lot.getSoLuongTon()
                        + ".");
            }
            lines.add(DisposalLine.of(row.maLo(), row.soLuong(), costOf(row.maLo())));
        }
        return new Validated(request.ngayHuy(), request.lyDo(), optional(request.moTa(), "Mô tả lý do", 200), lines);
    }

    /** Giá nhập gần nhất của lô; 0 nếu lô chưa có dòng nhập (dữ liệu cũ). */
    private BigDecimal costOf(Integer maLo) {
        return stockInLineRepository.findFirstByMaLoOrderByMaChiTietDesc(maLo)
                .map(StockInLine::getDonGiaNhap).orElse(BigDecimal.ZERO);
    }

    private void requireManager(InventoryActor actor) {
        Objects.requireNonNull(actor, "actor must not be null");
        authorizationService.require(actor.roles(), Permission.INVENTORY_MANAGE);
        if (actor.maNhanVien() == null || actor.maNhanVien().isBlank()) {
            throw new InventoryValidationException("Phiên đăng nhập chưa gắn với nhân viên — không thể ghi chứng từ.");
        }
    }

    private Disposal lock(String maPhieu) {
        String ma = normalizeCode(maPhieu);
        disposalRepository.findForUpdate(ma).orElseThrow(() -> notFound(ma));
        return disposalRepository.findWithLines(ma).orElseThrow(() -> notFound(ma));
    }

    static String formatCode(int number) {
        String digits = String.format(Locale.ROOT, "%03d", number);
        if (digits.length() > 8) {
            throw new InventoryValidationException("Đã hết dải mã phiếu hủy.");
        }
        return "HH" + digits;
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
        return new InventoryValidationException("Không tìm thấy phiếu hủy " + ma + ".");
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

    private DisposalDetailResponse toDetail(Disposal d) {
        Set<String> ids = new HashSet<>();
        ids.add(d.getMaNhanVienLap());
        ids.add(d.getMaNhanVienDuyet());
        ids.add(d.getMaNhanVienHuy());
        Map<String, String> employees = employeeNames(ids);
        Map<Integer, StockLot> lots = new LinkedHashMap<>();
        List<Integer> lotIds = d.getLines().stream().map(DisposalLine::getMaLo).toList();
        if (!lotIds.isEmpty()) {
            lotRepository.findByMaLoIn(lotIds).forEach(l -> lots.put(l.getMaLo(), l));
        }
        Map<String, ItemInfo> infos = new HashMap<>();
        List<DisposalLineResponse> lines = new ArrayList<>();
        for (DisposalLine line : d.getLines()) {
            StockLot lot = lots.get(line.getMaLo());
            if (lot == null) {
                continue;
            }
            ItemInfo info = infos.computeIfAbsent(lot.maMatHang(), ma -> itemInfo(ma, lot.getMaThuoc() != null));
            lines.add(new DisposalLineResponse(lot.getMaLo(), info.loai(), lot.maMatHang(), info.ten(), info.donVi(),
                    lot.getSoLo(), lot.getHanSuDung(), lot.getSoLuongTon(), line.getSoLuong(), line.getDonGia(),
                    line.thanhTien()));
        }
        return new DisposalDetailResponse(d.getMaPhieuHuy(), d.getNgayHuy(), d.getLyDo(), d.getMoTa(),
                d.getTrangThai(), d.getMaNhanVienLap(),
                employees.getOrDefault(d.getMaNhanVienLap(), d.getMaNhanVienLap()), d.getThoiDiemLap(),
                nameOf(employees, d.getMaNhanVienDuyet()), d.getThoiDiemDuyet(),
                nameOf(employees, d.getMaNhanVienHuy()), d.getThoiDiemHuy(), d.getLyDoHuy(), lines, d.tongTien());
    }

    private static String nameOf(Map<String, String> names, String id) {
        return id == null ? null : names.getOrDefault(id, id);
    }
}
