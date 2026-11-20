package consumption.data;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.Map;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PersistenciaSeguraTest {
    @BeforeAll static void aislar() throws Exception {
        System.setProperty("user.home", Files.createTempDirectory("maw-junit-home-").toString());
    }

    @Test void rechazadosSeConservanYExportanSinModificarOriginal() throws Exception {
        Files.createDirectories(Rutas.BASE);
        String original = "{\"versionFormato\":1,\"juegos\":[17],\"libros\":[],\"peliculas\":[],\"series\":[]}";
        Files.writeString(Rutas.DATOS, original);
        RepositorioJson repo = new RepositorioJson();
        assertTrue(repo.cargar().isEmpty());
        assertTrue(repo.tieneRechazados());
        assertEquals(1, repo.getAvisosCarga().size());
        Path exportado = Rutas.BASE.resolve("exportados.json");
        repo.exportarRechazados(exportado);
        assertTrue(Files.readString(exportado).contains("17"));
        assertEquals(original, Files.readString(Rutas.DATOS));
        assertThrows(IllegalStateException.class, () -> repo.exportarRechazados(exportado));
        repo.guardar(List.of());
        assertTrue(Files.readString(Rutas.DATOS).contains("17"));
    }

    @Test void avisosReconocidosSePersistenSinEliminarRechazados() {
        Preferencias p = new Preferencias();
        assertFalse(p.rechazadosYaVistos("abc"));
        p.marcarRechazadosVistos("abc");
        p.guardar();
        Preferencias recargadas = Preferencias.cargar();
        assertTrue(recargadas.rechazadosYaVistos("abc"));
        assertFalse(recargadas.rechazadosYaVistos("cambio"));
    }

    @Test void borradorGuardaUltimaEdicionYSePuedeRecuperar() {
        BorradorFormulario.guardar("JUEGO", Map.of("TITULO#T0", "Ultima tecla"));
        assertEquals("Ultima tecla", BorradorFormulario.leer("JUEGO").orElseThrow().campos.get("TITULO#T0"));
        BorradorFormulario.borrar("JUEGO");
        assertTrue(BorradorFormulario.leer("JUEGO").isEmpty());
    }

    @Test void borradorDanadoNoSeSobrescribePorLectura() throws Exception {
        Path archivo = Rutas.BASE.resolve("borrador-juego.json");
        Files.createDirectories(Rutas.BASE);
        Files.writeString(archivo, "{corrupto");
        assertThrows(IllegalStateException.class, () -> BorradorFormulario.leer("JUEGO"));
        assertEquals("{corrupto", Files.readString(archivo));
    }
}
