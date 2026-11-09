package consumption;

import consumption.model.*;
import consumption.ui.PaginaInicio;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.List;

public class App extends Application {

    private Scene scena;
    private boolean noche = true;

    @Override public void start(Stage stage){
        List<ItemConsumption> items = List.of(
                new Juego("Juego ejemplo A", 9.4, Estado.TERMINADO),
                new Pelicula("Película ejemplo B", 9.1, Estado.TERMINADO),
                new Libro("Libro ejemplo C", 8.8, Estado.EN_PROGRESO),
                new Juego("Juego ejemplo D", 8.5, Estado.TERMINADO),
                new Pelicula("Película ejemplo E", 7.9, Estado.TERMINADO),
                new Libro("Libro ejemplo F", 7.2, Estado.ABANDONADO),
                new Juego("Juego ejemplo G", 6.8, Estado.EN_PROGRESO),
                new Pelicula("Película ejemplo H", 6.0, Estado.PENDIENTE));
        scena = new Scene(new PaginaInicio(items, this::cambiarTema),1100, 680);
        aplicarTema();

        stage.setTitle("Consumption tier // nombre");
        stage.setScene(scena);
        stage.show();
    }

    private void cambiarTema(){
        noche = !noche;
        aplicarTema();
    }

    private void aplicarTema(){
        String base = getClass().getResource("/consumption/base.css").toExternalForm();
        String tema = getClass().getResource(noche ? "/consumption/noche.css" : "/consumption/dia.css").toExternalForm();

        scena.getStylesheets().setAll(base, tema);
    }

    public static void main(String[] args) {
        launch();
    }
}
