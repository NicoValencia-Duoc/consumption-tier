package consumption.model;

import java.text.Normalizer;
import java.util.Locale;

public final class NombreGenero {
    public static final int MAX_NUEVO = 40;
    private NombreGenero() {}

    public static String clave(String nombre) {
        if (nombre == null) return "";
        String texto = Normalizer.normalize(nombre.strip(), Normalizer.Form.NFC);
        texto = Normalizer.normalize(texto.toUpperCase(Locale.ROOT), Normalizer.Form.NFC);
        StringBuilder resultado = new StringBuilder();
        texto.codePoints().forEach(cp -> {
            if (cp == 'Ñ') {
                resultado.appendCodePoint(cp);
            } else {
                Normalizer.normalize(new String(Character.toChars(cp)), Normalizer.Form.NFD)
                        .codePoints().filter(c -> {
                            int tipo = Character.getType(c);
                            return tipo != Character.NON_SPACING_MARK && tipo != Character.COMBINING_SPACING_MARK
                                    && tipo != Character.ENCLOSING_MARK;
                        }).forEach(resultado::appendCodePoint);
            }
        });
        return resultado.toString().strip();
    }

    public static String mostrar(String nombre) { return clave(nombre); }

    public static String historico(String nombre) {
        String valor = nombre == null ? "" : nombre.strip();
        if (clave(valor).isEmpty()) throw new IllegalArgumentException("El genero esta vacio.");
        return valor;
    }

    public static String nuevo(String nombre) {
        String valor = historico(nombre);
        if (valor.codePointCount(0, valor.length()) > MAX_NUEVO)
            throw new IllegalArgumentException("El genero admite maximo 40 caracteres.");
        if (valor.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("El genero contiene caracteres de control.");
        return valor;
    }
}
