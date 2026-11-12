package consumption.data;

import consumption.model.*;

import com.google.gson.*;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RepositorioJson {
    private static final Path ARCHIVO =
            Rutas.DATOS;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new AdaptadorFecha().nullSafe())
            .setPrettyPrinting()
            .create();
    private static class Datos {
        List<Juego> juegos = new ArrayList<>();
        List<Libro> libros = new ArrayList<>();
        List<Pelicula> peliculas = new ArrayList<>();
        List<Serie> series = new ArrayList<>();
    }

    public List<ItemConsumption> cargar() {
        if (!Files.exists(ARCHIVO)) {
            return new ArrayList<>();
        }
        try (Reader lector = Files.newBufferedReader(ARCHIVO)) {
            Datos d = gson.fromJson(lector, Datos.class);
            List<ItemConsumption> todos = new ArrayList<>();
            if (d != null) {
                todos.addAll(d.juegos);
                todos.addAll(d.libros);
                todos.addAll(d.peliculas);
                if (d.series != null) todos.addAll(d.series);
            }
            return todos;
        } catch (IOException | JsonParseException e) {
            System.err.println("No se pudo leer el archivo: " + e.getMessage());
            respaldarDanado();
            return new ArrayList<>();
        }
    }

    public void guardar(List<ItemConsumption> items) {
        Datos d = new Datos();
        for (ItemConsumption i : items) {
            if (i instanceof Juego j) d.juegos.add(j);
            else if (i instanceof Libro l) d.libros.add(l);
            else if (i instanceof Pelicula p) d.peliculas.add(p);
            else if (i instanceof Serie s) d.series.add(s);
        }
        try {
            Files.createDirectories(ARCHIVO.getParent());
            try (Writer escritor = Files.newBufferedWriter(ARCHIVO)) {
                gson.toJson(d, escritor);
            }
        } catch (IOException e) {
            System.err.println("No se pudo guardar: " + e.getMessage());
        }
    }

    private void respaldarDanado() {
        try {
            Files.move(ARCHIVO, ARCHIVO.resolveSibling("datos-danado.json"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignorada) {
        }
    }
}
