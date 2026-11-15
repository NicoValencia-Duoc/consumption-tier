package consumption.ui;

import consumption.data.GestorImagenes;
import consumption.model.ItemConsumption;
import consumption.model.Estado;
import consumption.model.Formato;
import consumption.model.ReglasInicio;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.control.Tooltip;
import java.time.LocalDate;

public class TarjetaItem extends VBox {

    private static final ColorAdjust GRISES = new ColorAdjust(0, -1, -0.15, 0);
    public TarjetaItem(ItemConsumption item){

        boolean pendiente = item.getEstado() == Estado.PENDIENTE;

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

        if (item.isFavorito()) {
            Label estrella = new Label("★");
            estrella.getStyleClass().add("estrella");
            StackPane.setAlignment(estrella, Pos.TOP_LEFT);
            StackPane.setMargin(estrella, new Insets(2, 0, 0, 6));
            miniatura.getChildren().add(estrella);
        }

        EtiquetaTier tier = EtiquetaTier.de(item);
        if (tier != null) {
            StackPane.setAlignment(tier, Pos.TOP_RIGHT);
            StackPane.setMargin(tier, new Insets(5, 5, 0, 0));
            miniatura.getChildren().add(tier);
        }

        Label titulo = new Label(item.getTitulo());
        titulo.getStyleClass().add("titulo-item");

        Label estado = new Label(item.getEstado().getEtiqueta());
        estado.getStyleClass().addAll("estado", item.getEstado().getClaseCss());

        HBox fila;
        if (pendiente) {
            fila = new HBox(estado);
        } else {
            Label nota = new Label(Formato.decimal(item.getNota()));
            nota.getStyleClass().add("nota");
            Region espacio = new Region();
            HBox.setHgrow(espacio, Priority.ALWAYS);
            fila = new HBox(nota, espacio, estado);
        }
        fila.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(miniatura, titulo, fila);

        String dias = ReglasInicio.textoDias(item, LocalDate.now());
        if (dias != null) {
            Label diasEnCurso = new Label(dias);
            diasEnCurso.getStyleClass().add("dias-en-curso");
            getChildren().add(diasEnCurso);
        }

        if (pendiente) {
            getStyleClass().add("tarjeta-pendiente");
            miniatura.setEffect(GRISES);
            hoverProperty().addListener((obs, antes, ahora) -> miniatura.setEffect(ahora ? null : GRISES));
        }

        Tooltip globo = TooltipoComentario.de(item.getComentario());
        if (globo != null) Tooltip.install(this, globo);
    }
}