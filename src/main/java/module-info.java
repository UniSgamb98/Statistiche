module com.orodent.statistiche {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.orodent.statistiche to javafx.fxml;
    exports com.orodent.statistiche;
}