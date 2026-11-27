package consumption.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record ConsultaGeneros(String libre, List<String> claves, boolean valida) {
    private static final String OPERADOR = "genero:";
    public ConsultaGeneros { claves = List.copyOf(claves); }

    public static ConsultaGeneros analizar(String consulta) {
        String s = consulta == null ? "" : consulta;
        StringBuilder libre = new StringBuilder();
        List<String> claves = new ArrayList<>();
        int i = 0;
        boolean entreComillas = false;
        while (i < s.length()) {
            char c = s.charAt(i);
            boolean inicio = i == 0 || Character.isWhitespace(s.charAt(i - 1));
            if (!entreComillas && inicio && s.regionMatches(true, i, OPERADOR, 0, OPERADOR.length())) {
                i += OPERADOR.length();
                StringBuilder nombre = new StringBuilder();
                if (i == s.length() || Character.isWhitespace(s.charAt(i))) return invalida();
                if (s.charAt(i) == '"') {
                    i++;
                    boolean cerrada = false;
                    while (i < s.length()) {
                        c = s.charAt(i++);
                        if (c == '"') { cerrada = true; break; }
                        if (c == '\\') {
                            if (i == s.length()) return invalida();
                            c = s.charAt(i++);
                            if (c != '"' && c != '\\') return invalida();
                        }
                        nombre.append(c);
                    }
                    if (!cerrada || (i < s.length() && !Character.isWhitespace(s.charAt(i)))) return invalida();
                } else {
                    while (i < s.length() && !Character.isWhitespace(s.charAt(i))) {
                        c = s.charAt(i++);
                        if (c == '"' || c == '\\') return invalida();
                        nombre.append(c);
                    }
                }
                String clave = NombreGenero.clave(nombre.toString());
                if (clave.isEmpty()) return invalida();
                claves.add(clave);
                libre.append(' ');
            } else {
                if (entreComillas && c == '\\' && i + 1 < s.length()
                        && (s.charAt(i + 1) == '"' || s.charAt(i + 1) == '\\')) {
                    libre.append(c).append(s.charAt(i + 1));
                    i += 2;
                } else {
                    if (c == '"') entreComillas = !entreComillas;
                    libre.append(c);
                    i++;
                }
            }
        }
        String textoLibre = claves.isEmpty() ? s.strip() : libre.toString().strip().replaceAll("\\s+", " ");
        return new ConsultaGeneros(textoLibre, claves, true);
    }

    private static ConsultaGeneros invalida() { return new ConsultaGeneros("", List.of(), false); }

    private static List<String> claves(ItemConsumption item) {
        return item.getGeneros().stream().map(r -> NombreGenero.clave(r.nombre())).toList();
    }

    public boolean coincideTabla(ItemConsumption item) {
        if (!valida) return false;
        List<String> generos = claves(item);
        String claveLibre = NombreGenero.clave(libre);
        return generos.containsAll(claves) && (libre.isEmpty()
                || item.getTitulo().toLowerCase(Locale.ROOT).contains(libre.toLowerCase(Locale.ROOT))
                || (!claveLibre.isEmpty() && generos.stream().anyMatch(g -> g.contains(claveLibre))));
    }

    public boolean coincideInicio(ItemConsumption item) {
        if (!valida) return false;
        List<String> generos = claves(item);
        if (!generos.containsAll(claves)) return false;
        if (libre.isEmpty()) return true;
        String texto = ReglasInicio.normalizar(item.textoBusqueda());
        for (String palabra : libre.split("\\s+")) {
            String clave = NombreGenero.clave(palabra);
            if (!texto.contains(ReglasInicio.normalizar(palabra))
                    && (clave.isEmpty() || generos.stream().noneMatch(g -> g.contains(clave)))) return false;
        }
        return true;
    }
}