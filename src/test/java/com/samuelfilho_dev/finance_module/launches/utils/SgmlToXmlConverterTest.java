package com.samuelfilho_dev.finance_module.launches.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SgmlToXmlConverterTest {

    @Test
    void convert_shouldCloseLeafTagsAndKeepContainers() {
        var sgml = """
                <STMTTRN>
                <TRNTYPE>DEBIT
                <DTPOSTED>20240103
                <TRNAMT>-150.00
                </STMTTRN>
                """;

        var xml = SgmlToXmlConverter.convert(sgml);

        assertTrue(xml.contains("<STMTTRN>"));
        assertTrue(xml.contains("</STMTTRN>"));
        assertTrue(xml.contains("<TRNTYPE>DEBIT</TRNTYPE>"));
        assertTrue(xml.contains("<DTPOSTED>20240103</DTPOSTED>"));
        assertTrue(xml.contains("<TRNAMT>-150.00</TRNAMT>"));
    }

    @Test
    void convert_shouldEscapeXmlSpecialCharacters() {
        var xml = SgmlToXmlConverter.convert("<MEMO>A & B > D \"quote\" 'apos'");
        assertEquals("<MEMO>A &amp; B &gt; D &quot;quote&quot; &apos;apos&apos;</MEMO>\n", xml);
    }

    @Test
    void convert_shouldIgnoreBlankLines() {
        var xml = SgmlToXmlConverter.convert("\n\n<NAME>Salary\n\n");
        assertEquals("<NAME>Salary</NAME>\n", xml);
    }

    @Test
    void convert_shouldKeepUnrecognizedLinesUnchanged() {
        var xml = SgmlToXmlConverter.convert("plain text without tags");
        assertEquals("plain text without tags\n", xml);
    }
}
