package consumption.ui;

import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class CampoBusqueda extends HBox {

    private final TextField campo = new TextField();
    private final Label aviso = new Label("!");

    public CampoBusqueda() {
        super(6);
        getStyleClass().add("campo-busqueda");
        setAlignment(Pos.CENTER_LEFT);

        Label signo = new Label("$");
        signo.getStyleClass().add("campo-busqueda-signo");
        signo.setOnMouseClicked(e -> campo.requestFocus());

        campo.getStyleClass().add("campo-busqueda-texto");
        HBox.setHgrow(campo, Priority.ALWAYS);
        campo.focusedProperty().addListener((obs, antes, ahora) -> {
            getStyleClass().remove("campo-busqueda-foco");
            if (ahora) getStyleClass().add("campo-busqueda-foco");
        });

        aviso.getStyleClass().add("consulta-generos-invalida");
        aviso.setTooltip(new javafx.scene.control.Tooltip("CONSULTA DE GENERO INVALIDA"));
        aviso.setAccessibleText("CONSULTA DE GENERO INVALIDA");
        aviso.setVisible(false);
        aviso.managedProperty().bind(aviso.visibleProperty());
        getChildren().addAll(signo, campo, aviso);
    }

    public void consultaInvalida(boolean invalida) { aviso.setVisible(invalida); }

    public StringProperty textProperty() {
        return campo.textProperty();
    }

    public String getText() {
        return campo.getText();
    }

    public void setPromptText(String texto) {
        campo.setPromptText(texto);
    }
}
