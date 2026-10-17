package com.quanlynhakhoa.features.inventory.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

class XlsxExportTest {

    @Test
    void columnNames() {
        assertEquals("A", XlsxExport.columnName(0));
        assertEquals("Z", XlsxExport.columnName(25));
        assertEquals("AA", XlsxExport.columnName(26));
        assertEquals("AM", XlsxExport.columnName(38));
    }

    @Test
    void escapesXmlAndDropsControlCharacters() {
        assertEquals("A &amp; B &lt;x&gt; &quot;q&quot;", XlsxExport.escape("A & B <x> \"q\"\u0001"));
    }

    @Test
    void writesValidPackageWithTypedCells() throws Exception {
        File file = Files.createTempFile("n08-export", ".xlsx").toFile();
        file.deleteOnExit();
        XlsxExport.write(file, "Phieu nhap kho", "DANH SÁCH", "N08 Dental", List.of(
                new XlsxExport.Column("STT", 6, XlsxExport.Kind.CENTER),
                new XlsxExport.Column("Ngày nhập", 13, XlsxExport.Kind.DATE),
                new XlsxExport.Column("Tổng tiền (đ)", 16, XlsxExport.Kind.MONEY),
                new XlsxExport.Column("Ghi chú", 20, XlsxExport.Kind.TEXT)),
                List.of(Arrays.asList(1, LocalDate.of(2026, 10, 17), new BigDecimal("12500000"), "Dược phẩm & co"),
                        Arrays.asList(2, null, BigDecimal.ZERO, null)));
        try (ZipFile zip = new ZipFile(file)) {
            for (String part : List.of("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml",
                    "xl/_rels/workbook.xml.rels", "xl/styles.xml", "xl/worksheets/sheet1.xml")) {
                assertTrue(zip.getEntry(part) != null, part);
            }
            String sheet = new String(zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).readAllBytes(),
                    StandardCharsets.UTF_8);
            assertTrue(sheet.contains("<c r=\"B5\" s=\"8\"><v>46312</v></c>"), "Ngày lưu dạng số ngày của Excel");
            assertTrue(sheet.contains("<c r=\"C5\" s=\"7\"><v>12500000</v></c>"), "Tiền lưu dạng số, không phải chữ");
            assertTrue(sheet.contains("Dược phẩm &amp; co"));
            assertTrue(sheet.contains("<autoFilter ref=\"A4:D6\"/>"));
        }
    }
}
