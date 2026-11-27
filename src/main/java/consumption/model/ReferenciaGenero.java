package consumption.model;

import java.util.Objects;
import java.util.UUID;

public record ReferenciaGenero(UUID id, String nombre) {
    public ReferenciaGenero {
        Objects.requireNonNull(id, "id");
        nombre = NombreGenero.historico(nombre);
    }
    public static ReferenciaGenero de(TagGenero tag) {
        return new ReferenciaGenero(tag.id(), tag.nombre());
    }
}
