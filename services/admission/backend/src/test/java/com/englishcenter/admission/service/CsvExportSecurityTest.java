package com.englishcenter.admission.service;
import com.englishcenter.admission.util.CsvExportHelper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Dữ liệu tên/email không được trở thành công thức Excel khi xuất CSV. */
class CsvExportSecurityTest {
    @Test void untrustedFormulaCellsAreMadeLiteral() {
        for (String value : new String[]{"=1+1", "+SUM(1,2)", "-1+2", "@SUM(A1)", "\t=1+1", "  =1+1"}) {
            String escaped = CsvExportHelper.escapeCsvField(value);
            assertTrue(escaped.startsWith("'") || escaped.startsWith("\"'"));
        }
    }
    @Test void ordinaryCsvEscapingIsPreserved() {
        assertEquals("\"Nguyen, An\"", CsvExportHelper.escapeCsvField("Nguyen, An"));
        assertEquals("123", CsvExportHelper.escapeCsvField("123"));
        assertEquals("\"Đỗ, \"\"Ánh\"\"\nNguyễn\"", CsvExportHelper.escapeCsvField("Đỗ, \"Ánh\"\nNguyễn"));
        assertEquals("", CsvExportHelper.escapeCsvField(null));
        assertEquals("", CsvExportHelper.escapeCsvField(""));
    }

    @Test void leadingControlsAndUnicodeFormulaPrefixesAreMadeLiteral() {
        assertEquals("\"'\rđầu dòng\"", CsvExportHelper.escapeCsvField("\rđầu dòng"));
        assertEquals("\"'\nđầu dòng\"", CsvExportHelper.escapeCsvField("\nđầu dòng"));
        assertEquals("'\tđầu dòng", CsvExportHelper.escapeCsvField("\tđầu dòng"));
        for (String value : new String[]{"＝1+1", "＋SUM(A1)", "－1+2", "＠SUM(A1)", "\u00A0=1+1"}) {
            assertEquals("'" + value, CsvExportHelper.escapeCsvField(value));
        }
    }
}
