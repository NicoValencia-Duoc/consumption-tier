package consumption;

import consumption.data.Preferencias;
import consumption.data.RepositorioJson;
import consumption.model.ItemConsumption;
import consumption.ui.ReproductorMusica;
import consumption.ui.Tipografia;
import consumption.ui.VentanaPrincipal;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

public class App extends Application {

    private Scene scena;
    private boolean noche = true;
    private Preferencias preferencias;
    private ReproductorMusica reproductor;

    @Override
    public void start(Stage stage) {
        Tipografia.cargar();
        preferencias = Preferencias.cargar();
        noche = preferencias.isNoche();

        RepositorioJson repositorio = new RepositorioJson();
        List<ItemConsumption> items = repositorio.cargar();

        reproductor = new ReproductorMusica();
        reproductor.volumenProperty().set(preferencias.getVolumen());

        scena = new Scene(new VentanaPrincipal(items, repositorio, this::cambiarTema, reproductor, preferencias), 1295, 720);
        aplicarTema();

        stage.setTitle("THE MAW");
        stage.setScene(scena);
        stage.show();
        reproductor.iniciar();
    }

    @Override
    public void stop() {
        if (reproductor != null) {
            preferencias.setVolumen(reproductor.volumenProperty().get());
            preferencias.guardar();
            reproductor.detener();
        }
    }

    private void cambiarTema() {
        noche = !noche;
        aplicarTema();
        preferencias.setNoche(noche);
        preferencias.guardar();
    }

    private void aplicarTema() {
        String base = getClass().getResource("/consumption/base.css").toExternalForm();
        String tema = getClass().getResource(noche ? "/consumption/noche.css" : "/consumption/dia.css").toExternalForm();
        scena.getStylesheets().setAll(base, tema);
    }

    public static void main(String[] args) {
        launch();
    }
}