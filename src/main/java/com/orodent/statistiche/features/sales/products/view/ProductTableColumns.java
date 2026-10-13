package com.orodent.statistiche.features.sales.products.view;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import java.math.BigDecimal;
import java.util.function.Function;

/** Numeric cell values remain numeric, so sorting is independent of localized display text. */
final class ProductTableColumns {
    private ProductTableColumns() { }
    static <T> TableColumn<T, String> text(String title, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        column.setMinWidth(80);
        return column;
    }
    static <T> TableColumn<T, BigDecimal> numeric(String title, Function<T, BigDecimal> value, Function<BigDecimal, String> format) {
        TableColumn<T, BigDecimal> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleObjectProperty<>(value.apply(cell.getValue())));
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : format.apply(item));
            }
        });
        column.setMinWidth(80);
        column.setStyle("-fx-alignment: CENTER-RIGHT;");
        return column;
    }
}
