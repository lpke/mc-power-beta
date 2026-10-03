package local.luke.power.worldedit.carry;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import local.luke.power.storage.PowerConfig;
import local.luke.power.worldedit.*;
import local.luke.power.worldedit.core.*;
import local.luke.power.worldedit.mixin.CarryWorldAccess;
import net.minecraft.class_212;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.world.World;

/** BTA's empty-hand carry interaction, with journaled Beta block-entity transfers. */
public final class ContainerCarry {
  private static World world;
  private static Path file;
  private static CarryJournal held;
  private static boolean failed, loaded;
  private static long lastMessage;
  public static boolean carrying() { return held != null && !held.phase.equals("complete"); }
  public static void tick(Minecraft mc) {
    local.luke.power.input.InteractionState.carryingContainer = world == mc.world && carrying();
    if (world == mc.world && loaded) return;
    if (world != mc.world) { world=mc.world; file=null; held=null; failed=false; loaded=false; }
    if (world==null || world.isRemote || mc.player==null) return;
    loaded=true;
    try {
      File data=((CarryWorldAccess)world).power$storage().method_1736("power-beta-carry");
      if (data==null) return;
      file=data.toPath();
      if (Files.exists(file)) {
        held=CarryJournal.read(file);
        if (held.phase.equals("complete")) held=null;
        else recover(mc);
      }
    } catch (Exception e) { stop(mc,e); }
  }
  private static void recover(Minecraft mc) throws Exception {
    MinecraftWorld blocks=new MinecraftWorld(world);
    if (held.phase.equals("removing")) {
      if (mc.player.dimensionId != held.sourceDimension) throw new IOException("Return to the pickup dimension to recover the container");
      // The source is known to exist in this save; recovery may need its chunk.
      world.method_214(held.source.x()>>4,held.source.z()>>4);
      BlockValue found=blocks.get(held.source);
      if (found.same(held.value())) blocks.set(held.source,BlockValue.AIR);
      else if (found.id!=0) throw new IOException("The pickup location changed; recovery record preserved");
      world.method_195(true,null);
      held.phase="held"; held.write(file);
    } else if (held.phase.equals("placing")) {
      if (mc.player.dimensionId != held.targetDimension) throw new IOException("Return to the placement dimension to recover the container");
      world.method_214(held.target.x()>>4,held.target.z()>>4);
      BlockValue expected=at(held.value(),held.target), found=blocks.get(held.target);
      if (found.same(expected)) complete();
      else if (found.id==0) { held.phase="held"; held.target=null; held.write(file); }
      else throw new IOException("The placement location changed; recovery record preserved");
    }
  }
  public static boolean click(Minecraft mc,int button) {
    tick(mc);
    if (world==null || world.isRemote || mc.player==null || mc.player.health<=0 || mc.currentScreen!=null) return false;
    if (failed) return carrying();
    boolean carrying=carrying();
    if (carrying && button!=1) return true;
    if (!carrying && !local.luke.power.input.InteractionState.containerCarryEnabled) return false;
    if (button!=1 || (!carrying && (!(mc.player.field_161 != null && mc.player.field_161.field_2536) || mc.player.inventory.getSelectedItem()!=null))) return false;
    if (CreativeAccess.spectator(mc.player) || mc.player.field_1594 != null || mc.player.field_1595 != null) return carrying;
    if (WorldEditor.freecam() || WorldEditor.editing()) { if(carrying) message(mc,"Finish the current edit or return to your player before placing."); return carrying; }
    var hit=mc.field_2823;
    if (hit==null || hit.field_1983!=class_212.TILE) return carrying;
    Pos pos=new Pos(hit.field_1984,hit.field_1985,hit.field_1986);
    MinecraftWorld blocks=new MinecraftWorld(world);
    if (!blocks.loaded(pos) || file==null) return carrying;
    // Never allow synthetic or stale ray targets outside normal interaction reach.
    double dx=mc.player.x-pos.x()-.5, dy=mc.player.y-pos.y()-.5, dz=mc.player.z-pos.z()-.5;
    double reach=mc.interactionManager.method_1715()+1;
    if (dx*dx+dy*dy+dz*dz>reach*reach) return carrying;
    try {
      if (!carrying) {
        BlockValue value=blocks.get(pos);
        if (!Set.of(23,54,61,62).contains(value.id) || !(world.method_1777(pos.x(),pos.y(),pos.z()) instanceof Inventory)) return false;
        if (value.id==54 && adjacentChest(pos)) { message(mc,"Separate double chests before moving them."); return true; }
        if (value.nbt()==null) throw new IOException("Container data is unavailable");
        held=new CarryJournal(pos,mc.player.dimensionId,value);
        held.write(file); // This must succeed before the source can be touched.
        local.luke.power.input.InteractionState.carryingContainer = true;
        if (!blocks.get(pos).same(value)) throw new IOException("Container changed before pickup");
        blocks.set(pos,BlockValue.AIR);
        if (blocks.get(pos).id!=0) throw new IOException("Container removal failed");
        world.method_195(true,null);
        held.phase="held"; held.write(file);
        message(mc,"Carrying container. Use a block face to place it.");
      } else {
        int[][] offsets={{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
        if (hit.field_1987<0 || hit.field_1987>=offsets.length) return true;
        int[] side=offsets[hit.field_1987]; Pos target=new Pos(pos.x()+side[0],pos.y()+side[1],pos.z()+side[2]);
        if (!blocks.loaded(target) || blocks.get(target).id!=0 || held.block==54 && adjacentChest(target)
            || !world.method_156(held.block,target.x(),target.y(),target.z(),false,hit.field_1987)) { message(mc,"Choose an empty space clear of entities and other chests."); return true; }
        held.target=target; held.targetDimension=mc.player.dimensionId; held.phase="placing"; held.write(file);
        blocks.set(target,held.value());
        if (!blocks.get(target).same(at(held.value(),target))) throw new IOException("Placed container did not match its saved contents");
        complete(); message(mc,"Container placed.");
      }
      return true;
    } catch (Exception e) { stop(mc,e); return true; }
  }
  private static BlockValue at(BlockValue value,Pos pos) throws IOException {
    NbtCompound nbt=NbtIo.read(new DataInputStream(new ByteArrayInputStream(value.nbt())));
    nbt.putInt("x",pos.x()); nbt.putInt("y",pos.y()); nbt.putInt("z",pos.z());
    ByteArrayOutputStream out=new ByteArrayOutputStream(); NbtIo.write(nbt,new DataOutputStream(out));
    return new BlockValue(value.id,value.meta,out.toByteArray());
  }
  private static boolean adjacentChest(Pos p) {
    for(int[] d:new int[][]{{-1,0},{1,0},{0,-1},{0,1}})
      if(world.getBlockId(p.x()+d[0],p.y(),p.z()+d[1])==54) return true;
    return false;
  }
  private static void complete() throws IOException {
    world.method_195(true,null);
    held.phase="complete";
    // Retain a full recovery copy permanently; never reduce inventories to item drops.
    held.write(file.resolveSibling("power-beta-carried-"+held.id+".json"));
    held.write(file); held=null; local.luke.power.input.InteractionState.carryingContainer = false;
  }
  private static void stop(Minecraft mc,Exception failure) {
    failed=true; WorldEditor.LOG.error("Container movement stopped; recovery data preserved",failure);
    message(mc,"Container movement stopped safely. Recovery data is preserved in this world's data folder.");
  }
  private static void message(Minecraft mc,String text) {
    if(System.currentTimeMillis()-lastMessage<750)return; lastMessage=System.currentTimeMillis();
    mc.inGameHud.addChatMessage("§e"+text);
  }
}
