package com.englishcenter.admission.util;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tiện ích hỗ trợ xuất dữ liệu ra định dạng CSV chuẩn RFC 4180 (TASK-ADM-50).
 * Tự động chèn Byte Order Mark (BOM \uFEFF) ở đầu file để Microsoft Excel hiển thị đúng tiếng Việt có dấu.
 */
public final class CsvExportHelper {

    private static final String UTF8_BOM = "\uFEFF";
    private static final String CSV_DELIMITER = ",";
    private static final String CSV_NEWLINE = "\r\n";
    private static final Pattern FORMULA_PREFIX = Pattern.compile("(?U)^\\s*[=+@＝＋－＠-]");

    private CsvExportHelper() {
        // Ngăn chặn khởi tạo lớp tiện ích
    }

    /**
     * Chuyển đổi danh sách tiêu đề cột và các dòng dữ liệu thành mảng byte CSV chuẩn UTF-8 kèm BOM.
     *
     * @param headers Danh sách tiêu đề cột
     * @param rows    Danh sách các dòng dữ liệu (mỗi dòng là danh sách giá trị chuỗi)
     * @return Mảng byte của file CSV
     */
    public static byte[] generateCsvBytes(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        // 1. Thêm UTF-8 BOM để Excel tự nhận diện UTF-8
        sb.append(UTF8_BOM);

        // 2. Thêm dòng tiêu đề (Header row)
        if (headers != null && !headers.isEmpty()) {
            sb.append(formatRow(headers)).append(CSV_NEWLINE);
        }

        // 3. Thêm các dòng dữ liệu (Data rows)
        if (rows != null) {
            for (List<String> row : rows) {
                sb.append(formatRow(row)).append(CSV_NEWLINE);
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Định dạng một dòng CSV, bọc nháy kép các giá trị chứa dấu phẩy, dấu nháy hoặc xuống dòng.
     *
     * @param fields Danh sách trường trong dòng
     * @return Chuỗi một dòng CSV
     */
    private static String formatRow(List<String> fields) {
        StringBuilder rowBuilder = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                rowBuilder.append(CSV_DELIMITER);
            }
            rowBuilder.append(escapeCsvField(fields.get(i)));
        }
        return rowBuilder.toString();
    }

    /**
     * Xử lý ký tự đặc biệt theo chuẩn RFC 4180.
     *
     * @param field Chuỗi giá trị cần xử lý
     * @return Chuỗi an toàn trong CSV
     */
    public static String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        // Excel có thể thực thi công thức từ tên/email do người dùng nhập, kể cả có whitespace đầu.
        if (FORMULA_PREFIX.matcher(field).find() || field.startsWith("\t") || field.startsWith("\r") || field.startsWith("\n")) {
            field = "'" + field;
        }
        boolean containsSpecialChar = field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r");
        if (containsSpecialChar) {
            // Thay thế một dấu nháy kép thành hai dấu nháy kép
            String escaped = field.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }
        return field;
    }
}
