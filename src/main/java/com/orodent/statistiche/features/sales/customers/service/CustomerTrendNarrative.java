package com.orodent.statistiche.features.sales.customers.service;

import com.orodent.statistiche.features.sales.customers.model.CustomerTrendDirection;

import java.math.BigDecimal;

public final class CustomerTrendNarrative {
    public String describe(BigDecimal orderChange, BigDecimal revenueChange, BigDecimal quantityChange) {
        CustomerTrendDirection orders = CustomerTrendDirection.forOrders(orderChange);
        CustomerTrendDirection revenue = CustomerTrendDirection.forValue(revenueChange);
        CustomerTrendDirection quantity = CustomerTrendDirection.forValue(quantityChange);
        if (orders == CustomerTrendDirection.UNAVAILABLE || revenue == CustomerTrendDirection.UNAVAILABLE
                || quantity == CustomerTrendDirection.UNAVAILABLE) {
            return "Lo storico non è ancora sufficiente per confrontare frequenza degli ordini, fatturato e quantità acquistata.";
        }
        if (revenue == CustomerTrendDirection.UP && quantity == CustomerTrendDirection.UP) {
            return switch (orders) {
                case STABLE -> "Il cliente mantiene una frequenza regolare e aumenta il volume di affari.";
                case UP -> "Il cliente ordina più spesso e aumenta il volume di affari.";
                case DOWN -> "Il cliente ordina meno spesso, ma aumenta quantità e fatturato, concentrando gli acquisti in ordini mediamente più grandi.";
                default -> throw new IllegalStateException("Direzione ordini non disponibile");
            };
        }
        if (revenue == CustomerTrendDirection.STABLE && quantity == CustomerTrendDirection.STABLE) {
            return switch (orders) {
                case STABLE -> "Il rapporto commerciale si mantiene regolare, con quantità e fatturato sostanzialmente stabili.";
                case UP -> "Il cliente ordina più spesso, distribuendo quantità e fatturato simili su un maggior numero di ordini.";
                case DOWN -> "Il cliente ordina meno spesso, concentrando quantità e fatturato simili in un minor numero di ordini.";
                default -> throw new IllegalStateException("Direzione ordini non disponibile");
            };
        }
        if (revenue == CustomerTrendDirection.DOWN && quantity == CustomerTrendDirection.DOWN) {
            return switch (orders) {
                case STABLE -> "Il cliente ordina regolarmente, ma acquista meno unità e genera meno fatturato.";
                case UP -> "Il cliente ordina più spesso, ma acquista meno unità e genera meno fatturato: gli ordini sono mediamente più piccoli.";
                case DOWN -> "Il cliente riduce la frequenza degli ordini, la quantità acquistata e il fatturato.";
                default -> throw new IllegalStateException("Direzione ordini non disponibile");
            };
        }
        String opening = switch (orders) {
            case STABLE -> "Il cliente ordina regolarmente";
            case UP -> "Il cliente ordina più spesso";
            case DOWN -> "Il cliente ordina meno spesso";
            default -> throw new IllegalStateException("Direzione ordini non disponibile");
        };
        return opening + describePurchases(revenue, quantity);
    }

    private String describePurchases(CustomerTrendDirection revenue, CustomerTrendDirection quantity) {
        return switch (quantity) {
            case UP -> revenue == CustomerTrendDirection.DOWN
                    ? " e acquista più unità, ma il fatturato cala: il valore medio per unità si riduce."
                    : " e acquista più unità, mantenendo un fatturato simile: il valore medio per unità è tendenzialmente più basso.";
            case STABLE -> revenue == CustomerTrendDirection.DOWN
                    ? " e mantiene quantità simili, ma il fatturato cala: il valore medio per unità è tendenzialmente più basso."
                    : " e mantiene quantità simili, con un valore medio per unità tendenzialmente più alto.";
            case DOWN -> switch (revenue) {
                case UP -> " e acquista meno unità, ma genera più fatturato grazie a un valore medio per unità più alto.";
                case STABLE -> " e acquista meno unità, mantenendo un fatturato simile grazie a un valore medio per unità tendenzialmente più alto.";
                case DOWN -> " e acquista meno unità e genera meno fatturato.";
                default -> throw new IllegalStateException("Direzione fatturato non disponibile");
            };
            default -> throw new IllegalStateException("Direzione quantità non disponibile");
        };
    }
}
