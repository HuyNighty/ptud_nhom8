package com.quanlynhakhoa.features.inventory.service;

import com.quanlynhakhoa.features.auth.enums.Permission;
import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.service.AuthorizationService;
import com.quanlynhakhoa.features.inventory.dto.request.StockInLineRequest;
import com.quanlynhakhoa.features.inventory.dto.request.StockInRequest;
import com.quanlynhakhoa.features.inventory.dto.request.SupplierRequest;
import com.quanlynhakhoa.features.inventory.dto.response.StockInDetailResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockInLineResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockInRowResponse;
import com.quanlynhakhoa.features.inventory.dto.response.StockInSummaryResponse;
import com.quanlynhakhoa.features.inventory.dto.response.SupplierResponse;
import com.quanlynhakhoa.features.inventory.entity.MedicalSupply;
import com.quanlynhakhoa.features.inventory.entity.Medicine;
import com.quanlynhakhoa.features.inventory.entity.StockInLine;
import com.quanlynhakhoa.features.inventory.entity.StockInReceipt;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import com.quanlynhakhoa.features.inventory.entity.Supplier;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.enums.StockInStatus;
import com.quanlynhakhoa.features.inventory.enums.SupplierStatus;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.repository.MedicalSupplyRepository;
import com.quanlynhakhoa.features.inventory.repository.MedicineRepository;
import com.quanlynhakhoa.features.inventory.repository.StockInReceiptRepository;
import com.quanlynhakhoa.features.inventory.repository.StockLotRepository;
import com.quanlynhakhoa.features.inventory.repository.SupplierRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhập kho (UC30): nhà cung cấp, phiếu nhập và cộng tồn theo lô.
 *
 * <p>Vòng đời phiếu: tạo → CHO_XU_LY (sửa được) → "Nhập kho" = DA_HOAN_THANH, cộng tồn từng lô trong
 * cùng transaction; hoặc → DA_HUY kèm lý do. Phiếu đã hoàn thành/đã hủy giữ nguyên lịch sử,
 * chỉ sửa được ghi chú.</p>
 */
@Service
public class StockInService {

    static final int MAX_LINES = 200;
    static final int MAX_QUANTITY = 1_000_000;
    static final BigDecimal MAX_UNIT_PRICE = new BigDecimal("1000000000000");
    private static final Pattern LOT_NUMBER = Pattern.compile("[A-Za-z0-9._/-]{1,30}");
    private static final Pattern PHONE = Pattern.compile("[0-9+ ()-]{8,20}");

    private final AuthorizationService authorizationService;
    private final StockInReceiptRepository receiptRepository;
    private final SupplierRepository supplierRepository;
    private final StockLotRepository lotRepository;
    private final MedicineRepository medicineRepository;
    private final MedicalSupplyRepository supplyRepository;

    public StockInService(AuthorizationService authorizationService,
                          StockInReceiptRepository receiptRepository,
                          SupplierRepository supplierRepository,
                          StockLotRepository lotRepository,
                          MedicineRepository medicineRepository,
                          MedicalSupplyRepository supplyRepository) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.receiptRepository = Objects.requireNonNull(receiptRepository);
        this.supplierRepository = Objects.requireNonNull(supplierRepository);
        this.lotRepository = Objects.requireNonNull(lotRepository);
        this.medicineRepository = Objects.requireNonNull(medicineRepository);
        this.supplyRepository = Objects.requireNonNull(supplyRepository);
    }

    // ------------------------------------------------------------------- đọc

    @Transactional(readOnly = true)
    public StockInSummaryResponse summarize(Set<Role> roles) {
        return summarize(roles, LocalDate.now());
    }

    @Transactional(readOnly = true)
    StockInSummaryResponse summarize(Set<Role> roles, LocalDate today) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        List<StockInReceipt> receipts = receiptRepository.findAllWithLines();
        YearMonth thisMonth = YearMonth.from(today);
        YearMonth lastMonth = thisMonth.minusMonths(1);
        BigDecimal valueThis = BigDecimal.ZERO;
        BigDecimal valueLast = BigDecimal.ZERO;
        long pending = 0;
        long completed = 0;
        long cancelled = 0;
        for (StockInReceipt r : receipts) {
            switch (r.getTrangThai()) {
                case CHO_XU_LY -> pending++;
                case DA_HUY -> cancelled++;
                case DA_HOAN_THANH -> {
                    completed++;
                    YearMonth month = YearMonth.from(r.getNgayNhap());
                    if (month.equals(thisMonth)) {
                        valueThis = valueThis.add(r.tongTien());
                    } else if (month.equals(lastMonth)) {
                        valueLast = valueLast.add(r.tongTien());
                    }
                }
            }
        }
        return new StockInSummaryResponse(receipts.size(), valueThis, valueLast, pending, completed, cancelled);
    }

    /** Toàn bộ phiếu (mới nhất trước); màn hình lọc theo từ khóa/NCC/trạng thái/thời gian. */
    @Transactional(readOnly = true)
    public List<StockInRowResponse> list(Set<Role> roles) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        List<StockInReceipt> receipts = receiptRepository.findAllWithLines();
        Map<String, String> suppliers = supplierNames();
        Set<String> employeeIds = new HashSet<>();
        receipts.forEach(r -> employeeIds.add(r.getMaNhanVienLap()));
        Map<String, String> employees = employeeNames(employeeIds);
        List<StockInRowResponse> rows = new ArrayList<>(receipts.size());
        for (StockInReceipt r : receipts) {
            rows.add(new StockInRowResponse(r.getMaPhieuNhap(), r.getNgayNhap(), r.getMaNhaCungCap(),
                    suppliers.getOrDefault(r.getMaNhaCungCap(), r.getMaNhaCungCap()),
                    employees.getOrDefault(r.getMaNhanVienLap(), r.getMaNhanVienLap()),
                    r.tongTien(), r.getTrangThai()));
        }
        return List.copyOf(rows);
    }

    @Transactional(readOnly = true)
    public StockInDetailResponse getDetail(Set<Role> roles, String maPhieu) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        StockInReceipt receipt = receiptRepository.findWithLines(normalizeCode(maPhieu))
                .orElseThrow(() -> notFound(maPhieu));
        return toDetail(receipt);
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> listSuppliers(Set<Role> roles) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        return supplierRepository.findAllByOrderByTenNhaCungCapAsc().stream().map(StockInService::toResponse).toList();
    }

    // ------------------------------------------------------------------- ghi

    @Transactional
    public SupplierResponse createSupplier(Set<Role> roles, SupplierRequest request) {
        authorizationService.require(roles, Permission.INVENTORY_MANAGE);
        if (request == null) {
            throw new InventoryValidationException("Thiếu thông tin nhà cung cấp.");
        }
        String ten = required(request.ten(), "Tên nhà cung cấp", 150);
        String phone = required(request.soDienThoai(), "Số điện thoại", 20);
        if (!PHONE.matcher(phone).matches()) {
            throw new InventoryValidationException("Số điện thoại không hợp lệ.");
        }
        String email = optional(request.email(), "Email", 100);
        if (email != null && !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new InventoryValidationException("Email không hợp lệ.");
        }
        boolean duplicateName = supplierRepository.findAllByOrderByTenNhaCungCapAsc().stream()
                .anyMatch(s -> s.getTenNhaCungCap().equalsIgnoreCase(ten));
        if (duplicateName) {
            throw new InventoryValidationException("Nhà cung cấp \"" + ten + "\" đã có trong danh sách.");
        }
        Integer max = supplierRepository.findMaxCodeNumber();
        String ma = "NCC" + String.format(Locale.ROOT, "%03d", max == null ? 1 : max + 1);
        Supplier saved = supplierRepository.saveAndFlush(
                Supplier.create(ma, ten, phone, optional(request.diaChi(), "Địa chỉ", 255), email));
        return toResponse(saved);
    }

    @Transactional
    public StockInDetailResponse create(InventoryActor actor, StockInRequest request) {
        return create(actor, request, LocalDateTime.now());
    }

    @Transactional
    StockInDetailResponse create(InventoryActor actor, StockInRequest request, LocalDateTime now) {
        requireManager(actor);
        ValidatedReceipt v = validate(request, now.toLocalDate(), true);
        Integer max = receiptRepository.lockAndFindMaxCodeNumber();
        String ma = formatReceiptCode(max == null ? 1 : max + 1);
        StockInReceipt receipt = StockInReceipt.draft(ma, v.ngayNhap(), v.maNhaCungCap(), v.ghiChu(),
                actor.maNhanVien(), now.truncatedTo(ChronoUnit.SECONDS));
        receipt.replaceLines(v.lines());
        return toDetail(receiptRepository.saveAndFlush(receipt));
    }

    /** Sửa toàn bộ phiếu — chỉ khi còn CHO_XU_LY. */
    @Transactional
    public StockInDetailResponse update(InventoryActor actor, String maPhieu, StockInRequest request) {
        requireManager(actor);
        StockInReceipt receipt = lockReceipt(maPhieu);
        if (receipt.getTrangThai() != StockInStatus.CHO_XU_LY) {
            throw new InventoryValidationException("Chỉ sửa được phiếu đang chờ xử lý. Phiếu đã nhập kho "
                    + "hoặc đã hủy chỉ được sửa ghi chú.");
        }
        ValidatedReceipt v = validate(request, LocalDate.now(), !isSameSupplier(receipt, request));
        receipt.updateHeader(v.ngayNhap(), v.maNhaCungCap(), v.ghiChu());
        receipt.replaceLines(v.lines());
        return toDetail(receiptRepository.saveAndFlush(receipt));
    }

    @Transactional
    public StockInDetailResponse updateNote(InventoryActor actor, String maPhieu, String ghiChu) {
        requireManager(actor);
        StockInReceipt receipt = lockReceipt(maPhieu);
        if (receipt.getTrangThai() == StockInStatus.DA_HUY) {
            throw new InventoryValidationException("Phiếu đã hủy không được sửa.");
        }
        receipt.updateNote(optional(ghiChu, "Ghi chú", 300));
        return toDetail(receiptRepository.saveAndFlush(receipt));
    }

    /** "Nhập kho": tạo/cộng tồn từng lô và chuyển phiếu sang DA_HOAN_THANH trong một transaction. */
    @Transactional
    public StockInDetailResponse complete(InventoryActor actor, String maPhieu) {
        return complete(actor, maPhieu, LocalDateTime.now());
    }

    @Transactional
    StockInDetailResponse complete(InventoryActor actor, String maPhieu, LocalDateTime now) {
        requireManager(actor);
        StockInReceipt receipt = lockReceipt(maPhieu);
        if (receipt.getTrangThai() != StockInStatus.CHO_XU_LY) {
            throw new InventoryValidationException("Phiếu " + receipt.getMaPhieuNhap() + " không còn ở trạng thái "
                    + "chờ xử lý (" + receipt.getTrangThai().label() + ").");
        }
        if (receipt.getLines().isEmpty()) {
            throw new InventoryValidationException("Phiếu chưa có dòng hàng nào.");
        }
        // Khóa lô theo thứ tự cố định (mã hàng, số lô) để hai phiếu cùng lô không khóa chéo nhau (deadlock).
        List<StockInLine> ordered = receipt.getLines().stream()
                .sorted(java.util.Comparator.comparing(StockInLine::maMatHang).thenComparing(StockInLine::getSoLo))
                .toList();
        for (StockInLine line : ordered) {
            StockLot lot = (line.getMaThuoc() != null
                    ? lotRepository.findByMaThuocAndSoLo(line.getMaThuoc(), line.getSoLo())
                    : lotRepository.findByMaVatTuAndSoLo(line.getMaVatTu(), line.getSoLo()))
                    .orElse(null);
            if (lot == null) {
                lot = StockLot.open(line.getMaThuoc(), line.getMaVatTu(), line.getSoLo(), line.getHanSuDung(),
                        receipt.getMaNhaCungCap(), receipt.getNgayNhap());
            } else if (!Objects.equals(lot.getHanSuDung(), line.getHanSuDung())) {
                throw new InventoryValidationException("Lô " + line.getSoLo() + " của " + line.maMatHang()
                        + " đã có trong kho với hạn sử dụng khác (" + lot.getHanSuDung() + "). Kiểm tra lại số lô.");
            }
            lot.receive(line.getSoLuong());
            StockLot saved = lotRepository.saveAndFlush(lot);
            line.linkLot(saved.getMaLo());
        }
        receipt.markCompleted(actor.maNhanVien(), now.truncatedTo(ChronoUnit.SECONDS));
        return toDetail(receiptRepository.saveAndFlush(receipt));
    }

    @Transactional
    public StockInDetailResponse cancel(InventoryActor actor, String maPhieu, String lyDo) {
        requireManager(actor);
        String reason = required(lyDo, "Lý do hủy", 300);
        StockInReceipt receipt = lockReceipt(maPhieu);
        if (receipt.getTrangThai() != StockInStatus.CHO_XU_LY) {
            throw new InventoryValidationException("Chỉ hủy được phiếu đang chờ xử lý. Phiếu đã nhập kho cần "
                    + "điều chỉnh bằng phiếu xuất/hủy để giữ lịch sử tồn.");
        }
        receipt.cancel(actor.maNhanVien(), reason, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return toDetail(receiptRepository.saveAndFlush(receipt));
    }

    // ------------------------------------------------------------ kiểm tra

    record ValidatedReceipt(LocalDate ngayNhap, String maNhaCungCap, String ghiChu, List<StockInLine> lines) {
    }

    private static boolean isSameSupplier(StockInReceipt receipt, StockInRequest request) {
        return request != null && Objects.equals(receipt.getMaNhaCungCap(),
                request.maNhaCungCap() == null ? null : request.maNhaCungCap().trim().toUpperCase(Locale.ROOT));
    }

    /**
     * @param requireActiveSupplier phiếu mới / đổi NCC thì NCC phải đang hợp tác
     */
    private ValidatedReceipt validate(StockInRequest request, LocalDate today, boolean requireActiveSupplier) {
        if (request == null) {
            throw new InventoryValidationException("Thiếu dữ liệu phiếu nhập.");
        }
        LocalDate ngay = request.ngayNhap();
        if (ngay == null) {
            throw new InventoryValidationException("Chưa chọn ngày nhập.");
        }
        if (ngay.isAfter(today)) {
            throw new InventoryValidationException("Ngày nhập không được sau hôm nay.");
        }
        String maNcc = request.maNhaCungCap() == null ? "" : request.maNhaCungCap().trim().toUpperCase(Locale.ROOT);
        if (maNcc.isEmpty()) {
            throw new InventoryValidationException("Chưa chọn nhà cung cấp.");
        }
        Supplier supplier = supplierRepository.findById(maNcc)
                .orElseThrow(() -> new InventoryValidationException("Không tìm thấy nhà cung cấp " + maNcc + "."));
        if (requireActiveSupplier && supplier.getTrangThai() != SupplierStatus.DANG_HOP_TAC) {
            throw new InventoryValidationException("Nhà cung cấp " + supplier.getTenNhaCungCap() + " đã ngừng hợp tác.");
        }
        List<StockInLineRequest> rows = request.dong() == null ? List.of() : request.dong();
        if (rows.isEmpty()) {
            throw new InventoryValidationException("Phiếu nhập cần ít nhất một dòng hàng.");
        }
        if (rows.size() > MAX_LINES) {
            throw new InventoryValidationException("Một phiếu tối đa " + MAX_LINES + " dòng hàng.");
        }
        List<StockInLine> lines = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int index = 0;
        for (StockInLineRequest row : rows) {
            index++;
            lines.add(validateLine(row, index, ngay, seen));
        }
        return new ValidatedReceipt(ngay, maNcc, optional(request.ghiChu(), "Ghi chú", 300), lines);
    }

    private StockInLine validateLine(StockInLineRequest row, int index, LocalDate ngayNhap, Set<String> seen) {
        String where = "Dòng " + index + ": ";
        if (row == null || row.loai() == null) {
            throw new InventoryValidationException(where + "chưa chọn mặt hàng.");
        }
        String ma = row.maMatHang() == null ? "" : row.maMatHang().trim().toUpperCase(Locale.ROOT);
        boolean medicine = row.loai() == InventoryItemType.THUOC;
        boolean exists = medicine ? medicineRepository.existsById(ma) : supplyRepository.existsById(ma);
        if (!exists) {
            throw new InventoryValidationException(where + "không tìm thấy mặt hàng " + ma + ".");
        }
        String soLo = row.soLo() == null ? "" : row.soLo().trim().toUpperCase(Locale.ROOT);
        if (!LOT_NUMBER.matcher(soLo).matches()) {
            throw new InventoryValidationException(where + "số lô bắt buộc, tối đa 30 ký tự (chữ, số, . _ / -).");
        }
        if (!seen.add(ma + "|" + soLo)) {
            throw new InventoryValidationException(where + "mặt hàng " + ma + " lô " + soLo + " bị nhập trùng.");
        }
        LocalDate han = row.hanSuDung();
        if (medicine && han == null) {
            throw new InventoryValidationException(where + "thuốc bắt buộc có hạn sử dụng.");
        }
        if (han != null && !han.isAfter(ngayNhap)) {
            throw new InventoryValidationException(where + "hạn sử dụng phải sau ngày nhập (không nhập hàng đã hết hạn).");
        }
        if (row.soLuong() <= 0 || row.soLuong() > MAX_QUANTITY) {
            throw new InventoryValidationException(where + "số lượng phải từ 1 đến " + MAX_QUANTITY + ".");
        }
        BigDecimal price = row.donGia();
        if (price == null || price.signum() < 0 || price.compareTo(MAX_UNIT_PRICE) > 0) {
            throw new InventoryValidationException(where + "đơn giá không hợp lệ.");
        }
        if (price.stripTrailingZeros().scale() > 0) {
            throw new InventoryValidationException(where + "đơn giá tính theo đồng, không có phần lẻ.");
        }
        return StockInLine.of(medicine ? ma : null, medicine ? null : ma, soLo, han, row.soLuong(),
                price.setScale(0));
    }

    private void requireManager(InventoryActor actor) {
        Objects.requireNonNull(actor, "actor must not be null");
        authorizationService.require(actor.roles(), Permission.INVENTORY_MANAGE);
        if (actor.maNhanVien() == null || actor.maNhanVien().isBlank()) {
            throw new InventoryValidationException("Phiên đăng nhập chưa gắn với nhân viên — không thể ghi chứng từ.");
        }
    }

    private StockInReceipt lockReceipt(String maPhieu) {
        String ma = normalizeCode(maPhieu);
        receiptRepository.findForUpdate(ma).orElseThrow(() -> notFound(ma));
        return receiptRepository.findWithLines(ma).orElseThrow(() -> notFound(ma));
    }

    private static String normalizeCode(String ma) {
        return ma == null ? "" : ma.trim().toUpperCase(Locale.ROOT);
    }

    static String formatReceiptCode(int number) {
        String digits = String.format(Locale.ROOT, "%04d", number);
        if (digits.length() > 8) {
            throw new InventoryValidationException("Đã hết dải mã phiếu nhập.");
        }
        return "PN" + digits;
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
        return new InventoryValidationException("Không tìm thấy phiếu nhập " + ma + ".");
    }

    // ------------------------------------------------------------- chuyển đổi

    private Map<String, String> supplierNames() {
        Map<String, String> names = new HashMap<>();
        supplierRepository.findAllByOrderByTenNhaCungCapAsc().forEach(s -> names.put(s.getMaNhaCungCap(), s.getTenNhaCungCap()));
        return names;
    }

    private Map<String, String> employeeNames(Set<String> ids) {
        Map<String, String> names = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : receiptRepository.findEmployeeNames(ids)) {
                names.put((String) row[0], (String) row[1]);
            }
        }
        return names;
    }

    private StockInDetailResponse toDetail(StockInReceipt r) {
        Set<String> ids = new HashSet<>();
        ids.add(r.getMaNhanVienLap());
        if (r.getMaNhanVienNhapKho() != null) {
            ids.add(r.getMaNhanVienNhapKho());
        }
        if (r.getMaNhanVienHuy() != null) {
            ids.add(r.getMaNhanVienHuy());
        }
        Map<String, String> employees = employeeNames(ids);
        String supplierName = supplierRepository.findById(r.getMaNhaCungCap())
                .map(Supplier::getTenNhaCungCap).orElse(r.getMaNhaCungCap());
        List<StockInLineResponse> lines = new ArrayList<>();
        for (StockInLine line : r.getLines()) {
            String ten;
            String donVi;
            InventoryItemType loai;
            if (line.getMaThuoc() != null) {
                loai = InventoryItemType.THUOC;
                Medicine m = medicineRepository.findById(line.getMaThuoc()).orElse(null);
                ten = m == null ? line.getMaThuoc() : m.getTenThuoc();
                donVi = m == null ? "" : m.getDonViTinh();
            } else {
                loai = InventoryItemType.VAT_TU;
                MedicalSupply s = supplyRepository.findById(line.getMaVatTu()).orElse(null);
                ten = s == null ? line.getMaVatTu() : s.getTenVatTu();
                donVi = s == null ? "" : s.getDonViTinh();
            }
            lines.add(new StockInLineResponse(loai, line.maMatHang(), ten, donVi, line.getSoLo(),
                    line.getHanSuDung(), line.getSoLuong(), line.getDonGiaNhap(), line.thanhTien()));
        }
        return new StockInDetailResponse(r.getMaPhieuNhap(), r.getNgayNhap(), r.getMaNhaCungCap(), supplierName,
                r.getTrangThai(), r.getGhiChu(),
                nameOf(employees, r.getMaNhanVienLap()), r.getThoiDiemLap(),
                nameOf(employees, r.getMaNhanVienNhapKho()), r.getThoiDiemNhapKho(),
                nameOf(employees, r.getMaNhanVienHuy()), r.getThoiDiemHuy(), r.getLyDoHuy(),
                lines, r.tongTien());
    }

    private static String nameOf(Map<String, String> names, String id) {
        return id == null ? null : names.getOrDefault(id, id);
    }

    private static SupplierResponse toResponse(Supplier s) {
        return new SupplierResponse(s.getMaNhaCungCap(), s.getTenNhaCungCap(), s.getSoDienThoai(), s.getDiaChi(),
                s.getEmail(), s.getTrangThai() == SupplierStatus.DANG_HOP_TAC);
    }
}
