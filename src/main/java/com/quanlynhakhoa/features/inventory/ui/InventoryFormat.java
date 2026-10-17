package com.quanlynhakhoa.features.inventory.ui;

import com.quanlynhakhoa.features.inventory.enums.VoucherStatus;
import java.awt.Color;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** Định dạng tiền/ngày kiểu Việt Nam và màu trạng thái phiếu, dùng chung cho các màn kho. */
final class InventoryFormat {
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter DATE_INPUT =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private InventoryFormat() {
    }

    /** 12500000 → "12.500.000đ". */
    static String money(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        return format.format(amount == null ? BigDecimal.ZERO : amount) + "đ";
    }

    static String date(LocalDate value) {
        return value == null ? "—" : DATE.format(value);
    }

    static String dateTime(LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    /** "5/3/2027" hoặc "05/03/2027"; chuỗi rỗng → null. Sai định dạng → IllegalArgumentException. */
    static LocalDate parseDate(String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value, DATE_INPUT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày \"" + value + "\" không đúng dạng dd/MM/yyyy.");
        }
    }

    /** "12.500.000" / "12500000" / "12 500 000đ" → 12500000. Không phải số nguyên → IllegalArgumentException. */
    static BigDecimal parseMoney(String text) {
        String digits = text == null ? "" : text.replace(".", "").replace(",", "").replace(" ", "")
                .replace("đ", "").replace("Đ", "").trim();
        if (!digits.matches("\\d{1,13}")) {
            throw new IllegalArgumentException("Đơn giá phải là số nguyên (đồng).");
        }
        return new BigDecimal(digits);
    }

    static Color statusDot(VoucherStatus status) {
        return switch (status) {
            case CHO_XU_LY -> new Color(0xE5B300);
            case DA_HOAN_THANH -> InventoryUiKit.GREEN;
            case DA_HUY -> InventoryUiKit.RED;
        };
    }

    static Color statusFill(VoucherStatus status) {
        return switch (status) {
            case CHO_XU_LY -> new Color(0xFFF4CC);
            case DA_HOAN_THANH -> new Color(0xDDF3E4);
            case DA_HUY -> new Color(0xFDE2E2);
        };
    }

    static Color statusText(VoucherStatus status) {
        return switch (status) {
            case CHO_XU_LY -> new Color(0x8A6A00);
            case DA_HOAN_THANH -> new Color(0x1E6B34);
            case DA_HUY -> new Color(0xB42323);
        };
    }
}
