package consumption.data;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReentrantLock;

public final class BarreraEscritura {
    private static final Path DIR = Rutas.BASE.resolve("diario-restauracion-c14");
    private static final Path ACTIVE = DIR.resolve("active");
    private static final Path LOCK = DIR.resolve("operation.lock");
    private static final ReentrantLock JVM = new ReentrantLock(true);
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private BarreraEscritura() {}
    @FunctionalInterface public interface Accion<T> { T ejecutar() throws Exception; }
    public static <T> T conBloqueo(Accion<T> action) throws Exception {
        JVM.lock();
        try {
            int depth=DEPTH.get();
            if(depth>0) {
                DEPTH.set(depth+1);
                try { return action.ejecutar(); }
                finally { DEPTH.set(depth); }
            }
            Files.createDirectories(DIR);
            try(FileChannel channel=FileChannel.open(LOCK,StandardOpenOption.CREATE,StandardOpenOption.WRITE);
                FileLock lock=channel.lock()) {
                DEPTH.set(1);
                try { return action.ejecutar(); }
                finally { DEPTH.remove(); }
            }
        } finally { JVM.unlock(); }
    }
    public static void escritura(Runnable action) {
        try { conBloqueo(() -> {
            if(Files.exists(ACTIVE) && !DiarioRestauracion.enTransaccion())
                throw new IllegalStateException("Recuperacion pendiente: escritura bloqueada");
            action.run();return null;
        }); }
        catch(RuntimeException ex) { throw ex; }
        catch(Exception ex) { throw new IllegalStateException("No se pudo adquirir el bloqueo de escritura",ex); }
    }
}
