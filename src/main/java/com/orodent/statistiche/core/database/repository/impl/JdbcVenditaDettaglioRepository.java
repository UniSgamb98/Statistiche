package com.orodent.statistiche.core.database.repository.impl;

import com.orodent.statistiche.core.ConnectionProvider;
import com.orodent.statistiche.core.database.model.TipoOperazione;
import com.orodent.statistiche.core.database.model.VenditaDettaglio;
import com.orodent.statistiche.core.database.repository.RepositoryException;
import com.orodent.statistiche.core.database.repository.VenditaDettaglioRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class JdbcVenditaDettaglioRepository implements VenditaDettaglioRepository {
    private static final String COLUMNS = "vendita_id, sorgente, documento_id, numero_documento, "
            + "numero_riga, data_vendita, codice_cliente, codice_prodotto, descrizione_prodotto, "
            + "categoria_prodotto, quantita, prezzo_unitario, sconto_percentuale, sconto_importo, "
            + "importo_netto, tipo_operazione, canale, agente, importato_il";
    private static final String SELECT = "SELECT " + COLUMNS + " FROM vendite_dettaglio";
    private static final String INSERT = "INSERT INTO vendite_dettaglio (sorgente, documento_id, "
            + "numero_documento, numero_riga, data_vendita, codice_cliente, codice_prodotto, "
            + "descrizione_prodotto, categoria_prodotto, quantita, prezzo_unitario, "
            + "sconto_percentuale, sconto_importo, importo_netto, tipo_operazione, canale, agente, "
            + "importato_il) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE vendite_dettaglio SET sorgente=?, documento_id=?, "
            + "numero_documento=?, numero_riga=?, data_vendita=?, codice_cliente=?, codice_prodotto=?, "
            + "descrizione_prodotto=?, categoria_prodotto=?, quantita=?, prezzo_unitario=?, "
            + "sconto_percentuale=?, sconto_importo=?, importo_netto=?, tipo_operazione=?, canale=?, "
            + "agente=?, importato_il=? WHERE vendita_id=?";

    private final ConnectionProvider connectionProvider;

    public JdbcVenditaDettaglioRepository(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
    }

    @Override
    public VenditaDettaglio save(VenditaDettaglio vendita) {
        Objects.requireNonNull(vendita, "vendita");
        return vendita.venditaId() == null ? insert(vendita) : update(vendita);
    }

    private VenditaDettaglio insert(VenditaDettaglio vendita) {
        try {
            return connectionProvider.withConnection(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
                    bindValues(statement, vendita);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Il database non ha restituito l'identificativo generato");
                        }
                        return findById(connection, keys.getLong(1))
                                .orElseThrow(() -> new SQLException("Riga appena inserita non trovata"));
                    }
                }
            });
        } catch (RuntimeException exception) {
            throw repositoryError("inserimento", exception);
        }
    }

    private VenditaDettaglio update(VenditaDettaglio vendita) {
        try {
            return connectionProvider.withConnection(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
                    bindValues(statement, vendita);
                    statement.setLong(19, vendita.venditaId());
                    if (statement.executeUpdate() == 0) {
                        throw new SQLException("Vendita non trovata: " + vendita.venditaId());
                    }
                    return findById(connection, vendita.venditaId())
                            .orElseThrow(() -> new SQLException("Riga appena aggiornata non trovata"));
                }
            });
        } catch (RuntimeException exception) {
            throw repositoryError("aggiornamento", exception);
        }
    }

    @Override
    public Optional<VenditaDettaglio> findById(long venditaId) {
        return queryOne(SELECT + " WHERE vendita_id = ?", statement -> statement.setLong(1, venditaId));
    }

    private Optional<VenditaDettaglio> findById(Connection connection, long venditaId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT + " WHERE vendita_id = ?")) {
            statement.setLong(1, venditaId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<VenditaDettaglio> findByDocumento(String sorgente, String documentoId, int numeroRiga) {
        return queryOne(SELECT + " WHERE sorgente = ? AND documento_id = ? AND numero_riga = ?", statement -> {
            statement.setString(1, sorgente);
            statement.setString(2, documentoId);
            statement.setInt(3, numeroRiga);
        });
    }

    @Override
    public List<VenditaDettaglio> findAll() {
        return queryMany(SELECT + " ORDER BY vendita_id", statement -> { });
    }

    @Override
    public List<VenditaDettaglio> findByDateRange(LocalDate dal, LocalDate al) {
        return dateRangeQuery(SELECT + " WHERE data_vendita BETWEEN ? AND ? ORDER BY data_vendita, vendita_id",
                null, dal, al);
    }

    @Override
    public List<VenditaDettaglio> findByClienteAndDateRange(String codiceCliente, LocalDate dal, LocalDate al) {
        Objects.requireNonNull(codiceCliente, "codiceCliente");
        return dateRangeQuery(SELECT + " WHERE codice_cliente = ? AND data_vendita BETWEEN ? AND ? "
                + "ORDER BY data_vendita, vendita_id", codiceCliente, dal, al);
    }

    @Override
    public List<VenditaDettaglio> findByProdottoAndDateRange(String codiceProdotto, LocalDate dal, LocalDate al) {
        Objects.requireNonNull(codiceProdotto, "codiceProdotto");
        return dateRangeQuery(SELECT + " WHERE codice_prodotto = ? AND data_vendita BETWEEN ? AND ? "
                + "ORDER BY data_vendita, vendita_id", codiceProdotto, dal, al);
    }

    @Override
    public List<VenditaDettaglio> findByClienteAndProdottoAndDateRange(
            String codiceCliente, String codiceProdotto, LocalDate dal, LocalDate al) {
        Objects.requireNonNull(codiceCliente, "codiceCliente");
        Objects.requireNonNull(codiceProdotto, "codiceProdotto");
        return checkedDateRangeQuery(SELECT + " WHERE codice_cliente = ? AND codice_prodotto = ? "
                + "AND data_vendita BETWEEN ? AND ? ORDER BY data_vendita, vendita_id", dal, al,
                statement -> {
                    statement.setString(1, codiceCliente);
                    statement.setString(2, codiceProdotto);
                    statement.setDate(3, Date.valueOf(dal));
                    statement.setDate(4, Date.valueOf(al));
                });
    }

    @Override
    public List<VenditaDettaglio> findByCategoriaAndDateRange(String categoria, LocalDate dal, LocalDate al) {
        Objects.requireNonNull(categoria, "categoria");
        return dateRangeQuery(SELECT + " WHERE categoria_prodotto = ? AND data_vendita BETWEEN ? AND ? "
                + "ORDER BY data_vendita, vendita_id", categoria, dal, al);
    }

    private List<VenditaDettaglio> dateRangeQuery(String sql, String filter, LocalDate dal, LocalDate al) {
        return checkedDateRangeQuery(sql, dal, al, statement -> {
            int index = 1;
            if (filter != null) {
                statement.setString(index++, filter);
            }
            statement.setDate(index++, Date.valueOf(dal));
            statement.setDate(index, Date.valueOf(al));
        });
    }

    private List<VenditaDettaglio> checkedDateRangeQuery(
            String sql, LocalDate dal, LocalDate al, StatementBinder binder) {
        Objects.requireNonNull(dal, "dal");
        Objects.requireNonNull(al, "al");
        if (dal.isAfter(al)) {
            throw new IllegalArgumentException("La data iniziale non può essere successiva a quella finale");
        }
        return queryMany(sql, binder);
    }

    @Override
    public boolean deleteById(long venditaId) {
        try {
            return connectionProvider.withConnection(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM vendite_dettaglio WHERE vendita_id = ?")) {
                    statement.setLong(1, venditaId);
                    return statement.executeUpdate() > 0;
                }
            });
        } catch (RuntimeException exception) {
            throw repositoryError("eliminazione", exception);
        }
    }

    private Optional<VenditaDettaglio> queryOne(String sql, StatementBinder binder) {
        List<VenditaDettaglio> values = queryMany(sql, binder);
        return values.stream().findFirst();
    }

    private List<VenditaDettaglio> queryMany(String sql, StatementBinder binder) {
        try {
            return connectionProvider.withConnection(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    binder.bind(statement);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        List<VenditaDettaglio> result = new ArrayList<>();
                        while (resultSet.next()) {
                            result.add(map(resultSet));
                        }
                        return List.copyOf(result);
                    }
                }
            });
        } catch (RuntimeException exception) {
            throw repositoryError("lettura", exception);
        }
    }

    private static void bindValues(PreparedStatement statement, VenditaDettaglio vendita) throws SQLException {
        statement.setString(1, vendita.sorgente());
        statement.setString(2, vendita.documentoId());
        statement.setString(3, vendita.numeroDocumento());
        statement.setInt(4, vendita.numeroRiga());
        statement.setDate(5, Date.valueOf(vendita.dataVendita()));
        statement.setString(6, vendita.codiceCliente());
        statement.setString(7, vendita.codiceProdotto());
        statement.setString(8, vendita.descrizioneProdotto());
        statement.setString(9, vendita.categoriaProdotto());
        statement.setBigDecimal(10, vendita.quantita());
        statement.setBigDecimal(11, vendita.prezzoUnitario());
        statement.setBigDecimal(12, vendita.scontoPercentuale());
        statement.setBigDecimal(13, vendita.scontoImporto());
        statement.setBigDecimal(14, vendita.importoNetto());
        statement.setString(15, vendita.tipoOperazione().name());
        statement.setString(16, vendita.canale());
        statement.setString(17, vendita.agente());
        if (vendita.importatoIl() == null) {
            statement.setTimestamp(18, Timestamp.valueOf(LocalDateTime.now()));
        } else {
            statement.setTimestamp(18, Timestamp.valueOf(vendita.importatoIl()));
        }
    }

    private static VenditaDettaglio map(ResultSet resultSet) throws SQLException {
        Timestamp importatoIl = resultSet.getTimestamp("importato_il");
        return new VenditaDettaglio(
                resultSet.getLong("vendita_id"), resultSet.getString("sorgente"),
                resultSet.getString("documento_id"), resultSet.getString("numero_documento"),
                resultSet.getInt("numero_riga"), resultSet.getDate("data_vendita").toLocalDate(),
                resultSet.getString("codice_cliente"), resultSet.getString("codice_prodotto"),
                resultSet.getString("descrizione_prodotto"), resultSet.getString("categoria_prodotto"),
                resultSet.getBigDecimal("quantita"), resultSet.getBigDecimal("prezzo_unitario"),
                resultSet.getBigDecimal("sconto_percentuale"), resultSet.getBigDecimal("sconto_importo"),
                resultSet.getBigDecimal("importo_netto"),
                TipoOperazione.valueOf(resultSet.getString("tipo_operazione")),
                resultSet.getString("canale"), resultSet.getString("agente"),
                importatoIl == null ? null : importatoIl.toLocalDateTime());
    }

    private static RepositoryException repositoryError(String operation, RuntimeException exception) {
        if (exception instanceof RepositoryException repositoryException) {
            return repositoryException;
        }
        return new RepositoryException("Errore durante " + operation + " di vendite_dettaglio", exception);
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
