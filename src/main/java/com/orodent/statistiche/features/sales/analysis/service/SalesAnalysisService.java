package com.orodent.statistiche.features.sales.analysis.service;

import com.orodent.statistiche.core.ConnectionProvider;
import com.orodent.statistiche.features.sales.analysis.model.*;
import com.orodent.statistiche.features.sales.analysis.repository.SalesAnalysisRepository;
import com.orodent.statistiche.features.sales.analysis.repository.SalesAnalysisRepositoryImpl;
import com.orodent.statistiche.features.sales.customers.model.*;
import com.orodent.statistiche.features.sales.customers.repository.CustomerAnalysisRepository;
import com.orodent.statistiche.features.sales.customers.repository.CustomerAnalysisRepositoryImpl;

import java.sql.Connection;
import java.time.Year;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public final class SalesAnalysisService {
    private static final int ARCHIVE_LIMIT = 500;
    private final ConnectionProvider connectionProvider;
    private final Executor executor;
    private final Function<Connection, SalesAnalysisRepository> repositoryFactory;
    private final Function<Connection, CustomerAnalysisRepository> customerRepositoryFactory;

    public SalesAnalysisService(ConnectionProvider connectionProvider, Executor executor) {
        this(connectionProvider, executor, SalesAnalysisRepositoryImpl::new, CustomerAnalysisRepositoryImpl::new);
    }

    SalesAnalysisService(ConnectionProvider connectionProvider, Executor executor,
                         Function<Connection, SalesAnalysisRepository> repositoryFactory) {
        this(connectionProvider, executor, repositoryFactory, CustomerAnalysisRepositoryImpl::new);
    }

    SalesAnalysisService(ConnectionProvider connectionProvider, Executor executor,
                         Function<Connection, SalesAnalysisRepository> repositoryFactory,
                         Function<Connection, CustomerAnalysisRepository> customerRepositoryFactory) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
        this.executor = Objects.requireNonNull(executor);
        this.repositoryFactory = Objects.requireNonNull(repositoryFactory);
        this.customerRepositoryFactory = Objects.requireNonNull(customerRepositoryFactory);
    }

    public CompletableFuture<AnalysisPageData<CustomerAnalysisItem>> loadCustomers(Integer year) {
        return CompletableFuture.supplyAsync(() -> connectionProvider.withConnection(connection -> {
            SalesAnalysisRepository repository = repositoryFactory.apply(connection);
            List<Integer> years = repository.findAvailableYears();
            int selected = selectYear(year, years);
            List<CustomerAnalysisItem> items = repository.loadCustomers(selected);
            AnalysisSummary summary = new AnalysisSummary(
                    items.stream().map(CustomerAnalysisItem::revenue).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
                    items.stream().map(CustomerAnalysisItem::quantity).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
                    java.math.BigDecimal.ZERO, items.size());
            return new AnalysisPageData<>(years, selected, summary, items);
        }), executor);
    }

    public CompletableFuture<CustomerOverviewData> loadCustomerOverview(Integer year) {
        return CompletableFuture.supplyAsync(() -> connectionProvider.withConnection(connection -> {
            SalesAnalysisRepository repository = repositoryFactory.apply(connection);
            List<Integer> years = repository.findAvailableYears();
            int selected = selectYear(year, years);
            List<CustomerAnalysisItem> items = repository.loadCustomers(selected);
            AnalysisSummary summary = new AnalysisSummary(
                    items.stream().map(CustomerAnalysisItem::revenue).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
                    items.stream().map(CustomerAnalysisItem::quantity).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
                    java.math.BigDecimal.ZERO, items.size());
            AnalysisPageData<CustomerAnalysisItem> page = new AnalysisPageData<>(years, selected, summary, items);
            List<TopCustomerHistory> top = customerRepositoryFactory.apply(connection)
                    .loadTopCustomerHistory(selected, 5);
            return new CustomerOverviewData(page, top);
        }), executor);
    }

    public CompletableFuture<CustomerDetailData> loadCustomerDetail(
            String customerCode, int year, int previousYears
    ) {
        return CompletableFuture.supplyAsync(() -> connectionProvider.withConnection(connection -> {
            CustomerAnalysisRepository repository = customerRepositoryFactory.apply(connection);
            CustomerDetail base = repository.loadBaseDetail(customerCode, year);
            List<java.time.LocalDate> dates = repository.loadPurchaseDates(customerCode, year);
            java.math.BigDecimal averageFrequency = averageFrequency(dates);
            java.math.BigDecimal typicalFrequency = typicalFrequency(dates);
            java.time.LocalDate reference = year == java.time.Year.now().getValue()
                    ? java.time.LocalDate.now() : java.time.LocalDate.of(year, 12, 31);
            long recency = java.time.temporal.ChronoUnit.DAYS.between(base.lastPurchase(), reference);
            CustomerDetail detail = new CustomerDetail(base.code(), base.name(), base.country(), base.category(),
                    base.priceList(), base.agent(), base.customerType(), base.revenue(), base.quantity(), base.documents(),
                    base.averageDocumentValue(), base.averageDocumentQuantity(), averageFrequency, typicalFrequency,
                    base.firstPurchase(), base.lastPurchase(), Math.max(recency, 0));
            return new CustomerDetailData(detail, repository.loadYearlyHistory(customerCode),
                    repository.loadMonthlyHistory(customerCode, Math.max(year - previousYears, 0), year),
                    repository.loadProducts(customerCode, year));
        }), executor);
    }

    public CompletableFuture<List<CustomerMonthlyValue>> loadCustomerMonthlyHistory(
            String customerCode, int year, int previousYears
    ) {
        return CompletableFuture.supplyAsync(() -> connectionProvider.withConnection(connection ->
                customerRepositoryFactory.apply(connection).loadMonthlyHistory(
                        customerCode, Math.max(year - previousYears, 0), year
                )
        ), executor);
    }

    private java.math.BigDecimal averageFrequency(List<java.time.LocalDate> dates) {
        List<Long> intervals = intervals(dates);
        if (intervals.isEmpty()) return null;
        long total = intervals.stream().mapToLong(Long::longValue).sum();
        return java.math.BigDecimal.valueOf(total)
                .divide(java.math.BigDecimal.valueOf(intervals.size()), 1, java.math.RoundingMode.HALF_UP);
    }

    private java.math.BigDecimal typicalFrequency(List<java.time.LocalDate> dates) {
        List<Long> intervals = intervals(dates);
        if (intervals.isEmpty()) return null;
        int middle = intervals.size() / 2;
        return intervals.size() % 2 == 1
                ? java.math.BigDecimal.valueOf(intervals.get(middle))
                : java.math.BigDecimal.valueOf(intervals.get(middle - 1) + intervals.get(middle))
                        .divide(java.math.BigDecimal.valueOf(2), 1, java.math.RoundingMode.HALF_UP);
    }

    private List<Long> intervals(List<java.time.LocalDate> dates) {
        List<Long> values = new java.util.ArrayList<>();
        for (int index = 1; index < dates.size(); index++) {
            values.add(java.time.temporal.ChronoUnit.DAYS.between(dates.get(index - 1), dates.get(index)));
        }
        values.sort(Long::compareTo);
        return values;
    }

    public CompletableFuture<AnalysisPageData<ProductAnalysisItem>> loadProducts(Integer year) {
        return CompletableFuture.supplyAsync(() -> connectionProvider.withConnection(connection -> {
            SalesAnalysisRepository repository = repositoryFactory.apply(connection);
            List<Integer> years = repository.findAvailableYears();
            int selected = selectYear(year, years);
            List<ProductAnalysisItem> items = repository.loadProducts(selected);
            AnalysisSummary summary = new AnalysisSummary(
                    items.stream().map(ProductAnalysisItem::revenue).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
                    items.stream().map(ProductAnalysisItem::quantity).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add),
                    java.math.BigDecimal.ZERO, items.size());
            return new AnalysisPageData<>(years, selected, summary, items);
        }), executor);
    }

    public CompletableFuture<AnalysisPageData<DiscountAnalysisItem>> loadDiscounts(Integer year) {
        return CompletableFuture.supplyAsync(() -> connectionProvider.withConnection(connection -> {
            SalesAnalysisRepository repository = repositoryFactory.apply(connection);
            List<Integer> years = repository.findAvailableYears();
            int selected = selectYear(year, years);
            return new AnalysisPageData<>(years, selected, repository.loadDiscountSummary(selected),
                    repository.loadDiscountsByCustomer(selected));
        }), executor);
    }

    public CompletableFuture<AnalysisPageData<ReturnAnalysisItem>> loadReturns(Integer year) {
        return CompletableFuture.supplyAsync(() -> connectionProvider.withConnection(connection -> {
            SalesAnalysisRepository repository = repositoryFactory.apply(connection);
            List<Integer> years = repository.findAvailableYears();
            int selected = selectYear(year, years);
            return new AnalysisPageData<>(years, selected, repository.loadReturnSummary(selected),
                    repository.loadReturnsByCustomer(selected));
        }), executor);
    }

    public CompletableFuture<AnalysisPageData<SalesArchiveItem>> loadArchive(Integer year, String search) {
        return CompletableFuture.supplyAsync(() -> connectionProvider.withConnection(connection -> {
            SalesAnalysisRepository repository = repositoryFactory.apply(connection);
            List<Integer> years = repository.findAvailableYears();
            int selected = selectYear(year, years);
            List<SalesArchiveItem> items = repository.loadArchive(selected, search, ARCHIVE_LIMIT);
            return new AnalysisPageData<>(years, selected,
                    new AnalysisSummary(java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                            java.math.BigDecimal.ZERO, items.size()), items);
        }), executor);
    }

    private int selectYear(Integer requested, List<Integer> years) {
        if (requested != null && years.contains(requested)) return requested;
        return years.isEmpty() ? Year.now().getValue() : years.getFirst();
    }
}
