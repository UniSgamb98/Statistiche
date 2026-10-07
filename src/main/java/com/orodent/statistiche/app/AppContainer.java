package com.orodent.statistiche.app;

import com.orodent.statistiche.core.ConnectionProvider;
import com.orodent.statistiche.core.Database;
import com.orodent.statistiche.core.database.service.VenditeCsvImportService;
import com.orodent.statistiche.features.sales.dashboard.service.SalesDashboardService;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class AppContainer implements ApplicationInitializer {

    private static final int BACKGROUND_WORKERS = 3;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final Database database;
    private final ExecutorService backgroundExecutor;
    private final ExecutorService salesImportExecutor;
    private final VenditeCsvImportService venditeCsvImportService;
    private final SalesDashboardService salesDashboardService;

    public AppContainer() {
        database = new Database();
        backgroundExecutor = Executors.newFixedThreadPool(
                BACKGROUND_WORKERS,
                namedDaemonThreadFactory("statistiche-worker-")
        );
        salesImportExecutor = Executors.newSingleThreadExecutor(
                namedDaemonThreadFactory("statistiche-sales-import-")
        );
        venditeCsvImportService = new VenditeCsvImportService(database, salesImportExecutor);
        salesDashboardService = new SalesDashboardService(database, backgroundExecutor);
    }

    public Executor backgroundExecutor() {
        return backgroundExecutor;
    }

    public ConnectionProvider connectionProvider() {
        return database;
    }

    public VenditeCsvImportService venditeCsvImportService() {
        return venditeCsvImportService;
    }

    public SalesDashboardService salesDashboardService() {
        return salesDashboardService;
    }

    @Override
    public void initialize() {
        database.start();
    }

    @Override
    public void shutdown() {
        shutdownExecutor(salesImportExecutor);
        shutdownExecutor(backgroundExecutor);
        database.stop();
    }

    private java.util.concurrent.ThreadFactory namedDaemonThreadFactory(String prefix) {
        AtomicInteger sequence = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    System.err.println("Un executor non è terminato entro il timeout.");
                }
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
