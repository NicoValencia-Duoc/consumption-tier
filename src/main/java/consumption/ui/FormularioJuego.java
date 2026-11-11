package consumption.ui;

import consumption.model.Estado;
import consumption.model.Juego;

import javafx.scene.control.TextField;
import java.util.List;

public class FormularioJuego extends FormularioItem<Juego> {

    private final TextField plataforma = new TextField();
    private final TextField genero = new TextField();
    private final TextField desarrollador = new TextField();
    private final TextField anio = new TextField();
    private final TextField horas = new TextField();

    @Override protected List<String> nombresCriterios() { return Juego.CRITERIOS; }

    @Override protected String nombreTipo() { return "JUEGO"; }

    @Override protected void agregarCamposPropios() {
        agregarFila("PLATAFORMA", plataforma);
        agregarFila("GÉNERO", genero);
        agregarFila("DESARROLLADOR", desarrollador);
        agregarFila("AÑO", anio);
        agregarFila("HORAS JUGADAS", horas);
    }

    @Override protected void cargarPropios(Juego j) {
        plataforma.setText(j.getPlataforma());
        genero.setText(j.getGenero());
        desarrollador.setText(j.getDesarrollador());
        anio.setText(String.valueOf(j.getAnio()));
        horas.setText(String.valueOf(j.getHorasJugadas()));
    }

    @Override protected Juego crear(String titulo, double nota, Estado estado) {
        return new Juego(titulo, nota, estado, texto(plataforma), texto(genero), texto(desarrollador),
                entero(anio, "El año"), decimal(horas, "Las horas"));
    }

    @Override protected void aplicarPropios(Juego j) {
        j.setPlataforma(texto(plataforma));
        j.setGenero(texto(genero));
        j.setDesarrollador(texto(desarrollador));
        j.setAnio(entero(anio, "El año"));
        j.setHorasJugadas(decimal(horas, "Las horas"));
    }
}
