package consumption.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Cabecera extends HBox {

    private final List<Button> pestanas = new ArrayList<>();
    private final Button pestanaDetalle = new Button("DETALLE");

    public Cabecera(Consumer<String> alElegirPestana, Runnable alCambiarTema) {

        Label logo = new Label("Consumption Tier // Nombre");
        logo.getStyleClass().add("logo");
        HBox contenedorPestanas = new HBox(6);
        for (String nombre : new String[] {"INICIO", "JUEGOS", "LIBROS", "PELICULAS"}) {
            Button b = new Button(nombre);
            b.getStyleClass().add("pestana");
            b.setOnAction(e -> {
                marcarActiva(nombre);
                alElegirPestana.accept(nombre);
            });
            pestanas.add(b);
            contenedorPestanas.getChildren().add(b);
        }
        pestanaDetalle.getStyleClass().add("pestana");
        contenedorPestanas.getChildren().add(pestanaDetalle);

        Label cancion = new Label("PISTA 03 - NOMBRE DE CANCION");
        cancion.getStyleClass().add("cancion");

        Button tema = new Button("DIA / NOCHE");
        tema.getStyleClass().add("pestana");
        tema.setOnAction(event -> alCambiarTema.run());

        Region izq = new Region();
        Region der = new Region();
        HBox.setHgrow(izq, Priority.ALWAYS);
        HBox.setHgrow(der, Priority.ALWAYS);

        getChildren().addAll(logo, izq, contenedorPestanas, der, cancion, tema);
        setSpacing(20);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("cabecera");
        marcarActiva("INICIO");
    }

    public void marcarActiva(String nombre) {
        for (Button b : pestanas) {
            b.getStyleClass().remove("pestana-activa");
            if (b.getText().equals(nombre))
                b.getStyleClass().add("pestana-activa");
        }
        boolean detalle = nombre.equals("DETALLE");
        pestanaDetalle.setVisible(detalle);
        pestanaDetalle.setManaged(detalle);
        pestanaDetalle.getStyleClass().remove("pestana-activa");
        if (detalle) pestanaDetalle.getStyleClass().add("pestana-activa");
    }
}
