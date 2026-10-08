package com.orodent.statistiche.features.sales.customers.model;
import com.orodent.statistiche.features.sales.analysis.model.AnalysisPageData;
import com.orodent.statistiche.features.sales.analysis.model.CustomerAnalysisItem;
import java.util.List;
public record CustomerOverviewData(AnalysisPageData<CustomerAnalysisItem> customers,
                                   List<TopCustomerHistory> topHistory) {
    public CustomerOverviewData { topHistory=List.copyOf(topHistory); }
}
