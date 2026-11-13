package consumption.ui;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class ReglasForm {

    private static final String[] MESES = {
            "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
            "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"};

    private static final DateTimeFormatter LEER =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter MOSTRAR = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private ReglasForm() {
    }

    public static LocalDate leerFecha(String texto) {
        String t = (texto == null) ? "" : texto.trim();
        if (t.isEmpty()) return null;
        try {
            return LocalDate.parse(t, LEER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Fecha invalida.");
        }
    }

    public static String formatoFecha(LocalDate fecha) {
        return (fecha == null) ? "" : fecha.format(MOSTRAR);
    }

    public static String nombreMes(Month mes) {
        return MESES[mes.getValue() - 1];
    }

    public static int desfase(YearMonth mes) {
        return mes.atDay(1).getDayOfWeek().getValue() - 1;
    }

    public static Double promedio(List<Double> valores) {
        var calificados = valores.stream().filter(Objects::nonNull).mapToDouble(Double::doubleValue);
        var media = calificados.average();
        return media.isPresent() ? Math.round(media.getAsDouble() * 10) / 10.0 : null;
    }

    public static String numero(double valor) {
        return (valor == Math.rint(valor)) ? String.valueOf((long) valor) : String.valueOf(valor);
    }

    public static int indiceDeCampo(String mensaje, List<String> etiquetas) {
        String m = sinTildes(mensaje);
        for (int i = 0; i < etiquetas.size(); i++) {
            String[] palabras = sinTildes(etiquetas.get(i)).split("[^a-z0-9]+");
            String palabra = "";
            for (String p : palabras) {
                if (!p.isEmpty()) {
                    palabra = p;
                    break;
                }
            }
            if (palabra.length() < 3) continue;
            String raiz = (palabra.length() > 4 && palabra.endsWith("s"))
                    ? palabra.substring(0, palabra.length() - 1) : palabra;
            if (Pattern.compile("\\b" + Pattern.quote(raiz) + "\\w*").matcher(m).find()) return i;
        }
        return -1;
    }

    private static String sinTildes(String texto) {
        String t = (texto == null) ? "" : texto;
        return Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }
}