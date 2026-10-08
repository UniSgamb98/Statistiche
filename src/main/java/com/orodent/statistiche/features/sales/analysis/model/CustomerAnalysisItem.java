package com.orodent.statistiche.features.sales.analysis.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerAnalysisItem(String code, String name, BigDecimal revenue, BigDecimal quantity,
                                   int documents, LocalDate lastSale) { }
