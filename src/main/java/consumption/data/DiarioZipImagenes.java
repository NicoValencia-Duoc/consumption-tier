package consumption.data;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

public final class DiarioZipImagenes {
    private static final Path DIR = Rutas.BASE.resolve("diario-zip-imagenes-b03");
    private static final Path ACTIVE = DIR.resolve("active");
    private DiarioZipImagenes() {}

    public static void iniciar(List<Path> targets) throws IOException {
        if (Files.exists(ACTIVE)) throw new IOException("Restauracion ZIP pendiente");
        Files.createDirectories(DIR);
        String id = UUID.randomUUID().toString();
        Path tx = DIR.resolve(id); Files.createDirectory(tx);
        Properties manifest = new Properties(); manifest.setProperty("id", id);
        manifest.setProperty("count", Integer.toString(targets.size()));
        for (int i=0;i<targets.size();i++) {
            Path target=targets.get(i).toAbsolutePath().normalize();
            if (!target.startsWith(Rutas.BASE.toAbsolutePath().normalize())) throw new IOException("Ruta fuera de base");
            if (Files.isSymbolicLink(target)) throw new IOException("Destino simbolico");
            manifest.setProperty("path."+i, Rutas.BASE.toAbsolutePath().normalize().relativize(target).toString());
            boolean exists=Files.exists(target, LinkOption.NOFOLLOW_LINKS);
            manifest.setProperty("exists."+i, Boolean.toString(exists));
            if(exists) {
                Path backup=tx.resolve(i+".before"); Files.copy(target, backup);
                force(backup);
                manifest.setProperty("sha."+i, digest(backup));
            }
        }
        Path props=tx.resolve("manifest.tmp");
        try(OutputStream stream=Files.newOutputStream(props)){manifest.store(stream,"ZIP restore before images and JSON");}
        force(props);
        Files.move(props,tx.resolve("manifest"),StandardCopyOption.ATOMIC_MOVE);
        forceDirectory(tx);
        atomicWrite(ACTIVE,id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        forceDirectory(DIR);
    }

    public static void confirmar() throws IOException {
        if(!Files.exists(ACTIVE)) throw new IOException("No hay diario ZIP activo");
        String id=Files.readString(ACTIVE).trim();
        Path tx=DIR.resolve(id);
        atomicWrite(tx.resolve("committed"),id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Files.delete(ACTIVE);
        forceDirectory(DIR);
    }

    public static void recuperarPendiente() throws IOException {
        try { BarreraEscritura.conBloqueo(() -> { recuperarInterno(); return null; }); }
        catch(IOException e){throw e;}catch(Exception e){throw new IOException("Recuperacion ZIP fallida",e);}
    }
    private static void recuperarInterno() throws IOException {
        if(!Files.exists(ACTIVE)) return;
        String id=Files.readString(ACTIVE).trim();
        if(!id.matches("[0-9a-fA-F-]{36}")) throw new IOException("ID de diario ZIP invalido");
        Path tx=DIR.resolve(id);
        Properties p=new Properties();
        try(InputStream in=Files.newInputStream(tx.resolve("manifest"))){p.load(in);}
        if(!id.equals(p.getProperty("id"))) throw new IOException("Diario ZIP no coincide");
        if(Files.exists(tx.resolve("committed"))) {
            if(!id.equals(Files.readString(tx.resolve("committed")).trim()))throw new IOException("Commit ZIP invalido");
            Files.delete(ACTIVE); return;
        }
        int count=Integer.parseInt(p.getProperty("count","-1"));
        if(count<2 || count>10002) throw new IOException("Cantidad invalida de destinos");
        List<Path> targets=new ArrayList<>();
        for(int i=0;i<count;i++) {
            String relative=p.getProperty("path."+i);
            if(relative==null)throw new IOException("Falta destino en diario");
            Path target=Rutas.BASE.resolve(relative).toAbsolutePath().normalize();
            if(!target.startsWith(Rutas.BASE.toAbsolutePath().normalize()) || Files.isSymbolicLink(target))throw new IOException("Destino inseguro");
            String exists=p.getProperty("exists."+i);
            if("true".equals(exists)) {
                Path before=tx.resolve(i+".before");
                if(!Files.isRegularFile(before) || !digest(before).equals(p.getProperty("sha."+i)))throw new IOException("Snapshot alterado: "+i);
            } else if(!"false".equals(exists))throw new IOException("Flag invalido");
            targets.add(target);
        }
        for(int i=0;i<count;i++) {
            Path target=targets.get(i);
            if("true".equals(p.getProperty("exists."+i))) {
                Files.createDirectories(target.getParent());
                Path temp=Files.createTempFile(target.getParent(),"zip-rollback-",".tmp");
                try { Files.copy(tx.resolve(i+".before"),temp,StandardCopyOption.REPLACE_EXISTING);force(temp);
                    RecuperacionDatos.mover(temp,target); }
                finally {Files.deleteIfExists(temp);}
            } else Files.deleteIfExists(target);
        }
        Files.delete(ACTIVE); forceDirectory(DIR);
    }
    private static String digest(Path file) throws IOException {
        try {MessageDigest md=MessageDigest.getInstance("SHA-256");
            try(InputStream in=Files.newInputStream(file)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}
            return HexFormat.of().formatHex(md.digest());
        }catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
    }
    private static void force(Path file) throws IOException {try(FileChannel ch=FileChannel.open(file,StandardOpenOption.READ)){ch.force(true);}}
    private static void forceDirectory(Path dir) {try(FileChannel ch=FileChannel.open(dir,StandardOpenOption.READ)){ch.force(true);}catch(IOException|UnsupportedOperationException ignored){}}
    private static void atomicWrite(Path dest,byte[] data)throws IOException {
        Path temp=Files.createTempFile(dest.getParent(),"journal-",".tmp");
        try {Files.write(temp,data);force(temp);Files.move(temp,dest,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        finally {Files.deleteIfExists(temp);}
    }
}
