package com.orodent.statistiche.core.database.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** Rappresenta una riga della tabella {@code vendite_dettaglio}. */
public record VenditaDettaglio(
        Long venditaId,
        String sorgente,
        String documentoId,
        String numeroDocumento,
        int numeroRiga,
        LocalDate dataVendita,
        String codiceCliente,
        String codiceProdotto,
        String descrizioneProdotto,
        String categoriaProdotto,
        BigDecimal quantita,
        BigDecimal prezzoUnitario,
        BigDecimal scontoPercentuale,
        BigDecimal scontoImporto,
        BigDecimal importoNetto,
        TipoOperazione tipoOperazione,
        String canale,
        String agente,
        LocalDateTime importatoIl
) {
    public VenditaDettaglio {
        requireText(sorgente, "sorgente");
        requireText(documentoId, "documentoId");
        Objects.requireNonNull(dataVendita, "dataVendita non può essere null");
        requireText(codiceCliente, "codiceCliente");
        requireText(codiceProdotto, "codiceProdotto");
        Objects.requireNonNull(quantita, "quantita non può essere null");
        if (quantita.signum() == 0) {
            throw new IllegalArgumentException("quantita non può essere zero");
        }
        if (scontoPercentuale != null
                && (scontoPercentuale.signum() < 0
                || scontoPercentuale.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("scontoPercentuale deve essere compreso tra 0 e 100");
        }
        tipoOperazione = tipoOperazione == null ? TipoOperazione.VENDITA : tipoOperazione;
        scontoPercentuale = scontoPercentuale == null ? BigDecimal.ZERO : scontoPercentuale;
        scontoImporto = scontoImporto == null ? BigDecimal.ZERO : scontoImporto;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " non può essere vuoto");
        }
    }

    public VenditaDettaglio withVenditaId(long id) {
        return new VenditaDettaglio(id, sorgente, documentoId, numeroDocumento, numeroRiga,
                dataVendita, codiceCliente, codiceProdotto, descrizioneProdotto,
                categoriaProdotto, quantita, prezzoUnitario, scontoPercentuale,
                scontoImporto, importoNetto, tipoOperazione, canale, agente, importatoIl);
    }
}
