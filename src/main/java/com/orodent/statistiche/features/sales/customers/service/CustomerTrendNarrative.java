package com.orodent.statistiche.features.sales.customers.service;

import java.math.BigDecimal;

public final class CustomerTrendNarrative {
    public String describe(BigDecimal orderChange, BigDecimal revenueChange) {
        if (orderChange == null || revenueChange == null) {
            return "Lo storico non è ancora sufficiente per descrivere l'andamento del cliente.";
        }
        Direction orders = direction(orderChange, BigDecimal.TEN);
        Direction revenue = direction(revenueChange, BigDecimal.valueOf(3));
        if (orders == Direction.DOWN && revenue == Direction.DOWN)
            return "Il cliente sta riducendo sia la frequenza sia il valore complessivo degli acquisti.";
        if (orders == Direction.DOWN && revenue != Direction.DOWN)
            return "Il cliente ordina meno spesso, ma gli acquisti mantengono un valore complessivo sostenuto.";
        if (orders == Direction.UP && revenue == Direction.UP)
            return "Il cliente cresce sia per frequenza degli ordini sia per valore degli acquisti.";
        if (orders == Direction.UP && revenue != Direction.UP)
            return "Il cliente ordina più spesso, ma con acquisti mediamente meno importanti.";
        if (revenue == Direction.DOWN)
            return "La frequenza degli ordini è regolare, ma il valore complessivo degli acquisti si sta riducendo.";
        if (revenue == Direction.UP)
            return "La frequenza degli ordini è regolare e il valore complessivo degli acquisti sta aumentando.";
        return "Il rapporto commerciale si mantiene regolare, senza variazioni significative.";
    }

    private Direction direction(BigDecimal value, BigDecimal threshold) {
        if (value.compareTo(threshold) >= 0) return Direction.UP;
        if (value.compareTo(threshold.negate()) <= 0) return Direction.DOWN;
        return Direction.STABLE;
    }

    private enum Direction { UP, STABLE, DOWN }
}
