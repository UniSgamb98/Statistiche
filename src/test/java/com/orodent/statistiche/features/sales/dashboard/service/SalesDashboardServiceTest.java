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
import java.util.Optional;
import java.time.Year;
import java.util.Map;
import com.orodent.statistiche.features.sales.projection.model.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    void projectsAnnualSeasonalityIndependentlyOfMonthlyFilter() {
        int year = Year.now().getValue();
        FakeRepository repository = projectionRepository(year);
        SalesDashboardService service = new SalesDashboardService(connectionProvider(), Runnable::run, connection -> repository);
        SalesDashboardData wholeYear = service.load(year, null, null).join();
        SalesDashboardData filtered = service.load(year, LocalDate.of(year, 3, 1), LocalDate.of(year, 3, 31)).join();
        RevenueProjection projection = filtered.annualComparisons().getLast().projection();
        assertEquals(ProjectionMethod.SEASONAL, projection.method());
        assertEquals(ProjectionConfidence.MEDIUM, projection.confidence());
        assertEquals(new BigDecimal("120.00"), projection.projectedAnnualRevenue());
        assertEquals(new BigDecimal("60.00"), projection.projectedRemainingRevenue());
        assertEquals(wholeYear.annualComparisons().getLast().projection(), projection);
        assertNull(filtered.annualComparisons().getFirst().projection());
    }

    @Test
    void usesLinearProjectionWithoutHistoricalSeasonality() {
        int year = Year.now().getValue();
        FakeRepository repository = projectionRepository(year);
        repository.history = List.of();
        SalesDashboardService service = new SalesDashboardService(connectionProvider(), Runnable::run, connection -> repository);
        assertEquals(ProjectionMethod.LINEAR, service.load(year, null, null).join()
                .annualComparisons().getLast().projection().method());
    }

    @Test
    void leavesProjectionUnavailableWithoutCoverageOrWithFutureDates() {
        int year = Year.now().getValue();
        FakeRepository repository = projectionRepository(year);
        SalesDashboardService service = new SalesDashboardService(connectionProvider(), Runnable::run, connection -> repository);
        repository.coverage = Optional.empty();
        assertNull(service.load(year, null, null).join().annualComparisons().getLast().projection());
        repository.coverage = Optional.of(new SalesDataCoverage(LocalDate.of(year, 1, 1), LocalDate.now().plusDays(1)));
        assertNull(service.load(year, null, null).join().annualComparisons().getLast().projection());
    }

    @Test
    void doesNotExtrapolateNonPositiveNetRevenue() {
        int year = Year.now().getValue();
        FakeRepository repository = projectionRepository(year);
        repository.annualSummaries = Map.of(year, new SalesSummary(new BigDecimal("-10"), BigDecimal.ZERO, 1, 2, BigDecimal.ZERO));
        SalesDashboardService service = new SalesDashboardService(connectionProvider(), Runnable::run, connection -> repository);
        assertNull(service.load(year, null, null).join().annualComparisons().getLast().projection());
    }

    private FakeRepository projectionRepository(int year) {
        FakeRepository repository = new FakeRepository();
        repository.years = List.of(year, year - 1, year - 2);
        SalesSummary historical = new SalesSummary(new BigDecimal("100"), BigDecimal.TEN, 1, 2, BigDecimal.ZERO);
        SalesSummary current = new SalesSummary(new BigDecimal("60"), BigDecimal.TEN, 1, 2, BigDecimal.ZERO);
        repository.annualSummaries = Map.of(year, current, year - 1, historical, year - 2, historical);
        repository.coverage = Optional.of(new SalesDataCoverage(LocalDate.of(year, 1, 1), LocalDate.of(year, 1, 1)));
        repository.history = List.of(
                new DailyRevenueValue(LocalDate.of(year - 2, 1, 1), new BigDecimal("50")),
                new DailyRevenueValue(LocalDate.of(year - 2, 12, 31), new BigDecimal("50")),
                new DailyRevenueValue(LocalDate.of(year - 1, 1, 1), new BigDecimal("50")),
                new DailyRevenueValue(LocalDate.of(year - 1, 12, 31), new BigDecimal("50")));
        return repository;
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
        private Optional<SalesDataCoverage> coverage = Optional.empty();
        private List<DailyRevenueValue> history = List.of();
        private Map<Integer, SalesSummary> annualSummaries = Map.of();

        @Override
        public Optional<SalesDataCoverage> loadDataCoverage(int year) {
            return coverage;
        }

        @Override
        public List<DailyRevenueValue> loadDailyRevenueHistory(int fromYear, int toYear) {
            return history;
        }

        @Override
        public List<Integer> findAvailableYears() {
            return years;
        }

        @Override
        public SalesSummary loadSummary(SalesFilter filter) {
            summaryFilter = filter;
            return annualSummaries.getOrDefault(filter.from().getYear(), summary);
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
