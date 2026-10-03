package local.luke.power;
import local.luke.power.config.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
/** First-run defaults are installed before config libraries and recipe registries initialize. */
public final class Bootstrap implements PreLaunchEntrypoint {
  public void onPreLaunch(){
    Path game=FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
    try{
      FileTransaction.recover(game);
      try(var in=getClass().getResourceAsStream("/assets/powerbeta/defaults/index.txt")){
        if(in==null)return;
        for(String name:new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\n")){
          if(name.isBlank())continue;Path path=game.resolve(name).normalize();if(!path.startsWith(game))throw new IOException("Invalid default path");
          if(Files.exists(path))continue;
          try(var source=getClass().getResourceAsStream("/assets/powerbeta/defaults/"+name)){if(source==null)throw new IOException("Missing defaults: "+name);FileTransaction.atomicWrite(path,source.readAllBytes());}
        }
      }
    }catch(Exception e){throw new IllegalStateException("Power Beta could not safely prepare its configuration",e);}
  }
}
