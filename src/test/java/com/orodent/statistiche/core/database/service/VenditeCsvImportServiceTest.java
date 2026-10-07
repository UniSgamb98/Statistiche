package com.orodent.statistiche.core.database.service;

import com.orodent.statistiche.core.ConnectionProvider;
import com.orodent.statistiche.core.csv.CsvImporter;
import com.orodent.statistiche.core.csv.CsvReader;
import com.orodent.statistiche.core.database.csv.VenditaDettaglioCsvMapper;
import com.orodent.statistiche.core.database.csv.VenditaDettaglioValidator;
import com.orodent.statistiche.core.database.model.VenditaDettaglio;
import com.orodent.statistiche.core.database.repository.VenditaDettaglioRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VenditeCsvImportServiceTest {

    private static final String HEADER = "Data reg.;Nr. doc.;MA_InventoryEntries_StoragePhase1;"
            + "MA_InventoryReasons_Reason;MA_InventoryEntries_CustSupp;Articolo;Descrizione;"
            + "Unità misura;Quantità;Divisa;Qta UM base;Valore unitario;Sconto;Imp. sconto;"
            + "Importo riga;Importo Riga in Divisa Base";

    @Test
    void replacesOneYearInOneTransactionOnBackgroundThread() throws Exception {
        TransactionProbe transaction = new TransactionProbe();
        RecordingRepository repository = new RecordingRepository();
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "test-import"));
        Path file = csvFile(row("06/10/2026", "001005") + "\n" + row("01/10/2026", "000976"));

        VenditeCsvImportService service = service(transaction, repository, executor);
        try {
            VenditeImportReport report = service.importFile(file).join();

            assertTrue(report.imported());
            assertEquals(2026, report.year());
            assertEquals(2, report.totalRows());
            assertEquals(7, report.deletedRows());
            assertEquals(2, report.insertedRows());
            assertEquals(2026, repository.deletedYear);
            assertEquals(2, repository.inserted.size());
            assertEquals("test-import", repository.threadName);
            assertTrue(transaction.committed);
            assertFalse(transaction.rolledBack);
        } finally {
            executor.shutdownNow();
            Files.deleteIfExists(file);
        }
    }

    @Test
    void rejectsFilesContainingMultipleYearsWithoutOpeningTransaction() throws Exception {
        TransactionProbe transaction = new TransactionProbe();
        RecordingRepository repository = new RecordingRepository();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Path file = csvFile(row("06/10/2026", "001005") + "\n" + row("01/10/2025", "000976"));

        VenditeCsvImportService service = service(transaction, repository, executor);
        try {
            VenditeImportReport report = service.importFile(file).join();

            assertFalse(report.imported());
            assertEquals(1, report.documentErrors().size());
            assertFalse(transaction.opened);
            assertTrue(repository.inserted.isEmpty());
        } finally {
            executor.shutdownNow();
            Files.deleteIfExists(file);
        }
    }

    @Test
    void rejectsInvalidRowsWithoutDeletingExistingData() throws Exception {
        TransactionProbe transaction = new TransactionProbe();
        RecordingRepository repository = new RecordingRepository();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        String invalidRow = row("06/10/2026", "001005").replace(";1;EUR;", ";0;EUR;");
        Path file = csvFile(invalidRow);

        VenditeCsvImportService service = service(transaction, repository, executor);
        try {
            VenditeImportReport report = service.importFile(file).join();

            assertFalse(report.imported());
            assertEquals(1, report.rowErrors().size());
            assertFalse(transaction.opened);
            assertEquals(null, repository.deletedYear);
        } finally {
            executor.shutdownNow();
            Files.deleteIfExists(file);
        }
    }

    @Test
    void rollsBackDeletionWhenBatchInsertFails() throws Exception {
        TransactionProbe transaction = new TransactionProbe();
        RecordingRepository repository = new RecordingRepository();
        repository.failInsert = true;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Path file = csvFile(row("06/10/2026", "001005"));

        VenditeCsvImportService service = service(transaction, repository, executor);
        try {
            assertThrows(CompletionException.class, () -> service.importFile(file).join());
            assertFalse(transaction.committed);
            assertTrue(transaction.rolledBack);
        } finally {
            executor.shutdownNow();
            Files.deleteIfExists(file);
        }
    }

    private VenditeCsvImportService service(
            TransactionProbe transaction,
            RecordingRepository repository,
            ExecutorService executor
    ) {
        CsvImporter<VenditaDettaglio> importer = new CsvImporter<>(
                new CsvReader(),
                new VenditaDettaglioCsvMapper(),
                new VenditaDettaglioValidator()
        );
        return new VenditeCsvImportService(transaction, importer, executor, connection -> repository);
    }

    private Path csvFile(String rows) throws Exception {
        Path file = Files.createTempFile("vendite-", ".csv");
        Files.writeString(file, HEADER + "\n" + rows);
        return file;
    }

    private String row(String date, String document) {
        return date + ";" + document + ";MAGMOD;VEND;20ZR1036;ZR9816MSA2;"
                + "Prodotto;N.;1;EUR;1;211;55+10;125,54;85,46;85,46";
    }

    private static final class TransactionProbe implements ConnectionProvider {
        private boolean opened;
        private boolean committed;
        private boolean rolledBack;

        @Override
        public Connection openConnection() {
            opened = true;
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "commit" -> committed = true;
                            case "rollback" -> rolledBack = true;
                            default -> { }
                        }
                        Class<?> returnType = method.getReturnType();
                        if (!returnType.isPrimitive()) {
                            return null;
                        }
                        if (returnType == boolean.class) {
                            return false;
                        }
                        return 0;
                    }
            );
        }
    }

    private static final class RecordingRepository implements VenditaDettaglioRepository {
        private Integer deletedYear;
        private final List<VenditaDettaglio> inserted = new ArrayList<>();
        private String threadName;
        private boolean failInsert;

        @Override
        public int deleteByYear(int year) {
            deletedYear = year;
            threadName = Thread.currentThread().getName();
            return 7;
        }

        @Override
        public int insertAll(List<VenditaDettaglio> vendite) {
            if (failInsert) {
                throw new IllegalStateException("Errore simulato");
            }
            inserted.addAll(vendite);
            return vendite.size();
        }

        @Override public VenditaDettaglio insert(VenditaDettaglio vendita) { throw unsupported(); }
        @Override public void update(VenditaDettaglio vendita) { throw unsupported(); }
        @Override public Optional<VenditaDettaglio> findById(long venditaId) { throw unsupported(); }
        @Override public Optional<VenditaDettaglio> findByDocumento(String sorgente, String documentoId, int numeroRiga) { throw unsupported(); }
        @Override public List<VenditaDettaglio> findAll() { throw unsupported(); }
        @Override public List<VenditaDettaglio> findByDateRange(LocalDate dal, LocalDate al) { throw unsupported(); }
        @Override public List<VenditaDettaglio> findByClienteAndDateRange(String cliente, LocalDate dal, LocalDate al) { throw unsupported(); }
        @Override public List<VenditaDettaglio> findByProdottoAndDateRange(String prodotto, LocalDate dal, LocalDate al) { throw unsupported(); }
        @Override public List<VenditaDettaglio> findByClienteAndProdottoAndDateRange(String cliente, String prodotto, LocalDate dal, LocalDate al) { throw unsupported(); }
        @Override public List<VenditaDettaglio> findByCategoriaAndDateRange(String categoria, LocalDate dal, LocalDate al) { throw unsupported(); }
        @Override public void deleteById(long venditaId) { throw unsupported(); }

        private UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException();
        }
    }
}
