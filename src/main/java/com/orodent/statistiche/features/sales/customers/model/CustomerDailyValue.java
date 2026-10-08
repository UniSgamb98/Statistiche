package com.orodent.statistiche.features.sales.customers.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerDailyValue(LocalDate date, BigDecimal revenue) { }
