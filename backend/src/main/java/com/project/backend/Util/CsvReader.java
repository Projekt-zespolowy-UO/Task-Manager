package com.project.backend.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC 4180 CSV parser that mirrors the format produced by the task CSV
 * export: fields wrapped in double quotes, embedded quotes escaped as {@code ""},
 * and rows separated by {@code \r\n}. It also tolerates plain {@code \n} line
 * endings, unquoted fields, and a leading UTF-8 byte order mark.
 */
public final class CsvReader {

    private static final char BYTE_ORDER_MARK = '\uFEFF';

    private CsvReader() {
    }

    public static List<List<String>> parse(String content) {
        List<List<String>> rows = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return rows;
        }

        // Strip a leading UTF-8 BOM if present (the export writes one).
        if (content.charAt(0) == BYTE_ORDER_MARK) {
            content = content.substring(1);
        }

        List<String> currentRow = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int length = content.length();

        for (int i = 0; i < length; i++) {
            char c = content.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < length && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
                continue;
            }

            switch (c) {
                case '"' -> inQuotes = true;
                case ',' -> {
                    currentRow.add(field.toString());
                    field.setLength(0);
                }
                case '\r' -> {
                    // Ignored: row breaks are driven by the following '\n'.
                }
                case '\n' -> {
                    currentRow.add(field.toString());
                    field.setLength(0);
                    rows.add(currentRow);
                    currentRow = new ArrayList<>();
                }
                default -> field.append(c);
            }
        }

        // Flush the trailing field/row when the content does not end with a newline.
        if (field.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(field.toString());
            rows.add(currentRow);
        }

        return rows;
    }
}
