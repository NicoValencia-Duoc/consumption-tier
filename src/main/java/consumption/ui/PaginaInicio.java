package consumption.ui;

import consumption.model.ItemConsumption;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Comparator;
import java.util.List;

public class PaginaInicio extends VBox{

    public PaginaInicio(List<ItemConsumption> items){

        setSpacing(14);
        getChildren().addAll(
                crearBarraHerramientas(), crearSubtitulo(), crearGaleria(items)
        );
    }

    private HBox crearBarraHerramientas(){

        TextField buscador = new TextField();
        buscador.setPromptText("BUSCAR POR TITULO...");
        buscador.getStyleClass().add("buscador");
        HBox.setHgrow(buscador, Priority.ALWAYS);

        HBox filtros = new HBox(6);
        String[] nombres = {"TODO", "JUEGOS", "LIBROS", "PELICULAS", "FAVORITOS"};
        for (int i = 0; i < nombres.length;i++){
            Button b = new Button(nombres[i]);
            b.getStyleClass().add("filtro");
            if (i == 0)
                b.getStyleClass().add("filtro-activo");
            filtros.getChildren().add(b);
        }
        HBox barra = new HBox(12, buscador, filtros);
        barra.setAlignment(Pos.CENTER_LEFT);
        return barra;
    }

    private Label crearSubtitulo(){

        Label l = new Label("ORDENADO POR NOTA ▼ · TODO LO CONSUMIDO");
        l.getStyleClass().add("subtitulo");
        return l;
    }

    private ScrollPane crearGaleria(List<ItemConsumption> items){
        TilePane galeria = new TilePane(14, 14);
        galeria.setPrefColumns(4);
        items.stream().sorted(Comparator.comparingDouble(ItemConsumption::getNota).reversed()).forEach(i -> galeria.getChildren().add(new TarjetaItem(i)));
        ScrollPane scroll = new ScrollPane(galeria);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

}