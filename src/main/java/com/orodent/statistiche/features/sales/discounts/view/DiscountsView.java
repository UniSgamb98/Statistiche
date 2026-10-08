package com.orodent.statistiche.features.sales.discounts.view;

import com.orodent.statistiche.features.sales.analysis.model.*;
import com.orodent.statistiche.features.sales.analysis.view.AnalysisView;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.text.NumberFormat;
import java.util.Locale;

public final class DiscountsView extends AnalysisView<DiscountAnalysisItem> {
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.ITALY);
    private final NumberFormat number = NumberFormat.getNumberInstance(Locale.ITALY);

    public DiscountsView() { super("Analisi sconti", "Sconti concessi e incidenza per cliente"); }

    @Override protected void configureTable(TableView<DiscountAnalysisItem> table) {
        table.getColumns().addAll(column("Codice", DiscountAnalysisItem::code),
                column("Cliente", DiscountAnalysisItem::name),
                column("Fatturato", item -> currency.format(item.revenue())),
                column("Sconto concesso", item -> currency.format(item.discount())),
                column("Sconto medio", item -> number.format(item.averagePercentage()) + "%"));
    }

    @Override protected void updateSummary(AnalysisPageData<DiscountAnalysisItem> data) {
        metrics.getChildren().setAll(metric("Sconti concessi", currency.format(data.summary().primaryValue())),
                metric("Sconto medio", number.format(data.summary().secondaryValue()) + "%"),
                metric("Fatturato netto", currency.format(data.summary().tertiaryValue())),
                metric("Documenti", number.format(data.summary().count())));
    }

    private TableColumn<DiscountAnalysisItem, String> column(String title,
            java.util.function.Function<DiscountAnalysisItem, String> value) {
        TableColumn<DiscountAnalysisItem, String> column = new TableColumn<>(title);
        column.setCellValueFactory(item -> new SimpleStringProperty(value.apply(item.getValue())));
        return column;
    }
}
