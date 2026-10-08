package com.orodent.statistiche.core.database.service;

import com.orodent.statistiche.core.csv.CsvImportResult;

import java.util.List;

public record ClientiImportReport(
        int totalRows,
        int deletedRows,
        int insertedRows,
        List<CsvImportResult.RowError> rowErrors,
        List<CsvImportResult.DocumentError> documentErrors
) {
    public ClientiImportReport {
        rowErrors = List.copyOf(rowErrors);
        documentErrors = List.copyOf(documentErrors);
    }

    public boolean imported() {
        return rowErrors.isEmpty() && documentErrors.isEmpty() && insertedRows > 0;
    }
}
