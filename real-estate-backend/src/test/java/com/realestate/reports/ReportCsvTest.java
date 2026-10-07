package com.realestate.reports;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportCsvTest {

    @Test
    void writesHeaderAndQuotesCommaQuoteAndNewlineValues() {
        String csv = ReportCsv.create(
                List.of("Name", "Notes"),
                List.of(List.of("Lead, One", "Said \"hello\"\nnext line"))
        );

        assertEquals("\"Name\",\"Notes\"\r\n\"Lead, One\",\"Said \"\"hello\"\"\nnext line\"\r\n", csv);
    }

    @Test
    void formulaLikeTextIsEscapedToReduceSpreadsheetFormulaInjection() {
        String csv = ReportCsv.create(List.of("Value"), List.of(List.of("=1+1")));

        assertEquals("\"Value\"\r\n\"'=1+1\"\r\n", csv);
    }

    @Test
    void emptyDataStillProducesCsvHeader() {
        assertEquals("\"ID\",\"Name\"\r\n", ReportCsv.create(List.of("ID", "Name"), List.of()));
    }
}
