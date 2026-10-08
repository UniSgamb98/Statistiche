package com.orodent.statistiche.features.sales.customers.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerTrendNarrativeTest {
    private final CustomerTrendNarrative narrative = new CustomerTrendNarrative();

    @Test
    void explainsFewerButMoreValuableOrders() {
        assertEquals("Il cliente ordina meno spesso, ma gli acquisti mantengono un valore complessivo sostenuto.",
                narrative.describe(new BigDecimal("-15"), new BigDecimal("8")));
    }

    @Test
    void explainsStableOrdersWithLowerRevenue() {
        assertEquals("La frequenza degli ordini è regolare, ma il valore complessivo degli acquisti si sta riducendo.",
                narrative.describe(BigDecimal.ZERO, new BigDecimal("-12")));
    }
}
