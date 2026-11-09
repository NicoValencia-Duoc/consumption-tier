package consumption.ui;

import consumption.model.*;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.*;
import java.util.List;

public class VentanaPrincipal extends VBox{

    private final List<ItemConsumption> items;
    private final StackPane contenido = new StackPane();

    public VentanaPrincipal(List<ItemConsumption> items, Runnable alCambiarTema){
        this.items = items;
        setSpacing(14);
        setPadding(new Insets(20, 28, 20, 28));

        Cabecera cabecera = new Cabecera(this::mostrar, alCambiarTema);
        VBox.setVgrow(contenido, Priority.ALWAYS);
        getChildren().addAll(cabecera, contenido);

        mostrar("INICIO");
    }

    private void mostrar(String pestana){

        Node pagina = switch(pestana){
            case "JUEGOS" -> paginaJuegos();
            case "LIBROS" -> paginaLibros();
            case "PELICULAS" -> paginaPeliculas();
            default -> new PaginaInicio(items);
        };
        contenido.getChildren().setAll(pagina);
    }

    private Node paginaJuegos(){

        List<Juego> juegos = items.stream().filter(Juego.class::isInstance).map(Juego.class::cast).toList();
        PaginaTabla<Juego> p = new PaginaTabla<>("JUEGOS", juegos);
        p.agregarColumna("PLATAFORMA", Juego::getPlataforma);
        p.agregarColumna("GENERO", Juego::getGenero);
        p.agregarColumna("DESARROLLADORA", Juego::getDesarrollador);
        p.agregarColumna("AÑO", Juego::getAnio);
        p.agregarColumna("HORAS", Juego::getHorasJugadas);
        return p;
    }

    private Node paginaLibros(){

        List<Libro> libros = items.stream().filter(Libro.class::isInstance).map(Libro.class::cast).toList();
        PaginaTabla<Libro> p = new PaginaTabla<>("LIBROS", libros);
        p.agregarColumna("AUTOR", Libro::getAutor);
        p.agregarColumna("GENERO", Libro::getGenero);
        p.agregarColumna("PAGINAS", Libro::getPaginas);
        p.agregarColumna("AÑO", Libro::getAnio);
        return p;
    }

    private Node paginaPeliculas(){

        List<Pelicula> peliculas = items.stream().filter(Pelicula.class::isInstance).map(Pelicula.class::cast).toList();
        PaginaTabla<Pelicula> p = new PaginaTabla<>("PELICULAS", peliculas);
        p.agregarColumna("DIRECTOR", Pelicula::getDirector);
        p.agregarColumna("GENERO", Pelicula::getGenero);
        p.agregarColumna("DURACION (MIN)", Pelicula::getDuracionMin);
        p.agregarColumna("AÑO", Pelicula::getAnio);
        return p;
    }
}

