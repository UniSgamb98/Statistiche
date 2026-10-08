package com.orodent.statistiche.features.sales.products.controller;

import com.orodent.statistiche.features.sales.analysis.controller.AnalysisController;
import com.orodent.statistiche.features.sales.analysis.model.ProductAnalysisItem;
import com.orodent.statistiche.features.sales.analysis.service.SalesAnalysisService;
import com.orodent.statistiche.features.sales.products.view.ProductsView;

public final class ProductsController extends AnalysisController<ProductAnalysisItem> {

    public ProductsController(ProductsView view, SalesAnalysisService service) {
        super(view, service::loadProducts);
    }
}
