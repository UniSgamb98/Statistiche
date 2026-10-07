package com.orodent.statistiche.features.home.view;

import com.orodent.statistiche.core.components.AppHeader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class HomeView extends BorderPane {

    private final AppHeader header = new AppHeader(
            "Panoramica",
            "Il punto di partenza per importare e analizzare le vendite"
    );
    private final Button importButton = new Button("Importa le vendite");

    public HomeView() {
        getStyleClass().add("page");
        setTop(header);
        setCenter(buildContent());
    }

    public AppHeader header() {
        return header;
    }

    public Button importButton() {
        return importButton;
    }

    private VBox buildContent() {
        VBox content = new VBox(24);
        content.setPadding(new Insets(36));

        VBox hero = new VBox(12);
        hero.getStyleClass().add("hero-card");
        Label eyebrow = new Label("DATI SEMPRE AGGIORNATI");
        eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label("Trasforma le esportazioni del gestionale in informazioni utili.");
        title.getStyleClass().add("hero-title");
        title.setWrapText(true);
        Label description = new Label(
                "Importa il file annuale delle vendite. L'applicazione lo controllerà e sostituirà "
                        + "i dati dell'anno in modo sicuro e transazionale."
        );
        description.getStyleClass().add("hero-description");
        description.setWrapText(true);
        importButton.getStyleClass().add("primary-button");
        hero.getChildren().addAll(eyebrow, title, description, importButton);

        HBox cards = new HBox(18);
        cards.getChildren().addAll(
                infoCard("01", "Importa", "Seleziona il CSV annuale esportato dal gestionale."),
                infoCard("02", "Controlla", "Date, importi e campi obbligatori vengono validati."),
                infoCard("03", "Analizza", "I dati saranno pronti per dashboard e statistiche.")
        );
        cards.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));

        content.getChildren().addAll(hero, cards);
        return content;
    }

    private VBox infoCard(String number, String title, String description) {
        VBox card = new VBox(10);
        card.getStyleClass().add("info-card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);
        Label numberLabel = new Label(number);
        numberLabel.getStyleClass().add("card-number");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("muted-label");
        descriptionLabel.setWrapText(true);
        card.getChildren().addAll(numberLabel, titleLabel, descriptionLabel);
        return card;
    }
}
