package com.orodent.statistiche.features.sales.returns.view;

import com.orodent.statistiche.features.sales.analysis.model.*;
import com.orodent.statistiche.features.sales.analysis.view.AnalysisView;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.text.NumberFormat;
import java.util.Locale;

public final class ReturnsView extends AnalysisView<ReturnAnalysisItem> {
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.ITALY);
    private final NumberFormat number = NumberFormat.getNumberInstance(Locale.ITALY);

    public ReturnsView() { super("Resi e note di credito", "Valore e quantità delle rettifiche per cliente"); }

    @Override protected void configureTable(TableView<ReturnAnalysisItem> table) {
        table.getColumns().addAll(column("Codice", ReturnAnalysisItem::code),
                column("Cliente", ReturnAnalysisItem::name),
                column("Importo", item -> currency.format(item.amount())),
                column("Quantità", item -> number.format(item.quantity())),
                column("Documenti", item -> number.format(item.documents())));
    }

    @Override protected void updateSummary(AnalysisPageData<ReturnAnalysisItem> data) {
        metrics.getChildren().setAll(metric("Valore rettifiche", currency.format(data.summary().primaryValue())),
                metric("Quantità resa", number.format(data.summary().secondaryValue())),
                metric("Documenti", number.format(data.summary().count())));
    }

    private TableColumn<ReturnAnalysisItem, String> column(String title,
            java.util.function.Function<ReturnAnalysisItem, String> value) {
        TableColumn<ReturnAnalysisItem, String> column = new TableColumn<>(title);
        column.setCellValueFactory(item -> new SimpleStringProperty(value.apply(item.getValue())));
        return column;
    }
}
