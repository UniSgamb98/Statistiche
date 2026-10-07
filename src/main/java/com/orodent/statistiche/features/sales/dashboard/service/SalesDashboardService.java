package com.orodent.statistiche.features.sales.dashboard.service;

import com.orodent.statistiche.core.ConnectionProvider;
import com.orodent.statistiche.features.sales.dashboard.model.SalesDashboardData;
import com.orodent.statistiche.features.sales.dashboard.model.SalesFilter;
import com.orodent.statistiche.features.sales.dashboard.model.SalesRankingItem;
import com.orodent.statistiche.features.sales.dashboard.model.SalesSummary;
import com.orodent.statistiche.features.sales.dashboard.repository.SalesStatisticsRepository;
import com.orodent.statistiche.features.sales.dashboard.repository.SalesStatisticsRepositoryImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public final class SalesDashboardService {

    private static final int RANKING_LIMIT = 10;

    private final ConnectionProvider connectionProvider;
    private final Executor executor;
    private final Function<Connection, SalesStatisticsRepository> repositoryFactory;

    public SalesDashboardService(ConnectionProvider connectionProvider, Executor executor) {
        this(connectionProvider, executor, SalesStatisticsRepositoryImpl::new);
    }

    SalesDashboardService(
            ConnectionProvider connectionProvider,
            Executor executor,
            Function<Connection, SalesStatisticsRepository> repositoryFactory
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "connectionProvider");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.repositoryFactory = Objects.requireNonNull(repositoryFactory, "repositoryFactory");
    }

    public CompletableFuture<SalesDashboardData> load(Integer year, LocalDate from, LocalDate to) {
        return CompletableFuture.supplyAsync(() -> loadSync(year, from, to), executor);
    }

    private SalesDashboardData loadSync(Integer requestedYear, LocalDate from, LocalDate to) {
        return connectionProvider.withConnection(connection -> {
            SalesStatisticsRepository repository = repositoryFactory.apply(connection);
            List<Integer> years = repository.findAvailableYears();
            int selectedYear = selectYear(requestedYear, years);
            SalesFilter filter = createFilter(selectedYear, from, to);
            SalesSummary summary = repository.loadSummary(filter);
            List<SalesRankingItem> customers = withPercentages(
                    repository.loadTopCustomers(filter, RANKING_LIMIT),
                    summary.netRevenue()
            );
            List<SalesRankingItem> products = withPercentages(
                    repository.loadTopProducts(filter, RANKING_LIMIT),
                    summary.netRevenue()
            );
            return new SalesDashboardData(
                    years,
                    selectedYear,
                    filter,
                    summary,
                    repository.loadMonthlySales(filter),
                    customers,
                    products
            );
        });
    }

    private int selectYear(Integer requestedYear, List<Integer> years) {
        if (requestedYear != null && years.contains(requestedYear)) {
            return requestedYear;
        }
        return years.isEmpty() ? Year.now().getValue() : years.getFirst();
    }

    private SalesFilter createFilter(int year, LocalDate from, LocalDate to) {
        if (from == null || to == null || from.getYear() != year || to.getYear() != year) {
            return SalesFilter.wholeYear(year);
        }
        return new SalesFilter(from, to);
    }

    private List<SalesRankingItem> withPercentages(
            List<SalesRankingItem> values,
            BigDecimal total
    ) {
        if (total.signum() == 0) {
            return values;
        }
        return values.stream()
                .map(value -> value.withPercentage(
                        value.netRevenue()
                                .multiply(BigDecimal.valueOf(100))
                                .divide(total, 2, RoundingMode.HALF_UP)
                ))
                .toList();
    }
}
