package com.orodent.statistiche.features.sales.products.view;

import com.orodent.statistiche.features.sales.products.model.*;
import com.orodent.statistiche.features.sales.products.service.ProductTotalsCalculator;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.math.BigDecimal;
import java.util.Comparator;

final class ProductVariantsPane extends VBox {
    private final ProductFormats formats = new ProductFormats();
    private final ComboBox<ProductVariantDimension> dimension = new ComboBox<>();
    private final ComboBox<ProductMetric> metric = new ComboBox<>();
    private final TableView<ProductVariantValue> table = new TableView<>();
    private ProductDetailData data;
    private boolean updating;

    ProductVariantsPane() {
        setSpacing(12); getStyleClass().add("product-tab-content");
        metric.getItems().setAll(ProductMetric.values()); metric.setValue(ProductMetric.QUANTITY);
        dimension.valueProperty().addListener((observable, before, after) -> { if (!updating) render(); });
        metric.valueProperty().addListener((observable, before, after) -> render());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(360); table.setPlaceholder(new Label("Nessuna variante nel periodo"));
        table.getColumns().addAll(ProductTableColumns.text("Variante", ProductVariantValue::variant),
                ProductTableColumns.numeric("Quantità", value -> value.totals().quantity(), formats::number),
                ProductTableColumns.numeric("Fatturato", value -> value.totals().revenue(), formats::money));
        var share = ProductTableColumns.<ProductVariantValue>numeric("Quota %", value -> share(value), formats::percent);
        share.setMinWidth(150);
        share.setCellFactory(ignored -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty); setText(null); setGraphic(null);
                if (!empty) {
                    ProgressBar bar = new ProgressBar(value == null ? 0 : Math.max(0, Math.min(1, value.doubleValue() / 100)));
                    bar.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(bar, Priority.ALWAYS);
                    HBox line = new HBox(8, bar, new Label(formats.percent(value))); line.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    setGraphic(line);
                }
            }
        });
        table.getColumns().add(share);
        getChildren().addAll(new HBox(10, new Label("Ripartizione"), dimension, new Label("Misura"), metric), table);
    }
    void show(ProductDetailData data) {
        this.data = data; updating = true;
        ProductVariantDimension previous = dimension.getValue();
        dimension.getItems().setAll(data.variants().keySet().stream().sorted().toList());
        dimension.setValue(dimension.getItems().contains(previous) ? previous : dimension.getItems().getFirst());
        updating = false; render();
    }
    private BigDecimal share(ProductVariantValue value) {
        return data == null ? null : ProductTotalsCalculator.share(metric.getValue().value(value.totals()), metric.getValue().value(data.group().current()));
    }
    private void render() {
        if (data == null || dimension.getValue() == null) return;
        table.getSortOrder().clear();
        table.getItems().setAll(data.variants().get(dimension.getValue()).stream().sorted(
                Comparator.comparing((ProductVariantValue value) -> metric.getValue().value(value.totals())).reversed()
                        .thenComparing(ProductVariantValue::variant)).toList());
        table.refresh();
    }
}
