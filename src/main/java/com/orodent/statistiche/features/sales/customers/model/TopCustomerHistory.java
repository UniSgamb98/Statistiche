package com.orodent.statistiche.features.sales.customers.model;
import java.math.BigDecimal;
public record TopCustomerHistory(String customerCode, String customerName, int year, BigDecimal revenue) { }
