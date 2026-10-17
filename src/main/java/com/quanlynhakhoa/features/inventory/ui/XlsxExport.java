package com.quanlynhakhoa.features.inventory.ui;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Ghi file Excel .xlsx (Office Open XML) có định dạng, không cần thư viện ngoài.
 *
 * <p>Bố cục: dòng 1 tiêu đề, dòng 2 mô tả, dòng 4 tiêu đề cột (cố định khi cuộn, có bộ lọc), dữ liệu từ dòng 5.
 * Căn lề theo kiểu cột: chữ căn trái, số/tiền căn phải với dấu phân cách hàng nghìn, ngày và mã căn giữa.
 * Ngày lưu đúng kiểu ngày của Excel nên sắp xếp/lọc được.</p>
 */
final class XlsxExport {

    /** Kiểu cột quyết định căn lề và định dạng số. */
    enum Kind {
        TEXT(4), CENTER(5), INTEGER(6), MONEY(7), DATE(8);

        final int style;

        Kind(int style) {
            this.style = style;
        }
    }

    /** @param width độ rộng theo đơn vị ký tự của Excel */
    record Column(String title, double width, Kind kind) {
    }

    private static final int HEADER_ROW = 4;
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);

    private XlsxExport() {
    }

    /**
     * @param rows mỗi phần tử ứng với một cột: String, Number (Integer/Long/BigDecimal), LocalDate hoặc null
     */
    static void write(File target, String sheetName, String title, String subtitle,
                      List<Column> columns, List<List<Object>> rows) throws IOException {
        Objects.requireNonNull(target);
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Cần ít nhất một cột");
        }
        String safeSheet = sheetName.replaceAll("[\\\\/?*\\[\\]:]", " ").strip();
        safeSheet = safeSheet.isEmpty() ? "Sheet1" : safeSheet.substring(0, Math.min(31, safeSheet.length()));
        try (OutputStream file = Files.newOutputStream(target.toPath());
             ZipOutputStream zip = new ZipOutputStream(file, StandardCharsets.UTF_8)) {
            entry(zip, "[Content_Types].xml", CONTENT_TYPES);
            entry(zip, "_rels/.rels", ROOT_RELS);
            entry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS);
            entry(zip, "xl/workbook.xml", workbook(safeSheet, columns.size(), rows.size()));
            entry(zip, "xl/styles.xml", STYLES);
            entry(zip, "xl/worksheets/sheet1.xml", sheet(title, subtitle, columns, rows));
        }
    }

    // ------------------------------------------------------------------ sheet

    private static String sheet(String title, String subtitle, List<Column> columns, List<List<Object>> rows) {
        String lastCol = columnName(columns.size() - 1);
        int lastRow = HEADER_ROW + Math.max(rows.size(), 1);
        StringBuilder x = new StringBuilder(4096 + rows.size() * columns.size() * 48);
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">")
                .append("<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>")
                .append("<dimension ref=\"A1:").append(lastCol).append(lastRow).append("\"/>")
                .append("<sheetViews><sheetView workbookViewId=\"0\" showGridLines=\"0\">")
                .append("<pane ySplit=\"").append(HEADER_ROW).append("\" topLeftCell=\"A").append(HEADER_ROW + 1)
                .append("\" activePane=\"bottomLeft\" state=\"frozen\"/>")
                .append("<selection pane=\"bottomLeft\" activeCell=\"A").append(HEADER_ROW + 1)
                .append("\" sqref=\"A").append(HEADER_ROW + 1).append("\"/>")
                .append("</sheetView></sheetViews>")
                .append("<sheetFormatPr defaultRowHeight=\"15\"/>")
                .append("<cols>");
        for (int i = 0; i < columns.size(); i++) {
            x.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                    .append("\" width=\"").append(columns.get(i).width()).append("\" customWidth=\"1\"/>");
        }
        x.append("</cols><sheetData>");

        x.append("<row r=\"1\" ht=\"24\" customHeight=\"1\">");
        textCell(x, "A1", title, 1);
        x.append("</row>");
        x.append("<row r=\"2\">");
        textCell(x, "A2", subtitle, 2);
        x.append("</row>");

        x.append("<row r=\"").append(HEADER_ROW).append("\" ht=\"22\" customHeight=\"1\">");
        for (int c = 0; c < columns.size(); c++) {
            textCell(x, columnName(c) + HEADER_ROW, columns.get(c).title(), 3);
        }
        x.append("</row>");

        int r = HEADER_ROW;
        for (List<Object> row : rows) {
            r++;
            x.append("<row r=\"").append(r).append("\" ht=\"18\" customHeight=\"1\">");
            for (int c = 0; c < columns.size(); c++) {
                Object value = c < row.size() ? row.get(c) : null;
                cell(x, columnName(c) + r, value, columns.get(c).kind());
            }
            x.append("</row>");
        }
        x.append("</sheetData>");
        x.append("<autoFilter ref=\"A").append(HEADER_ROW).append(':').append(lastCol).append(lastRow).append("\"/>");
        x.append("<mergeCells count=\"2\"><mergeCell ref=\"A1:").append(lastCol).append("1\"/>")
                .append("<mergeCell ref=\"A2:").append(lastCol).append("2\"/></mergeCells>");
        x.append("<pageMargins left=\"0.4\" right=\"0.4\" top=\"0.5\" bottom=\"0.5\" header=\"0.3\" footer=\"0.3\"/>");
        x.append("<pageSetup paperSize=\"9\" orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/>");
        x.append("</worksheet>");
        return x.toString();
    }

    private static void cell(StringBuilder x, String ref, Object value, Kind kind) {
        if (value == null || (value instanceof String s && s.isEmpty())) {
            x.append("<c r=\"").append(ref).append("\" s=\"").append(kind.style).append("\"/>");
        } else if (value instanceof LocalDate date) {
            x.append("<c r=\"").append(ref).append("\" s=\"").append(Kind.DATE.style).append("\"><v>")
                    .append(ChronoUnit.DAYS.between(EXCEL_EPOCH, date)).append("</v></c>");
        } else if (value instanceof BigDecimal number) {
            x.append("<c r=\"").append(ref).append("\" s=\"").append(kind.style).append("\"><v>")
                    .append(number.toPlainString()).append("</v></c>");
        } else if (value instanceof Number number) {
            x.append("<c r=\"").append(ref).append("\" s=\"").append(kind.style).append("\"><v>")
                    .append(number).append("</v></c>");
        } else {
            textCell(x, ref, value.toString(), kind.style);
        }
    }

    private static void textCell(StringBuilder x, String ref, String text, int style) {
        x.append("<c r=\"").append(ref).append("\" s=\"").append(style).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                .append(escape(text)).append("</t></is></c>");
    }

    /** 0 → A, 25 → Z, 26 → AA. */
    static String columnName(int index) {
        StringBuilder name = new StringBuilder();
        int n = index + 1;
        while (n > 0) {
            int rem = (n - 1) % 26;
            name.insert(0, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return name.toString();
    }

    static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                default -> {
                    // Bỏ ký tự điều khiển không hợp lệ trong XML (trừ tab/xuống dòng).
                    if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') {
                        out.append(ch);
                    }
                }
            }
        }
        return out.toString();
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String workbook(String sheetName, int columnCount, int rowCount) {
        String range = "'" + sheetName.replace("'", "''") + "'!$A$" + HEADER_ROW + ":$"
                + columnName(columnCount - 1) + "$" + (HEADER_ROW + Math.max(rowCount, 1));
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets><sheet name=\"" + escape(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                + "<definedNames><definedName name=\"_xlnm._FilterDatabase\" localSheetId=\"0\" hidden=\"1\">"
                + escape(range) + "</definedName></definedNames>"
                + "</workbook>";
    }

    private static final String CONTENT_TYPES = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" "
            + "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" "
            + "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" "
            + "ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
            + "</Types>";

    private static final String ROOT_RELS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" "
            + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" "
            + "Target=\"xl/workbook.xml\"/></Relationships>";

    private static final String WORKBOOK_RELS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" "
            + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" "
            + "Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" "
            + "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" "
            + "Target=\"styles.xml\"/></Relationships>";

    /**
     * cellXfs: 0 mặc định · 1 tiêu đề · 2 mô tả · 3 tiêu đề cột · 4 chữ (trái) · 5 giữa ·
     * 6 số nguyên #,##0 (phải) · 7 tiền #,##0 (phải) · 8 ngày dd/mm/yyyy (giữa).
     */
    private static final String STYLES = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"dd/mm/yyyy\"/></numFmts>"
            + "<fonts count=\"4\">"
            + "<font><sz val=\"11\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><b/><sz val=\"14\"/><color rgb=\"FF1B2233\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><i/><sz val=\"10\"/><color rgb=\"FF5E6678\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "<font><b/><sz val=\"11\"/><color rgb=\"FF1B2233\"/><name val=\"Calibri\"/><family val=\"2\"/></font>"
            + "</fonts>"
            + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE9EDF5\"/><bgColor indexed=\"64\"/></patternFill></fill>"
            + "</fills>"
            + "<borders count=\"2\"><border><left/><right/><top/><bottom/><diagonal/></border>"
            + "<border><left style=\"thin\"><color rgb=\"FFC9CFDB\"/></left><right style=\"thin\"><color rgb=\"FFC9CFDB\"/></right>"
            + "<top style=\"thin\"><color rgb=\"FFC9CFDB\"/></top><bottom style=\"thin\"><color rgb=\"FFC9CFDB\"/></bottom>"
            + "<diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"9\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyAlignment=\"1\">"
            + "<alignment horizontal=\"left\" vertical=\"center\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
            + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" "
            + "applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\" applyAlignment=\"1\">"
            + "<alignment horizontal=\"left\" vertical=\"center\" indent=\"1\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\" applyAlignment=\"1\">"
            + "<alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            + "<xf numFmtId=\"3\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" "
            + "applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"right\" vertical=\"center\" indent=\"1\"/></xf>"
            + "<xf numFmtId=\"3\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" "
            + "applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"right\" vertical=\"center\" indent=\"1\"/></xf>"
            + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" "
            + "applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";
}
