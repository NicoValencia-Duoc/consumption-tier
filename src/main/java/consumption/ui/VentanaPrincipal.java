package consumption.ui;

import consumption.data.RepositorioJson;
import consumption.data.Rutas;
import consumption.model.*;
import consumption.data.GestorImagenes;
import consumption.data.RecuperacionDatos;
import consumption.data.RespaldoZip;
import consumption.data.Preferencias;
import consumption.data.Papelera;
import consumption.data.DiarioRestauracion;
import consumption.data.EliminacionTransaccional;
import consumption.data.LimpiezaImagenes;
import java.util.Set;
import java.util.HashSet;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.layout.*;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;

public class VentanaPrincipal extends VBox {

    private final List<ItemConsumption> items;
    private final RepositorioJson repositorio;
    private final Preferencias preferencias;
    private final Cabecera cabecera;
    private final StackPane contenido = new StackPane();
    private String pestanaActual = "INICIO";
    private final Aviso aviso = new Aviso();
    private final Papelera papelera = new Papelera();
    private final PieMaw pie;
    private Runnable alCambiarEscala = () -> {};
    private final Timeline relojRacha = new Timeline(new KeyFrame(Duration.seconds(30), e -> verificarCambioDeDia()));
    private LocalDate fechaRachaMostrada;
    private int diasRachaMostrados;
    private Consumer<String> alCambiarTitulo = titulo -> {};

    public void setAlCambiarTitulo(Consumer<String> accion) {
        alCambiarTitulo = java.util.Objects.requireNonNull(accion);
        actualizarTitulo();
    }

    private void actualizarTitulo() {
        alCambiarTitulo.accept("THE MAW · " + consumption.model.NivelMaw.de(items).name()
                + " · " + diasRachaMostrados + " " + (diasRachaMostrados == 1 ? "DÍA" : "DÍAS"));
    }

    public void setAlCambiarEscala(Runnable accion) { alCambiarEscala = accion; }

    public VentanaPrincipal(List<ItemConsumption> items, RepositorioJson repositorio,
                            Consumer<Tema> alElegirTema, ReproductorMusica reproductor,
                            Preferencias preferencias) {
        this.items = items;
        this.repositorio = repositorio;
        this.preferencias = preferencias;
        if (Numeracion.asignar(items)) repositorio.guardar(items);
        setSpacing(14);
        setPadding(new Insets(20, 28, 20, 28));

        cabecera = new Cabecera(this::mostrar, alElegirTema, preferencias, this::abrirOpciones);
        cabecera.setViewOrder(-1);

        StackPane zona = new StackPane(contenido, aviso);
        zona.setMinHeight(0);
        contenido.setMinHeight(0);
        VBox.setVgrow(zona, Priority.ALWAYS);
        pie = new PieMaw(reproductor);
        pie.setMaxWidth(Double.MAX_VALUE);
        getChildren().addAll(cabecera, zona, pie);

        String inicial = preferencias.getUltimaPestana();
        cabecera.marcarActiva(inicial);
        mostrar(inicial);
        relojRacha.setCycleCount(Timeline.INDEFINITE);
        relojRacha.play();
    }

    public void notificarIntegridadSiCambio() {
        if (!repositorio.tieneRechazados()) return;
        String huella = repositorio.huellaRechazados();
        if (preferencias.rechazadosYaVistos(huella)) return;
        aviso.mostrar("DATOS PARA REVISAR // CONSULTA OPCIONES");
        try {
            preferencias.marcarRechazadosVistos(huella);
            preferencias.guardar();
        } catch (RuntimeException ex) {
            System.err.println("No se pudo guardar el estado del aviso de integridad: " + ex.getMessage());
        }
    }

    private void abrirOpciones() {
        DialogoOpciones.mostrar(getScene().getWindow(), getScene().getStylesheets(),
                preferencias, () -> mostrar(pestanaActual), alCambiarEscala,
                this::abrirPapelera, this::abrirCarpetaDatos, this::revisarPortadasHuerfanas,
                this::crearRespaldo, this::restaurarRespaldo, repositorio);
    }

    private void mostrar(String pestana) {
        pestanaActual = pestana;

        if (!pestana.equals(preferencias.getUltimaPestana())) {
            preferencias.setUltimaPestana(pestana);
            preferencias.guardar();
        }

        Node pagina = switch (pestana) {
            case "JUEGOS" -> paginaJuegos();
            case "LIBROS" -> paginaLibros();
            case "PELICULAS" -> paginaPeliculas();
            case "SERIES" -> paginaSeries();
            case "PENDIENTES" -> paginaPendientes();
            case "ESTADISTICAS" -> new PaginaEstadisticas(items);
            default -> paginaInicio();
        };
        contenido.getChildren().setAll(pagina);
        actualizarContadores();
        actualizarRacha();
        actualizarTitulo();
    }

    private void actualizarRacha() {
        fechaRachaMostrada = LocalDate.now();
        diasRachaMostrados = RachaMaw.dias(items, fechaRachaMostrada);
        cabecera.actualizarTierRacha(RachaMaw.tier(diasRachaMostrados));
    }

    private void verificarCambioDeDia() {
        if (!LocalDate.now().equals(fechaRachaMostrada)) {
            actualizarRacha();
            actualizarTitulo();
        }
    }

    private void actualizarContadores() {
        Map<String, Integer> cantidades = new LinkedHashMap<>();
        cantidades.put("INICIO", items.size());
        cantidades.put("JUEGOS", contar(Juego.class));
        cantidades.put("LIBROS", contar(Libro.class));
        cantidades.put("PELICULAS", contar(Pelicula.class));
        cantidades.put("SERIES", contar(Serie.class));
        cantidades.put("PENDIENTES", (int) items.stream().filter(i -> i.getEstado() == Estado.PENDIENTE).count());
        cabecera.setContadores(cantidades);

        int sinDevorar = cantidades.get("PENDIENTES");
        int devorados = Math.toIntExact(consumption.model.NivelMaw.contarTerminados(items));
        pie.actualizar(devorados, sinDevorar);
    }

    private int contar(Class<? extends ItemConsumption> tipo) {
        return (int) items.stream().filter(tipo::isInstance).count();
    }

    private Node paginaJuegos() {
        List<Juego> juegos = items.stream().filter(Juego.class::isInstance).map(Juego.class::cast).toList();
        PaginaTabla<Juego> p = new PaginaTabla<>("JUEGOS", juegos, preferencias);
        p.agregarColumna("plataforma", "PLATAFORMA", Juego::getPlataforma, true);
        p.agregarColumna("genero", "GENERO", Juego::getGenero, true);
        p.agregarColumna("desarrollador", "DESARROLLADORA", Juego::getDesarrollador, false);
        p.agregarColumna("anio", "AÑO", Juego::getAnio, false);
        p.agregarColumna("horas", "HORAS", Juego::getHorasJugadas, false);
        conectar(p, FormularioJuego::new);
        return p;
    }

    private Node paginaLibros() {
        List<Libro> libros = items.stream().filter(Libro.class::isInstance).map(Libro.class::cast).toList();
        PaginaTabla<Libro> p = new PaginaTabla<>("LIBROS", libros, preferencias);
        p.agregarColumna("autor", "AUTORIA", Libro::getAutor, true);
        p.agregarColumna("genero", "GENERO", Libro::getGenero, true);
        p.agregarColumna("paginas", "PAGINAS", Libro::getPaginas, false);
        p.agregarColumna("anio", "AÑO", Libro::getAnio, false);
        conectar(p, FormularioLibro::new);
        return p;
    }

    private Node paginaPeliculas() {
        List<Pelicula> peliculas = items.stream().filter(Pelicula.class::isInstance).map(Pelicula.class::cast).toList();
        PaginaTabla<Pelicula> p = new PaginaTabla<>("PELICULAS", peliculas, preferencias);
        p.agregarColumna("director", "DIRIGIDA POR", Pelicula::getDirector, true);
        p.agregarColumna("genero", "GENERO", Pelicula::getGenero, true);
        p.agregarColumna("duracion", "DURACION (MIN)", Pelicula::getDuracionMin, false);
        p.agregarColumna("anio", "AÑO", Pelicula::getAnio, false);
        conectar(p, FormularioPelicula::new);
        return p;
    }

    private Node paginaSeries() {
        List<Serie> series = items.stream().filter(Serie.class::isInstance).map(Serie.class::cast).toList();
        PaginaTabla<Serie> p = new PaginaTabla<>("SERIES", series, preferencias);
        p.agregarColumna("creador", "CREADOR", Serie::getCreador, true);
        p.agregarColumna("genero", "GENERO", Serie::getGenero, true);
        p.agregarColumna("temporadas", "TEMPORADAS", Serie::getTemporadas, false);
        p.agregarColumna("episodios", "EPISODIOS", Serie::getEpisodios, false);
        p.agregarColumna("anio", "AÑO", Serie::getAnio, false);
        p.agregarColumna("origen", "ORIGEN", s -> s.getOrigen().isEmpty() ? "-" : s.getOrigen(), false);
        conectar(p, FormularioSerie::new);
        return p;
    }

    private Node paginaPendientes() {
        List<ItemConsumption> pendientes = items.stream()
                .filter(i -> i.getEstado() == Estado.PENDIENTE).toList();
        PaginaTabla<ItemConsumption> p = new PaginaTabla<>("PENDIENTES", pendientes, preferencias);
        p.agregarColumna("tipo", "TIPO", i -> Nombres.singular(i.getTipo()), true);
        p.agregarColumna("genero", "GENERO", ItemConsumption::getGenero, true);
        conectarComunes(p);
        p.setAlAgregar(this::agregarPendiente);
        p.setAlEditar(this::editarPendiente);
        return p;
    }

    private <T extends ItemConsumption> void conectarComunes(PaginaTabla<T> pagina) {
        pagina.setAlVerDetalle(this::verDetalle);
        pagina.setAlFavorito(this::alternarFavorito);
        pagina.setAlFavoritosEnLote(this::alternarFavoritosEnLote);
        pagina.setAlEliminar(this::eliminarLote);
        pagina.setAlCambiarEstado((item, transicion) -> cambiarEstado(item, transicion, false));
        pagina.montar();
    }

    private <T extends ItemConsumption> void conectar(PaginaTabla<T> pagina, Supplier<FormularioItem<T>> formulario) {
        conectarComunes(pagina);
        pagina.setAlAgregar(() -> agregarCon(formulario.get()));
        pagina.setAlEditar(seleccionado -> {
            FormularioItem<T> editor = formulario.get();
            abrir(editor, seleccionado).ifPresent(editado -> {
                if (guardarFormulario(editor, seleccionado, editado)) {
                    aviso.mostrar("REGISTRO ACTUALIZADO");
                }
            });
        });
    }

    private void agregarPendiente() {
        ChoiceDialog<String> eleccion = new ChoiceDialog<>("JUEGO", "JUEGO", "LIBRO", "PELICULA", "SERIE");
        eleccion.setTitle("AGREGAR PENDIENTE");
        eleccion.setHeaderText(null);
        eleccion.setContentText("¿Que quieres agregar?");
        eleccion.getDialogPane().getStylesheets().setAll(hojas());
        Terminal.dialogo(eleccion, "AGREGAR PENDIENTE", "boton-accion", null);

        eleccion.showAndWait().ifPresent(this::agregarDeTipo);
    }

    private void agregarDeTipo(String tipo) {
        FormularioItem<? extends ItemConsumption> formulario = switch (tipo) {
            case "JUEGO" -> new FormularioJuego();
            case "LIBRO" -> new FormularioLibro();
            case "SERIE" -> new FormularioSerie();
            default -> new FormularioPelicula();
        };
        agregarCon(formulario);
    }

    private <T extends ItemConsumption> void agregarCon(FormularioItem<T> formulario) {
        abrir(formulario, null).ifPresent(nuevo -> {
            if (guardarFormulario(formulario, null, nuevo)) {
                avisarGuardado(nuevo);
            }
        });
    }

    private void avisarGuardado(ItemConsumption item) {
        if (item.getEstado() == Estado.PENDIENTE) {
            aviso.mostrar("PENDIENTE GUARDADO");
            return;
        }
        aviso.mostrar(switch (item.getTipo()) {
            case "JUEGOS" -> "JUEGO DEVORADO";
            case "LIBROS" -> "LIBRO DEVORADO";
            case "SERIES" -> "SERIE DEVORADA";
            default -> "PELICULA DEVORADA";
        });
    }

    private void editarPendiente(ItemConsumption item) {
        editarCon(formularioPara(item), item);
    }

    private FormularioItem<? extends ItemConsumption> formularioPara(ItemConsumption item) {
        if (item instanceof Juego) return new FormularioJuego();
        if (item instanceof Libro) return new FormularioLibro();
        if (item instanceof Serie) return new FormularioSerie();
        return new FormularioPelicula();
    }

    @SuppressWarnings("unchecked")
    private <T extends ItemConsumption> void editarCon(FormularioItem<T> formulario, ItemConsumption item) {
        T anterior = (T) item;
        abrir(formulario, anterior).ifPresent(editado -> {
            if (guardarFormulario(formulario, anterior, editado)) {
                aviso.mostrar("REGISTRO ACTUALIZADO");
            }
        });
    }

    private PaginaInicio paginaInicio() {
        PaginaInicio inicio = new PaginaInicio(items, this::verDetalle, this::agregarDeTipo, preferencias.isTarjetasCompactas());
        inicio.setAlEditar(item -> editarCon(formularioPara(item), item));
        inicio.setAlFavorito(this::alternarFavorito);
        inicio.setAlEliminar(this::eliminarLote);
        inicio.setAlCambiarEstado((item, transicion) -> cambiarEstado(item, transicion, false));
        return inicio;
    }

    private void verDetalle(ItemConsumption item) {
        cabecera.marcarActiva("DETALLE");
        contenido.getChildren().setAll(new PaginaDetalle(item,
                () -> repositorio.guardar(items),
                () -> {
                    cabecera.marcarActiva(pestanaActual);
                    mostrar(pestanaActual);
                },
                transicion -> cambiarEstado(item, transicion, true),
                () -> editarDesdeDetalle(item)));
    }

    private void editarDesdeDetalle(ItemConsumption item) {
        int numero = item.getNumero();
        editarCon(formularioPara(item), item);
        ItemConsumption actual = items.stream()
                .filter(i -> i.getNumero() == numero)
                .findFirst().orElse(item);
        verDetalle(actual);
    }
    private void alternarFavoritosEnLote(List<? extends ItemConsumption> lote) {
        if (lote.isEmpty()) return;
        Map<ItemConsumption, Boolean> anteriores = new LinkedHashMap<>();
        for (ItemConsumption item : lote) {
            anteriores.putIfAbsent(item, item.isFavorito());
            item.setFavorito(!item.isFavorito());
        }
        try {
            repositorio.guardar(items);
        } catch (RuntimeException ex) {
            anteriores.forEach(ItemConsumption::setFavorito);
            avisar(Alert.AlertType.ERROR,
                    "No se pudo guardar el lote de favoritos. Se restauraron los estados anteriores: "
                            + ex.getMessage());
        }
    }

    private void alternarFavorito(ItemConsumption item) {
        boolean anterior = item.isFavorito();
        item.setFavorito(!anterior);
        try {
            repositorio.guardar(items);
        } catch (RuntimeException ex) {
            item.setFavorito(anterior);
            avisar(Alert.AlertType.ERROR,
                    "No se pudo guardar el favorito. Se restauro su estado anterior: "
                            + ex.getMessage());
        }
    }

    private void revisarPortadasHuerfanas() {
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
        confirmar.getDialogPane().getStylesheets().setAll(hojas());
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

    private void abrirCarpetaDatos() {
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
    private void crearRespaldo() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar respaldo");
        selector.setInitialFileName("respaldo-" + LocalDate.now() + ".zip");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Respaldo (.zip)", "*.zip"));
        File archivo = selector.showSaveDialog(getScene().getWindow());
        if (archivo == null) return;

        try {
            repositorio.guardar(items);
            RespaldoZip.crear(archivo.toPath());
            avisar(Alert.AlertType.INFORMATION, "Respaldo guardado en " + archivo.getName());
        } catch (IOException | RuntimeException ex) {
            avisar(Alert.AlertType.ERROR, "No se pudo crear el respaldo: " + ex.getMessage());
        }
    }

    private void restaurarRespaldo() {
        Alert aviso = new Alert(Alert.AlertType.CONFIRMATION,
                "Restaurar reemplaza tus datos actuales por los del respaldo. Antes se guarda una copia de emergencia de tus datos en la carpeta consumption-personal. Las portadas del respaldo se agregan; las que ya tienes no se borran. ¿Continuar?",
                ButtonType.OK, ButtonType.CANCEL);
        aviso.setHeaderText(null);
        aviso.setTitle("RESTAURAR RESPALDO");
        aviso.getDialogPane().getStylesheets().setAll(hojas());
        Terminal.dialogo(aviso, "RESTAURAR RESPALDO", "boton-peligro", "RESTAURAR");
        if (aviso.showAndWait().filter(b -> b == ButtonType.OK).isEmpty()) return;

        FileChooser selector = new FileChooser();
        selector.setTitle("Elige un respaldo");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Respaldo (.zip)", "*.zip"));
        File archivo = selector.showOpenDialog(getScene().getWindow());
        if (archivo == null) return;

        try {
            this.aviso.confirmarPendiente();
            Path copiaEmergencia = RespaldoZip.restaurar(archivo.toPath());
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
            mostrar(pestanaActual);
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

    private void avisar(Alert.AlertType tipo, String texto) {
        Alert alerta = new Alert(tipo, texto, ButtonType.OK);
        alerta.setHeaderText(null);
        alerta.getDialogPane().getStylesheets().setAll(hojas());
        Terminal.dialogo(alerta, tipo == Alert.AlertType.ERROR ? "!! ERROR" : "AVISO", "boton-accion", null);
        alerta.showAndWait();
    }

    private <T extends ItemConsumption> boolean guardarFormulario(
            FormularioItem<T> formulario, T anterior, T nuevo) {
        int indice = anterior == null ? -1 : items.indexOf(anterior);
        if (anterior != null && indice < 0) {
            formulario.revertirPortadaNoGuardada();
            avisar(Alert.AlertType.ERROR, "No se encontro el registro original.");
            return false;
        }

        if (anterior == null) {
            nuevo.setNumero(Numeracion.siguiente(items));
            items.add(nuevo);
        } else {
            items.set(indice, nuevo);
        }

        try {
            repositorio.guardar(items);
        } catch (RuntimeException ex) {
            if (anterior == null) {
                items.remove(nuevo);
            } else {
                items.set(indice, anterior);
            }

            formulario.revertirPortadaNoGuardada();

            avisar(Alert.AlertType.ERROR,
                    "No se pudo guardar el registro. Se conservaron los datos anteriores: "
                            + ex.getMessage());
            return false;
        }

        formulario.confirmarPortadaGuardada();
        formulario.confirmarBorradorGuardado();
        try {
            mostrar(pestanaActual);
        } catch (RuntimeException ex) {
            avisar(Alert.AlertType.WARNING,
                    "El registro se guardo correctamente, pero no se pudo "
                            + "actualizar la pantalla: " + ex.getMessage());
        }

        return true;
    }

    private void guardarYRefrescar() {
        repositorio.guardar(items);
        mostrar(pestanaActual);
    }

    private boolean confirmarEliminar(List<ItemConsumption> lote) {
        List<String> titulos = lote.stream().map(ItemConsumption::getTitulo).toList();
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
                ReglasAcciones.textoConfirmarEliminar(titulos),
                ButtonType.OK, ButtonType.CANCEL);
        alerta.setTitle("CONFIRMAR");
        alerta.setHeaderText(null);
        alerta.getDialogPane().getStylesheets().setAll(hojas());
        Terminal.dialogo(alerta, "CONFIRMAR ELIMINACION", "boton-peligro", "ELIMINAR");
        return alerta.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }
    private void eliminarLote(List<? extends ItemConsumption> seleccion) {
        List<ItemConsumption> lote = new ArrayList<>(seleccion);
        if (lote.isEmpty() || !confirmarEliminar(lote)) return;

        Set<String> nuevosIds;
        try {
            nuevosIds = EliminacionTransaccional.ejecutar(items, lote, repositorio, papelera);
        } catch (Exception ex) {
            avisar(Alert.AlertType.ERROR,
                    "No se completo la eliminacion. Si persiste el problema, "
                            + "cierra THE MAW antes de realizar otras operaciones: " + ex.getMessage());
            return;
        }
        try {
            mostrar(pestanaActual);
        } catch (RuntimeException ex) {
            avisar(Alert.AlertType.WARNING,
                    "Los registros se eliminaron, pero no se pudo actualizar la pantalla: "
                            + ex.getMessage());
        }
        aviso.mostrarConDeshacer(
                ReglasAcciones.textoEliminado(lote.size()),
                () -> {
                    try {
                        DiarioRestauracion.ejecutar(() -> {
                            List<ItemConsumption> nuevos = new ArrayList<>(items);
                            nuevos.addAll(lote);
                            repositorio.guardar(nuevos);
                            papelera.retirar(nuevosIds);
                        });
                        items.addAll(lote);
                    } catch (Exception ex) {
                        avisar(Alert.AlertType.ERROR,
                                "No se completo DESHACER. Cierra THE MAW para evitar operaciones "
                                        + "sobre datos posiblemente desactualizados: " + ex.getMessage());
                        javafx.application.Platform.exit();
                        return;
                    }
                    try {
                        mostrar(pestanaActual);
                        aviso.mostrar(ReglasAcciones.textoRestaurado(lote.size()));
                    } catch (RuntimeException ex) {
                        avisar(Alert.AlertType.WARNING,
                                "Los registros se recuperaron, pero no se pudo actualizar la pantalla: "
                                        + ex.getMessage());
                    }
                },
                null);
    }

    private void abrirPapelera() {
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
        selector.getDialogPane().getStylesheets().setAll(hojas());
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
            mostrar(pestanaActual);
            aviso.mostrar("REGISTRO RESTAURADO");
        } catch (RuntimeException ex) {
            avisar(Alert.AlertType.WARNING,
                    "El registro se restauro, pero no se pudo actualizar la pantalla: "
                            + ex.getMessage());
        }
    }

    private void cambiarEstado(ItemConsumption item,
                               Transicion transicion,
                               boolean desdeDetalle) {
        int numero = item.getNumero();
        try {
            TransicionPersistida.ejecutar(item, transicion, LocalDate.now(),
                    () -> repositorio.guardar(items));
        } catch (IllegalArgumentException ex) {
            avisar(Alert.AlertType.ERROR, ex.getMessage());
            return;
        } catch (RuntimeException ex) {
            avisar(Alert.AlertType.ERROR,
                    "No se pudo guardar el cambio de estado. Se restauro el estado anterior: "
                            + ex.getMessage());
            return;
        }

        try {
            if (transicion == Transicion.TERMINAR) {
                editarParaCalificar(item);
                avisarGuardado(vigente(item, numero));
            } else {
                aviso.mostrar(ReglasAcciones.textoAviso(transicion));
            }

            if (desdeDetalle) verDetalle(vigente(item, numero));
            else mostrar(pestanaActual);
        } catch (RuntimeException ex) {
            avisar(Alert.AlertType.WARNING,
                    "El cambio de estado se guardo correctamente, pero no se pudo "
                            + "completar la actualizacion de pantalla: " + ex.getMessage());
        }
    }

    private ItemConsumption vigente(ItemConsumption item, int numero) {
        if (items.contains(item)) return item;
        return items.stream()
                .filter(i -> i.getNumero() == numero)
                .findFirst().orElse(item);
    }

    @SuppressWarnings("unchecked")
    private <T extends ItemConsumption> void editarParaCalificar(ItemConsumption item) {
        FormularioItem<T> formulario = (FormularioItem<T>) formularioPara(item);
        formulario.setIrACriterios(true);
        T anterior = (T) item;
        abrir(formulario, anterior).ifPresent(editado ->
                guardarFormulario(formulario, anterior, editado));
    }

    public void alCerrar() {
        relojRacha.stop();
        aviso.confirmarPendiente();
    }

    private List<String> hojas() {
        return getScene().getStylesheets();
    }

    private boolean existeDuplicado(String tipo, String titulo, ItemConsumption propio) {
        String buscado = titulo.trim();
        return items.stream().anyMatch(i -> i != propio
                && i.getTipo().equals(tipo)
                && i.getTitulo().equalsIgnoreCase(buscado));
    }

    private <T extends ItemConsumption> Optional<T> abrir(FormularioItem<T> formulario, T existente) {
        formulario.setComprobadorDuplicado(this::existeDuplicado);
        return formulario.mostrar(existente, hojas());
    }
}