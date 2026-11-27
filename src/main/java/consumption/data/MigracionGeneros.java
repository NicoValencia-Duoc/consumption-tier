package consumption.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.BiPredicate;

public final class MigracionGeneros {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private MigracionGeneros() {}

    public static final class Plan {
        private final byte[] origen;
        private final byte[] destino;
        private Plan(byte[] origen, byte[] destino) {
            this.origen = origen.clone();
            this.destino = destino.clone();
        }
        public String sha256Origen() { return sha256(origen); }
        public String sha256Destino() { return sha256(destino); }
        public JsonObject datosMigrados() {
            return JsonParser.parseString(new String(destino, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    public static Optional<Plan> preparar(byte[] bytes, BiPredicate<String, JsonObject> registroValido)
            throws IOException {
        byte[] original = bytes.clone();
        String texto = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(original)).toString();
        JsonObject raiz = JsonParser.parseString(texto).getAsJsonObject();
        int version = FormatoGeneros.version(raiz);
        JsonObject destino = FormatoGeneros.migrar(raiz, registroValido);
        if (version == FormatoGeneros.VERSION) return Optional.empty();
        return Optional.of(new Plan(original, GSON.toJson(destino).getBytes(StandardCharsets.UTF_8)));
    }

    static Plan planReconstruccion(byte[] origen, JsonObject destino) {
        return new Plan(origen, GSON.toJson(destino).getBytes(StandardCharsets.UTF_8));
    }

    public static Path aplicar(Plan plan) {
        Rutas.exigirGenerosHabilitados();
        Path[] respaldo = new Path[1];
        BarreraEscritura.escritura(() -> {
            try { respaldo[0] = aplicarBloqueado(plan); }
            catch (IOException ex) { throw new IllegalStateException("No se pudo migrar datos.json.", ex); }
        });
        return respaldo[0];
    }

    private static Path aplicarBloqueado(Plan plan) throws IOException {
        Path original = Rutas.DATOS;
        Path carpeta = original.getParent();
        comprobarBytes(original, plan.origen, "Los datos cambiaron desde la preparacion de la migracion.");
        JsonObject datos = plan.datosMigrados();
        if (FormatoGeneros.version(datos) != FormatoGeneros.VERSION)
            throw new IOException("Destino de migracion incompatible.");
        FormatoGeneros.leerBiblioteca(datos);
        Path respaldo = Files.createTempFile(carpeta, "datos-antes-generos-", ".json");
        boolean respaldoVerificado = false;
        Path temporal = null;
        try {
            escribirDurable(respaldo, plan.origen);
            comprobarBytes(respaldo, plan.origen, "No se pudo verificar el respaldo previo.");
            respaldoVerificado = true;
            temporal = Files.createTempFile(carpeta, "datos-migracion-generos-", ".tmp");
            escribirDurable(temporal, plan.destino);
            comprobarBytes(temporal, plan.destino, "No se pudo verificar el destino temporal.");
            comprobarBytes(original, plan.origen, "Los datos cambiaron durante la migracion.");
            Files.move(temporal, original, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return respaldo;
        } finally {
            if (!respaldoVerificado) limpiar(respaldo);
            if (temporal != null) limpiar(temporal);
        }
    }

    private static void escribirDurable(Path archivo, byte[] bytes) throws IOException {
        try (FileChannel canal = FileChannel.open(archivo, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) canal.write(buffer);
            canal.force(true);
        }
    }
    private static void comprobarBytes(Path archivo, byte[] esperado, String mensaje) throws IOException {
        if (!Arrays.equals(Files.readAllBytes(archivo), esperado)) throw new IOException(mensaje);
    }
    private static void limpiar(Path archivo) {
        try { Files.deleteIfExists(archivo); }
        catch (IOException ex) { System.err.println("Temporal pendiente: " + archivo); }
    }
    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
