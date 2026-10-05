module com.orodent.statistiche {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.apache.derby.server;


    opens com.orodent.statistiche to javafx.fxml;
    exports com.orodent.statistiche;
}