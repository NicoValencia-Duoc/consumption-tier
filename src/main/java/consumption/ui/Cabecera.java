package consumption.ui;

import consumption.data.Preferencias;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Cabecera extends HBox {

    private final List<Button> pestanas = new ArrayList<>();
    private final Button pestanaDetalle = new Button("DETALLE");
    private final Preferencias preferencias;
    private final HBox logo = new HBox();
    private final Label nombre = new Label();
    private final Label globo = new Label("▲ HAZ CLIC EN TU NOMBRE PARA CAMBIARLO");
    private final PauseTransition cierreGlobo = new PauseTransition(Duration.seconds(12));

    public Cabecera(Consumer<String> alElegirPestana, Runnable alCambiarTema, Preferencias preferencias) {
        this.preferencias = preferencias;

        Label marca = new Label("Consumption Tier // ");
        marca.getStyleClass().add("logo");
        nombre.setText(preferencias.getNombreUsuario());
        nombre.getStyleClass().addAll("logo", "nombre-usuario");
        nombre.setOnMouseClicked(e -> editarNombre());
        logo.setAlignment(Pos.CENTER_LEFT);
        logo.getChildren().addAll(marca, nombre);
        prepararGlobo();

        HBox contenedorPestanas = new HBox(6);
        for (String nombrePestana : new String[] {"INICIO", "JUEGOS", "LIBROS", "PELICULAS", "SERIES", "PENDIENTES", "ESTADISTICAS"}) {
            Button b = new Button(nombrePestana);
            b.getStyleClass().add("pestana");
            b.setOnAction(e -> {
                marcarActiva(nombrePestana);
                alElegirPestana.accept(nombrePestana);
            });
            pestanas.add(b);
            contenedorPestanas.getChildren().add(b);
        }
        pestanaDetalle.getStyleClass().add("pestana");
        contenedorPestanas.getChildren().add(pestanaDetalle);

        Button tema = new Button("DIA / NOCHE");
        tema.getStyleClass().add("pestana");
        tema.setOnAction(event -> alCambiarTema.run());

        Region izq = new Region();
        Region der = new Region();
        HBox.setHgrow(izq, Priority.ALWAYS);
        HBox.setHgrow(der, Priority.ALWAYS);

        getChildren().addAll(logo, izq, contenedorPestanas, der, tema);
        setSpacing(20);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("cabecera");
        marcarActiva("INICIO");
    }

    private void prepararGlobo() {
        if (preferencias.isBienvenidaVista()) return;
        globo.getStyleClass().add("globo");
        globo.setManaged(false);
        globo.layoutXProperty().bind(nombre.layoutXProperty());
        globo.layoutYProperty().bind(logo.heightProperty().add(16));
        globo.setOnMouseClicked(e -> cerrarGlobo());
        logo.getChildren().add(globo);
        cierreGlobo.setOnFinished(e -> cerrarGlobo());
        cierreGlobo.play();
    }

    private void cerrarGlobo() {
        if (!logo.getChildren().remove(globo)) return;
        cierreGlobo.stop();
        preferencias.setBienvenidaVista(true);
        preferencias.guardar();
    }

    private void editarNombre() {
        cerrarGlobo();
        int posicion = logo.getChildren().indexOf(nombre);
        if (posicion < 0) return;

        TextField campo = new TextField(preferencias.getNombreUsuario());
        campo.getStyleClass().add("campo-nombre");
        campo.setPrefWidth(200);
        campo.setTextFormatter(new TextFormatter<String>(cambio ->
                cambio.getControlNewText().length() <= Preferencias.MAX_NOMBRE ? cambio : null));
        logo.getChildren().set(posicion, campo);
        Platform.runLater(() -> {
            campo.requestFocus();
            campo.selectAll();
        });

        boolean[] terminado = {false};
        Consumer<Boolean> terminar = guardar -> {
            if (terminado[0]) return;
            terminado[0] = true;
            if (guardar && !campo.getText().isBlank()) {
                preferencias.setNombreUsuario(campo.getText());
                preferencias.guardar();
                nombre.setText(preferencias.getNombreUsuario());
            }
            int lugar = logo.getChildren().indexOf(campo);
            if (lugar >= 0) logo.getChildren().set(lugar, nombre);
        };

        campo.setOnAction(e -> terminar.accept(true));
        campo.focusedProperty().addListener((obs, antes, ahora) -> {
            if (!ahora) terminar.accept(true);
        });
        campo.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                terminar.accept(false);
                e.consume();
            }
        });
    }

    public void marcarActiva(String nombrePestana) {
        for (Button b : pestanas) {
            b.getStyleClass().remove("pestana-activa");
            if (b.getText().equals(nombrePestana))
                b.getStyleClass().add("pestana-activa");
        }
        boolean detalle = nombrePestana.equals("DETALLE");
        pestanaDetalle.setVisible(detalle);
        pestanaDetalle.setManaged(detalle);
        pestanaDetalle.getStyleClass().remove("pestana-activa");
        if (detalle) pestanaDetalle.getStyleClass().add("pestana-activa");
    }
}