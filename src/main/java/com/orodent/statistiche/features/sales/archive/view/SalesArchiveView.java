package com.orodent.statistiche.features.sales.archive.view;

import com.orodent.statistiche.features.sales.analysis.model.*;
import com.orodent.statistiche.features.sales.analysis.view.AnalysisView;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class SalesArchiveView extends AnalysisView<SalesArchiveItem> {
    private final TextField search = new TextField();
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.ITALY);
    private final NumberFormat number = NumberFormat.getNumberInstance(Locale.ITALY);

    public SalesArchiveView() {
        super("Archivio vendite", "Consulta le righe importate e risali ai dati di origine");
        search.setPromptText("Documento, cliente o prodotto");
        search.setPrefWidth(300);
        Label label = new Label("Ricerca"); label.getStyleClass().add("filter-label");
        filterBar.getChildren().add(1, new VBox(5, label, search));
        search.setOnAction(event -> refreshButton().fire());
    }

    public String searchText() { return search.getText(); }

    @Override protected void configureTable(TableView<SalesArchiveItem> table) {
        table.getColumns().addAll(column("Data", item -> item.date().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))),
                column("Documento", SalesArchiveItem::document), column("Cliente", SalesArchiveItem::customerName),
                column("Codice cliente", SalesArchiveItem::customerCode), column("Prodotto", SalesArchiveItem::productCode),
                column("Descrizione", SalesArchiveItem::productDescription),
                column("Quantità", item -> number.format(item.quantity())),
                column("Sconto", item -> number.format(item.discountPercentage()) + "%"),
                column("Netto", item -> currency.format(item.netAmount())),
                column("Operazione", item -> item.operation().name()));
    }

    @Override protected void updateSummary(AnalysisPageData<SalesArchiveItem> data) {
        metrics.getChildren().setAll(metric("Righe visualizzate", number.format(data.summary().count())),
                metric("Limite risultati", "500"));
    }

    private TableColumn<SalesArchiveItem, String> column(String title,
            java.util.function.Function<SalesArchiveItem, String> value) {
        TableColumn<SalesArchiveItem, String> column = new TableColumn<>(title);
        column.setCellValueFactory(item -> new SimpleStringProperty(value.apply(item.getValue())));
        return column;
    }
}
