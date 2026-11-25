package consumption.data;

import com.google.gson.JsonParser;
import consumption.model.Estado;
import consumption.model.ItemConsumption;
import consumption.model.Juego;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepositorioRechazadosTest {
    @BeforeAll static void aislar() throws Exception {
        System.setProperty("user.home", Files.createTempDirectory("maw-junit-home-").toString());
    }

    private static String registroValido() throws Exception {
        Files.createDirectories(Rutas.BASE);
        Files.deleteIfExists(Rutas.DATOS);
        RepositorioJson auxiliar = new RepositorioJson();
        auxiliar.cargar();
        Juego juego = new Juego("Recuperado", 8.0, Estado.PENDIENTE, "PC", "Accion", "Estudio", 2020, 0);
        auxiliar.guardar(List.of(juego));
        return JsonParser.parseString(Files.readString(Rutas.DATOS))
                .getAsJsonObject().getAsJsonArray("juegos").get(0).toString();
    }

    private static RepositorioJson conRechazado() throws Exception {
        Files.writeString(Rutas.DATOS,
                "{\"versionFormato\":1,\"juegos\":[17],\"libros\":[],\"peliculas\":[],\"series\":[]}");
        RepositorioJson repo = new RepositorioJson();
        assertTrue(repo.cargar().isEmpty());
        return repo;
    }

    @Test void unRechazadoCorregidoSePuedeRecuperar() throws Exception {
        String valido = registroValido();
        RepositorioJson repo = conRechazado();
        RepositorioJson.Rechazado r = repo.getRechazados().get(0);
        assertEquals("juegos", r.getCategoria());
        ItemConsumption item = repo.interpretar(r, valido);
        assertEquals("Recuperado", item.getTitulo());
        repo.quitarRechazado(r);
        assertEquals(0, repo.cantidadRechazados());
        repo.guardar(List.of(item));
        String guardado = Files.readString(Rutas.DATOS);
        assertTrue(guardado.contains("Recuperado"));
        assertFalse(guardado.contains("17"));
    }

    @Test void unTextoInvalidoSeRechazaSinTocarNada() throws Exception {
        RepositorioJson repo = conRechazado();
        RepositorioJson.Rechazado r = repo.getRechazados().get(0);
        assertThrows(IllegalArgumentException.class, () -> repo.interpretar(r, "{roto"));
        assertThrows(IllegalArgumentException.class, () -> repo.interpretar(r, "17"));
        assertEquals(1, repo.cantidadRechazados());
    }

    @Test void descartarGuardaCopiaYSePuedeReponer() throws Exception {
        RepositorioJson repo = conRechazado();
        RepositorioJson.Rechazado r = repo.getRechazados().get(0);
        java.nio.file.Path copia = repo.guardarCopiaDescartado(r);
        assertTrue(Files.readString(copia).contains("17"));
        repo.quitarRechazado(r);
        assertEquals(0, repo.cantidadRechazados());
        repo.reponerRechazado(r);
        assertEquals(1, repo.cantidadRechazados());
        assertEquals(1, repo.getRechazados().size());
    }
}
