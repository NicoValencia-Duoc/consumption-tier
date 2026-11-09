package consumption;

import consumption.model.*;

import java.time.LocalDate;
import java.util.List;

public class DatosEjemplo {

    public static List<ItemConsumption> crear() {
        return List.of(
                con(new Juego("Juego ejemplo A", 9.4, Estado.TERMINADO, "PC", "RPG", "Estudio A", 2020, 85.5),
                        120, "2025-01-10", "2025-02-20"),
                con(new Pelicula("Película ejemplo B", 9.1, Estado.TERMINADO, "Director B", "Ciencia ficción", 148, 2010),
                        100, "2025-03-02", "2025-03-02"),
                con(new Libro("Libro ejemplo C", 8.8, Estado.EN_PROGRESO, "Autor C", "Fantasía", 420, 2015),
                        60, "2025-05-01", null),
                con(new Juego("Juego ejemplo D", 8.5, Estado.TERMINADO, "PS5", "Acción", "Estudio D", 2018, 40),
                        100, "2024-11-05", "2024-11-30"),
                con(new Pelicula("Película ejemplo E", 7.9, Estado.TERMINADO, "Director E", "Drama", 120, 2005),
                        100, "2025-04-12", "2025-04-12"),
                con(new Libro("Libro ejemplo F", 7.2, Estado.ABANDONADO, "Autor F", "Ensayo", 300, 2001),
                        35, "2025-02-01", null),
                con(new Juego("Juego ejemplo G", 6.8, Estado.EN_PROGRESO, "Switch", "Aventura", "Estudio G", 2022, 12),
                        45, "2025-06-15", null),
                con(new Pelicula("Película ejemplo H", 6.0, Estado.PENDIENTE, "Director H", "Comedia", 95, 2019),
                        0, null, null));
    }

    private static <T extends ItemConsumption> T con (T item, double porcentaje, String inicio, String termino){
        item.setPorcentaje(porcentaje);
        if (inicio != null)
            item.setFechaInicio(LocalDate.parse(inicio));
        if (termino != null)
            item.setFechaTermino(LocalDate.parse(termino));
        return item;
    }
}
