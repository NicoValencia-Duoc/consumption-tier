package consumption.model;

import java.util.Objects;
import java.util.UUID;

public record TagGenero(UUID id, AmbitoGenero ambito, String nombre, boolean activo) {
    public TagGenero {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(ambito, "ambito");
        nombre = NombreGenero.historico(nombre);
    }
    public String clave() { return NombreGenero.clave(nombre); }
    public String etiqueta() { return NombreGenero.mostrar(nombre); }
    TagGenero darDeBaja() { return new TagGenero(id, ambito, nombre, false); }
}