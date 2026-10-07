package com.orodent.statistiche.features.sales.dashboard.service;

import com.orodent.statistiche.core.ConnectionProvider;
import com.orodent.statistiche.features.sales.dashboard.model.MonthlySales;
import com.orodent.statistiche.features.sales.dashboard.model.SalesDashboardData;
import com.orodent.statistiche.features.sales.dashboard.model.SalesFilter;
import com.orodent.statistiche.features.sales.dashboard.model.SalesRankingItem;
import com.orodent.statistiche.features.sales.dashboard.model.SalesSummary;
import com.orodent.statistiche.features.sales.dashboard.repository.SalesStatisticsRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesDashboardServiceTest {

    @Test
    void loadsLatestYearAndCalculatesRankingPercentages() {
        FakeRepository repository = new FakeRepository();
        SalesDashboardService service = new SalesDashboardService(
                connectionProvider(),
                Runnable::run,
                connection -> repository
        );

        SalesDashboardData data = service.load(null, null, null).join();

        assertEquals(2026, data.selectedYear());
        assertEquals(LocalDate.of(2026, 1, 1), data.filter().from());
        assertEquals(LocalDate.of(2026, 12, 31), data.filter().to());
        assertEquals(new BigDecimal("25.00"), data.topCustomers().getFirst().percentage());
        assertEquals(2026, repository.summaryFilter.from().getYear());
        assertEquals(List.of(2025, 2026), data.annualComparisons().stream()
                .map(comparison -> comparison.year()).toList());
        assertEquals(new BigDecimal("0.0"), data.annualComparisons().get(1).revenueChangePercentage());
        assertEquals(2025, data.previousMonthlySales().isEmpty() ? 0 : data.selectedYear() - 1);
    }

    @Test
    void preservesCustomRangeInsideSelectedYear() {
        FakeRepository repository = new FakeRepository();
        SalesDashboardService service = new SalesDashboardService(
                connectionProvider(),
                Runnable::run,
                connection -> repository
        );

        SalesDashboardData data = service.load(
                2025,
                LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 3, 31)
        ).join();

        assertEquals(2025, data.selectedYear());
        assertEquals(LocalDate.of(2025, 2, 1), data.filter().from());
        assertEquals(LocalDate.of(2025, 3, 31), data.filter().to());
    }

    @Test
    void returnsEmptyDashboardWhenDatabaseHasNoSales() {
        FakeRepository repository = new FakeRepository();
        repository.years = List.of();
        repository.summary = SalesSummary.empty();
        SalesDashboardService service = new SalesDashboardService(
                connectionProvider(),
                Runnable::run,
                connection -> repository
        );

        SalesDashboardData data = service.load(null, null, null).join();

        assertTrue(data.empty());
        assertTrue(data.availableYears().isEmpty());
    }

    private ConnectionProvider connectionProvider() {
        return () -> (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    if (method.getReturnType().isPrimitive()) {
                        return 0;
                    }
                    return null;
                }
        );
    }

    private static final class FakeRepository implements SalesStatisticsRepository {
        private List<Integer> years = List.of(2026, 2025);
        private SalesSummary summary = new SalesSummary(
                new BigDecimal("1000"),
                new BigDecimal("20"),
                4,
                5,
                new BigDecimal("100")
        );
        private SalesFilter summaryFilter;

        @Override
        public List<Integer> findAvailableYears() {
            return years;
        }

        @Override
        public SalesSummary loadSummary(SalesFilter filter) {
            summaryFilter = filter;
            return summary;
        }

        @Override
        public List<MonthlySales> loadMonthlySales(SalesFilter filter) {
            return List.of(new MonthlySales(1, summary.netRevenue(), summary.quantity(), summary.documents()));
        }

        @Override
        public List<SalesRankingItem> loadTopCustomers(SalesFilter filter, int limit) {
            return List.of(new SalesRankingItem(
                    "C1", null, new BigDecimal("250"), BigDecimal.ONE, 1, BigDecimal.ZERO
            ));
        }

        @Override
        public List<SalesRankingItem> loadTopProducts(SalesFilter filter, int limit) {
            return List.of();
        }
    }
}
