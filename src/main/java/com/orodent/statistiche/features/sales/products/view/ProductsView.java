package com.orodent.statistiche.features.sales.products.view;

import com.orodent.statistiche.features.sales.analysis.model.ProductAnalysisItem;
import com.orodent.statistiche.features.sales.analysis.model.AnalysisPageData;
import com.orodent.statistiche.features.sales.analysis.view.AnalysisView;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.text.NumberFormat;
import java.util.Locale;

public final class ProductsView extends AnalysisView<ProductAnalysisItem> {
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.ITALY);
    private final NumberFormat number = NumberFormat.getNumberInstance(Locale.ITALY);

    public ProductsView() { super("Analisi prodotti", "Prodotti ordinati per fatturato netto"); }

    @Override protected void configureTable(TableView<ProductAnalysisItem> table) {
        table.getColumns().addAll(column("Codice", ProductAnalysisItem::code),
                column("Descrizione", ProductAnalysisItem::description),
                column("Fatturato", item -> currency.format(item.revenue())),
                column("Quantità", item -> number.format(item.quantity())),
                column("Clienti", item -> number.format(item.customers())),
                column("Documenti", item -> number.format(item.documents())));
    }

    @Override protected void updateSummary(AnalysisPageData<ProductAnalysisItem> data) {
        metrics.getChildren().setAll(metric("Prodotti venduti", number.format(data.summary().count())),
                metric("Fatturato", currency.format(data.summary().primaryValue())),
                metric("Quantità", number.format(data.summary().secondaryValue())));
    }

    private TableColumn<ProductAnalysisItem, String> column(String title,
            java.util.function.Function<ProductAnalysisItem, String> value) {
        TableColumn<ProductAnalysisItem, String> column = new TableColumn<>(title);
        column.setCellValueFactory(item -> new SimpleStringProperty(value.apply(item.getValue())));
        return column;
    }
}
