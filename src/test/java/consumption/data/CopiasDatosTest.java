package consumption.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import consumption.model.Estado;
import consumption.model.ItemConsumption;
import consumption.model.Juego;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class CopiasDatosTest {
    @BeforeAll static void aislar() throws Exception {
        System.setProperty("user.home", Files.createTempDirectory("maw-junit-home-").toString());
    }

    private static Juego juego(String titulo) {
        return new Juego(titulo, 8.0, Estado.PENDIENTE, "PC", "Accion", "Estudio", 2020, 0);
    }

    private static void limpiar() throws IOException {
        Files.createDirectories(Rutas.BASE);
        Files.deleteIfExists(Rutas.DATOS);
        Files.deleteIfExists(Rutas.PAPELERA);
        if (Files.isDirectory(Rutas.RESPALDOS)) {
            try (Stream<Path> archivos = Files.list(Rutas.RESPALDOS)) {
                for (Path archivo : (Iterable<Path>) archivos::iterator) Files.deleteIfExists(archivo);
            }
        }
    }

    private static void guardarTitulos(String... titulos) throws IOException {
        RepositorioJson repo = new RepositorioJson();
        repo.cargar();
        List<ItemConsumption> items = new java.util.ArrayList<>();
        for (String titulo : titulos) items.add(juego(titulo));
        repo.guardar(items);
    }

    private static Path zipTemporal() throws IOException {
        return Files.createTempDirectory("maw-zip-").resolve("copia.zip");
    }

    @Test void papeleraEnviaYConservaEntradasAlRecargar() throws Exception {
        limpiar();
        Papelera papelera = new Papelera();
        papelera.enviar(List.of(juego("Uno"), juego("Dos")));
        assertEquals(2, papelera.listar().size());
        Papelera recargada = new Papelera();
        assertEquals(2, recargada.listar().size());
        assertEquals("Uno", recargada.listar().get(0).item().getTitulo());
    }

    @Test void papeleraRetiraSoloLasEntradasPedidas() throws Exception {
        limpiar();
        Papelera papelera = new Papelera();
        papelera.enviar(List.of(juego("Uno"), juego("Dos")));
        String idUno = papelera.listar().get(0).id();
        papelera.retirar(List.of(idUno));
        assertEquals(1, papelera.listar().size());
        assertEquals("Dos", new Papelera().listar().get(0).item().getTitulo());
    }

    @Test void papeleraDepuraSoloLasVencidas() throws Exception {
        limpiar();
        Papelera papelera = new Papelera();
        papelera.enviar(List.of(juego("Vieja"), juego("Reciente")));
        JsonObject raiz = JsonParser.parseString(Files.readString(Rutas.PAPELERA)).getAsJsonObject();
        JsonArray entradas = raiz.getAsJsonArray("entradas");
        LocalDateTime vencida = LocalDateTime.now().minusDays(Papelera.DIAS_RETENCION + 1);
        entradas.get(0).getAsJsonObject().addProperty("eliminadaEn", vencida.toString());
        Files.writeString(Rutas.PAPELERA, raiz.toString());

        Papelera recargada = new Papelera();
        assertEquals(2, recargada.listar().size());
        recargada.depurarVencidas();
        assertEquals(1, recargada.listar().size());
        assertEquals("Reciente", recargada.listar().get(0).item().getTitulo());
        assertEquals(1, new Papelera().listar().size());
    }

    @Test void papeleraDanadaNoSePisaNiSeLee() throws Exception {
        limpiar();
        Files.writeString(Rutas.PAPELERA, "{corrupto");
        assertThrows(IllegalStateException.class, Papelera::new);
        assertEquals("{corrupto", Files.readString(Rutas.PAPELERA));
    }

    @Test void respaldoZipCreaYRestauraLosDatos() throws Exception {
        limpiar();
        guardarTitulos("Original");
        Path zip = zipTemporal();
        RespaldoZip.crear(zip);
        assertTrue(Files.size(zip) > 0);

        guardarTitulos("Cambiado");
        assertTrue(Files.readString(Rutas.DATOS).contains("Cambiado"));

        Path emergencia = RespaldoZip.restaurar(zip);
        String datos = Files.readString(Rutas.DATOS);
        assertTrue(datos.contains("Original"));
        assertFalse(datos.contains("Cambiado"));
        assertNotNull(emergencia);
        assertTrue(Files.readString(emergencia).contains("Cambiado"));
    }

    @Test void respaldoZipSinDatosNoSeCrea() throws Exception {
        limpiar();
        Path zip = zipTemporal();
        assertThrows(IOException.class, () -> RespaldoZip.crear(zip));
        assertFalse(Files.exists(zip));
    }

    @Test void respaldoZipRechazaEntradasInseguras() throws Exception {
        limpiar();
        guardarTitulos("Intacto");
        Path zip = zipTemporal();
        try (ZipOutputStream salida = new ZipOutputStream(Files.newOutputStream(zip))) {
            salida.putNextEntry(new ZipEntry("datos.json"));
            salida.write("{\"versionFormato\":1,\"juegos\":[]}".getBytes());
            salida.closeEntry();
            salida.putNextEntry(new ZipEntry("../fuera.txt"));
            salida.write(1);
            salida.closeEntry();
        }
        assertThrows(IOException.class, () -> RespaldoZip.restaurar(zip));
        assertTrue(Files.readString(Rutas.DATOS).contains("Intacto"));
    }

    @Test void respaldoZipSinDatosJsonNoSirve() throws Exception {
        limpiar();
        guardarTitulos("Intacto");
        Path zip = zipTemporal();
        try (ZipOutputStream salida = new ZipOutputStream(Files.newOutputStream(zip))) {
            salida.putNextEntry(new ZipEntry("imagenes/a.png"));
            salida.write(1);
            salida.closeEntry();
        }
        assertThrows(IOException.class, () -> RespaldoZip.restaurar(zip));
        assertTrue(Files.readString(Rutas.DATOS).contains("Intacto"));
    }

    @Test void recuperacionRestauraSoloDatosYSePuedeRevertir() throws Exception {
        limpiar();
        guardarTitulos("Respaldado");
        Path zip = zipTemporal();
        RespaldoZip.crear(zip);
        guardarTitulos("Actual");

        Path emergencia = RecuperacionDatos.restaurarSoloDatos(zip);
        assertTrue(Files.readString(Rutas.DATOS).contains("Respaldado"));
        assertNotNull(emergencia);

        RecuperacionDatos.revertir(emergencia);
        String datos = Files.readString(Rutas.DATOS);
        assertTrue(datos.contains("Actual"));
        assertFalse(datos.contains("Respaldado"));
    }

    @Test void recuperacionRechazaVersionesYFormasInvalidas() {
        assertThrows(IOException.class, () -> RecuperacionDatos.comprobarContenido("[]".getBytes()));
        assertThrows(IOException.class, () -> RecuperacionDatos.comprobarContenido("{corrupto".getBytes()));
        assertThrows(IOException.class, () ->
                RecuperacionDatos.comprobarContenido("{\"versionFormato\":99}".getBytes()));
        assertThrows(IOException.class, () ->
                RecuperacionDatos.comprobarContenido("{\"juegos\":5}".getBytes()));
    }

    @Test void recuperacionElijeElRespaldoLegibleMasReciente() throws Exception {
        limpiar();
        guardarTitulos("Bueno");
        assertTrue(RespaldoAutomatico.crearSiCambio(LocalDateTime.of(2026, 1, 1, 10, 0, 0)));
        Files.writeString(Rutas.RESPALDOS.resolve("auto-2999-01-01_00-00-00.zip"), "no es un zip");

        Path elegido = RecuperacionDatos.ultimoRespaldoLegible().orElseThrow();
        assertTrue(elegido.getFileName().toString().startsWith("auto-2026-01-01"));
    }
}
