package consumption;

import consumption.data.RepositorioJson;
import consumption.model.*;
import consumption.ui.PaginaInicio;
import consumption.ui.VentanaPrincipal;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.List;

public class App extends Application {

    private Scene scena;
    private boolean noche = true;

    @Override public void start(Stage stage) {
        RepositorioJson repositorio = new RepositorioJson();
        List<ItemConsumption> items = repositorio.cargar();

        scena = new Scene(new VentanaPrincipal(items, repositorio, this::cambiarTema), 1295, 720);
        aplicarTema();

        stage.setTitle("Catálogo personal");
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
