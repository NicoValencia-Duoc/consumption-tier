package consumption.ui;

import consumption.data.DiarioRestauracion;
import consumption.data.GestorImagenes;
import consumption.data.LimpiezaImagenes;
import consumption.data.Papelera;
import consumption.data.RecuperacionDatos;
import consumption.data.RepositorioJson;
import consumption.data.RespaldoZip;
import consumption.data.Rutas;
import consumption.model.ItemConsumption;
import consumption.model.Numeracion;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

final class AccionesDatos {
    private final List<ItemConsumption> items;
    private final RepositorioJson repositorio;
    private final Papelera papelera;
    private final Aviso aviso;
    private final Supplier<Window> ventana;
    private final Supplier<List<String>> hojas;
    private final Runnable refrescar;

    AccionesDatos(List<ItemConsumption> items, RepositorioJson repositorio, Papelera papelera, Aviso aviso,
                  Supplier<Window> ventana, Supplier<List<String>> hojas, Runnable refrescar) {
        this.items = items;
        this.repositorio = repositorio;
        this.papelera = papelera;
        this.aviso = aviso;
        this.ventana = ventana;
        this.hojas = hojas;
        this.refrescar = refrescar;
    }

    private void avisar(Alert.AlertType tipo, String texto) {
        Avisos.mostrar(hojas.get(), tipo, texto);
    }

    void revisarPortadasHuerfanas() {
        final LimpiezaImagenes.Informe informe;
        try {
            informe = LimpiezaImagenes.analizar();
        } catch (RuntimeException ex) {
            avisar(Alert.AlertType.ERROR, ex.getMessage());
            return;
        }

        if (informe.candidatas().isEmpty()) {
            avisar(Alert.AlertType.INFORMATION,
                    "No se encontraron portadas huerfanas con nombre administrado por THE MAW.");
            return;
        }

        StringBuilder detalle = new StringBuilder();
        int limite = Math.min(15, informe.candidatas().size());
        for (int i = 0; i < limite; i++) {
            var candidata = informe.candidatas().get(i);
            detalle.append("\n- ").append(candidata.nombre());
        }
        if (informe.candidatas().size() > limite) {
            detalle.append("\n... y ").append(informe.candidatas().size() - limite)
                    .append(" mas.");
        }

        double mib = informe.bytes() / 1024.0 / 1024.0;
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION,
                "Candidatas: " + informe.candidatas().size()
                        + " | Espacio: " + String.format(java.util.Locale.ROOT, "%.2f MiB", mib)
                        + "\n\nSe moveran a imagenes-cuarentena/; NO se borraran."
                        + "\nCrea primero un respaldo ZIP."
                        + "\n\nVista previa:" + detalle,
                ButtonType.OK, ButtonType.CANCEL);
        confirmar.setTitle("REVISAR PORTADAS HUERFANAS");
        confirmar.setHeaderText("MOVER A CUARENTENA");
        confirmar.getDialogPane().getStylesheets().setAll(hojas.get());
        Terminal.dialogo(confirmar, "PORTADAS HUERFANAS", "boton-peligro", "MOVER");
        if (confirmar.showAndWait().filter(b -> b == ButtonType.OK).isEmpty()) return;

        try {
            int movidas = LimpiezaImagenes.ponerEnCuarentena(informe);
            avisar(Alert.AlertType.INFORMATION,
                    "Portadas movidas: " + movidas
                            + "\nCarpeta: " + LimpiezaImagenes.carpetaCuarentena()
                            + "\nPuedes restaurarlas manualmente con THE MAW cerrado.");
        } catch (RuntimeException ex) {
            avisar(Alert.AlertType.ERROR, ex.getMessage());
        }
    }

    void abrirCarpetaDatos() {
        try {
            Files.createDirectories(Rutas.BASE);
            if (!Desktop.isDesktopSupported()
                    || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                avisar(Alert.AlertType.ERROR,
                        "No se puede abrir la carpeta automaticamente. Ruta: " + Rutas.BASE);
                return;
            }
            Desktop.getDesktop().open(Rutas.BASE.toFile());
        } catch (IOException | SecurityException | UnsupportedOperationException ex) {
            avisar(Alert.AlertType.ERROR,
                    "No se pudo abrir la carpeta: " + ex.getMessage()
                            + "\nRuta: " + Rutas.BASE);
        }
    }
    void crearRespaldo() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar respaldo");
        selector.setInitialFileName("respaldo-" + LocalDate.now() + ".zip");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Respaldo (.zip)", "*.zip"));
        File archivo = selector.showSaveDialog(ventana.get());
        if (archivo == null) return;

        try {
            repositorio.guardar(items);
            RespaldoZip.crear(archivo.toPath());
            avisar(Alert.AlertType.INFORMATION, "Respaldo guardado en " + archivo.getName());
        } catch (IOException | RuntimeException ex) {
            avisar(Alert.AlertType.ERROR, "No se pudo crear el respaldo: " + ex.getMessage());
        }
    }

    void restaurarRespaldo() {
        Alert aviso = new Alert(Alert.AlertType.CONFIRMATION,
                "Restaurar reemplaza tus datos actuales por los del respaldo. Antes se guarda una copia de emergencia de tus datos en la carpeta consumption-personal. Las portadas del respaldo se agregan; las que ya tienes no se borran. ¿Continuar?",
                ButtonType.OK, ButtonType.CANCEL);
        aviso.setHeaderText(null);
        aviso.setTitle("RESTAURAR RESPALDO");
        aviso.getDialogPane().getStylesheets().setAll(hojas.get());
        Terminal.dialogo(aviso, "RESTAURAR RESPALDO", "boton-peligro", "RESTAURAR");
        if (aviso.showAndWait().filter(b -> b == ButtonType.OK).isEmpty()) return;

        FileChooser selector = new FileChooser();
        selector.setTitle("Elige un respaldo");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Respaldo (.zip)", "*.zip"));
        File archivo = selector.showOpenDialog(ventana.get());
        if (archivo == null) return;

        try {
            this.aviso.confirmarPendiente();
            Path copiaEmergencia = RespaldoZip.restaurar(archivo.toPath(), repositorio.usaGeneros());
            final List<ItemConsumption> cargados;
            try {
                cargados = repositorio.cargar();
            } catch (RuntimeException ex) {
                revertirRestauracion(copiaEmergencia, ex);
                return;
            }
            GestorImagenes.limpiarCache();
            items.clear();
            items.addAll(cargados);
            if (Numeracion.asignar(items)) repositorio.guardar(items);
            refrescar.run();
            avisar(Alert.AlertType.INFORMATION, "Respaldo restaurado: " + items.size() + " items.");
        } catch (IOException | RuntimeException ex) {
            avisar(Alert.AlertType.ERROR, "No se pudo completar la restauracion. "
                    + "Comprueba tus datos y la copia de emergencia antes de continuar: "
                    + ex.getMessage());
        }
    }

    private void revertirRestauracion(Path copiaEmergencia, RuntimeException causa) {
        String texto = "El respaldo no se pudo cargar (" + causa.getMessage()
                + "). Se volvieron a poner tus datos anteriores.";
        try {
            RecuperacionDatos.revertir(copiaEmergencia);
            repositorio.cargar();
        } catch (IOException | RuntimeException ex) {
            texto = "El respaldo no se pudo cargar y tampoco se pudo volver al estado anterior ("
                    + ex.getMessage() + "). Tu copia de emergencia es "
                    + (copiaEmergencia == null ? "(no habia datos previos)" : copiaEmergencia.getFileName())
                    + " en la carpeta consumption-personal. Cierra la app y no la abras hasta revisarla.";
        }
        avisar(Alert.AlertType.ERROR, texto);
    }

    void abrirPapelera() {
        aviso.confirmarPendiente();
        try {
            papelera.depurarVencidas();
        } catch (RuntimeException ex) {
            avisar(Alert.AlertType.ERROR,
                    "No se pudo revisar la papelera: " + ex.getMessage());
            return;
        }
        List<Papelera.Entrada> disponibles = papelera.listar();
        if (disponibles.isEmpty()) {
            avisar(Alert.AlertType.INFORMATION, "LA PAPELERA ESTA VACIA");
            return;
        }

        ChoiceDialog<Papelera.Entrada> selector =
                new ChoiceDialog<>(disponibles.get(0), disponibles);
        selector.setTitle("THE MAW — PAPELERA");
        selector.setHeaderText("RESTAURAR REGISTRO · 15 DIAS");
        selector.setContentText("Selecciona un registro:");
        selector.getDialogPane().getStylesheets().setAll(hojas.get());
        Terminal.dialogo(selector, "PAPELERA", "boton-accion", "RESTAURAR");

        javafx.scene.control.ComboBox<Papelera.Entrada> combo =
                (javafx.scene.control.ComboBox<Papelera.Entrada>)
                        selector.getDialogPane().lookup(".combo-box");

        if (combo != null) {
            combo.setMinWidth(420);
            combo.setPrefWidth(420);
            combo.setMaxWidth(Double.MAX_VALUE);
        }

        selector.getDialogPane().setMinWidth(600);
        selector.getDialogPane().setPrefWidth(600);

        Optional<Papelera.Entrada> elegido = selector.showAndWait();
        if (elegido.isEmpty()) return;

        Papelera.Entrada entrada = elegido.get();
        ItemConsumption item = entrada.item();

        if (repositorio.usaGeneros()) {
            try {
                var plan = repositorio.prepararRecuperacion(item);
                var autorizados = DialogoRecuperacionGeneros.elegir(ventana.get(), hojas.get(), item.getTitulo(), plan);
                if (autorizados.isEmpty()) return;
                papelera.recuperarConGeneros(entrada, items, repositorio, plan, autorizados.get());
            } catch (Exception ex) {
                avisar(Alert.AlertType.ERROR, "No se completo RESTAURAR. Cierra THE MAW para evitar operaciones sobre datos posiblemente desactualizados: " + ex.getMessage());
                javafx.application.Platform.exit();
                return;
            }
            try { refrescar.run(); aviso.mostrar("REGISTRO RESTAURADO"); }
            catch (RuntimeException ex) { avisar(Alert.AlertType.WARNING, "El registro se restauro, pero no se pudo actualizar la pantalla: " + ex.getMessage()); }
            return;
        }

        int numeroEnPapelera = item.getNumero();
        boolean ocupado = items.stream().anyMatch(i -> i.getNumero() == item.getNumero());
        if (ocupado || item.getNumero() <= 0)
            item.setNumero(Numeracion.siguiente(items));

        try {
            DiarioRestauracion.ejecutar(() -> {
                List<ItemConsumption> nuevos = new ArrayList<>(items);
                nuevos.add(item);
                repositorio.guardar(nuevos);
                papelera.retirar(Set.of(entrada.id()));
            });
            items.add(item);
        } catch (Exception ex) {
            item.setNumero(numeroEnPapelera);
            avisar(Alert.AlertType.ERROR,
                    "No se completo RESTAURAR. Cierra THE MAW para evitar operaciones "
                            + "sobre datos posiblemente desactualizados: " + ex.getMessage());
            javafx.application.Platform.exit();
            return;
        }
        try {
            refrescar.run();
            aviso.mostrar("REGISTRO RESTAURADO");
        } catch (RuntimeException ex) {
            avisar(Alert.AlertType.WARNING,
                    "El registro se restauro, pero no se pudo actualizar la pantalla: "
                            + ex.getMessage());
        }
    }
}