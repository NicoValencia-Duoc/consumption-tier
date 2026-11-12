package consumption.ui;

import consumption.data.RepositorioJson;
import consumption.model.*;
import consumption.data.GestorImagenes;
import consumption.data.RespaldoZip;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.layout.*;
import java.util.List;
import java.util.function.Supplier;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

public class VentanaPrincipal extends VBox {

    private final List<ItemConsumption> items;
    private final RepositorioJson repositorio;
    private final Cabecera cabecera;
    private final StackPane contenido = new StackPane();
    private String pestanaActual = "INICIO";

    public VentanaPrincipal(List<ItemConsumption> items, RepositorioJson repositorio,
                            Runnable alCambiarTema, ReproductorMusica reproductor) {
        this.items = items;
        this.repositorio = repositorio;
        setSpacing(14);
        setPadding(new Insets(20, 28, 20, 28));

        cabecera = new Cabecera(this::mostrar, alCambiarTema);
        VBox.setVgrow(contenido, Priority.ALWAYS);
        getChildren().addAll(cabecera, contenido, new BarraMusica(reproductor));

        mostrar("INICIO");
    }

    private void mostrar(String pestana) {
        pestanaActual = pestana;
        Node pagina = switch (pestana) {
            case "JUEGOS" -> paginaJuegos();
            case "LIBROS" -> paginaLibros();
            case "PELICULAS" -> paginaPeliculas();
            case "SERIES" -> paginaSeries();
            case "PENDIENTES" -> paginaPendientes();
            case "ESTADISTICAS" -> new PaginaEstadisticas(items, this::crearRespaldo, this::restaurarRespaldo);
            default -> new PaginaInicio(items, this::verDetalle);
        };
        contenido.getChildren().setAll(pagina);
    }

    private Node paginaJuegos() {
        List<Juego> juegos = items.stream().filter(Juego.class::isInstance).map(Juego.class::cast).toList();
        PaginaTabla<Juego> p = new PaginaTabla<>("JUEGOS", juegos);
        p.agregarColumna("PLATAFORMA", Juego::getPlataforma);
        p.agregarColumna("GENERO", Juego::getGenero);
        p.agregarColumna("DESARROLLADORA", Juego::getDesarrollador);
        p.agregarColumna("AÑO", Juego::getAnio);
        p.agregarColumna("HORAS", Juego::getHorasJugadas);
        conectar(p, FormularioJuego::new);
        return p;
    }

    private Node paginaLibros() {
        List<Libro> libros = items.stream().filter(Libro.class::isInstance).map(Libro.class::cast).toList();
        PaginaTabla<Libro> p = new PaginaTabla<>("LIBROS", libros);
        p.agregarColumna("AUTOR", Libro::getAutor);
        p.agregarColumna("GENERO", Libro::getGenero);
        p.agregarColumna("PAGINAS", Libro::getPaginas);
        p.agregarColumna("AÑO", Libro::getAnio);
        conectar(p, FormularioLibro::new);
        return p;
    }

    private Node paginaPeliculas() {
        List<Pelicula> peliculas = items.stream().filter(Pelicula.class::isInstance).map(Pelicula.class::cast).toList();
        PaginaTabla<Pelicula> p = new PaginaTabla<>("PELICULAS", peliculas);
        p.agregarColumna("DIRECTOR", Pelicula::getDirector);
        p.agregarColumna("GENERO", Pelicula::getGenero);
        p.agregarColumna("DURACION (MIN)", Pelicula::getDuracionMin);
        p.agregarColumna("AÑO", Pelicula::getAnio);
        conectar(p, FormularioPelicula::new);
        return p;
    }

    private Node paginaSeries() {
        List<Serie> series = items.stream().filter(Serie.class::isInstance).map(Serie.class::cast).toList();
        PaginaTabla<Serie> p = new PaginaTabla<>("SERIES", series);
        p.agregarColumna("CREADOR", Serie::getCreador);
        p.agregarColumna("GENERO", Serie::getGenero);
        p.agregarColumna("TEMPORADAS", Serie::getTemporadas);
        p.agregarColumna("EPISODIOS", Serie::getEpisodios);
        p.agregarColumna("AÑO", Serie::getAnio);
        p.agregarColumna("ORIGEN", s -> s.getOrigen().isEmpty() ? "-" : s.getOrigen());
        conectar(p, FormularioSerie::new);
        return p;
    }

    private Node paginaPendientes() {
        List<ItemConsumption> pendientes = items.stream()
                .filter(i -> i.getEstado() == Estado.PENDIENTE).toList();
        PaginaTabla<ItemConsumption> p = new PaginaTabla<>("PENDIENTES", pendientes);
        p.agregarColumna("TIPO", ItemConsumption::getTipo);
        p.agregarColumna("GENERO", ItemConsumption::getGenero);
        conectarComunes(p);
        p.setAlAgregar(this::agregarPendiente);
        p.setAlEditar(this::editarPendiente);
        return p;
    }

    private <T extends ItemConsumption> void conectarComunes(PaginaTabla<T> pagina) {
        pagina.setAlVerDetalle(this::verDetalle);
        pagina.setAlFavorito(this::alternarFavorito);
        pagina.setAlEliminar(seleccionado -> {
            if (confirmarEliminar(seleccionado)) {
                items.remove(seleccionado);
                GestorImagenes.eliminar(seleccionado.getRutaImagen());
                guardarYRefrescar();
            }
        });
    }

    private <T extends ItemConsumption> void conectar(PaginaTabla<T> pagina, Supplier<FormularioItem<T>> formulario) {
        conectarComunes(pagina);
        pagina.setAlAgregar(() -> formulario.get().mostrar(null, hojas()).ifPresent(nuevo -> {
            items.add(nuevo);
            guardarYRefrescar();
        }));
        pagina.setAlEditar(seleccionado ->
                formulario.get().mostrar(seleccionado, hojas()).ifPresent(editado -> guardarYRefrescar()));
    }

    private void agregarPendiente() {
        ChoiceDialog<String> eleccion = new ChoiceDialog<>("JUEGO", "JUEGO", "LIBRO", "PELICULA","SERIE");
        eleccion.setTitle("AGREGAR PENDIENTE");
        eleccion.setHeaderText(null);
        eleccion.setContentText("¿Que quieres agregar?");
        eleccion.getDialogPane().getStylesheets().setAll(hojas());

        eleccion.showAndWait().ifPresent(tipo -> {
            FormularioItem<? extends ItemConsumption> formulario = switch (tipo) {
                case "JUEGO" -> new FormularioJuego();
                case "LIBRO" -> new FormularioLibro();
                case "SERIE" -> new FormularioSerie();
                default -> new FormularioPelicula();
            };
            formulario.mostrar(null, hojas()).ifPresent(nuevo -> {
                items.add(nuevo);
                guardarYRefrescar();
            });
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
        formulario.mostrar((T) item, hojas()).ifPresent(editado -> guardarYRefrescar());
    }

    private void verDetalle(ItemConsumption item) {
        cabecera.marcarActiva("DETALLE");
        contenido.getChildren().setAll(new PaginaDetalle(item,
                () -> repositorio.guardar(items),
                () -> {
                    cabecera.marcarActiva(pestanaActual);
                    mostrar(pestanaActual);
                }));
    }

    private void alternarFavorito(ItemConsumption item) {
        item.setFavorito(!item.isFavorito());
        repositorio.guardar(items);
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
        } catch (IOException ex) {
            avisar(Alert.AlertType.ERROR, "No se pudo crear el respaldo: " + ex.getMessage());
        }
    }

    private void restaurarRespaldo() {
        Alert aviso = new Alert(Alert.AlertType.CONFIRMATION,
                "Restaurar reemplaza TODOS tus datos e imagenes actuales por los del respaldo. ¿Continuar?",
                ButtonType.OK, ButtonType.CANCEL);
        aviso.setHeaderText(null);
        aviso.setTitle("RESTAURAR RESPALDO");
        aviso.getDialogPane().getStylesheets().setAll(hojas());
        if (aviso.showAndWait().filter(b -> b == ButtonType.OK).isEmpty()) return;

        FileChooser selector = new FileChooser();
        selector.setTitle("Elige un respaldo");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Respaldo (.zip)", "*.zip"));
        File archivo = selector.showOpenDialog(getScene().getWindow());
        if (archivo == null) return;

        try {
            RespaldoZip.restaurar(archivo.toPath());
            GestorImagenes.limpiarCache();
            items.clear();
            items.addAll(repositorio.cargar());
            mostrar(pestanaActual);
            avisar(Alert.AlertType.INFORMATION, "Respaldo restaurado: " + items.size() + " items.");
        } catch (IOException ex) {
            avisar(Alert.AlertType.ERROR, "No se pudo restaurar: " + ex.getMessage());
        }
    }

    private void avisar(Alert.AlertType tipo, String texto) {
        Alert alerta = new Alert(tipo, texto, ButtonType.OK);
        alerta.setHeaderText(null);
        alerta.getDialogPane().getStylesheets().setAll(hojas());
        alerta.showAndWait();
    }

    private void guardarYRefrescar() {
        repositorio.guardar(items);
        mostrar(pestanaActual);
    }

    private boolean confirmarEliminar(ItemConsumption item) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar «" + item.getTitulo() + "»? Esta accion no se puede deshacer.",
                ButtonType.OK, ButtonType.CANCEL);
        alerta.setTitle("CONFIRMAR");
        alerta.setHeaderText(null);
        alerta.getDialogPane().getStylesheets().setAll(hojas());
        return alerta.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private List<String> hojas() {
        return getScene().getStylesheets();
    }
}