package local.luke.power.worldedit;

import java.util.List;
import java.util.Set;
import local.luke.power.worldedit.core.*;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Box;

public final class NavigationActions {
  private static final Set<Integer> HAZARDS = Set.of(8, 9, 10, 11, 51, 81);
  private final Minecraft mc;
  private final Editor editor;
  private final Navigation navigation;

  public NavigationActions(Minecraft mc, Editor editor) {
    this.mc = mc;
    this.editor = editor;
    navigation =
        new Navigation(
            new Navigation.Terrain() {
              public boolean loaded(Pos p) {
                return mc.world.method_239(p.x(), p.y(), p.z());
              }

              public boolean clear(Pos p) {
                return bodyClear(p);
              }

              public boolean floor(Pos p) {
                int id = mc.world.getBlockId(p.x(), p.y() - 1, p.z());
                if (id <= 0 || HAZARDS.contains(id)) return false;
                Block b = Block.BLOCKS[id];
                Box box = b == null ? null : b.method_1624(mc.world, p.x(), p.y() - 1, p.z());
                return box != null && box.maxY <= p.y() + .001 && box.maxY >= p.y() - .001;
              }
            });
  }

  private boolean bodyClear(Pos p) {
    if (HAZARDS.contains(mc.world.getBlockId(p.x(), p.y(), p.z()))
        || HAZARDS.contains(mc.world.getBlockId(p.x(), p.y() + 1, p.z()))) return false;
    Box body =
        Box.create(p.x() + .2, p.y() + .001, p.z() + .2, p.x() + .8, p.y() + 1.8, p.z() + .8);
    return mc.world.method_190(mc.player, body).isEmpty();
  }

  public void run(String command, int amount, boolean forceFlight, boolean forceGlass) {
    if (mc.player.field_1595 != null || mc.player.field_1594 != null)
      throw new IllegalArgumentException("Dismount before teleporting.");
    if (forceFlight && forceGlass) throw new IllegalArgumentException("Use either -f or -g.");
    Pos from = feet();
    Pos to =
        switch (command) {
          case "up" -> navigation.up(from, amount);
          case "ascend", "asc" -> navigation.floor(from, 1, amount);
          case "descend", "desc" -> navigation.floor(from, -1, amount);
          case "ceil" -> navigation.ceiling(from, amount);
          case "unstuck", "!" -> navigation.unstuck(from);
          case "jumpto", "j" -> trace(false);
          case "thru" -> trace(true);
          default -> throw new IllegalArgumentException("Unknown navigation command.");
        };
    if (!navigation.clear(to)) throw new IllegalArgumentException("Destination is obstructed.");
    boolean platform = command.equals("up") || command.equals("ceil");
    boolean flight = platform && !forceGlass && CreativeAccess.flightAvailable(mc.player);
    if (forceFlight && !flight)
      throw new IllegalArgumentException("Creative flight is unavailable.");
    if (platform && !flight && !navigation.safe(to)) {
      Pos below = to.add(new Pos(0, -1, 0));
      BlockValue old = editor.engine.read(below);
      if (old.id != 0) throw new IllegalArgumentException("No empty space for a glass platform.");
      // A platform is an ordinary undoable edit. Teleport only after it was placed.
      editor.engine.submit(
          List.of(below), p -> new BlockValue(20, 0, null), () -> teleport(to, false));
    } else teleport(to, flight);
  }

  private Pos feet() {
    return new Pos(
        (int) Math.floor(mc.player.x),
        (int) Math.floor(mc.player.boundingBox.minY + .00001),
        (int) Math.floor(mc.player.z));
  }

  private Pos trace(boolean through) {
    double yaw = Math.toRadians(mc.player.yaw), pitch = Math.toRadians(mc.player.pitch);
    double dx = -Math.sin(yaw) * Math.cos(pitch),
        dy = -Math.sin(pitch),
        dz = Math.cos(yaw) * Math.cos(pitch);
    boolean hit = false;
    Pos last = null;
    for (int step = 1; step <= 1024; step++) {
      double d = step * .25;
      Pos p =
          new Pos(
              (int) Math.floor(mc.player.x + dx * d),
              (int) Math.floor(mc.player.y + dy * d),
              (int) Math.floor(mc.player.z + dz * d));
      if (p.equals(last)) continue;
      last = p;
      if (!p.valid() || !mc.world.method_239(p.x(), p.y(), p.z())) break;
      int id = mc.world.getBlockId(p.x(), p.y(), p.z());
      Block block = id == 0 ? null : Block.BLOCKS[id];
      boolean solid = block != null && block.method_1624(mc.world, p.x(), p.y(), p.z()) != null;
      if (solid) {
        hit = true;
        if (!through) {
          // Search upward only within this target column, without loading terrain.
          for (int y = p.y() + 1; y <= 126; y++) {
            Pos top = new Pos(p.x(), y, p.z());
            if (navigation.safe(top)) return top;
          }
          break;
        }
      } else if (through && hit) {
        // Use the first walkable space beyond the wall, up to four blocks below the ray.
        for (int y = p.y(); y >= Math.max(1, p.y() - 4); y--) {
          Pos dest = new Pos(p.x(), y, p.z());
          if (navigation.safe(dest)) return dest;
        }
      }
    }
    throw new IllegalArgumentException("No safe destination within 256 loaded blocks.");
  }

  private void teleport(Pos to, boolean flight) {
    if (!WorldEditor.available(mc) || !navigation.clear(to))
      throw new IllegalArgumentException(
          "Destination or player state changed; teleport cancelled.");
    if (flight && !CreativeAccess.fly(mc.player))
      throw new IllegalArgumentException("Could not enable creative flight.");
    mc.player.method_1341(to.x() + .5, to.y(), to.z() + .5, mc.player.yaw, mc.player.pitch);
    mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
    ((local.luke.power.worldedit.mixin.EntityAccess) mc.player).worldedit$fallDistance(0);
    editor.message.accept("Moved to " + to.x() + ", " + to.y() + ", " + to.z() + ".");
  }
}
