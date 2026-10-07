package com.orodent.statistiche.core.database.csv;

import com.orodent.statistiche.core.csv.CsvImportResult;
import com.orodent.statistiche.core.csv.CsvImporter;
import com.orodent.statistiche.core.csv.CsvMappingException;
import com.orodent.statistiche.core.csv.CsvReadOptions;
import com.orodent.statistiche.core.csv.CsvReader;
import com.orodent.statistiche.core.database.model.TipoOperazione;
import com.orodent.statistiche.core.database.model.VenditaDettaglio;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VenditaDettaglioCsvMapperTest {

    private static final String HEADER = "Data reg.;Nr. doc.;MA_InventoryEntries_StoragePhase1;"
            + "MA_InventoryReasons_Reason;MA_InventoryEntries_CustSupp;Articolo;Descrizione;"
            + "Unità misura;Quantità;Divisa;Qta UM base;Valore unitario;Sconto;Imp. sconto;"
            + "Importo riga;Importo Riga in Divisa Base";

    private final VenditaDettaglioCsvMapper mapper = new VenditaDettaglioCsvMapper();
    private final VenditaDettaglioValidator validator = new VenditaDettaglioValidator();

    @Test
    void mapsSalesRowAndCalculatesCompoundDiscount() {
        String row = "06/10/2026;001005;MAGMOD;VEND;20ZR1036;ZR9816MSA2;"
                + "ORODENT EOS 98X16 COLOR: A2;N.;1;EUR;1;211;55+10;125,54;85,46;85,46";

        VenditaDettaglio vendita = readAndMap(row);

        assertEquals(LocalDate.of(2026, 10, 6), vendita.dataVendita());
        assertEquals("001005", vendita.documentoId());
        assertEquals("001005", vendita.numeroDocumento());
        assertEquals(1, vendita.numeroRiga());
        assertEquals("MAGMOD", vendita.sorgente());
        assertEquals("20ZR1036", vendita.codiceCliente());
        assertEquals("ZR9816MSA2", vendita.codiceProdotto());
        assertEquals(new BigDecimal("1"), vendita.quantita());
        assertEquals(new BigDecimal("211"), vendita.prezzoUnitario());
        assertEquals(new BigDecimal("59.5"), vendita.scontoPercentuale());
        assertEquals(new BigDecimal("125.54"), vendita.scontoImporto());
        assertEquals(new BigDecimal("85.46"), vendita.importoNetto());
        assertEquals(TipoOperazione.VENDITA, vendita.tipoOperazione());
        assertTrue(validator.validate(vendita).isEmpty());
    }

    @Test
    void mapsVenOReasonAsSale() {
        String row = "01/10/2026;000976;MAGMOD;VEN-O;20ZR774;ZR9816MSTA2;"
                + "ORODENT THOR 98x16 COLOR: A2;N.;2;EUR;2;206;60+10;263,68;148,32;148,32";

        VenditaDettaglio vendita = readAndMap(row);

        assertEquals(TipoOperazione.VENDITA, vendita.tipoOperazione());
        assertEquals(new BigDecimal("64"), vendita.scontoPercentuale());
    }

    @Test
    void reportsInvalidDateAsMappingError() {
        String row = "31/02/2026;001005;MAGMOD;VEND;20ZR1036;ZR9816MSA2;"
                + "Prodotto;N.;1;EUR;1;211;55+10;125,54;85,46;85,46";

        CsvMappingException exception = assertThrows(CsvMappingException.class, () -> readAndMap(row));

        assertEquals(VenditaDettaglioCsvMapper.DATA_REGISTRAZIONE, exception.column());
        assertEquals("31/02/2026", exception.rawValue());
    }

    @Test
    void importerReportsZeroQuantityAsRowValidationError() {
        String row = "06/10/2026;001005;MAGMOD;VEND;20ZR1036;ZR9816MSA2;"
                + "Prodotto;N.;0;EUR;0;211;55+10;0;0;0";
        CsvImporter<VenditaDettaglio> importer = new CsvImporter<>(new CsvReader(), mapper, validator);

        CsvImportResult<VenditaDettaglio> result = importer.importReader(
                new StringReader(HEADER + "\n" + row),
                CsvReadOptions.semicolonSeparated()
        );

        assertEquals(1, result.totalRows());
        assertTrue(result.validRows().isEmpty());
        assertEquals(1, result.rowErrors().size());
        assertEquals(VenditaDettaglioCsvMapper.QUANTITA, result.rowErrors().getFirst().field());
        assertEquals("0", result.rowErrors().getFirst().rawValue());
        assertFalse(result.valid());
    }

    private VenditaDettaglio readAndMap(String row) {
        return mapper.map(new CsvReader()
                .read(new StringReader(HEADER + "\n" + row), CsvReadOptions.semicolonSeparated())
                .rows()
                .getFirst());
    }
}
