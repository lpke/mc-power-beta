package local.luke.power.config;
import com.google.gson.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

/** Durable backup before any settings writer runs. A pending save is recovered before mod init. */
public final class FileTransaction implements AutoCloseable {
  private final Path game,dir;
  private final JsonObject manifest;
  private boolean committed;
  private FileTransaction(Path game,Path dir,JsonObject manifest){this.game=game;this.dir=dir;this.manifest=manifest;}
  public static FileTransaction begin(Path game,List<Path> files)throws IOException {
    game=game.toAbsolutePath().normalize();Path root=game.resolve("config/power-beta/backups");Files.createDirectories(root);
    Path dir=Files.createTempDirectory(root,"save-");JsonObject manifest=new JsonObject();
    int index=0;
    for(Path file:files){
      Path path=file.toAbsolutePath().normalize();if(!path.startsWith(game)||Files.isSymbolicLink(path))throw new IOException("Settings file is outside the instance: "+path.getFileName());
      String relative=game.relativize(path).toString();
      if(Files.exists(path)){
        if(Files.size(path)>4*1024*1024)throw new IOException("Settings file exceeds 4 MiB: "+relative);
        String backup="file-"+(index++);durableWrite(dir.resolve(backup),Files.readAllBytes(path));manifest.addProperty(relative,backup);
      }else manifest.add(relative,JsonNull.INSTANCE);
    }
    durableWrite(dir.resolve("pending.json"),Catalog.JSON.toJson(manifest).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    return new FileTransaction(game,dir,manifest);
  }
  public void commit()throws IOException{Files.move(dir.resolve("pending.json"),dir.resolve("complete.json"),StandardCopyOption.ATOMIC_MOVE);committed=true;}
  public void close()throws IOException{if(!committed){restore(game,dir,manifest);Files.move(dir.resolve("pending.json"),dir.resolve("recovered.json"),StandardCopyOption.REPLACE_EXISTING);}}
  public static void recover(Path game)throws IOException {
    Path root=game.resolve("config/power-beta/backups");if(!Files.isDirectory(root))return;
    try(var dirs=Files.list(root)){for(Path dir:dirs.sorted().toList()){
      Path pending=dir.resolve("pending.json");if(!Files.exists(pending))continue;
      JsonObject data=JsonParser.parseString(Files.readString(pending)).getAsJsonObject();restore(game.toAbsolutePath().normalize(),dir,data);
      Files.move(pending,dir.resolve("recovered.json"),StandardCopyOption.REPLACE_EXISTING);
    }}
  }
  private static void restore(Path game,Path dir,JsonObject data)throws IOException {
    for(var e:data.entrySet()){
      Path destination=game.resolve(e.getKey()).normalize();if(!destination.startsWith(game)||Files.isSymbolicLink(destination))throw new IOException("Invalid backup destination");
      if(e.getValue().isJsonNull())Files.deleteIfExists(destination);
      else {Path source=dir.resolve(e.getValue().getAsString()).normalize();if(!source.startsWith(dir)||!Files.isRegularFile(source))throw new IOException("Invalid settings backup");atomicWrite(destination,Files.readAllBytes(source));}
    }
  }
  public static void atomicWrite(Path path,byte[] bytes)throws IOException {
    Files.createDirectories(path.getParent());Path tmp=Files.createTempFile(path.getParent(),"power-beta-",".tmp");
    try{durableWrite(tmp,bytes);try{Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(tmp);}
  }
  private static void durableWrite(Path path,byte[] bytes)throws IOException{Files.write(path,bytes);try(var ch=FileChannel.open(path,StandardOpenOption.WRITE)){ch.force(true);}}
}
