package consumption.ui;

import consumption.model.ItemConsumption;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class TarjetaItem extends VBox {
    public TarjetaItem(ItemConsumption item){

        getStyleClass().add("tarjeta");
        setSpacing(8);
        setPrefWidth(235);

        Label letra = new Label(item.getLetra());
        letra.getStyleClass().add("letra");
        Label tipo = new Label(item.getTipo());
        tipo.getStyleClass().add("tipo");

        StackPane miniatura = new StackPane(letra, tipo);
        StackPane.setAlignment(tipo, Pos.BOTTOM_LEFT);

        miniatura.getStyleClass().add("miniatura");

        Label titulo = new Label(item.getTitulo());
        titulo.getStyleClass().add("titulo-item");

        Label nota = new Label(String.format("%.1f", item.getNota()));
        nota.getStyleClass().add("nota");

        Label estado = new Label(item.getEstado().getEtiqueta());
        estado.getStyleClass().addAll("estado", item.getEstado().getClaseCss());

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox fila = new HBox(nota, espacio, estado);
        fila.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(miniatura, titulo, fila);
    }
}
