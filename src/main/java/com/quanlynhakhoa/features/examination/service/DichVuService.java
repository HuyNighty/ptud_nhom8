package com.quanlynhakhoa.features.examination.service;

import com.quanlynhakhoa.features.examination.dto.request.DichVuCreateRequest;
import com.quanlynhakhoa.features.examination.dto.request.DichVuUpdateRequest;
import com.quanlynhakhoa.features.examination.dto.response.DichVuResponse;
import com.quanlynhakhoa.features.examination.entity.DichVu;
import com.quanlynhakhoa.features.examination.repository.DichVuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class DichVuService {

    private final DichVuRepository repository;
    private final DichVuAccessPolicy accessPolicy;
    private final Clock clock;

    public DichVuService(
            DichVuRepository repository,
            DichVuAccessPolicy accessPolicy,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional(readOnly = true)
    public List<DichVuResponse> timKiem(String tuKhoa, Boolean dangApDung) {
        accessPolicy.requireRead();
        String keyword = tuKhoa == null ? "" : tuKhoa.strip().toLowerCase(Locale.ROOT);

        return repository.findAllByOrderByTenDichVuAsc().stream()
                .filter(item -> dangApDung == null || item.isDangApDung() == dangApDung)
                .filter(item -> keyword.isEmpty()
                        || item.getMaDichVu().toLowerCase(Locale.ROOT).contains(keyword)
                        || item.getTenDichVu().toLowerCase(Locale.ROOT).contains(keyword))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DichVuResponse taoMoi(DichVuCreateRequest request) {
        accessPolicy.requireManage();
        if (request == null) {
            throw invalid("Dữ liệu dịch vụ không được để trống.");
        }

        String code = normalizedCode(request.maDichVu());
        String name = normalizedRequired(request.tenDichVu(), 100, "Tên dịch vụ");
        String description = normalizedOptional(request.moTa(), 500, "Mô tả");
        BigDecimal price = normalizedPrice(request.donGia());
        boolean clinicalLab = requireBoolean(request.laCanLamSang());

        if (repository.existsById(code)) {
            throw new DichVuException(
                    DichVuException.Code.CODE_ALREADY_EXISTS,
                    "Mã dịch vụ đã tồn tại: " + code
            );
        }
        if (repository.existsByTenDichVuIgnoreCase(name)) {
            throw new DichVuException(
                    DichVuException.Code.NAME_ALREADY_EXISTS,
                    "Tên dịch vụ đã tồn tại: " + name
            );
        }

        DichVu entity = new DichVu(code, name, description, price, clinicalLab, nowUtc());
        return toResponse(repository.save(entity));
    }

    @Transactional
    public DichVuResponse capNhat(String maDichVu, DichVuUpdateRequest request) {
        accessPolicy.requireManage();
        String code = normalizedCode(maDichVu);
        if (request == null) {
            throw invalid("Dữ liệu dịch vụ không được để trống.");
        }

        DichVu entity = repository.findById(code)
                .orElseThrow(() -> new DichVuException(
                        DichVuException.Code.NOT_FOUND,
                        "Không tìm thấy dịch vụ: " + code
                ));

        String name = normalizedRequired(request.tenDichVu(), 100, "Tên dịch vụ");
        String description = normalizedOptional(request.moTa(), 500, "Mô tả");
        BigDecimal price = normalizedPrice(request.donGia());
        boolean clinicalLab = requireBoolean(request.laCanLamSang());

        if (repository.existsByTenDichVuIgnoreCaseAndMaDichVuNot(name, code)) {
            throw new DichVuException(
                    DichVuException.Code.NAME_ALREADY_EXISTS,
                    "Tên dịch vụ đã được dùng bởi dịch vụ khác: " + name
            );
        }

        entity.capNhatThongTin(name, description, price, clinicalLab, nowUtc());
        return toResponse(repository.save(entity));
    }

    @Transactional
    public DichVuResponse doiTrangThai(String maDichVu, boolean dangApDung) {
        accessPolicy.requireManage();
        String code = normalizedCode(maDichVu);
        DichVu entity = repository.findById(code)
                .orElseThrow(() -> new DichVuException(
                        DichVuException.Code.NOT_FOUND,
                        "Không tìm thấy dịch vụ: " + code
                ));

        entity.capNhatTrangThai(dangApDung, nowUtc());
        return toResponse(repository.save(entity));
    }

    private DichVuResponse toResponse(DichVu item) {
        return new DichVuResponse(
                item.getMaDichVu(),
                item.getTenDichVu(),
                item.getMoTa(),
                item.getDonGia(),
                item.isLaCanLamSang(),
                item.isDangApDung(),
                item.getTaoLuc(),
                item.getCapNhatLuc()
        );
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.ofInstant(
                clock.instant().truncatedTo(ChronoUnit.MILLIS), ZoneOffset.UTC
        );
    }

    private static String normalizedCode(String value) {
        return normalizedRequired(value, 10, "Mã dịch vụ").toUpperCase(Locale.ROOT);
    }

    private static String normalizedRequired(String value, int maxLength, String label) {
        if (value == null || value.isBlank()) {
            throw invalid(label + " không được để trống.");
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw invalid(label + " không được vượt quá " + maxLength + " ký tự.");
        }
        return normalized;
    }

    private static String normalizedOptional(String value, int maxLength, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw invalid(label + " không được vượt quá " + maxLength + " ký tự.");
        }
        return normalized;
    }

    private static BigDecimal normalizedPrice(BigDecimal value) {
        if (value == null) {
            throw invalid("Đơn giá không được để trống.");
        }
        BigDecimal price;
        try {
            price = value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw invalid("Đơn giá chỉ được có tối đa 2 chữ số thập phân.");
        }
        if (price.signum() < 0) {
            throw invalid("Đơn giá không được âm.");
        }
        if (Math.max(0, price.precision() - price.scale()) > 10) {
            throw invalid("Đơn giá vượt quá giới hạn DECIMAL(12,2).");
        }
        return price;
    }

    private static boolean requireBoolean(Boolean value) {
        if (value == null) {
            throw invalid("Vui lòng xác định dịch vụ có phải cận lâm sàng hay không.");
        }
        return value;
    }

    private static DichVuException invalid(String message) {
        return new DichVuException(DichVuException.Code.INVALID_DATA, message);
    }
}
