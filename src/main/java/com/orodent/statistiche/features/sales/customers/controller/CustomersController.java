package com.orodent.statistiche.features.sales.customers.controller;

import com.orodent.statistiche.features.sales.analysis.controller.AnalysisController;
import com.orodent.statistiche.features.sales.analysis.model.CustomerAnalysisItem;
import com.orodent.statistiche.features.sales.analysis.service.SalesAnalysisService;
import com.orodent.statistiche.features.sales.customers.view.CustomersView;

public final class CustomersController extends AnalysisController<CustomerAnalysisItem> {

    public CustomersController(CustomersView view, SalesAnalysisService service) {
        super(view, service::loadCustomers);
    }
}
