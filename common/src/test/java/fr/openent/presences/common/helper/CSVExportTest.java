package fr.openent.presences.common.helper;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class CSVExportTest {
    CSVExport ce = new CSVExport() {
        @Override
        public void generate() {
        }
    };

    @Test
    @DisplayName("CSVExport should init truthy")
    public void cSVExport_should_init_truthy() {
        assertEquals("Filename should init with empty value", ce.filename, "");
        assertEquals("Value should init with UTH-8 BOM", ce.value.toString(), "\uFEFF");
        assertEquals("Default separator should init with semicolon", ce.SEPARATOR, ";");
        assertEquals("Header should init with empty string", ce.header, "");
        assertEquals("End of line should init with \\n", ce.EOL, "\n");
    }

    @Test
    @DisplayName("Set string CSV header should returns it with end of line")
    public void set_string_CSV_header_should_return_it_with_end_of_line() {
        ce.setHeader("");
        String header = "this is a header";
        ce.setHeader(header);
        assertEquals(ce.header, header + "\n");
    }

    @Test
    @DisplayName("Set List CSV header should returns it with end of line")
    public void set_list_CSV_header_should_returns_it_with_end_of_line() {
        List<String> csvHeaders = Arrays.asList("this", "is", "a", "header");
        ce.setHeader(csvHeaders);
        assertEquals(ce.header, "this;is;a;header;\n");
    }
}