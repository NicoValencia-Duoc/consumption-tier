# Consumption Tier

Biblioteca personal que reúne en un solo lugar tus juegos, libros, películas y series, hecha con **JavaFX + Maven** (Java 21). Estética cyberpunk con tema día/noche y música de fondo.

## Qué hace

- Tablas por tipo (juegos, libros, películas y series) con nota por criterios, comentarios e imagen de portada.
- Búsqueda, filtros, orden, favoritos y lista de pendientes.
- Pestaña de estadísticas por tipo.
- Exportar cualquier tabla a CSV (listo para Excel).
- Respaldo y restauración de tus datos e imágenes en un `.zip`.
- Música de fondo en orden aleatorio, con pausa, siguiente, volumen y lista de pistas.
- Recuerda el tema (día o noche) y el volumen entre sesiones.

## Requisitos

- Java 21 (JDK)
- Maven (o el Maven que trae IntelliJ IDEA)

## Cómo ejecutarlo

```
mvn javafx:run
```

O abre el proyecto en IntelliJ IDEA, recarga Maven y ejecuta `consumption.App` (o `Launcher`).

## Dónde se guardan tus datos

En la carpeta `consumption-personal` dentro de tu usuario (por ejemplo `C:\Users\tu-usuario\consumption-personal`):

| Archivo | Contenido |
|---|---|
| `datos.json` | Tus juegos, libros y películas |
| `imagenes/` | Las portadas que agregas |
| `preferencias.json` | Volumen y tema |

Si no existe `datos.json`, la app parte con datos de ejemplo.

## Música

Las canciones están en `src/main/resources/consumption/musica/` y se listan en `lista.txt`, con el formato:

```
archivo | título | artista | fuente o licencia
```

Para agregar o quitar una canción, edita `lista.txt` y vuelve a ejecutar.

### Créditos

Música obtenida de la [Free Music Archive](https://freemusicarchive.org/). Cada pista conserva la licencia de su autor.

| Pista | Artista | Licencia |
|---|---|---|
| [Bounty at Corpo Plaza (instrumental)](https://freemusicarchive.org/music/manylegs/outtakes-and-instrumentals/bounty-at-corpo-plaza-instrumental/) | manylegs | CC BY 4.0 |
| [The Dotted Line](https://freemusicarchive.org/music/PC-ONE/Division/The_Dotted_Line/) | PC-ONE | CC BY 4.0 |
| [Beyond The Universe (Transcendance)](https://freemusicarchive.org/music/Megatone/First_Space_Voyage/05_megatone_-_beyond_the_universe__transcendance/) | Megatone | CC BY-NC-SA 3.0 |
| [Psyche](https://freemusicarchive.org/music/Wolf_Asylum/Dogflower/Wolf_Asylum_-_Dogflower_-EP-_-_04_Psyche/) | Wolf Asylum | CC BY-NC-ND 4.0 |
| [Waiting Becomes Not Waiting](https://freemusicarchive.org/music/Heather_Perkins/Little_Humans/Waiting_Becomes_Not_Waiting/) | Heather Perkins | CC BY-NC-SA 3.0 US |
| [Submechanophobia](https://freemusicarchive.org/music/aldous-ichnite/submechanophobia/submechanophobia/) | Aldous Ichnite | CC BY-NC 4.0 |
| [Rock Hard Sports Electro - Medium1](https://freemusicarchive.org/music/lite-saturation/rock-hard-sports-electro/rock-hard-sports-electro-medium1/) | Lite Saturation | CC BY-ND 4.0 |

Varias licencias son **no comerciales** (NC) o **sin obra derivada** (ND): este proyecto es personal y educativo, y las pistas no deben usarse con fines comerciales ni modificarse.

## Tipografías

[Orbitron](https://fonts.google.com/specimen/Orbitron) y [Rajdhani](https://fonts.google.com/specimen/Rajdhani), de Google Fonts, bajo licencia SIL Open Font License.

## Estructura

```
src/main/java/consumption/
├── App.java, Launcher.java
├── model/   Juego, Libro, Pelicula, Serie, Estado, Estadisticas
├── data/    RepositorioJson, GestorImagenes, ExportadorCsv, RespaldoZip, Preferencias, Rutas
└── ui/      ventana principal, tablas, formularios, detalle, estadísticas, reproductor
src/main/resources/consumption/
├── base.css, noche.css, dia.css
├── iconos/, fuentes/, musica/
```

Autor

Nicolás Valencia — proyecto personal.
