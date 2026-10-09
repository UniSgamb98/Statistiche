package com.orodent.statistiche.features.sales.customers.model;

import com.orodent.statistiche.features.sales.analysis.model.AnalysisPageData;
import com.orodent.statistiche.features.sales.analysis.model.CustomerAnalysisItem;

public record CustomerOverviewData(AnalysisPageData<CustomerAnalysisItem> customers) { }
