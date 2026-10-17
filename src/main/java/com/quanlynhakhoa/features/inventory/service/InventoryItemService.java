package com.quanlynhakhoa.features.inventory.service;

import com.quanlynhakhoa.features.auth.enums.Permission;
import com.quanlynhakhoa.features.auth.enums.Role;
import com.quanlynhakhoa.features.auth.service.AuthorizationService;
import com.quanlynhakhoa.features.inventory.dto.request.InventoryItemRequest;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryCatalogSummaryResponse;
import com.quanlynhakhoa.features.inventory.dto.response.InventoryItemResponse;
import com.quanlynhakhoa.features.inventory.entity.MedicalSupply;
import com.quanlynhakhoa.features.inventory.entity.Medicine;
import com.quanlynhakhoa.features.inventory.enums.InventoryItemType;
import com.quanlynhakhoa.features.inventory.exception.InventoryValidationException;
import com.quanlynhakhoa.features.inventory.repository.MedicalSupplyRepository;
import com.quanlynhakhoa.features.inventory.repository.MedicineRepository;
import com.quanlynhakhoa.features.inventory.repository.StockLotRepository;
import com.quanlynhakhoa.features.inventory.entity.StockLot;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh mục Thuốc & vật tư (UC27 tìm kiếm, UC29 quản lý vật tư) — M1: tạo, sửa, tìm.
 * Tồn kho/lô/phiếu thuộc M2 và không xử lý ở đây.
 */
@Service
public class InventoryItemService {

    static final int MAX_TEN = 150;
    static final int MAX_DON_VI = 30;
    static final int MAX_HAM_LUONG = 50;
    static final int MAX_HOAT_CHAT = 150;
    static final int MAX_MO_TA = 300;
    static final int MAX_DINH_MUC = 1_000_000;
    /** Tiền tố 2 ký tự + 3..8 chữ số → tối đa 10 ký tự, khớp VARCHAR(10). */
    private static final Pattern CODE_DIGITS = Pattern.compile("\\d{3,8}");

    private final AuthorizationService authorizationService;
    private final MedicineRepository medicineRepository;
    private final MedicalSupplyRepository supplyRepository;
    private final StockLotRepository lotRepository;

    public InventoryItemService(AuthorizationService authorizationService,
                                MedicineRepository medicineRepository,
                                MedicalSupplyRepository supplyRepository,
                                StockLotRepository lotRepository) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.medicineRepository = Objects.requireNonNull(medicineRepository);
        this.supplyRepository = Objects.requireNonNull(supplyRepository);
        this.lotRepository = Objects.requireNonNull(lotRepository);
    }

    /** Tồn khả dụng và hạn gần nhất của một mặt hàng, tính từ các lô còn tồn. */
    record StockSnapshot(int tonKho, LocalDate hanGanNhat) {
        static final StockSnapshot EMPTY = new StockSnapshot(0, null);
    }

    Map<String, StockSnapshot> stockByItem(LocalDate today) {
        Map<String, StockSnapshot> result = new HashMap<>();
        for (StockLot lot : lotRepository.findBySoLuongTonGreaterThan(0)) {
            if (!InventoryRules.usable(lot.getHanSuDung(), today)) {
                continue;
            }
            StockSnapshot current = result.getOrDefault(lot.maMatHang(), StockSnapshot.EMPTY);
            LocalDate nearest = current.hanGanNhat();
            if (lot.getHanSuDung() != null && (nearest == null || lot.getHanSuDung().isBefore(nearest))) {
                nearest = lot.getHanSuDung();
            }
            result.put(lot.maMatHang(), new StockSnapshot(current.tonKho() + lot.getSoLuongTon(), nearest));
        }
        return result;
    }

    /**
     * @param loai null = cả thuốc và vật tư
     */
    @Transactional(readOnly = true)
    public List<InventoryItemResponse> search(Set<Role> roles, String keyword, InventoryItemType loai) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        Map<String, StockSnapshot> stock = stockByItem(LocalDate.now());
        List<InventoryItemResponse> result = new ArrayList<>();
        if (loai == null || loai == InventoryItemType.THUOC) {
            medicineRepository.search(normalized).forEach(m -> result.add(toResponse(m, stock.get(m.getMaThuoc()))));
        }
        if (loai == null || loai == InventoryItemType.VAT_TU) {
            supplyRepository.search(normalized).forEach(v -> result.add(toResponse(v, stock.get(v.getMaVatTu()))));
        }
        return List.copyOf(result);
    }

    /** Số mặt hàng theo loại và số mặt hàng có tồn khả dụng dưới định mức. */
    @Transactional(readOnly = true)
    public InventoryCatalogSummaryResponse summarize(Set<Role> roles) {
        authorizationService.require(roles, Permission.INVENTORY_READ);
        long soThuoc = medicineRepository.count();
        long soVatTu = supplyRepository.count();
        Map<String, StockSnapshot> stock = stockByItem(LocalDate.now());
        int canBoSung = 0;
        for (Medicine m : medicineRepository.search("")) {
            if (stock.getOrDefault(m.getMaThuoc(), StockSnapshot.EMPTY).tonKho() < m.getDinhMucToiThieu()) {
                canBoSung++;
            }
        }
        for (MedicalSupply v : supplyRepository.search("")) {
            if (stock.getOrDefault(v.getMaVatTu(), StockSnapshot.EMPTY).tonKho() < v.getDinhMucToiThieu()) {
                canBoSung++;
            }
        }
        return new InventoryCatalogSummaryResponse(soThuoc + soVatTu, soThuoc, soVatTu, true, canBoSung);
    }

    /** Gợi ý mã kế tiếp (VD: TH001, VT014). Người dùng vẫn có thể sửa trước khi lưu. */
    @Transactional(readOnly = true)
    public String suggestNextCode(Set<Role> roles, InventoryItemType loai) {
        authorizationService.require(roles, Permission.INVENTORY_MANAGE);
        Objects.requireNonNull(loai, "loai must not be null");
        Integer max = loai == InventoryItemType.THUOC
                ? medicineRepository.findMaxCodeNumber()
                : supplyRepository.findMaxCodeNumber();
        return formatCode(loai, max == null ? 1 : max + 1);
    }

    @Transactional
    public InventoryItemResponse create(Set<Role> roles, InventoryItemRequest request) {
        authorizationService.require(roles, Permission.INVENTORY_MANAGE);
        Normalized n = normalize(request);
        try {
            if (n.loai == InventoryItemType.THUOC) {
                if (medicineRepository.existsById(n.ma)) {
                    throw duplicate(n.ma);
                }
                return toResponse(medicineRepository.saveAndFlush(Medicine.create(
                        n.ma, n.ten, n.donViTinh, n.kiemSoatDacBiet, n.hamLuong, n.hoatChat, n.dinhMuc)), null);
            }
            if (supplyRepository.existsById(n.ma)) {
                throw duplicate(n.ma);
            }
            return toResponse(supplyRepository.saveAndFlush(MedicalSupply.create(
                    n.ma, n.ten, n.donViTinh, n.moTa, n.dinhMuc)), null);
        } catch (DataIntegrityViolationException e) {
            // Hai người cùng lưu một mã: PK chặn ở DB, báo lỗi thân thiện thay vì lỗi SQL.
            throw duplicate(n.ma);
        }
    }

    /** Mã và loại không đổi sau khi tạo (là khóa được lô/phiếu tham chiếu ở M2). */
    @Transactional
    public InventoryItemResponse update(Set<Role> roles, InventoryItemRequest request) {
        authorizationService.require(roles, Permission.INVENTORY_MANAGE);
        Normalized n = normalize(request);
        if (n.loai == InventoryItemType.THUOC) {
            Medicine medicine = medicineRepository.findById(n.ma).orElseThrow(() -> notFound(n.ma));
            medicine.updateInfo(n.ten, n.donViTinh, n.kiemSoatDacBiet, n.hamLuong, n.hoatChat, n.dinhMuc);
            return toResponse(medicineRepository.saveAndFlush(medicine), stockByItem(LocalDate.now()).get(n.ma));
        }
        MedicalSupply supply = supplyRepository.findById(n.ma).orElseThrow(() -> notFound(n.ma));
        supply.updateInfo(n.ten, n.donViTinh, n.moTa, n.dinhMuc);
        return toResponse(supplyRepository.saveAndFlush(supply), stockByItem(LocalDate.now()).get(n.ma));
    }

    // ------------------------------------------------------------- validation

    private record Normalized(InventoryItemType loai, String ma, String ten, String donViTinh, int dinhMuc,
                              String hamLuong, String hoatChat, boolean kiemSoatDacBiet, String moTa) {
    }

    static Normalized normalize(InventoryItemRequest r) {
        if (r == null) {
            throw new InventoryValidationException("Thiếu dữ liệu mặt hàng.");
        }
        if (r.loai() == null) {
            throw new InventoryValidationException("Chưa chọn loại mặt hàng.");
        }
        InventoryItemType loai = r.loai();
        String ma = r.ma() == null ? "" : r.ma().trim().toUpperCase(Locale.ROOT);
        validateCode(loai, ma);
        String ten = required(r.ten(), "Tên", MAX_TEN);
        String donVi = required(r.donViTinh(), "Đơn vị tính", MAX_DON_VI);
        if (r.dinhMucToiThieu() < 0 || r.dinhMucToiThieu() > MAX_DINH_MUC) {
            throw new InventoryValidationException(
                    "Định mức tối thiểu phải từ 0 đến " + MAX_DINH_MUC + ".");
        }
        if (loai == InventoryItemType.THUOC) {
            return new Normalized(loai, ma, ten, donVi, r.dinhMucToiThieu(),
                    optional(r.hamLuong(), "Hàm lượng", MAX_HAM_LUONG),
                    optional(r.hoatChat(), "Hoạt chất", MAX_HOAT_CHAT),
                    r.kiemSoatDacBiet(), null);
        }
        return new Normalized(loai, ma, ten, donVi, r.dinhMucToiThieu(), null, null, false,
                optional(r.moTa(), "Mô tả", MAX_MO_TA));
    }

    static void validateCode(InventoryItemType loai, String ma) {
        String prefix = loai.codePrefix();
        if (!ma.startsWith(prefix) || !CODE_DIGITS.matcher(ma.substring(prefix.length())).matches()) {
            throw new InventoryValidationException(
                    "Mã " + loai.label().toLowerCase(Locale.ROOT) + " phải có dạng " + prefix
                            + " + 3 đến 8 chữ số (VD: " + prefix + "001).");
        }
    }

    static String formatCode(InventoryItemType loai, int number) {
        String digits = String.format(Locale.ROOT, "%03d", number);
        if (digits.length() > 8) {
            throw new InventoryValidationException("Đã hết dải mã cho " + loai.label() + ".");
        }
        return loai.codePrefix() + digits;
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

    private static InventoryValidationException duplicate(String ma) {
        return new InventoryValidationException("Mã " + ma + " đã tồn tại.");
    }

    private static InventoryValidationException notFound(String ma) {
        return new InventoryValidationException("Không tìm thấy mặt hàng " + ma + ".");
    }

    private static InventoryItemResponse toResponse(Medicine m, StockSnapshot stock) {
        StockSnapshot s = stock == null ? StockSnapshot.EMPTY : stock;
        return new InventoryItemResponse(InventoryItemType.THUOC, m.getMaThuoc(), m.getTenThuoc(),
                m.getDonViTinh(), m.getDinhMucToiThieu(), m.isDaDatHang(),
                m.getHamLuong(), m.getHoatChat(), m.isKiemSoatDacBiet(), null,
                s.tonKho(), s.hanGanNhat(), InventoryRules.classifyStock(s.tonKho(), m.getDinhMucToiThieu()));
    }

    private static InventoryItemResponse toResponse(MedicalSupply v, StockSnapshot stock) {
        StockSnapshot s = stock == null ? StockSnapshot.EMPTY : stock;
        return new InventoryItemResponse(InventoryItemType.VAT_TU, v.getMaVatTu(), v.getTenVatTu(),
                v.getDonViTinh(), v.getDinhMucToiThieu(), v.isDaDatHang(),
                null, null, false, v.getMoTa(),
                s.tonKho(), s.hanGanNhat(), InventoryRules.classifyStock(s.tonKho(), v.getDinhMucToiThieu()));
    }
}
