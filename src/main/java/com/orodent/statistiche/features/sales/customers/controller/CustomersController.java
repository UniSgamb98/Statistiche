package com.orodent.statistiche.features.sales.customers.controller;

import com.orodent.statistiche.features.sales.analysis.model.CustomerAnalysisItem;
import com.orodent.statistiche.features.sales.analysis.service.SalesAnalysisService;
import com.orodent.statistiche.features.sales.customers.model.CustomerDetailData;
import com.orodent.statistiche.features.sales.customers.model.CustomerOverviewData;
import com.orodent.statistiche.features.sales.customers.view.CustomersView;
import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class CustomersController {
    private final CustomersView view;
    private final SalesAnalysisService service;
    private CompletableFuture<CustomerOverviewData> overviewLoad;
    private CompletableFuture<CustomerDetailData> detailLoad;
    private CustomerAnalysisItem selectedCustomer;
    private boolean disposed;

    public CustomersController(CustomersView view, SalesAnalysisService service) {
        this.view = view;
        this.service = service;
        view.refreshButton().setOnAction(event -> loadOverview(view.yearBox().getValue()));
        view.onCustomerSelected(this::loadDetail);
        view.onComparisonYearsChanged(() -> {
            if (selectedCustomer != null) loadDetail(selectedCustomer);
        });
    }

    public void loadInitialData() { loadOverview(null); }

    private void loadOverview(Integer year) {
        if (overviewLoad != null) return;
        view.showLoading();
        CompletableFuture<CustomerOverviewData> future = service.loadCustomerOverview(year);
        overviewLoad = future;
        future.whenComplete((data, error) -> Platform.runLater(() -> {
            if (overviewLoad != future) return;
            overviewLoad = null;
            if (disposed) return;
            if (error == null) view.showOverview(data); else view.showError(unwrap(error));
        }));
    }

    private void loadDetail(CustomerAnalysisItem customer) {
        selectedCustomer = customer;
        view.showDetailLoading(customer);
        CompletableFuture<CustomerDetailData> future = service.loadCustomerDetail(
                customer.code(), view.yearBox().getValue(), view.comparisonYears());
        detailLoad = future;
        future.whenComplete((data, error) -> Platform.runLater(() -> {
            if (detailLoad != future) return;
            detailLoad = null;
            if (disposed) return;
            if (error == null) view.showDetail(data); else view.showDetailError(unwrap(error));
        }));
    }

    private Throwable unwrap(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
    }

    public void dispose() {
        disposed = true;
        overviewLoad = null;
        detailLoad = null;
    }
}
