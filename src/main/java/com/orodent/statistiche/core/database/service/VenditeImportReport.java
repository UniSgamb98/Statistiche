package com.orodent.statistiche.core.database.service;

import com.orodent.statistiche.core.csv.CsvImportResult;

import java.util.List;

public record VenditeImportReport(
        Integer year,
        int totalRows,
        int deletedRows,
        int insertedRows,
        List<CsvImportResult.RowError> rowErrors,
        List<CsvImportResult.DocumentError> documentErrors
) {

    public VenditeImportReport {
        if (totalRows < 0 || deletedRows < 0 || insertedRows < 0) {
            throw new IllegalArgumentException("I contatori di importazione non possono essere negativi");
        }
        rowErrors = List.copyOf(rowErrors);
        documentErrors = List.copyOf(documentErrors);
    }

    public boolean imported() {
        return rowErrors.isEmpty()
                && documentErrors.isEmpty()
                && year != null
                && insertedRows == totalRows;
    }
}
