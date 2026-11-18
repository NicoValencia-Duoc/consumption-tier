package consumption;

import consumption.data.Preferencias;
import consumption.data.RespaldoAutomatico;
import consumption.data.RepositorioJson;
import consumption.data.RecuperacionDatos;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Optional;
import consumption.model.ItemConsumption;
import consumption.ui.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.List;

public class App extends Application {

    private Scene scena;
    private Tema tema = Tema.INDUSTRIAL_DIURNO;
    private Preferencias preferencias;
    private ReproductorMusica reproductor;
    private VentanaPrincipal ventana;
    private boolean cargaSegura = false;

    @Override
    public void start(Stage stage) {
        Tipografia.cargar();
        preferencias = Preferencias.cargar();
        tema = Tema.de(preferencias.getTema());

        RepositorioJson repositorio = new RepositorioJson();
        List<ItemConsumption> items;
        try {
            items = repositorio.cargar();
        } catch (IllegalStateException errorOriginal) {
            Optional<Path> candidato;
            try {
                candidato = RecuperacionDatos.ultimoRespaldoLegible();
            } catch (IOException ex) {
                candidato = Optional.empty();
            }

            if (candidato.isEmpty()) {
                Alert alerta = new Alert(Alert.AlertType.ERROR,
                        "No se pudo leer datos.json y no se encontro un respaldo "
                                + "automatico legible. NO se modificaron los datos. "
                                + "Cierra THE MAW y conserva la carpeta consumption-personal.",
                        ButtonType.CLOSE);
                alerta.setTitle("THE MAW — RECUPERACION");
                alerta.setHeaderText("NO SE PUDO CARGAR LA BIBLIOTECA");
                alerta.showAndWait();
                Platform.exit();
                return;
            }

            Path respaldo = candidato.get();
            ButtonType restaurar = new ButtonType("RESTAURAR RESPALDO");
            Alert pregunta = new Alert(Alert.AlertType.CONFIRMATION,
                    "datos.json no se puede leer.\n\n"
                            + "Respaldo disponible: " + respaldo.getFileName() + "\n\n"
                            + "Se guardara una copia del archivo actual y se restaurara "
                            + "solo datos.json. Las imagenes no se tocaran. "
                            + "Los cambios posteriores al respaldo pueden perderse.",
                    restaurar, ButtonType.CANCEL);
            pregunta.setTitle("THE MAW — RECUPERACION");
            pregunta.setHeaderText("RECUPERAR BIBLIOTECA DESDE RESPALDO");

            if (pregunta.showAndWait().filter(restaurar::equals).isEmpty()) {
                Platform.exit();
                return;
            }

            Path copia = null;
            try {
                copia = RecuperacionDatos.restaurarSoloDatos(respaldo);
                repositorio = new RepositorioJson();
                items = repositorio.cargar();
            } catch (IOException | RuntimeException ex) {

                if (copia != null) {
                    try {
                        RecuperacionDatos.revertir(copia);
                    } catch (IOException falloReversion) {
                        ex.addSuppressed(falloReversion);
                    }
                }
                Alert alerta = new Alert(Alert.AlertType.ERROR,
                        "No se completo la recuperacion. Conserva los respaldos "
                                + "y la copia de emergencia. Detalle: " + ex.getMessage(),
                        ButtonType.CLOSE);
                alerta.setTitle("THE MAW — RECUPERACION FALLIDA");
                alerta.setHeaderText("NO SE ABRIRA LA BIBLIOTECA");
                alerta.showAndWait();
                Platform.exit();
                return;
            }
        }
        cargaSegura = true;

        reproductor = new ReproductorMusica();
        reproductor.volumenProperty().set(preferencias.getVolumen());

        ventana = new VentanaPrincipal(items, repositorio, this::elegirTema, reproductor, preferencias);
        scena = new Scene(Efectos.capa(ventana), 1295, 720);
        aplicarTema();

        ventana.setAlCambiarEscala(this::aplicarTema);
        stage.setTitle("THE MAW");
        try (InputStream icono = getClass().getResourceAsStream("/consumption/iconos/icono-app.png")) {
            if (icono != null) stage.getIcons().add(new Image(icono));
        } catch (Exception e) {
            System.err.println("No se pudo cargar el icono de la ventana: " + e.getMessage());
        }
        stage.setScene(scena);
        GeometriaVentana.aplicar(stage, preferencias);
        Terminal.registrarVentanaPrincipal(stage);
        stage.show();
        GeometriaVentana.registrar(stage, preferencias);

        if (!repositorio.getAvisosCarga().isEmpty()) {
            Alert alerta = new Alert(Alert.AlertType.WARNING,
                    "Se encontraron " + repositorio.getAvisosCarga().size()
                            + " registros invalidos. Se han conservado en datos.json. "
                            + "Los demas registros estan disponibles.\n\n"
                            + String.join("\n", repositorio.getAvisosCarga()),
                    ButtonType.OK);
            alerta.setTitle("THE MAW — DATOS PARA REVISAR");
            alerta.setHeaderText("REGISTROS CONSERVADOS SIN CARGAR");
            alerta.getDialogPane().setMinWidth(650);
            alerta.showAndWait();
        }
    }

    @Override
    public void stop() {
        if (ventana != null) ventana.alCerrar();
        if (reproductor != null) {
            preferencias.setVolumen(reproductor.volumenProperty().get());
            preferencias.guardar();
            reproductor.detener();
        }
        if (cargaSegura) RespaldoAutomatico.crearSiCambio();
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
        int escala = preferencias.getEscalaTexto();
        if (escala != 100) {
            String recursoEscala = "/consumption/texto-" + escala + ".css";
            java.net.URL cssEscala = getClass().getResource(recursoEscala);

            if (cssEscala != null) {
                scena.getStylesheets().add(cssEscala.toExternalForm());
            } else {
                System.err.println("No se encontro la hoja de escala: " + recursoEscala);
            }
        }
        scena.setFill(javafx.scene.paint.Color.web(tema.getFondo()));
    }

    public static void main(String[] args) {
        launch();
    }
}