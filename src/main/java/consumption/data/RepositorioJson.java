package consumption.data;

import consumption.model.*;
import com.google.gson.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RepositorioJson {
    private static final Path ARCHIVO = Rutas.DATOS;
    private static final int VERSION_ACTUAL = 1;
    private static final List<String> CATEGORIAS =
            List.of("juegos", "libros", "peliculas", "series");

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new AdaptadorFecha().nullSafe())
            .setPrettyPrinting()
            .create();

    private final JsonObject conservados = new JsonObject();
    private final List<String> avisos = new ArrayList<>();
    private boolean cargaCompletada = false;
    private final boolean modoGeneros;
    private BibliotecaGeneros biblioteca = BibliotecaGeneros.vacia();
    private byte[] instantanea;

    public RepositorioJson() { this(false); }
    private RepositorioJson(boolean modoGeneros) { this.modoGeneros = modoGeneros; }
    public static RepositorioJson paraGeneros() { return new RepositorioJson(true); }
    public boolean usaGeneros() { return modoGeneros; }
    public BibliotecaGeneros bibliotecaGeneros() { exigirGeneros(); return biblioteca; }
    private void exigirGeneros() {
        if (!modoGeneros || !cargaCompletada)
            throw new IllegalStateException("El repositorio de generos no esta cargado.");
    }

    public static final class Rechazado {
        private final String categoria;
        private final JsonElement elemento;
        private final String motivo;

        private Rechazado(String categoria, JsonElement elemento, String motivo) {
            this.categoria = categoria;
            this.elemento = elemento;
            this.motivo = motivo;
        }

        public String getCategoria() { return categoria; }
        public String getMotivo() { return motivo; }

        public String getResumen() {
            if (elemento.isJsonObject() && elemento.getAsJsonObject().has("titulo")) {
                JsonElement titulo = elemento.getAsJsonObject().get("titulo");
                if (titulo.isJsonPrimitive()) return titulo.getAsString();
            }
            return motivo.contains(":") ? motivo.substring(0, motivo.indexOf(':')) : motivo;
        }

        public String getTexto() {
            return new GsonBuilder().setPrettyPrinting().create().toJson(elemento);
        }
    }

    private final List<Rechazado> detalleRechazados = new ArrayList<>();

    public int cantidadRechazados() {
        return conservados.entrySet().stream()
                .filter(e -> e.getValue().isJsonArray())
                .mapToInt(e -> e.getValue().getAsJsonArray().size()).sum();
    }

    public boolean tieneRechazados() {
        return conservados.entrySet().stream().anyMatch(e -> e.getValue().isJsonArray()
                && e.getValue().getAsJsonArray().size() > 0);
    }

    public String huellaRechazados() {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            for (String categoria : CATEGORIAS) {
                JsonArray arr = conservados.getAsJsonArray(categoria);
                if (arr == null || arr.isEmpty()) continue;
                digest.update(categoria.getBytes(StandardCharsets.UTF_8));
                digest.update(arr.toString().getBytes(StandardCharsets.UTF_8));
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public Path exportarRechazados(Path destino) {
        if (!cargaCompletada || !tieneRechazados())
            throw new IllegalStateException("No hay registros rechazados disponibles para exportar.");
        JsonObject exportacion = new JsonObject();
        exportacion.addProperty("versionExportacion", 1);
        for (String categoria : CATEGORIAS) {
            JsonArray arr = conservados.getAsJsonArray(categoria);
            exportacion.add(categoria, arr == null ? new JsonArray() : arr.deepCopy());
        }
        Path temporal = null;
        try {
            Path padre = destino.toAbsolutePath().getParent();
            Files.createDirectories(padre);
            if (Files.exists(destino))
                throw new java.nio.file.FileAlreadyExistsException(destino.toString());
            temporal = Files.createTempFile(padre, "rechazados-", ".tmp");
            Files.writeString(temporal, gson.toJson(exportacion), StandardCharsets.UTF_8);
            try { Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temporal, destino); }
            return destino;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo exportar los registros rechazados.", ex);
        } finally {
            if (temporal != null) try { Files.deleteIfExists(temporal); }
            catch (IOException ex) { System.err.println("Temporal pendiente: " + temporal); }
        }
    }

    public List<Rechazado> getRechazados() {
        return List.copyOf(detalleRechazados);
    }

    public ItemConsumption interpretar(Rechazado rechazado, String texto) {
        Class<? extends ItemConsumption> tipo = switch (rechazado.categoria) {
            case "juegos" -> Juego.class;
            case "libros" -> Libro.class;
            case "peliculas" -> Pelicula.class;
            case "series" -> Serie.class;
            default -> throw new IllegalArgumentException("Categoria desconocida: " + rechazado.categoria);
        };
        JsonElement elemento;
        try {
            elemento = JsonParser.parseString(texto);
        } catch (JsonParseException ex) {
            throw new IllegalArgumentException("JSON mal formado: " + mensaje(ex));
        }
        if (!elemento.isJsonObject()) throw new IllegalArgumentException("No es un objeto JSON");
        try {
            ItemConsumption item = gson.fromJson(elemento, tipo);
            if (modoGeneros) {
                validar(item, false);
                JsonObject fila = elemento.getAsJsonObject();
                if (fila.has("genero") && !fila.get("genero").isJsonNull()
                        && (!fila.get("genero").isJsonPrimitive() || !fila.getAsJsonPrimitive("genero").isString()))
                    throw new IllegalArgumentException("Genero historico no textual.");
                if (fila.has(FormatoGeneros.REFERENCIAS)) {
                    if (fila.has("genero")) throw new IllegalArgumentException("Genero legado y referencias simultaneos.");
                    item.setGeneros(FormatoGeneros.leerReferencias(fila));
                }
            } else validar(item);
            return item;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(mensaje(ex));
        }
    }

    public void quitarRechazado(Rechazado rechazado) {
        JsonArray lista = conservados.getAsJsonArray(rechazado.categoria);
        if (lista == null || !lista.remove(rechazado.elemento)) {
            throw new IllegalStateException("El registro ya no esta pendiente.");
        }
        detalleRechazados.remove(rechazado);
    }

    public void reponerRechazado(Rechazado rechazado) {
        JsonArray lista = conservados.getAsJsonArray(rechazado.categoria);
        if (lista == null) {
            lista = new JsonArray();
            conservados.add(rechazado.categoria, lista);
        }
        lista.add(rechazado.elemento);
        if (!detalleRechazados.contains(rechazado)) detalleRechazados.add(rechazado);
    }

    public Path guardarCopiaDescartado(Rechazado rechazado) {
        try {
            Path carpeta = Rutas.BASE.resolve("descartados");
            Files.createDirectories(carpeta);
            String sello = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS"));
            Path destino = carpeta.resolve("descartado-" + rechazado.categoria + "-" + sello + ".json");
            Files.writeString(destino, rechazado.getTexto(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
            return destino;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar la copia del registro: " + ex.getMessage(), ex);
        }
    }

    public List<String> getAvisosCarga() {
        return List.copyOf(avisos);
    }

    public List<ItemConsumption> cargar() {
        if (modoGeneros) return cargarGeneros();
        avisos.clear();
        detalleRechazados.clear();
        conservados.entrySet().clear();
        cargaCompletada = false;
        if (!Files.exists(ARCHIVO)) {
            cargaCompletada = true;
            return new ArrayList<>();
        }

        try (Reader lector = Files.newBufferedReader(ARCHIVO, StandardCharsets.UTF_8)) {
            JsonElement raiz = JsonParser.parseReader(lector);
            if (!raiz.isJsonObject()) {
                throw new JsonParseException("La raiz no es un objeto JSON.");
            }
            JsonObject objeto = raiz.getAsJsonObject();
            int version = 0;
            if (objeto.has("versionFormato")) {
                JsonElement valor = objeto.get("versionFormato");
                if (!valor.isJsonPrimitive() || !valor.getAsJsonPrimitive().isNumber()) {
                    throw new JsonParseException("versionFormato no es numerico.");
                }
                version = valor.getAsInt();
            }
            if (version < 0 || version > VERSION_ACTUAL) {
                throw new JsonParseException("Version incompatible: " + version);
            }

            List<ItemConsumption> todos = new ArrayList<>();
            leerCategoria(objeto, "juegos", Juego.class, todos);
            leerCategoria(objeto, "libros", Libro.class, todos);
            leerCategoria(objeto, "peliculas", Pelicula.class, todos);
            leerCategoria(objeto, "series", Serie.class, todos);
            cargaCompletada = true;
            return todos;
        } catch (IOException | JsonParseException | IllegalStateException e) {
            throw new IllegalStateException(
                    "No se pudo cargar datos.json. No se ha sobrescrito. "
                            + "Conserva el archivo y sus respaldos.", e);
        }
    }

    private List<ItemConsumption> cargarGeneros() {
        Rutas.exigirGenerosHabilitados();
        cargaCompletada = false;
        avisos.clear(); detalleRechazados.clear(); conservados.entrySet().clear();
        try {
            return RecuperacionDatos.ejecutarProtegido(() -> {
                if (!Files.exists(ARCHIVO)) {
                    biblioteca = BibliotecaGeneros.vacia(); instantanea = null;
                    cargaCompletada = true; return new ArrayList<>();
                }
                byte[] bytes = Files.readAllBytes(ARCHIVO);
                var plan = prepararMigracionGeneros(bytes);
                if (plan.isPresent()) {
                    MigracionGeneros.aplicar(plan.get());
                    bytes = Files.readAllBytes(ARCHIVO);
                }
                JsonObject raiz = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                if (FormatoGeneros.version(raiz) != FormatoGeneros.VERSION)
                    throw new JsonParseException("Formato de generos incompatible.");
                BibliotecaGeneros nueva = FormatoGeneros.leerBiblioteca(raiz);
                List<ItemConsumption> items = new ArrayList<>();
                for (String categoria : CATEGORIAS) leerCategoriaGeneros(raiz, categoria, nueva, items);
                biblioteca = nueva; instantanea = bytes;
                cargaCompletada = true; return items;
            });
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cargar el formato de generos. Escritura bloqueada; conserva los datos.", ex);
        }
    }

    private void leerCategoriaGeneros(JsonObject raiz, String categoria, BibliotecaGeneros nueva,
                                      List<ItemConsumption> destino) {
        JsonElement valor = raiz.get(categoria);
        if (valor == null) return;
        if (!valor.isJsonArray()) throw new JsonParseException(categoria + " no es una lista JSON.");
        JsonArray rechazados = new JsonArray();
        int posicion = 0;
        for (JsonElement elemento : valor.getAsJsonArray()) {
            posicion++;
            try {
                if (!elemento.isJsonObject()) throw new IllegalArgumentException("No es un objeto JSON");
                JsonObject fila = elemento.getAsJsonObject();
                if (!fila.has(FormatoGeneros.REFERENCIAS) && !registroLegadoMigrable(categoria, fila.deepCopy()))
                    throw new IllegalArgumentException("Registro legado rechazado; requiere recuperacion expresa.");
                ItemConsumption item = gson.fromJson(fila, tipoCategoria(categoria));
                validar(item, false);
            } catch (RuntimeException ex) {
                JsonElement copia = elemento.deepCopy(); rechazados.add(copia);
                String motivo = "registro " + posicion + ": " + mensaje(ex);
                avisos.add(categoria + " " + motivo);
                detalleRechazados.add(new Rechazado(categoria, copia, motivo));
                continue;
            }
            destino.add(leerRegistroMigrado(categoria, elemento.getAsJsonObject(), nueva));
        }
        conservados.add(categoria, rechazados);
    }

    private <T extends ItemConsumption> void leerCategoria(
            JsonObject raiz, String nombre, Class<T> tipo, List<ItemConsumption> destino) {
        JsonElement valor = raiz.get(nombre);
        if (valor == null) return;
        if (!valor.isJsonArray()) {
            throw new JsonParseException(nombre + " no es una lista JSON.");
        }
        JsonArray rechazados = new JsonArray();
        int posicion = 0;
        for (JsonElement elemento : valor.getAsJsonArray()) {
            posicion++;
            try {
                if (!elemento.isJsonObject()) {
                    throw new IllegalArgumentException("No es un objeto JSON");
                }
                T item = gson.fromJson(elemento, tipo);
                validar(item);
                destino.add(item);
            } catch (RuntimeException ex) {
                JsonElement copia = elemento.deepCopy();
                rechazados.add(copia);
                String motivo = mensaje(ex);
                avisos.add(nombre + " [registro " + posicion + "]: " + motivo);
                detalleRechazados.add(new Rechazado(nombre, copia, "registro " + posicion + ": " + motivo));
            }
        }
        conservados.add(nombre, rechazados);
    }

    private String mensaje(RuntimeException ex) {
        String detalle = ex.getMessage();
        return detalle == null || detalle.isBlank()
                ? ex.getClass().getSimpleName() : detalle;
    }

    public java.util.Optional<MigracionGeneros.Plan> prepararMigracionGeneros(byte[] origen) throws IOException {
        var plan = MigracionGeneros.preparar(origen, this::registroLegadoMigrable);
        if (plan.isPresent()) {
            JsonObject anterior = JsonParser.parseString(new String(origen, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject migrado = plan.get().datosMigrados();
            BibliotecaGeneros biblioteca = FormatoGeneros.leerBiblioteca(migrado);
            for (String categoria : CATEGORIAS) {
                if (!anterior.has(categoria)) continue;
                JsonArray antes = anterior.getAsJsonArray(categoria);
                JsonArray despues = migrado.getAsJsonArray(categoria);
                if (antes.size() != despues.size()) throw new JsonParseException("Cantidad migrada incoherente.");
                for (int indice = 0; indice < antes.size(); indice++) {
                    JsonElement a = antes.get(indice), b = despues.get(indice);
                    if (a.isJsonObject() && registroLegadoMigrable(categoria, a.getAsJsonObject().deepCopy())) {
                        leerRegistroMigrado(categoria, b.getAsJsonObject(), biblioteca);
                        JsonObject copiaAntes = a.getAsJsonObject().deepCopy();
                        JsonObject copiaDespues = b.getAsJsonObject().deepCopy();
                        copiaAntes.remove("genero");
                        copiaDespues.remove(FormatoGeneros.REFERENCIAS);
                        if (!copiaAntes.equals(copiaDespues))
                            throw new JsonParseException("La migracion altero otros campos del registro.");
                    } else if (!a.equals(b)) {
                        throw new JsonParseException("La migracion altero un registro rechazado.");
                    }
                }
            }
        }
        return plan;
    }

    private static Class<? extends ItemConsumption> tipoCategoria(String categoria) {
        return switch (categoria) {
            case "juegos" -> Juego.class;
            case "libros" -> Libro.class;
            case "peliculas" -> Pelicula.class;
            case "series" -> Serie.class;
            default -> throw new IllegalArgumentException("Categoria desconocida: " + categoria);
        };
    }

    private boolean registroLegadoMigrable(String categoria, JsonObject fila) {
        JsonElement genero = fila.get("genero");
        if (genero != null && !genero.isJsonNull()
                && (!genero.isJsonPrimitive() || !genero.getAsJsonPrimitive().isString())) return false;
        try {
            fila.remove("genero");
            ItemConsumption item = gson.fromJson(fila, tipoCategoria(categoria));
            validar(item, false);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    ItemConsumption leerRegistroMigrado(String categoria, JsonObject fila, BibliotecaGeneros biblioteca) {
        if (fila.has("genero")) throw new JsonParseException("Registro con genero legado sin resolver.");
        List<ReferenciaGenero> referencias = FormatoGeneros.leerReferencias(fila);
        ItemConsumption item = gson.fromJson(fila, tipoCategoria(categoria));
        validar(item, false);
        biblioteca.resolver(AmbitoGenero.deTipo(item.getTipo()), referencias);
        item.setGeneros(referencias);
        return item;
    }

    private void validar(ItemConsumption i) {
        validar(i, true);
    }

    private void validar(ItemConsumption i, boolean generoLegado) {
        if (i == null) throw new IllegalArgumentException("Registro vacio");
        i.setTitulo(i.getTitulo());
        i.setEstado(i.getEstado());
        i.setNota(i.getNota());
        i.setPorcentaje(i.getPorcentaje());
        i.setComentario(i.getComentario());
        i.setConQuien(i.getConQuien());
        if (i.getVecesConsumido() < 0 || i.getVecesConsumido() > ItemConsumption.MAX_VECES) {
            throw new IllegalArgumentException("Contador fuera de rango");
        }
        for (var criterio : i.getCriterios().entrySet()) {
            String nombre = criterio.getKey();
            Double valor = criterio.getValue();
            if (!i.getNombresCriterios().contains(nombre) || valor == null
                    || !Double.isFinite(valor) || valor < 0 || valor > 10) {
                throw new IllegalArgumentException("Criterio invalido: " + nombre);
            }
        }
        if (!Double.isFinite(i.getNota()) || !Double.isFinite(i.getPorcentaje())) {
            throw new IllegalArgumentException("Nota o avance no finito");
        }
        if (i instanceof Juego j) {
            j.setPlataforma(j.getPlataforma());
            if (generoLegado) j.setGenero(j.getGenero());
            j.setDesarrollador(j.getDesarrollador());
            j.setAnio(j.getAnio());
            j.setHorasJugadas(j.getHorasJugadas());
            if (!Double.isFinite(j.getHorasJugadas()))
                throw new IllegalArgumentException("Horas no finitas");
        } else if (i instanceof Libro l) {
            l.setAutor(l.getAutor());
            if (generoLegado) l.setGenero(l.getGenero());
            l.setPaginas(l.getPaginas());
            l.setAnio(l.getAnio());
        } else if (i instanceof Pelicula p) {
            p.setDirector(p.getDirector());
            if (generoLegado) p.setGenero(p.getGenero());
            p.setDuracionMin(p.getDuracionMin());
            p.setAnio(p.getAnio());
        } else if (i instanceof Serie s) {
            s.setCreador(s.getCreador());
            if (generoLegado) s.setGenero(s.getGenero());
            s.setAnio(s.getAnio());
            s.setTemporadas(s.getTemporadas());
            s.setEpisodios(s.getEpisodios());
            s.setOrigen(s.getOrigen());
        }
        i.validarCoherencia();
    }

    public void guardar(List<ItemConsumption> items) {
        BarreraEscritura.escritura(() -> guardarInterno(items));
    }

    private void guardarInterno(List<ItemConsumption> items) {
        if (!cargaCompletada) {
            throw new IllegalStateException(
                    "Primero debe completarse la carga antes de guardar.");
        }
        if (modoGeneros) {
            guardarGeneros(items, biblioteca, java.util.Map.of());
            return;
        }
        if (items.stream().anyMatch(ItemConsumption::tieneGenerosMigrados))
            throw new IllegalStateException("No se pueden guardar referencias de genero en el formato legado.");
        JsonObject datos = new JsonObject();
        datos.addProperty("versionFormato", VERSION_ACTUAL);
        for (String categoria : CATEGORIAS) {
            JsonArray lista = new JsonArray();
            for (ItemConsumption i : items) {
                if (pertenece(i, categoria)) lista.add(gson.toJsonTree(i));
            }
            JsonArray pendientes = conservados.getAsJsonArray(categoria);
            if (pendientes != null) {
                for (JsonElement elemento : pendientes) lista.add(elemento.deepCopy());
            }
            datos.add(categoria, lista);
        }

        Path temporal = null;
        try {
            Files.createDirectories(ARCHIVO.getParent());
            temporal = Files.createTempFile(ARCHIVO.getParent(), "datos-", ".tmp");
            try (Writer escritor = Files.newBufferedWriter(temporal, StandardCharsets.UTF_8)) {
                gson.toJson(datos, escritor);
            }
            try {
                Files.move(temporal, ARCHIVO,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporal, ARCHIVO, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar datos.json.", e);
        } finally {
            if (temporal != null) {
                try { Files.deleteIfExists(temporal); }
                catch (IOException e) {
                    System.err.println("No se pudo limpiar temporal: " + temporal);
                }
            }
        }
    }

    static byte[] prepararRespaldoGeneros(byte[] bytes) throws IOException {
        RepositorioJson lector = paraGeneros();
        var plan = lector.prepararMigracionGeneros(bytes);
        JsonObject raiz = plan.isPresent() ? plan.get().datosMigrados()
                : JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        BibliotecaGeneros nueva = FormatoGeneros.leerBiblioteca(raiz);
        List<ItemConsumption> items = new ArrayList<>();
        for (String categoria : CATEGORIAS) lector.leerCategoriaGeneros(raiz, categoria, nueva, items);
        return plan.isPresent() ? lector.gson.toJson(raiz).getBytes(StandardCharsets.UTF_8) : bytes.clone();
    }

    public static MigracionGeneros.Plan prepararReconstruccionGeneros(byte[] origen) throws IOException {
        RepositorioJson lector = paraGeneros();
        String texto = StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(origen)).toString();
        JsonObject raiz = JsonParser.parseString(texto).getAsJsonObject();
        if (FormatoGeneros.version(raiz) != FormatoGeneros.VERSION)
            throw new IllegalArgumentException("La reconstruccion requiere formato 2.");
        boolean danada = false;
        try { FormatoGeneros.leerBiblioteca(raiz); }
        catch (JsonParseException ex) { danada = true; }
        if (!danada) throw new IllegalArgumentException("La biblioteca se puede leer; no corresponde reconstruirla.");
        List<ItemConsumption> activos = new ArrayList<>();
        for (String categoria : CATEGORIAS) {
            JsonElement valor = raiz.get(categoria);
            if (valor == null) continue;
            if (!valor.isJsonArray()) throw new JsonParseException("Categoria invalida: " + categoria);
            for (JsonElement elemento : valor.getAsJsonArray()) {
                if (!elemento.isJsonObject()) continue;
                JsonObject fila = elemento.getAsJsonObject();
                if (!fila.has(FormatoGeneros.REFERENCIAS) && !lector.registroLegadoMigrable(categoria, fila.deepCopy())) continue;
                ItemConsumption item;
                try { item = lector.gson.fromJson(fila, tipoCategoria(categoria)); lector.validar(item, false); }
                catch (RuntimeException ex) { continue; }
                if (fila.has("genero")) throw new JsonParseException("Registro activo con formato ambiguo.");
                item.setGeneros(FormatoGeneros.leerReferencias(fila));
                activos.add(item);
            }
        }
        BibliotecaGeneros reconstruida = FormatoGeneros.reconstruir(activos);
        JsonObject destino = raiz.deepCopy();
        destino.add(FormatoGeneros.BIBLIOTECA, FormatoGeneros.escribirBiblioteca(reconstruida));
        List<ItemConsumption> comprobados = new ArrayList<>();
        for (String categoria : CATEGORIAS) lector.leerCategoriaGeneros(destino, categoria, reconstruida, comprobados);
        return MigracionGeneros.planReconstruccion(origen, destino);
    }

    public RecuperacionGeneros.Plan prepararRecuperacion(ItemConsumption historico) {
        exigirGeneros();
        return RecuperacionGeneros.preparar(biblioteca, historico);
    }

    public ItemConsumption copiarParaRecuperacion(ItemConsumption historico) {
        ItemConsumption copia = gson.fromJson(gson.toJsonTree(historico), historico.getClass());
        if (modoGeneros) validar(copia, false);
        if (historico.tieneGenerosMigrados()) copia.setGeneros(historico.getGeneros());
        return copia;
    }

    public void guardarRecuperacion(List<ItemConsumption> nuevos, ItemConsumption recuperado,
                                    RecuperacionGeneros.Plan plan, java.util.Set<Integer> autorizados) {
        BarreraEscritura.escritura(() -> {
            exigirGeneros();
            if (plan.biblioteca() != biblioteca) throw new IllegalStateException("La biblioteca cambio; revisa la recuperacion.");
            if (!plan.corresponde(recuperado)) throw new IllegalArgumentException("El plan no corresponde al registro.");
            if (nuevos.stream().filter(i -> i == recuperado).count() != 1)
                throw new IllegalArgumentException("Registro recuperado no incluido de forma unica.");
            var resultado = plan.resolver(autorizados);
            var cambios = new java.util.IdentityHashMap<ItemConsumption, List<ReferenciaGenero>>();
            cambios.put(recuperado, resultado.referencias());
            guardarGeneros(nuevos, resultado.biblioteca(), cambios);
        });
    }

    public record RecuperacionSolicitada(ItemConsumption item, RecuperacionGeneros.Plan plan,
                                         java.util.Set<Integer> autorizados) {
        public RecuperacionSolicitada { autorizados = java.util.Set.copyOf(autorizados); }
    }

    public void guardarRecuperaciones(List<ItemConsumption> nuevos, List<RecuperacionSolicitada> solicitudes) {
        BarreraEscritura.escritura(() -> {
            exigirGeneros();
            BibliotecaGeneros acumulada = biblioteca;
            var cambios = new java.util.IdentityHashMap<ItemConsumption, List<ReferenciaGenero>>();
            for (var solicitud : solicitudes) {
                var item = solicitud.item();
                var plan = solicitud.plan();
                if (plan.biblioteca() != biblioteca || !plan.corresponde(item)
                        || nuevos.stream().filter(i -> i == item).count() != 1 || cambios.containsKey(item))
                    throw new IllegalArgumentException("Plan de recuperacion de lote invalido o desactualizado.");
                var resultado = plan.resolverSobre(acumulada, solicitud.autorizados());
                acumulada = resultado.biblioteca();
                cambios.put(item, resultado.referencias());
            }
            guardarGeneros(nuevos, acumulada, cambios);
        });
    }

    public List<ReferenciaGenero> recuperarGenerosBorrador(RecuperacionGeneros.Plan plan,
                                                           java.util.Set<Integer> autorizados, List<ItemConsumption> guardados) {
        var resultado = new java.util.concurrent.atomic.AtomicReference<List<ReferenciaGenero>>();
        BarreraEscritura.escritura(() -> {
            exigirGeneros();
            if (plan.biblioteca() != biblioteca) throw new IllegalStateException("La biblioteca cambio; revisa el borrador.");
            var resuelto = plan.resolver(autorizados);
            if (resuelto.biblioteca() != biblioteca) guardarGeneros(guardados, resuelto.biblioteca(), java.util.Map.of());
            resultado.set(resuelto.referencias());
        });
        return resultado.get();
    }

    public void recuperarRechazado(Rechazado rechazado, ItemConsumption recuperado,
                                   List<ItemConsumption> guardados, RecuperacionGeneros.Plan plan,
                                   java.util.Set<Integer> autorizados) {
        BarreraEscritura.escritura(() -> {
            exigirGeneros();
            List<ItemConsumption> nuevos = new ArrayList<>(guardados);
            nuevos.add(recuperado);
            JsonArray lista = conservados.getAsJsonArray(rechazado.categoria);
            if (lista == null || !detalleRechazados.contains(rechazado))
                throw new IllegalStateException("El rechazado ya no existe.");
            JsonArray anterior = lista.deepCopy();
            List<Rechazado> detalles = List.copyOf(detalleRechazados);
            quitarRechazado(rechazado);
            try { guardarRecuperacion(nuevos, recuperado, plan, autorizados); }
            catch (RuntimeException ex) {
                conservados.add(rechazado.categoria, anterior);
                detalleRechazados.clear(); detalleRechazados.addAll(detalles);
                throw ex;
            }
            guardados.add(recuperado);
        });
    }

    public void invalidarCargaGeneros() { if (modoGeneros) cargaCompletada = false; }

    public TagGenero altaGenero(AmbitoGenero ambito, String nombre, List<ItemConsumption> guardados) {
        TagGenero[] resultado = new TagGenero[1];
        BarreraEscritura.escritura(() -> {
            exigirGeneros();
            var alta = biblioteca.alta(ambito, nombre);
            if (alta.biblioteca() != biblioteca) guardarGeneros(guardados, alta.biblioteca(), java.util.Map.of());
            resultado[0] = alta.tag();
        });
        return resultado[0];
    }

    public void bajaGenero(AmbitoGenero ambito, java.util.UUID id, List<ItemConsumption> guardados) {
        BarreraEscritura.escritura(() -> {
            exigirGeneros();
            BibliotecaGeneros nueva = biblioteca.baja(ambito, id);
            var cambios = new java.util.IdentityHashMap<ItemConsumption, List<ReferenciaGenero>>();
            for (ItemConsumption item : guardados) {
                if (AmbitoGenero.deTipo(item.getTipo()) != ambito) continue;
                var referencias = item.getGeneros();
                var restantes = referencias.stream().filter(r -> !r.id().equals(id)).toList();
                if (restantes.size() != referencias.size()) cambios.put(item, restantes);
            }
            guardarGeneros(guardados, nueva, cambios);
        });
    }

    private void guardarGeneros(List<ItemConsumption> items, BibliotecaGeneros nueva,
                                java.util.Map<ItemConsumption, List<ReferenciaGenero>> cambios) {
        exigirGeneros();
        Rutas.exigirGenerosHabilitados();
        var unicos = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<ItemConsumption, Boolean>());
        for (ItemConsumption item : items) {
            if (item == null || !unicos.add(item) || CATEGORIAS.stream().noneMatch(c -> pertenece(item, c)))
                throw new IllegalArgumentException("Registro activo duplicado o de tipo no reconocido.");
        }
        if (!unicos.containsAll(cambios.keySet())) throw new IllegalArgumentException("Cambio de genero sin registro activo.");
        JsonObject datos = instantanea == null ? new JsonObject()
                : JsonParser.parseString(new String(instantanea, StandardCharsets.UTF_8)).getAsJsonObject();
        datos.addProperty("versionFormato", FormatoGeneros.VERSION);
        datos.add(FormatoGeneros.BIBLIOTECA, FormatoGeneros.escribirBiblioteca(nueva));
        for (String categoria : CATEGORIAS) {
            JsonArray lista = new JsonArray();
            for (ItemConsumption item : items) {
                if (!pertenece(item, categoria)) continue;
                if (!item.tieneGenerosMigrados() && !cambios.containsKey(item)) throw new IllegalArgumentException("Registro sin referencias de generos.");
                var referencias = cambios.getOrDefault(item, item.getGeneros());
                nueva.resolver(AmbitoGenero.deTipo(item.getTipo()), referencias);
                JsonObject fila = gson.toJsonTree(item).getAsJsonObject();
                fila.remove("genero");
                fila.add(FormatoGeneros.REFERENCIAS, FormatoGeneros.escribirReferencias(referencias));
                lista.add(fila);
            }
            JsonArray rechazados = conservados.getAsJsonArray(categoria);
            if (rechazados != null) for (JsonElement elemento : rechazados) lista.add(elemento.deepCopy());
            datos.add(categoria, lista);
        }
        byte[] bytes = gson.toJson(datos).getBytes(StandardCharsets.UTF_8);
        Path temporal = null;
        try {
            Files.createDirectories(ARCHIVO.getParent());
            if (!java.util.Arrays.equals(instantanea, Files.exists(ARCHIVO) ? Files.readAllBytes(ARCHIVO) : null))
                throw new IOException("Los datos cambiaron desde la ultima carga. Recarga antes de guardar.");
            temporal = Files.createTempFile(ARCHIVO.getParent(), "datos-generos-", ".tmp");
            try (var canal = java.nio.channels.FileChannel.open(temporal, StandardOpenOption.WRITE)) {
                var buffer = java.nio.ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) canal.write(buffer);
                canal.force(true);
            }
            if (!java.util.Arrays.equals(bytes, Files.readAllBytes(temporal)))
                throw new IOException("No se pudo verificar la escritura temporal.");
            if (!java.util.Arrays.equals(instantanea, Files.exists(ARCHIVO) ? Files.readAllBytes(ARCHIVO) : null))
                throw new IOException("Los datos cambiaron durante la escritura. Recarga antes de guardar.");
            Files.move(temporal, ARCHIVO, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            instantanea = bytes; biblioteca = nueva;
            cambios.forEach(ItemConsumption::setGeneros);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar el formato de generos.", ex);
        } finally {
            if (temporal != null) try { Files.deleteIfExists(temporal); }
            catch (IOException ex) { System.err.println("Temporal pendiente: " + temporal); }
        }
    }

    private boolean pertenece(ItemConsumption item, String categoria) {
        return switch (categoria) {
            case "juegos" -> item instanceof Juego;
            case "libros" -> item instanceof Libro;
            case "peliculas" -> item instanceof Pelicula;
            case "series" -> item instanceof Serie;
            default -> false;
        };
    }
}