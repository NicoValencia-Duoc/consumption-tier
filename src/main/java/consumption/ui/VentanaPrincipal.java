package consumption.ui;

import consumption.data.RepositorioJson;
import consumption.model.*;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Supplier;

public class VentanaPrincipal extends VBox {

    private final List<ItemConsumption> items;
    private final RepositorioJson repositorio;
    private final Cabecera cabecera;
    private final StackPane contenido = new StackPane();
    private String pestanaActual = "INICIO";

    public VentanaPrincipal(List<ItemConsumption> items, RepositorioJson repositorio, Runnable alCambiarTema) {

        this.items = items;
        this.repositorio = repositorio;
        setSpacing(14);
        setPadding(new Insets(20, 28, 20, 28));

        cabecera = new Cabecera(this::mostrar, alCambiarTema);
        VBox.setVgrow(contenido, Priority.ALWAYS);
        getChildren().addAll(cabecera, contenido);

        mostrar("INICIO");
    }

    private void mostrar(String pestana) {

        pestanaActual = pestana;
        Node pagina = switch (pestana) {
            case "JUEGOS" -> paginaJuegos();
            case "LIBROS" -> paginaLibros();
            case "PELICULAS" -> paginaPeliculas();
            default -> new PaginaInicio(items);
        };
        contenido.getChildren().setAll(pagina);
    }

    private Node paginaJuegos() {

        List<Juego> juegos = items.stream().filter(Juego.class::isInstance).map(Juego.class::cast).toList();
        PaginaTabla<Juego> p = new PaginaTabla<>("JUEGOS", juegos);
        p.agregarColumna("PLATAFORMA", Juego::getPlataforma);
        p.agregarColumna("GÉNERO", Juego::getGenero);
        p.agregarColumna("DESARROLLADOR", Juego::getDesarrollador);
        p.agregarColumna("AÑO", Juego::getAnio);
        p.agregarColumna("HORAS", Juego::getHorasJugadas);
        conectar(p, FormularioJuego::new);
        return p;
    }

    private Node paginaLibros() {

        List<Libro> libros = items.stream().filter(Libro.class::isInstance).map(Libro.class::cast).toList();
        PaginaTabla<Libro> p = new PaginaTabla<>("LIBROS", libros);
        p.agregarColumna("AUTOR", Libro::getAutor);
        p.agregarColumna("GÉNERO", Libro::getGenero);
        p.agregarColumna("PÁGINAS", Libro::getPaginas);
        p.agregarColumna("AÑO", Libro::getAnio);
        conectar(p, FormularioLibro::new);
        return p;
    }

    private Node paginaPeliculas() {

        List<Pelicula> pelis = items.stream().filter(Pelicula.class::isInstance).map(Pelicula.class::cast).toList();
        PaginaTabla<Pelicula> p = new PaginaTabla<>("PELÍCULAS", pelis);
        p.agregarColumna("DIRECTOR", Pelicula::getDirector);
        p.agregarColumna("GÉNERO", Pelicula::getGenero);
        p.agregarColumna("DURACIÓN (MIN)", Pelicula::getDuracionMin);
        p.agregarColumna("AÑO", Pelicula::getAnio);
        conectar(p, FormularioPelicula::new);
        return p;
    }

    private <T extends ItemConsumption> void conectar(PaginaTabla<T> pagina, Supplier<FormularioItem<T>> formulario) {

        pagina.setAlVerDetalle(this::verDetalle);
        pagina.setAlAgregar(() -> formulario.get().mostrar(null, hojas()).ifPresent(nuevo -> {
            items.add(nuevo);
            guardarYRefrescar();
        }));

        pagina.setAlEditar(seleccionado ->
                formulario.get().mostrar(seleccionado, hojas()).ifPresent(editado -> guardarYRefrescar()));

        pagina.setAlEliminar(seleccionado -> {
            if (confirmarEliminar(seleccionado)) {
                items.remove(seleccionado);
                guardarYRefrescar();
            }
        });
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

    private void guardarYRefrescar() {
        repositorio.guardar(items);
        mostrar(pestanaActual);
    }

    private boolean confirmarEliminar(ItemConsumption item) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar «" + item.getTitulo() + "»? Esta acción no se puede deshacer.",
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

