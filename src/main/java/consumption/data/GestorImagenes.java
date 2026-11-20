package consumption.data;

import consumption.model.ItemConsumption;
import javafx.scene.image.Image;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.LinkOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GestorImagenes {

    private static final Path CARPETA =
            Rutas.IMAGENES;
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MAX_LADO = 1200;
    private static final float CALIDAD_JPG = 0.85f;
    private static final Map<String, Image> CACHE = new HashMap<>();

    public static void validar(File origen) {
        String nombre = origen.getName().toLowerCase();
        if (!nombre.matches(".*\\.(png|jpg|jpeg|gif|bmp)")) {
            throw new IllegalArgumentException("La imagen debe ser PNG, JPG, GIF o BMP.");
        }
        try {
            if (Files.size(origen.toPath()) > MAX_BYTES) {
                throw new IllegalArgumentException("La imagen pesa mas de 5 MB.");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer la imagen.");
        }
    }

    public static String copiar(File origen) {
        final String[] resultado = new String[1];
        BarreraEscritura.escritura(() -> resultado[0] = copiarProtegido(origen));
        return resultado[0];
    }

    private static String copiarProtegido(File origen) {
        validar(origen);
        String extension = origen.getName().toLowerCase(java.util.Locale.ROOT);
        extension = extension.substring(extension.lastIndexOf('.') + 1);

        if (extension.equals("gif")) {
            return copiarOriginal(origen, ".gif");
        }

        Image imagen = new Image(origen.toURI().toString(), false);
        if (imagen.isError() || imagen.getWidth() <= 0 || imagen.getHeight() <= 0) {
            throw new IllegalArgumentException("El archivo no contiene una imagen valida.");
        }
        if (imagen.getWidth() > 6000 || imagen.getHeight() > 6000) {
            throw new IllegalArgumentException("La imagen supera 6000 pixeles por lado.");
        }

        BufferedImage original = SwingFXUtils.fromFXImage(imagen, null);
        if (original == null) {
            throw new IllegalArgumentException("No se pudo procesar la imagen.");
        }

        boolean transparente = tieneTransparencia(original);
        BufferedImage reducida = reducir(original, MAX_LADO, transparente);
        String formato = transparente ? "png" : "jpg";
        String sufijo = "." + formato;

        Path temporal = null;
        try {
            Files.createDirectories(CARPETA);
            temporal = Files.createTempFile(CARPETA, "maw-opt-", ".tmp");
            if (formato.equals("jpg")) {
                escribirJpg(reducida, temporal);
            } else if (!ImageIO.write(reducida, "png", temporal.toFile())) {
                throw new IOException("No hay codificador PNG disponible.");
            }

            boolean originalCompatible = extension.equals("jpg")
                    || extension.equals("jpeg") || extension.equals("png");
            if (originalCompatible && Files.size(temporal) >= Files.size(origen.toPath())) {
                Files.deleteIfExists(temporal);
                temporal = null;
                return copiarOriginal(origen, "." + extension);
            }

            String nombre = UUID.randomUUID() + sufijo;
            Files.move(temporal, CARPETA.resolve(nombre), StandardCopyOption.ATOMIC_MOVE);
            temporal = null;
            return nombre;
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo guardar la portada optimizada.", ex);
        } finally {
            if (temporal != null) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException ex) {
                    System.err.println("No se pudo limpiar temporal de portada: " + ex.getMessage());
                }
            }
        }
    }

    private static String copiarOriginal(File origen, String extension) {
        try {
            Files.createDirectories(CARPETA);
            String nombre = UUID.randomUUID() + extension;
            Path temporal = Files.createTempFile(CARPETA, "maw-copia-", ".tmp");
            try {
                Files.copy(origen.toPath(), temporal, StandardCopyOption.REPLACE_EXISTING);
                Files.move(temporal, CARPETA.resolve(nombre), StandardCopyOption.ATOMIC_MOVE);
                return nombre;
            } finally {
                Files.deleteIfExists(temporal);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo copiar la imagen.", ex);
        }
    }

    private static boolean tieneTransparencia(BufferedImage imagen) {
        if (!imagen.getColorModel().hasAlpha()) return false;
        for (int y = 0; y < imagen.getHeight(); y++) {
            for (int x = 0; x < imagen.getWidth(); x++) {
                if ((imagen.getRGB(x, y) >>> 24) != 255) return true;
            }
        }
        return false;
    }

    private static BufferedImage reducir(BufferedImage origen, int ladoMaximo,
                                         boolean transparencia) {
        int ancho = origen.getWidth();
        int alto = origen.getHeight();
        double factor = Math.min(1.0, (double) ladoMaximo / Math.max(ancho, alto));
        int nuevoAncho = Math.max(1, (int) Math.round(ancho * factor));
        int nuevoAlto = Math.max(1, (int) Math.round(alto * factor));

        int tipo = transparencia ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resultado = new BufferedImage(nuevoAncho, nuevoAlto, tipo);
        Graphics2D g = resultado.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(origen, 0, 0, nuevoAncho, nuevoAlto, null);
        } finally {
            g.dispose();
        }
        return resultado;
    }

    private static void escribirJpg(BufferedImage imagen, Path destino) throws IOException {
        Iterator<ImageWriter> disponibles = ImageIO.getImageWritersByFormatName("jpeg");
        if (!disponibles.hasNext()) {
            throw new IOException("No hay codificador JPEG disponible.");
        }
        ImageWriter escritor = disponibles.next();
        try (ImageOutputStream salida = ImageIO.createImageOutputStream(destino.toFile())) {
            if (salida == null) throw new IOException("No se pudo abrir la salida JPEG.");
            escritor.setOutput(salida);
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            if (parametros.canWriteCompressed()) {
                parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parametros.setCompressionQuality(CALIDAD_JPG);
            }
            escritor.write(null, new IIOImage(imagen, null, null), parametros);
        } finally {
            escritor.dispose();
        }
    }

    public static void eliminar(String nombre) {
        if (nombre == null) return;
        if (!nombre.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg|jpeg|gif|bmp)")) {
            throw new IllegalArgumentException("Nombre de portada no valido.");
        }
        BarreraEscritura.escritura(() -> {
            try {
                Path destino = CARPETA.resolve(nombre);
                if (Files.isSymbolicLink(destino))
                    throw new IOException("No se permite eliminar un enlace simbolico.");
                Files.deleteIfExists(destino);
                CACHE.keySet().removeIf(k -> k.startsWith(nombre + "@"));
            } catch (IOException ex) {
                throw new IllegalStateException("No se pudo eliminar la portada: " + nombre, ex);
            }
        });
    }

    public static Image imagenDe(ItemConsumption item, double ancho, double alto) {
        String uri;
        String clave;
        if (item.getRutaImagen() != null && Files.exists(CARPETA.resolve(item.getRutaImagen()))) {
            clave = item.getRutaImagen();
            uri = CARPETA.resolve(clave).toUri().toString();
        } else {
            clave = nombreIcono(item.getTipo());
            var recurso = GestorImagenes.class.getResource("/consumption/iconos/" + clave);
            if (recurso == null) return null;
            uri = recurso.toExternalForm();
        }
        return CACHE.computeIfAbsent(clave + "@" + ancho + "x" + alto,
                k -> new Image(uri, ancho, alto, true, true));
    }

    public static Image iconoDeTipo(String tipo, double ancho, double alto) {
        String clave = nombreIcono(tipo);
        var recurso = GestorImagenes.class.getResource("/consumption/iconos/" + clave);
        if (recurso == null) return null;
        String uri = recurso.toExternalForm();
        return CACHE.computeIfAbsent(clave + "@" + ancho + "x" + alto,
                k -> new Image(uri, ancho, alto, true, true));
    }

    private static String nombreIcono(String tipo) {
        return switch (tipo) {
            case "JUEGOS" -> "juego.png";
            case "LIBROS" -> "libro.png";
            case "SERIES" -> "serie.png";
            default -> "pelicula.png";
        };
    }

    public static void limpiarCache() {
        CACHE.clear();
    }
}