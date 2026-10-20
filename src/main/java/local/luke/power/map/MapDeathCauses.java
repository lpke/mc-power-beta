package local.luke.power.map;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;

/** Captures the same scoped description as /death, without opening its recovery journal. */
public final class MapDeathCauses {
  private record Pending(PlayerEntity player,Object world,String cause,long time) {}
  private static Pending pending;
  private MapDeathCauses() {}
  public static void capture(PlayerEntity player,String cause) {
    if (!MapConfig.current().deathpoints) return;
    Minecraft mc=(Minecraft)FabricLoader.getInstance().getGameInstance();
    if(player!=mc.player||player.world!=mc.world||player.world.isRemote)return;
    pending=new Pending(player,player.world,cause==null?"":cause,System.nanoTime());
  }
  public static String consume() {
    Pending value=pending;pending=null;if(value==null)return "";
    Minecraft mc=(Minecraft)FabricLoader.getInstance().getGameInstance();
    return mc.player==value.player&&mc.world==value.world&&System.nanoTime()-value.time<30_000_000_000L?value.cause:"";
  }
  public static void clear(){pending=null;}
  public static String caption(Waypoint point) {
    if(!point.death||point.deathCause==null||point.deathCause.isBlank()||point.deathCause.equals("Unknown cause"))return "";
    return switch(point.deathCause) {
      case "Fall" -> "Died from a fall";
      case "Suffocation" -> "Suffocated";
      case "Drowning" -> "Drowned";
      case "Void" -> "Fell into the void";
      default -> point.deathCause.startsWith("Killed by ")?point.deathCause:"Died to "+point.deathCause.toLowerCase(java.util.Locale.ROOT);
    };
  }
}
