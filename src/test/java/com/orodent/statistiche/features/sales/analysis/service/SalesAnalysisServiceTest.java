package com.orodent.statistiche.features.sales.analysis.service;

import com.orodent.statistiche.core.ConnectionProvider;
import com.orodent.statistiche.features.sales.analysis.model.*;
import com.orodent.statistiche.features.sales.analysis.repository.SalesAnalysisRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SalesAnalysisServiceTest {
    @Test
    void selectsLatestYearAndLoadsRequestedAnalysis() {
        FakeRepository repository = new FakeRepository();
        SalesAnalysisService service = new SalesAnalysisService(connectionProvider(), Runnable::run, c -> repository);

        AnalysisPageData<CustomerAnalysisItem> data = service.loadCustomers(null).join();

        assertEquals(2026, data.selectedYear());
        assertEquals("Cliente Uno", data.items().getFirst().name());
        assertEquals(2026, repository.loadedYear);
    }

    @Test
    void passesSearchTextToArchiveRepository() {
        FakeRepository repository = new FakeRepository();
        SalesAnalysisService service = new SalesAnalysisService(connectionProvider(), Runnable::run, c -> repository);

        service.loadArchive(2025, "cliente uno").join();

        assertEquals(2025, repository.loadedYear);
        assertEquals("cliente uno", repository.search);
    }

    private ConnectionProvider connectionProvider() {
        return () -> (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class}, (proxy, method, args) -> null);
    }

    private static final class FakeRepository implements SalesAnalysisRepository {
        int loadedYear;
        String search;
        public List<Integer> findAvailableYears() { return List.of(2026, 2025); }
        public List<CustomerAnalysisItem> loadCustomers(int year) {
            loadedYear = year;
            return List.of(new CustomerAnalysisItem("C1", "Cliente Uno", BigDecimal.TEN,
                    BigDecimal.ONE, 1, java.time.LocalDate.of(year, 1, 1)));
        }
        public List<ProductAnalysisItem> loadProducts(int year) { loadedYear = year; return List.of(); }
        public AnalysisSummary loadDiscountSummary(int year) { return AnalysisSummary.empty(); }
        public List<DiscountAnalysisItem> loadDiscountsByCustomer(int year) { loadedYear = year; return List.of(); }
        public AnalysisSummary loadReturnSummary(int year) { return AnalysisSummary.empty(); }
        public List<ReturnAnalysisItem> loadReturnsByCustomer(int year) { loadedYear = year; return List.of(); }
        public List<SalesArchiveItem> loadArchive(int year, String search, int limit) {
            loadedYear = year; this.search = search; return List.of();
        }
    }
}
