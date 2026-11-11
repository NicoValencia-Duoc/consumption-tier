package consumption.ui;

import consumption.data.GestorImagenes;
import consumption.model.ItemConsumption;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

public class TarjetaItem extends VBox {
    public TarjetaItem(ItemConsumption item){

        getStyleClass().add("tarjeta");
        setSpacing(8);
        setPrefWidth(235);

        StackPane miniatura = new StackPane();
        miniatura.getStyleClass().add("miniatura");

        Image imagen = GestorImagenes.imagenDe(item, 215, 104);
        if (imagen != null) {
            miniatura.getChildren().add(new ImageView(imagen));
        } else {
            Label letra = new Label(item.getLetra());
            letra.getStyleClass().add("letra");
            miniatura.getChildren().add(letra);
        }

        Label tipo = new Label(item.getTipo());
        tipo.getStyleClass().add("tipo");
        StackPane.setAlignment(tipo, Pos.BOTTOM_LEFT);
        miniatura.getChildren().add(tipo);

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
        if (item.isFavorito()) {
            Label estrella = new Label("★");
            estrella.getStyleClass().add("estrella");
            StackPane.setAlignment(estrella, Pos.TOP_RIGHT);
            miniatura.getChildren().add(estrella);
        }

        getChildren().addAll(miniatura, titulo, fila);
    }
}