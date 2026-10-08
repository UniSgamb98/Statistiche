package com.orodent.statistiche.features.sales.customers.view;

import com.orodent.statistiche.features.sales.analysis.model.CustomerAnalysisItem;
import com.orodent.statistiche.features.sales.analysis.model.AnalysisPageData;
import com.orodent.statistiche.features.sales.analysis.view.AnalysisView;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class CustomersView extends AnalysisView<CustomerAnalysisItem> {
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.ITALY);
    private final NumberFormat number = NumberFormat.getNumberInstance(Locale.ITALY);

    public CustomersView() { super("Analisi clienti", "Fatturato, quantità e ultima vendita per cliente"); }

    @Override protected void configureTable(TableView<CustomerAnalysisItem> table) {
        table.getColumns().addAll(column("Codice", CustomerAnalysisItem::code),
                column("Ragione sociale", CustomerAnalysisItem::name),
                column("Fatturato", item -> currency.format(item.revenue())),
                column("Quantità", item -> number.format(item.quantity())),
                column("Documenti", item -> number.format(item.documents())),
                column("Ultima vendita", item -> item.lastSale().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
    }

    @Override protected void updateSummary(AnalysisPageData<CustomerAnalysisItem> data) {
        metrics.getChildren().setAll(metric("Clienti attivi", number.format(data.summary().count())),
                metric("Fatturato", currency.format(data.summary().primaryValue())),
                metric("Quantità", number.format(data.summary().secondaryValue())));
    }

    private TableColumn<CustomerAnalysisItem, String> column(String title,
            java.util.function.Function<CustomerAnalysisItem, String> value) {
        TableColumn<CustomerAnalysisItem, String> column = new TableColumn<>(title);
        column.setCellValueFactory(item -> new SimpleStringProperty(value.apply(item.getValue())));
        return column;
    }
}
