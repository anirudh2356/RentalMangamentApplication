package com.realestate.reports;

import java.util.List;

final class ReportCsv {

    private ReportCsv() {
    }

    static String create(List<String> headers, List<List<String>> rows) {
        StringBuilder csv = new StringBuilder();
        appendRow(csv, headers);
        for (List<String> row : rows) {
            appendRow(csv, row);
        }
        return csv.toString();
    }

    private static void appendRow(StringBuilder csv, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                csv.append(',');
            }
            csv.append(escape(values.get(index)));
        }
        csv.append("\r\n");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        String safe = value;
        if (safe.matches("^[\\t\\r ]*[=+@].*")) {
            safe = "'" + safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
