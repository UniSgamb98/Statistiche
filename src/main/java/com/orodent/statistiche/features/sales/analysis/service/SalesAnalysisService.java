package com.orodent.statistiche.features.sales.analysis.service;

import com.orodent.statistiche.core.ConnectionProvider;
import com.orodent.statistiche.features.sales.analysis.model.*;
import com.orodent.statistiche.features.sales.analysis.repository.SalesAnalysisRepository;
import com.orodent.statistiche.features.sales.analysis.repository.SalesAnalysisRepositoryImpl;

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

    public SalesAnalysisService(ConnectionProvider connectionProvider, Executor executor) {
        this(connectionProvider, executor, SalesAnalysisRepositoryImpl::new);
    }

    SalesAnalysisService(ConnectionProvider connectionProvider, Executor executor,
                         Function<Connection, SalesAnalysisRepository> repositoryFactory) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
        this.executor = Objects.requireNonNull(executor);
        this.repositoryFactory = Objects.requireNonNull(repositoryFactory);
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
