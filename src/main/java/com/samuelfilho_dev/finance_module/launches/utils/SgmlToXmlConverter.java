package com.samuelfilho_dev.finance_module.launches.utils;

import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

/**
 * Converte o corpo de um documento OFX 1.x (SGML) em XML bem formado.
 * <p>
 * No OFX 1.x, tags "folha" (que carregam um valor, ex.: {@code <TRNAMT>-50.00})
 * não possuem tag de fechamento — a linha seguinte já é a próxima tag. Já as
 * tags "container" (agregados, ex.: {@code <STMTTRN>}) sempre têm fechamento
 * explícito. Exemplo de entrada:
 *
 * <pre>{@code
 * <STMTTRN>
 * <TRNTYPE>DEBIT
 * <DTPOSTED>20240103
 * <TRNAMT>-150.00
 * </STMTTRN>
 * }</pre>
 * <p>
 * é convertido para:
 *
 * <pre>{@code
 * <STMTTRN>
 * <TRNTYPE>DEBIT</TRNTYPE>
 * <DTPOSTED>20240103</DTPOSTED>
 * <TRNAMT>-150.00</TRNAMT>
 * </STMTTRN>
 * }</pre>
 */

@NoArgsConstructor
public class SgmlToXmlConverter {
    private static final Pattern LEAF_TAG = Pattern.compile("^<([A-Za-z0-9./_]+)>([^<]+)$");
    private static final Pattern OPEN_TAG = Pattern.compile("^<([A-Za-z0-9./_]+)>$");
    private static final Pattern CLOSE_TAG = Pattern.compile("^</([A-Za-z0-9./_]+)>$");

    public static String convert(String body) {
        var xml = new StringBuilder(body.length() + 256);

        for (String rawLine : body.split("\\r?\\n")) {
            var line = rawLine.strip();

            if (line.isEmpty()) {
                continue;
            }

            var close = CLOSE_TAG.matcher(line);
            var open = OPEN_TAG.matcher(line);
            var leaf = LEAF_TAG.matcher(line);

            if (close.matches() || open.matches()) {
                xml.append(line).append("\n");
            } else if (leaf.matches()) {
                var tag = leaf.group(1);
                var value = escapeXml(leaf.group(2).strip());

                xml.append('<').append(tag).append('>')
                        .append(value)
                        .append("</").append(tag).append('>')
                        .append('\n');
            } else {
                xml.append(line).append("\n");
            }
        }

        return xml.toString();
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

}
