package consumption;

import consumption.data.Preferencias;
import consumption.data.RespaldoAutomatico;
import consumption.data.RepositorioJson;
import consumption.model.ItemConsumption;
import consumption.ui.GeometriaVentana;
import consumption.ui.ReproductorMusica;
import consumption.ui.Tipografia;
import consumption.ui.VentanaPrincipal;
import consumption.ui.Tema;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.List;

public class App extends Application {

    private Scene scena;
    private Tema tema = Tema.NOCTURNO;
    private Preferencias preferencias;
    private ReproductorMusica reproductor;
    private VentanaPrincipal ventana;

    @Override
    public void start(Stage stage) {
        Tipografia.cargar();
        preferencias = Preferencias.cargar();
        tema = Tema.de(preferencias.getTema());

        RepositorioJson repositorio = new RepositorioJson();
        List<ItemConsumption> items = repositorio.cargar();

        reproductor = new ReproductorMusica();
        reproductor.volumenProperty().set(preferencias.getVolumen());

        ventana = new VentanaPrincipal(items, repositorio, this::elegirTema, reproductor, preferencias);
        scena = new Scene(ventana, 1295, 720);
        aplicarTema();

        stage.setTitle("THE MAW");
        try (InputStream icono = getClass().getResourceAsStream("/consumption/iconos/icono-app.png")) {
            if (icono != null) stage.getIcons().add(new Image(icono));
        } catch (Exception e) {
            System.err.println("No se pudo cargar el icono de la ventana: " + e.getMessage());
        }
        stage.setScene(scena);
        GeometriaVentana.aplicar(stage, preferencias);
        stage.show();
        GeometriaVentana.registrar(stage, preferencias);
    }

    @Override
    public void stop() {
        if (ventana != null) ventana.alCerrar();
        if (reproductor != null) {
            preferencias.setVolumen(reproductor.volumenProperty().get());
            preferencias.guardar();
            reproductor.detener();
        }
        RespaldoAutomatico.crearSiCambio();
    }

    private void elegirTema(Tema nuevo) {
        tema = nuevo;
        aplicarTema();
        preferencias.setTema(nuevo.name());
        preferencias.guardar();
    }

    private void aplicarTema() {
        String base = getClass().getResource("/consumption/base.css").toExternalForm();
        String actual = getClass().getResource(tema.getRecurso()).toExternalForm();
        String estilo = getClass().getResource("/consumption/estilo.css").toExternalForm();
        scena.getStylesheets().setAll(base, estilo, actual);
        scena.setFill(javafx.scene.paint.Color.web(tema.getFondo()));
    }

    public static void main(String[] args) {
        launch();
    }
}