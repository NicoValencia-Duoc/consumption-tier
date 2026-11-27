package consumption.data;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

public final class DiarioRestauracion {
    private static final Path DIR = Rutas.BASE.resolve("diario-restauracion-c14");
    private static final Path ACTIVE = DIR.resolve("active");
    private static final Path LOCK = DIR.resolve("operation.lock");
    private static final List<Path> TARGETS = List.of(Rutas.DATOS, Rutas.PAPELERA);
    private DiarioRestauracion() {}
    private static final ThreadLocal<Boolean> EN_TX = ThreadLocal.withInitial(() -> false);
    static boolean enTransaccion() { return EN_TX.get(); }
    @FunctionalInterface public interface Trabajo { void ejecutar() throws Exception; }
    private static void atomic(Path dest, byte[] bytes) throws IOException {
        Files.createDirectories(dest.getParent());
        Path temp = Files.createTempFile(dest.getParent(), "journal-", ".tmp");
        try {
            try(FileChannel ch = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                var buffer = java.nio.ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) ch.write(buffer);
                ch.force(true);
            }
            Files.move(temp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(temp); }
    }
    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    private static <T> T locked(Callable<T> action) throws Exception {
        return BarreraEscritura.conBloqueo(action::call);
    }
    @FunctionalInterface private interface Callable<T> { T call() throws Exception; }
    public static void recuperarAntesDeCargar() throws Exception { locked(() -> {recoverLocked();return null;}); }
    public static void ejecutar(Trabajo trabajo) throws Exception {
        locked(() -> {
            recoverLocked();
            String id=UUID.randomUUID().toString(); Path tx=DIR.resolve(id); Files.createDirectory(tx);
            Properties manifest=new Properties();manifest.setProperty("id",id);
            for(int i=0;i<TARGETS.size();i++) {
                Path file=TARGETS.get(i);boolean exists=Files.exists(file);
                manifest.setProperty("f"+i+".exists",Boolean.toString(exists));
                if(exists){byte[] b=Files.readAllBytes(file);atomic(tx.resolve("f"+i+".before"),b);
                    manifest.setProperty("f"+i+".sha",sha(b));manifest.setProperty("f"+i+".size",Integer.toString(b.length));}
            }
            ByteArrayOutputStream out=new ByteArrayOutputStream();manifest.store(out,"C14 experimental journal");
            atomic(tx.resolve("manifest"),out.toByteArray());atomic(ACTIVE,id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            try {
                EN_TX.set(true);
                try { trabajo.ejecutar(); } finally { EN_TX.remove(); }
                atomic(tx.resolve("committed"),id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                Files.delete(ACTIVE);
            } catch(Exception ex) {
                try {recoverLocked();}catch(Exception failure){ex.addSuppressed(failure);}
                throw ex;
            }
            return null;
        });
    }
    private static void recoverLocked() throws Exception {
        if(!Files.exists(ACTIVE))return;
        String id=Files.readString(ACTIVE).trim();
        if(!id.matches("[0-9a-fA-F-]{36}"))throw new IOException("Invalid transaction id");
        Path tx=DIR.resolve(id);Properties manifest=new Properties();
        try(InputStream in=Files.newInputStream(tx.resolve("manifest"))){manifest.load(in);}
        if(!id.equals(manifest.getProperty("id")))throw new IOException("Mismatched manifest id");
        if(Files.exists(tx.resolve("committed"))){
            if(!id.equals(Files.readString(tx.resolve("committed")).trim()))throw new IOException("Mismatched commit id");
            Files.delete(ACTIVE);return;
        }
        List<byte[]> restore=new ArrayList<>();
        for(int i=0;i<TARGETS.size();i++) {
            String flag=manifest.getProperty("f"+i+".exists");
            if("false".equals(flag))restore.add(null);
            else if("true".equals(flag)) {
                byte[] b=Files.readAllBytes(tx.resolve("f"+i+".before"));
                if(!sha(b).equals(manifest.getProperty("f"+i+".sha")) || b.length!=Long.parseLong(manifest.getProperty("f"+i+".size","-1")))
                    throw new IOException("Invalid snapshot "+i);
                restore.add(b);
            } else throw new IOException("Invalid existence flag "+i);
        }
        for(int i=0;i<TARGETS.size();i++) {
            if(i==1 && Boolean.getBoolean("maw.c17.failRollbackSecond")) throw new IOException("C17 injected failure during rollback second file");
            if(restore.get(i)==null)Files.deleteIfExists(TARGETS.get(i));
            else atomic(TARGETS.get(i),restore.get(i));
        }
        Files.delete(ACTIVE);
    }
}
