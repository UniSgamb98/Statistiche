package com.orodent.statistiche.features.sales.customers.repository;

import com.orodent.statistiche.core.database.repository.RepositoryException;
import com.orodent.statistiche.features.sales.customers.model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class CustomerAnalysisRepositoryImpl implements CustomerAnalysisRepository {
    private final Connection connection;
    public CustomerAnalysisRepositoryImpl(Connection connection) { this.connection = Objects.requireNonNull(connection); }

    @Override public CustomerDetail loadBaseDetail(String code, int year) {
        String sql = """
                SELECT v.codice_cliente, COALESCE(MAX(c.ragione_sociale),v.codice_cliente) nome,
                       MAX(c.codice_iso) paese, MAX(c.categoria) categoria, MAX(c.listino) listino,
                       MAX(c.agente) agente, MAX(c.tipo_cliente) tipo_cliente,
                       COALESCE(SUM(v.importo_netto),0) fatturato, COALESCE(SUM(v.quantita),0) quantita,
                       COUNT(DISTINCT v.documento_id) documenti, MIN(v.data_vendita) primo, MAX(v.data_vendita) ultimo
                FROM vendite_dettaglio v LEFT JOIN clienti c ON c.codice_cliente=v.codice_cliente
                WHERE v.codice_cliente=? AND YEAR(v.data_vendita)=? AND v.tipo_operazione='VENDITA'
                GROUP BY v.codice_cliente
                """;
        try (PreparedStatement ps=connection.prepareStatement(sql)) {
            ps.setString(1,code); ps.setInt(2,year);
            try (ResultSet rs=ps.executeQuery()) {
                if (!rs.next()) throw new RepositoryException("Nessuna vendita trovata per il cliente " + code);
                BigDecimal revenue=rs.getBigDecimal("fatturato"), quantity=rs.getBigDecimal("quantita");
                int documents=rs.getInt("documenti");
                return new CustomerDetail(code,rs.getString("nome"),rs.getString("paese"),rs.getString("categoria"),
                        rs.getString("listino"),rs.getString("agente"),rs.getString("tipo_cliente"),revenue,quantity,
                        documents,average(revenue,documents),average(quantity,documents),null,null,
                        rs.getDate("primo").toLocalDate(),rs.getDate("ultimo").toLocalDate(),0);
            }
        } catch(SQLException e){throw failure(e);}
    }

    @Override public List<LocalDate> loadPurchaseDates(String code,int year){
        String sql="SELECT DISTINCT data_vendita FROM vendite_dettaglio WHERE codice_cliente=? AND YEAR(data_vendita)=? AND tipo_operazione='VENDITA' ORDER BY data_vendita";
        try(PreparedStatement ps=connection.prepareStatement(sql)){ps.setString(1,code);ps.setInt(2,year);
            try(ResultSet rs=ps.executeQuery()){List<LocalDate> out=new ArrayList<>();while(rs.next())out.add(rs.getDate(1).toLocalDate());return List.copyOf(out);}
        }catch(SQLException e){throw failure(e);}
    }

    @Override public List<CustomerYearSummary> loadYearlyHistory(String code){
        String sql="""
                SELECT YEAR(data_vendita) anno, SUM(importo_netto) fatturato, SUM(quantita) quantita,
                       COUNT(DISTINCT documento_id) documenti FROM vendite_dettaglio
                WHERE codice_cliente=? AND tipo_operazione='VENDITA' GROUP BY YEAR(data_vendita) ORDER BY anno
                """;
        try(PreparedStatement ps=connection.prepareStatement(sql)){ps.setString(1,code);
            try(ResultSet rs=ps.executeQuery()){List<CustomerYearSummary> out=new ArrayList<>();while(rs.next())out.add(new CustomerYearSummary(rs.getInt("anno"),rs.getBigDecimal("fatturato"),rs.getBigDecimal("quantita"),rs.getInt("documenti")));return List.copyOf(out);}
        }catch(SQLException e){throw failure(e);}
    }

    @Override public List<CustomerMonthlyValue> loadMonthlyHistory(String code,int fromYear,int toYear){
        String sql="""
                SELECT YEAR(data_vendita) anno, MONTH(data_vendita) mese, SUM(importo_netto) fatturato,
                       SUM(quantita) quantita, COUNT(DISTINCT documento_id) documenti FROM vendite_dettaglio
                WHERE codice_cliente=? AND YEAR(data_vendita) BETWEEN ? AND ? AND tipo_operazione='VENDITA'
                GROUP BY YEAR(data_vendita),MONTH(data_vendita) ORDER BY anno,mese
                """;
        try(PreparedStatement ps=connection.prepareStatement(sql)){ps.setString(1,code);ps.setInt(2,fromYear);ps.setInt(3,toYear);
            try(ResultSet rs=ps.executeQuery()){List<CustomerMonthlyValue> out=new ArrayList<>();while(rs.next())out.add(new CustomerMonthlyValue(rs.getInt("anno"),rs.getInt("mese"),rs.getBigDecimal("fatturato"),rs.getBigDecimal("quantita"),rs.getInt("documenti")));return List.copyOf(out);}
        }catch(SQLException e){throw failure(e);}
    }

    @Override public List<CustomerProductItem> loadProducts(String code,int year){
        String sql="""
                SELECT codice_prodotto,MAX(descrizione_prodotto) descrizione,SUM(importo_netto) fatturato,SUM(quantita) quantita
                FROM vendite_dettaglio WHERE codice_cliente=? AND YEAR(data_vendita)=? AND tipo_operazione='VENDITA'
                GROUP BY codice_prodotto ORDER BY fatturato DESC FETCH FIRST 20 ROWS ONLY
                """;
        try(PreparedStatement ps=connection.prepareStatement(sql)){ps.setString(1,code);ps.setInt(2,year);
            try(ResultSet rs=ps.executeQuery()){List<CustomerProductItem> out=new ArrayList<>();while(rs.next())out.add(new CustomerProductItem(rs.getString("codice_prodotto"),rs.getString("descrizione"),rs.getBigDecimal("fatturato"),rs.getBigDecimal("quantita")));return List.copyOf(out);}
        }catch(SQLException e){throw failure(e);}
    }

    @Override public List<TopCustomerHistory> loadTopCustomerHistory(int selectedYear,int limit){
        String sql="""
                SELECT v.codice_cliente,COALESCE(MAX(c.ragione_sociale),v.codice_cliente) nome,
                       YEAR(v.data_vendita) anno,SUM(v.importo_netto) fatturato
                FROM vendite_dettaglio v LEFT JOIN clienti c ON c.codice_cliente=v.codice_cliente
                WHERE v.tipo_operazione='VENDITA' AND v.codice_cliente IN (
                    SELECT codice_cliente FROM vendite_dettaglio WHERE YEAR(data_vendita)=? AND tipo_operazione='VENDITA'
                    GROUP BY codice_cliente ORDER BY SUM(importo_netto) DESC FETCH FIRST %d ROWS ONLY)
                GROUP BY v.codice_cliente,YEAR(v.data_vendita) ORDER BY anno,v.codice_cliente
                """.formatted(limit);
        try(PreparedStatement ps=connection.prepareStatement(sql)){ps.setInt(1,selectedYear);
            try(ResultSet rs=ps.executeQuery()){List<TopCustomerHistory> out=new ArrayList<>();while(rs.next())out.add(new TopCustomerHistory(rs.getString("codice_cliente"),rs.getString("nome"),rs.getInt("anno"),rs.getBigDecimal("fatturato")));return List.copyOf(out);}
        }catch(SQLException e){throw failure(e);}
    }

    @Override public SalesDataCoverage loadDataCoverage(int year) {
        String sql = "SELECT MIN(data_vendita), MAX(data_vendita) FROM vendite_dettaglio "
                + "WHERE YEAR(data_vendita)=? AND tipo_operazione='VENDITA'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getDate(1) == null) {
                    throw new RepositoryException("Nessun dato disponibile per l'anno " + year);
                }
                return new SalesDataCoverage(rs.getDate(1).toLocalDate(), rs.getDate(2).toLocalDate());
            }
        } catch (SQLException e) { throw failure(e); }
    }

    @Override public List<CustomerDailyValue> loadDailyRevenueHistory(String code, int fromYear, int toYear) {
        String sql = """
                SELECT data_vendita, SUM(importo_netto) fatturato FROM vendite_dettaglio
                WHERE codice_cliente=? AND YEAR(data_vendita) BETWEEN ? AND ? AND tipo_operazione='VENDITA'
                GROUP BY data_vendita ORDER BY data_vendita
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, code); ps.setInt(2, fromYear); ps.setInt(3, toYear);
            try (ResultSet rs = ps.executeQuery()) {
                List<CustomerDailyValue> values = new ArrayList<>();
                while (rs.next()) values.add(new CustomerDailyValue(
                        rs.getDate("data_vendita").toLocalDate(), rs.getBigDecimal("fatturato")));
                return List.copyOf(values);
            }
        } catch (SQLException e) { throw failure(e); }
    }

    private BigDecimal average(BigDecimal value,int count){return count==0?BigDecimal.ZERO:value.divide(BigDecimal.valueOf(count),2,java.math.RoundingMode.HALF_UP);}
    private RepositoryException failure(SQLException e){return new RepositoryException("Errore durante l'analisi del cliente",e);}
}
