package consumption.ui;

import consumption.data.Preferencias;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollBar;
import javafx.geometry.Orientation;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Cabecera extends HBox {

    private static final String[][] GRUPOS = {
            {"INICIO"},
            {"JUEGOS", "LIBROS", "PELICULAS", "SERIES"},
            {"PENDIENTES", "ESTADISTICAS"}
    };

    private final Map<String, Button> pestanas = new LinkedHashMap<>();
    private final Button pestanaDetalle = new Button("DETALLE");
    private final Preferencias preferencias;
    private final HBox logo = new HBox();
    private final Label marca = new Label("THE MAW// ");
    private final Label nombre = new Label();
    private final Label globo = new Label("▲ HAZ CLIC EN TU NOMBRE PARA CAMBIARLO");
    private final PauseTransition cierreGlobo = new PauseTransition(Duration.seconds(12));

    public Cabecera(Consumer<String> alElegirPestana, Consumer<Tema> alElegirTema, Preferencias preferencias, Runnable alOpciones) {
        this.preferencias = preferencias;

        marca.getStyleClass().add("logo");
        Efectos.glitchAlPasar(marca);
        Label sesion = new Label("sesion: ");
        sesion.getStyleClass().add("etiqueta-sesion");
        nombre.setText(preferencias.getNombreUsuario());
        nombre.getStyleClass().addAll("logo", "nombre-usuario");
        nombre.setOnMouseClicked(e -> editarNombre());
        logo.setAlignment(Pos.CENTER_LEFT);
        logo.setMinWidth(Region.USE_PREF_SIZE);
        logo.getChildren().addAll(marca, sesion, nombre);
        prepararGlobo();

        HBox contenedorPestanas = new HBox(2);
        contenedorPestanas.setAlignment(Pos.CENTER_LEFT);
        contenedorPestanas.setMinWidth(Region.USE_PREF_SIZE);
        for (int g = 0; g < GRUPOS.length; g++) {
            if (g > 0) contenedorPestanas.getChildren().add(crearSeparador());
            for (String nombrePestana : GRUPOS[g]) {
                Button b = new Button(nombrePestana);
                b.getStyleClass().add("a2-exento");
                b.getStyleClass().add("pestana-barra");
                fijarAnchoNavegacion(b);
                b.setOnAction(e -> {
                    marcarActiva(nombrePestana);
                    alElegirPestana.accept(nombrePestana);
                });
                pestanas.put(nombrePestana, b);
                contenedorPestanas.getChildren().add(b);
            }
        }
        pestanaDetalle.getStyleClass().add("pestana-barra");
        fijarAnchoNavegacion(pestanaDetalle);
        contenedorPestanas.getChildren().add(pestanaDetalle);

        Button botonTema = Terminal.corchetes(new Button("TEMA ▾"));
        botonTema.getStyleClass().add("a2-button");
        botonTema.getStyleClass().add("pestana-barra");
        fijarAnchoNavegacion(botonTema);
        ContextMenu menuTema = new ContextMenu();
        botonTema.setOnAction(e -> {
            if (menuTema.isShowing()) {
                menuTema.hide();
                return;
            }
            llenarMenuTema(menuTema, alElegirTema);
            menuTema.show(botonTema, Side.BOTTOM, 0, 4);
        });

        Button engranaje = new Button("⚙");
        engranaje.getStyleClass().add("a2-exento");
        engranaje.getStyleClass().addAll("pestana-barra", "boton-opciones");
        engranaje.setTooltip(new javafx.scene.control.Tooltip("OPCIONES"));
        engranaje.setMinSize(36, 32);
        engranaje.setPrefSize(36, 32);
        engranaje.setMaxSize(36, 32);
        engranaje.setOnAction(e -> alOpciones.run());
        HBox grupoOpciones = new HBox(6, engranaje, botonTema);
        grupoOpciones.setAlignment(Pos.CENTER_LEFT);

        Region margenIzquierdo = new Region();
        Region margenDerecho = new Region();
        HBox.setHgrow(margenIzquierdo, Priority.ALWAYS);
        HBox.setHgrow(margenDerecho, Priority.ALWAYS);
        HBox pestanasCentradas = new HBox(margenIzquierdo, contenedorPestanas, margenDerecho);
        pestanasCentradas.setAlignment(Pos.CENTER);
        pestanasCentradas.setMinWidth(Region.USE_PREF_SIZE);

        ScrollPane desplazamiento = new ScrollPane(pestanasCentradas);
        desplazamiento.setFitToWidth(true);
        desplazamiento.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        desplazamiento.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        desplazamiento.setFitToHeight(true);
        desplazamiento.setPrefHeight(34);
        desplazamiento.setPannable(true);
        desplazamiento.setMinWidth(0);
        desplazamiento.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(desplazamiento, Priority.ALWAYS);
        desplazamiento.getStyleClass().add("desplazamiento-cabecera");

        logo.setMaxWidth(Region.USE_PREF_SIZE);
        grupoOpciones.setMinWidth(Region.USE_PREF_SIZE);
        grupoOpciones.setMaxWidth(Region.USE_PREF_SIZE);

        ScrollBar barraSuperior = new ScrollBar();
        barraSuperior.setOrientation(Orientation.HORIZONTAL);
        barraSuperior.setMin(0);
        barraSuperior.setMax(1);
        barraSuperior.setBlockIncrement(0.15);
        barraSuperior.setUnitIncrement(0.05);
        barraSuperior.getStyleClass().add("barra-superior-pestanas");

        barraSuperior.visibleAmountProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                () -> {
                    double total = pestanasCentradas.getWidth();
                    double visible = desplazamiento.getViewportBounds().getWidth();
                    if (total <= 0 || visible <= 0) return 1.0;
                    return Math.max(0.01, Math.min(1.0, visible / total));
                },
                pestanasCentradas.widthProperty(), desplazamiento.viewportBoundsProperty()));
        barraSuperior.valueProperty().bindBidirectional(desplazamiento.hvalueProperty());
        barraSuperior.visibleProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
                () -> pestanasCentradas.getWidth() > desplazamiento.getViewportBounds().getWidth() + 2,
                pestanasCentradas.widthProperty(), desplazamiento.viewportBoundsProperty()));

        barraSuperior.setManaged(true);
        VBox zonaNavegacion = new VBox(0, barraSuperior, desplazamiento);
        zonaNavegacion.setMinWidth(0);
        HBox.setHgrow(zonaNavegacion, Priority.ALWAYS);
        getChildren().addAll(logo, zonaNavegacion, grupoOpciones);
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("cabecera");
        marcarActiva("INICIO");
    }

    public void actualizarTierRacha(consumption.model.RachaMaw.TierRacha tier) {
        marca.setStyle(tier.color() == null ? "" : "-fx-text-fill: " + tier.color() + ";");
    }

    private static void fijarAnchoNavegacion(Button boton) {
        boton.setMinWidth(Region.USE_PREF_SIZE);
        boton.setMinHeight(Region.USE_PREF_SIZE);
        boton.setMaxWidth(Region.USE_PREF_SIZE);
    }

    private Region crearSeparador() {
        Region linea = new Region();
        linea.getStyleClass().add("separador-pestanas");
        linea.setMinSize(1, 18);
        linea.setPrefSize(1, 18);
        linea.setMaxSize(1, 18);
        HBox.setMargin(linea, new Insets(0, 8, 0, 8));
        return linea;
    }


    private void llenarMenuTema(ContextMenu menu, Consumer<Tema> alElegirTema) {
        Tema actual = Tema.de(preferencias.getTema());
        menu.getItems().clear();

        for (Tema t : Tema.values()) {
            MenuItem item = new MenuItem(
                    (t == actual ? "● " : "   ") + t.getNombre()
            );

            item.setOnAction(e -> alElegirTema.accept(t));
            menu.getItems().add(item);
        }
    }


    public void setContadores(Map<String, Integer> cantidades) {
        cantidades.forEach((nombrePestana, cantidad) -> {
            Button b = pestanas.get(nombrePestana);
            b.getStyleClass().add("a2-exento");
            if (b == null) return;
            Label numero = new Label(String.format("%02d", cantidad));
            numero.getStyleClass().add("contador-pestana");
            b.setText(nombrePestana);
            b.setGraphic(numero);
            b.setContentDisplay(ContentDisplay.RIGHT);
            b.setGraphicTextGap(8);
            b.setMinWidth(Region.USE_PREF_SIZE);
        });
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
        pestanas.forEach((clave, b) -> {
            b.getStyleClass().remove("pestana-barra-activa");
            if (clave.equals(nombrePestana)) b.getStyleClass().add("pestana-barra-activa");
        });
        boolean detalle = nombrePestana.equals("DETALLE");
        pestanaDetalle.setVisible(detalle);
        pestanaDetalle.setManaged(detalle);
        pestanaDetalle.getStyleClass().remove("pestana-barra-activa");
        if (detalle) pestanaDetalle.getStyleClass().add("pestana-barra-activa");
    }
}